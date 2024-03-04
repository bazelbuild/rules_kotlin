package io.bazel.kotlin.plugin.jdeps

import com.google.devtools.build.lib.view.proto.Deps
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import io.bazel.kotlin.builder.utils.jars.JarOwner
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassConstructorDescriptor
import org.jetbrains.kotlin.load.java.sources.JavaSourceElement
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryJavaClass
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryJavaField
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.load.kotlin.VirtualFileKotlinClass
import org.jetbrains.kotlin.load.kotlin.getContainingKotlinJvmBinaryClass
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.ImportedFromObjectCallableDescriptor
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ExpressionKotlinCallArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.tower.NewAbstractResolvedCall
import org.jetbrains.kotlin.resolve.calls.tower.PSIKotlinCallArgument
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.getImportableDescriptor
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedTypeAliasDescriptor
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.getAbbreviation
import org.jetbrains.kotlin.types.typeUtil.supertypes
import java.io.BufferedOutputStream
import java.io.File
import java.nio.file.Paths

/**
 * Kotlin compiler extension that tracks classes (and corresponding classpath jars) needed to
 * compile current kotlin target. Tracked data should include all classes whose changes could
 * affect target's compilation out : direct class dependencies (i.e. external classes directly
 * used), but also their superclass, interfaces, etc.
 * The primary use of this extension is to improve Kotlin module compilation avoidance in build
 * systems (like Buck).
 *
 * Tracking of classes and their ancestors is done via modules and class
 * descriptors that got generated during analysis/resolve phase of Kotlin compilation.
 *
 * Note: annotation processors dependencies may need to be tracked separately (and may not need
 * per-class ABI change tracking)
 *
 * @param project the current compilation project
 * @param configuration the current compilation configuration
 */
