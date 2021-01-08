package io.bazel.kotlin.plugin.jdeps

import com.google.devtools.build.lib.view.proto.Deps
import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import io.bazel.kotlin.builder.utils.jars.JarOwner
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.ClassBuilderFactory
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.codegen.inline.RemappingClassBuilder
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.load.java.sources.JavaSourceElement
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryJavaClass
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.load.kotlin.VirtualFileKotlinClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperclassesWithoutAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.org.objectweb.asm.commons.Remapper
import java.io.BufferedOutputStream
import java.io.File
import java.nio.file.Paths

/**
 * Kotlin compiler extension that tracks classes (and corresponding classpath jars) needed to
 * compile current kotlin target. Tracked data should include all classes whose changes could
 * affect target's compilation out : direct class dependencies (i.e external classes directly
 * used), but also their superclass, interfaces, etc.
 * The primary use of this extension is to improve Kotlin module compilation avoidance in build
 * systems (like Buck).
 *
 * Tracking of classes is done with a Remapper, which exposes all object types used by the class
 * bytecode being generated. Tracking of the ancestor classes is done via modules and class
 * descriptors that got generated during analysis/resolve phase of Kotlin compilation.
 *
 * Note: annotation processors dependencies may need to be tracked separatly (and may not need
 * per-class ABI change tracking)
 *
 * @param project the current compilation project
 * @param configuration the current compilation configuration
 */
class JdepsGenExtension(
  val project: MockProject,
  val configuration: CompilerConfiguration) :
  ClassBuilderInterceptorExtension, AnalysisHandlerExtension {

  companion object {

    /**
     * Returns whether class is coming from JVM runtime env. There is no need to track these
     * classes.
     *
     * @param className the class name of the class
     * @return whether class is provided by jvm runtime or not
     */
    fun isJvmClass(className: ClassId): Boolean {
      return className.asString().startsWith("java") || className.asString().startsWith("kotlin")
    }

    /**
     * Returns the path of the jar archive file corresponding to the provided class descriptor.
     *
     * @classDescriptor the class descriptor, typically obtained from compilation analyze phase
     * @return the path corresponding to the JAR where this class was loaded from, or null.
     */
    fun getClassCanonicalPath(classDescriptor: ClassDescriptor): String? {
      return when (val sourceElement: SourceElement = classDescriptor.source
      ) {
        is JavaSourceElement ->
          if (sourceElement.javaElement is BinaryJavaClass) {
            (sourceElement.javaElement as BinaryJavaClass).virtualFile.canonicalPath
          } else {
            // Ignore Java source local to this module.
            null
          }
        is KotlinJvmBinarySourceElement ->
          (sourceElement.binaryClass as VirtualFileKotlinClass).file.canonicalPath
        else -> null
      }
    }
  }

  private val moduleClasses: HashSet<ClassId> = HashSet() // classes defined in current module
  private val moduleDepClasses: HashSet<ClassId> = HashSet() // external  classes used directly by module
  private val classesCanonicalPaths: HashSet<String> = HashSet()
  private var moduleDescriptor: ModuleDescriptor? = null

  override fun analysisCompleted(
    project: Project,
    module: ModuleDescriptor,
    bindingTrace: BindingTrace,
    files: Collection<KtFile>
  ): AnalysisResult? {
    // Hold module descriptor until end of class generation, seems safe to do so. Otherwise, we need
    // to extract list of used classes differently (i.e not relaying on ClassBuilderInterceptor).
    moduleDescriptor = module

    // Capture import dependencies
    files.forEach {file ->
      file.importDirectives.forEach {import ->
        import.importPath?.let {
          moduleDepClasses.add(ClassId.topLevel(it.fqName))
        }
      }
    }

    return super.analysisCompleted(project, module, bindingTrace, files)
  }

  override fun interceptClassBuilderFactory(
    interceptedFactory: ClassBuilderFactory,
    bindingContext: BindingContext,
    diagnostics: DiagnosticSink
  ): ClassBuilderFactory {
    return UsageTrackerClassBuilderFactory(interceptedFactory, bindingContext)
  }

  private fun onClassBuilderComplete() {
    assert(moduleDescriptor != null)

    moduleDepClasses.filter(::isExternalModuleClass).forEach {
      moduleDescriptor?.findClassAcrossModuleDependencies(it)?.let(::handleExternalModuleClass)
    }
    moduleDescriptor = null

    writeOutput()
  }

  private fun handleExternalModuleClass(classDescriptor: ClassDescriptor) {
    // Ignore internal or JVM classes
    val className = ClassId.topLevel(classDescriptor.fqNameSafe)
    if (isExternalModuleClass(className)) {
      // Ignore already visited classes
      val path: String = getClassCanonicalPath(classDescriptor) ?: return
      if (classesCanonicalPaths.contains(path)) return

      classesCanonicalPaths.add(path)

      // Iterate through parent classes and interfaces
      classDescriptor.getAllSuperclassesWithoutAny().forEach(::handleExternalModuleClass)
      classDescriptor.getSuperInterfaces().forEach(::handleExternalModuleClass)
    }
  }

  private fun writeOutput() {
    // Build mapping jar -> [used classes]
    val result: HashMap<String, ArrayList<String>> = HashMap()
    classesCanonicalPaths.forEach {
      val parts = it.split("!/")
      result.computeIfAbsent(parts[0]) { ArrayList() }.add(parts[1])
    }

    // Build and write out deps.proto
    val targetLabel = configuration.getNotNull(JdepsGenConfigurationKeys.TARGET_LABEL)
    val jdepsOutput = configuration.getNotNull(JdepsGenConfigurationKeys.OUTPUT_JDEPS)
    val directDeps = configuration.getList(JdepsGenConfigurationKeys.DIRECT_DEPENDENCIES)

    val rootBuilder = Deps.Dependencies.newBuilder()
    rootBuilder.success = true
    rootBuilder.ruleLabel = targetLabel

    val unusedDeps = directDeps.subtract(result.keys)
    unusedDeps.forEach { jarPath ->
      val dependency = Deps.Dependency.newBuilder()
      dependency.kind = Deps.Dependency.Kind.UNUSED
      dependency.path = jarPath
      rootBuilder.addDependency(dependency)
    }

    result.forEach { (jarPath, classes) ->
      val dependency = Deps.Dependency.newBuilder()
      dependency.kind = Deps.Dependency.Kind.EXPLICIT
      dependency.path = jarPath
      rootBuilder.addDependency(dependency)
    }

    BufferedOutputStream(File(jdepsOutput).outputStream()).use {
      it.write(rootBuilder.build().toByteArray())
    }

    when (configuration.getNotNull(JdepsGenConfigurationKeys.STRICT_KOTLIN_DEPS)) {
      "warn" -> checkStrictDeps(result, directDeps, targetLabel)
      "error" -> {
        if(checkStrictDeps(result, directDeps, targetLabel)) error("Strict Deps Violations - please fix")
      }
    }
  }

  /**
   * Prints strict deps warnings and returns true if violations were found.
   */
  private fun checkStrictDeps(result: HashMap<String, ArrayList<String>>, directDeps: List<String>, targetLabel: String): Boolean {
    val missingStrictDeps = result.keys
      .filter { !directDeps.contains(it) }
      .map { JarOwner.readJarOwnerFromManifest(Paths.get(it)).label }

    if (missingStrictDeps.isNotEmpty()) {
      val open = "\u001b[35m\u001b[1m"
      val close = "\u001b[0m"
      val command =
        """
              |$open ** Please add the following dependencies:$close ${missingStrictDeps.joinToString(" ")} to $targetLabel 
              |$open ** You can use the following buildozer command:$close buildozer 'add deps ${missingStrictDeps.joinToString(" ")}' $targetLabel
            """.trimMargin()

      println(command)
      return true
    }
    return false
  }

  private fun isExternalModuleClass(className: ClassId): Boolean {
    return !isModuleClass(className) && !isJvmClass(className)
  }

  private fun isModuleClass(className: ClassId): Boolean {
    return moduleClasses.contains(className)
  }

  /*
     * Class builder factory, wrapping usage of UsageTrackerRemappingClassBuilder
     */
  private inner class UsageTrackerClassBuilderFactory(
    private val delegateFactory: ClassBuilderFactory, val bindingContext: BindingContext
  ) : ClassBuilderFactory {

    override fun newClassBuilder(origin: JvmDeclarationOrigin): ClassBuilder {
      return UsageTrackerRemappingClassBuilder(delegateFactory.newClassBuilder(origin))
    }

    override fun getClassBuilderMode() = delegateFactory.classBuilderMode

    override fun asText(builder: ClassBuilder?): String? {
      return delegateFactory.asText((builder as UsageTrackerRemappingClassBuilder).builder)
    }

    override fun asBytes(builder: ClassBuilder?): ByteArray? {
      return delegateFactory.asBytes((builder as UsageTrackerRemappingClassBuilder).builder)
    }

    override fun close() {
      onClassBuilderComplete()

      delegateFactory.close()
    }
  }

  /**
   * Class builder relying on remapper capability to track all external classes used by the class
   * whose bytecode is being currently generated.
   */
  private inner class UsageTrackerRemappingClassBuilder(internal val builder: ClassBuilder) :
    RemappingClassBuilder(builder, UsageTrackerRemapper()) {
    override fun defineClass(
      origin: PsiElement?,
      version: Int,
      access: Int,
      name: String,
      signature: String?,
      superName: String,
      interfaces: Array<out String>
    ) {
      moduleClasses.add(ClassId.fromString(name))
      super.defineClass(origin, version, access, name, signature, superName, interfaces)
    }
  }

  /**
   * Remapper used to track all external classes usage.
   */
  private inner class UsageTrackerRemapper : Remapper() {
    override fun map(typeName: String?): String {
      typeName?.let { moduleDepClasses.add(ClassId.fromString(it)) }
      return super.map(typeName)
    }

    override fun mapFieldName(owner: String?, name: String?, desc: String?): String {
      owner?.let { moduleDepClasses.add(ClassId.fromString(it)) }
      return super.mapFieldName(owner, name, desc)
    }

    override fun mapMethodName(owner: String?, name: String?, desc: String?): String {
      owner?.let { moduleDepClasses.add(ClassId.fromString(it)) }
      return super.mapMethodName(owner, name, desc)
    }
  }
}