class JdepsGenExtension(
  val configuration: CompilerConfiguration,
) :
  AnalysisHandlerExtension, StorageComponentContainerContributor {

  companion object {

    /**
     * Returns the path of the jar archive file corresponding to the provided descriptor.
     *
     * @descriptor the descriptor, typically obtained from compilation analyze phase
     * @return the path corresponding to the JAR where this class was loaded from, or null.
     */
    fun getClassCanonicalPath(descriptor: DeclarationDescriptorWithSource): String? {
      // Get the descriptor's source element which may be a type alias
      val sourceElement = if (descriptor.source != SourceElement.NO_SOURCE) {
        descriptor.source
      } else {
        when (val declarationDescriptor = descriptor.getImportableDescriptor()) {
          is DeserializedTypeAliasDescriptor -> {
            declarationDescriptor.containerSource
          }

          else -> {
            null
          }
        }
      }

      return when (sourceElement) {
        is JavaSourceElement ->
          if (sourceElement.javaElement is BinaryJavaClass) {
            (sourceElement.javaElement as BinaryJavaClass).virtualFile.canonicalPath
          } else if (sourceElement.javaElement is BinaryJavaField) {
            val containingClass = (sourceElement.javaElement as BinaryJavaField).containingClass
            if (containingClass is BinaryJavaClass) {
              containingClass.virtualFile.canonicalPath
            } else {
              null
            }
          } else {
            // Ignore Java source local to this module.
            null
          }

        is KotlinJvmBinarySourceElement ->
          (sourceElement.binaryClass as VirtualFileKotlinClass).file.canonicalPath

        is JvmPackagePartSource -> { // This case is needed to collect type aliases
          ((sourceElement.knownJvmBinaryClass) as VirtualFileKotlinClass).file.canonicalPath
        }

        else -> {
          null
        }
      }
    }

    private fun getClassCanonicalPath(typeConstructor: TypeConstructor): String? {
      return (typeConstructor.declarationDescriptor as? DeclarationDescriptorWithSource)?.let {
        getClassCanonicalPath(
          it,
        )
      }
    }
  }

  private val explicitClassesCanonicalPaths = mutableSetOf<String>()
  private val implicitClassesCanonicalPaths = mutableSetOf<String>()

  override fun registerModuleComponents(
    container: StorageComponentContainer,
    platform: TargetPlatform,
    moduleDescriptor: ModuleDescriptor,
  ) {
    container.useInstance(
      ClasspathCollectingChecker(explicitClassesCanonicalPaths, implicitClassesCanonicalPaths),
    )
  }

  class ClasspathCollectingChecker(
    private val explicitClassesCanonicalPaths: MutableSet<String>,
    private val implicitClassesCanonicalPaths: MutableSet<String>,
  ) : CallChecker, DeclarationChecker {

    override fun check(
      resolvedCall: ResolvedCall<*>,
      reportOn: PsiElement,
      context: CallCheckerContext,
    ) {
      // First collect type from the Resolved Call
      collectExplicitTypes(resolvedCall)

      resolvedCall.valueArguments.keys.forEach { valueArgument ->
        collectTypeReferences(valueArgument.type, isExplicit = false)
      }

      // And then collect types from any ResultingDescriptor
      val resultingDescriptor = resolvedCall.resultingDescriptor
      collectExplicitTypes(resultingDescriptor)

      val isExplicitReturnType = resultingDescriptor is JavaClassConstructorDescriptor
      resultingDescriptor.returnType?.let {
        collectTypeReferences(it, isExplicit = isExplicitReturnType, collectTypeArguments = false)
      }

      resultingDescriptor.valueParameters.forEach { valueParameter ->
        collectTypeReferences(valueParameter.type, isExplicit = false)
      }

      // Finally, collect types that depend on the type of the ResultingDescriptor and note that
      // these descriptors may be composed of multiple classes that we need to extract types from
      if (resultingDescriptor is DeclarationDescriptor) {
        val containingDeclaration = resultingDescriptor.containingDeclaration
        if (containingDeclaration is ClassDescriptor) {
          collectTypeReferences(containingDeclaration.defaultType)
        }

        if (resultingDescriptor is PropertyDescriptor) {
          (
            resultingDescriptor.getter
              ?.correspondingProperty as? SyntheticJavaPropertyDescriptor
            )
            ?.let { syntheticJavaPropertyDescriptor ->
              collectTypeReferences(syntheticJavaPropertyDescriptor.type, isExplicit = false)

              val functionDescriptor = syntheticJavaPropertyDescriptor.getMethod
              functionDescriptor.dispatchReceiverParameter?.type?.let { dispatchReceiverType ->
                collectTypeReferences(dispatchReceiverType, isExplicit = false)
              }
            }
        }
      }

      if (resultingDescriptor is ImportedFromObjectCallableDescriptor<*>) {
        collectTypeReferences(resultingDescriptor.containingObject.defaultType, isExplicit = false)
      }

      if (resultingDescriptor is ValueDescriptor) {
        collectTypeReferences(resultingDescriptor.type, isExplicit = false)
      }
    }

    private fun collectExplicitTypes(resultingDescriptor: CallableDescriptor) {
      val kotlinJvmBinaryClass = resultingDescriptor.getContainingKotlinJvmBinaryClass()
      if (kotlinJvmBinaryClass is VirtualFileKotlinClass) {
        explicitClassesCanonicalPaths.add(kotlinJvmBinaryClass.file.path)
      }
    }

    private fun collectExplicitTypes(resolvedCall: ResolvedCall<*>) {
      val kotlinCallArguments = (resolvedCall as? NewAbstractResolvedCall)
        ?.resolvedCallAtom?.atom?.argumentsInParenthesis

      // note that callArgument can be both a PSIKotlinCallArgument and an ExpressionKotlinCallArgument
      kotlinCallArguments?.forEach { callArgument ->
        if (callArgument is PSIKotlinCallArgument) {
          val dataFlowInfos = listOf(callArgument.dataFlowInfoBeforeThisArgument) +
            callArgument.dataFlowInfoAfterThisArgument

          dataFlowInfos.forEach { dataFlowInfo ->
            dataFlowInfo.completeTypeInfo.values().flatten().forEach { kotlinType ->
              collectTypeReferences(kotlinType, isExplicit = true)
            }
          }
        }

        if (callArgument is ExpressionKotlinCallArgument) {
          callArgument.receiver.allOriginalTypes.forEach { kotlinType ->
            collectTypeReferences(kotlinType, isExplicit = true)
          }
        }
      }
    }

    override fun check(
      declaration: KtDeclaration,
      descriptor: DeclarationDescriptor,
      context: DeclarationCheckerContext,
    ) {
      when (descriptor) {
        is ClassDescriptor -> {
          descriptor.typeConstructor.supertypes.forEach {
            collectTypeReferences(it)
          }
        }

        is FunctionDescriptor -> {
          descriptor.returnType?.let { collectTypeReferences(it, collectTypeArguments = false) }
          descriptor.valueParameters.forEach { valueParameter ->
            collectTypeReferences(valueParameter.type)
          }
          descriptor.annotations.forEach { annotation ->
            collectTypeReferences(annotation.type)
          }
          descriptor.extensionReceiverParameter?.value?.type?.let {
            collectTypeReferences(it)
          }
        }

        is PropertyDescriptor -> {
          collectTypeReferences(descriptor.type)
          descriptor.annotations.forEach { annotation ->
            collectTypeReferences(annotation.type)
          }
          descriptor.backingField?.annotations?.forEach { annotation ->
            collectTypeReferences(annotation.type)
          }
        }

        is LocalVariableDescriptor -> {
          collectTypeReferences(descriptor.type)
        }
      }
    }

    /**
     * Records direct and indirect references for a given type. Direct references are explicitly
     * used in the code, e.g: a type declaration or a generic type declaration. Indirect references
     * are other types required for compilation such as supertypes and interfaces of those explicit
     * types.
     */
    private fun collectTypeReferences(
      kotlinType: KotlinType,
      isExplicit: Boolean = true,
      collectTypeArguments: Boolean = true,
      visitedKotlinTypes: MutableSet<Pair<KotlinType, Boolean>> = mutableSetOf(),
    ) {
      val kotlintTypeAndIsExplicit = Pair(kotlinType, isExplicit)
      if (!visitedKotlinTypes.contains(kotlintTypeAndIsExplicit)) {
        visitedKotlinTypes.add(kotlintTypeAndIsExplicit)

        if (isExplicit) {
          getClassCanonicalPath(kotlinType.constructor)?.let {
            explicitClassesCanonicalPaths.add(it)
          }
        } else {
          getClassCanonicalPath(kotlinType.constructor)?.let {
            implicitClassesCanonicalPaths.add(it)
          }
        }

        // Collect type aliases aka abbreviations
        // See: https://github.com/Kotlin/KEEP/blob/master/proposals/type-aliases.md#type-alias-expansion
        kotlinType.getAbbreviation()?.let { abbreviationType ->
          collectTypeReferences(
            abbreviationType,
            isExplicit = isExplicit,
            collectTypeArguments = collectTypeArguments,
            visitedKotlinTypes = visitedKotlinTypes,
          )
        }

        kotlinType.supertypes().forEach { supertype ->
          collectTypeReferences(
            supertype,
            isExplicit = false,
            collectTypeArguments = collectTypeArguments,
            visitedKotlinTypes,
          )
        }

        if (collectTypeArguments) {
          kotlinType.arguments.map { it.type }.forEach { typeArgument ->
            collectTypeReferences(
              typeArgument,
              isExplicit = isExplicit,
              collectTypeArguments = true,
              visitedKotlinTypes = visitedKotlinTypes,
            )
          }
        }
      }
    }
  }

  override fun analysisCompleted(
    project: Project,
    module: ModuleDescriptor,
    bindingTrace: BindingTrace,
    files: Collection<KtFile>,
  ): AnalysisResult? {
    val directDeps = configuration.getList(JdepsGenConfigurationKeys.DIRECT_DEPENDENCIES)
    val targetLabel = configuration.getNotNull(JdepsGenConfigurationKeys.TARGET_LABEL)
    val explicitDeps = createDepsMap(explicitClassesCanonicalPaths)

    doWriteJdeps(directDeps, targetLabel, explicitDeps)

    doStrictDeps(configuration, targetLabel, directDeps, explicitDeps)

    return super.analysisCompleted(project, module, bindingTrace, files)
  }

  /**
   * Returns a map of jars to classes loaded from those jars.
   */
  private fun createDepsMap(classes: Set<String>): Map<String, List<String>> {
    val jarsToClasses = mutableMapOf<String, MutableList<String>>()
    classes.forEach {
      val parts = it.split("!/")
      val jarPath = parts[0]
      if (jarPath.endsWith(".jar")) {
        jarsToClasses.computeIfAbsent(jarPath) { ArrayList() }.add(parts[1])
      }
    }
    return jarsToClasses
  }

  private fun doWriteJdeps(
    directDeps: MutableList<String>,
    targetLabel: String,
    explicitDeps: Map<String, List<String>>,
  ) {
    val implicitDeps = createDepsMap(implicitClassesCanonicalPaths)

    // Build and write out deps.proto
    val jdepsOutput = configuration.getNotNull(JdepsGenConfigurationKeys.OUTPUT_JDEPS)

    val rootBuilder = Deps.Dependencies.newBuilder()
    rootBuilder.success = true
    rootBuilder.ruleLabel = targetLabel

    val unusedDeps = directDeps.subtract(explicitDeps.keys)
    unusedDeps.forEach { jarPath ->
      val dependency = Deps.Dependency.newBuilder()
      dependency.kind = Deps.Dependency.Kind.UNUSED
      dependency.path = jarPath
      rootBuilder.addDependency(dependency)
    }

    explicitDeps.forEach { (jarPath, _) ->
      val dependency = Deps.Dependency.newBuilder()
      dependency.kind = Deps.Dependency.Kind.EXPLICIT
      dependency.path = jarPath
      rootBuilder.addDependency(dependency)
    }

    implicitDeps.keys.subtract(explicitDeps.keys).forEach {
      val dependency = Deps.Dependency.newBuilder()
      dependency.kind = Deps.Dependency.Kind.IMPLICIT
      dependency.path = it
      rootBuilder.addDependency(dependency)
    }

    BufferedOutputStream(File(jdepsOutput).outputStream()).use {
      it.write(rootBuilder.build().toByteArray())
    }
  }

  private fun doStrictDeps(
    compilerConfiguration: CompilerConfiguration,
    targetLabel: String,
    directDeps: MutableList<String>,
    explicitDeps: Map<String, List<String>>,
  ) {
    when (compilerConfiguration.getNotNull(JdepsGenConfigurationKeys.STRICT_KOTLIN_DEPS)) {
      "warn" -> checkStrictDeps(explicitDeps, directDeps, targetLabel)
      "error" -> {
        if (checkStrictDeps(explicitDeps, directDeps, targetLabel)) {
          error(
            "Strict Deps Violations - please fix",
          )
        }
      }
    }
  }

  /**
   * Prints strict deps warnings and returns true if violations were found.
   */
  private fun checkStrictDeps(
    result: Map<String, List<String>>,
    directDeps: List<String>,
    targetLabel: String,
  ): Boolean {
    val missingStrictDeps = result.keys
      .filter { !directDeps.contains(it) }
      .map { JarOwner.readJarOwnerFromManifest(Paths.get(it)) }

    if (missingStrictDeps.isNotEmpty()) {
      val missingStrictLabels = missingStrictDeps.mapNotNull { it.label }

      val open = "\u001b[35m\u001b[1m"
      val close = "\u001b[0m"

      var command =
        """
        $open ** Please add the following dependencies:$close
        ${
          missingStrictDeps.map { it.label ?: it.jar }.joinToString(" ")
        } to $targetLabel
        """

      if (missingStrictLabels.isNotEmpty()) {
        command += """$open ** You can use the following buildozer command:$close
        buildozer 'add deps ${
          missingStrictLabels.joinToString(" ")
        }' $targetLabel
        """
      }

      println(command.trimIndent())
      return true
    }
    return false
  }
}
