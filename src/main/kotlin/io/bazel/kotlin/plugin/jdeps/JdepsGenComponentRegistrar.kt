package io.bazel.kotlin.plugin.jdeps

import com.google.devtools.build.lib.view.proto.Deps
import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import java.io.BufferedOutputStream
import java.io.File

class JdepsGenComponentRegistrar : ComponentRegistrar {

  override fun registerProjectComponents(
    project: MockProject,
    configuration: CompilerConfiguration
  ) {

    // Capture all types referenced by the compiler for this module and look up the jar from which
    // they were loaded from
    AnalysisHandlerExtension.registerExtension(
      project, JdepsAnalysisHandlerExtension()
    )

    // Build and write out deps.proto
    val targetLabel = configuration.getNotNull(JdepsGenConfigurationKeys.TARGET_LABEL)
    val jdepsOutput = configuration.getNotNull(JdepsGenConfigurationKeys.OUTPUT_JDEPS)

    val rootBuilder = Deps.Dependencies.newBuilder()
    rootBuilder.success = true
    rootBuilder.ruleLabel = targetLabel

    // TODO: For every jar from which a type was referenced add it here.
    val dependency = Deps.Dependency.newBuilder()
    dependency.kind = Deps.Dependency.Kind.EXPLICIT
    dependency.path = "/path/to/dependency.jar"
    rootBuilder.addDependency(dependency)

    BufferedOutputStream(File(jdepsOutput).outputStream()).use {
      it.write(rootBuilder.build().toByteArray())
    }
  }

  internal class JdepsAnalysisHandlerExtension : AnalysisHandlerExtension {

    override fun analysisCompleted(project: Project, module: ModuleDescriptor, bindingTrace: BindingTrace, files: Collection<KtFile>): AnalysisResult? {
      // TODO: Capture all referenced non-local types, e.g: supertypes, generics, return types etc
      files.forEach {file ->
        file.declarations.forEach { ktDeclaration ->
          when (ktDeclaration) {
            is KtClassOrObject -> {

            }
            is KtFunction -> {
            }
            is KtProperty -> {
              val variable = bindingTrace.bindingContext.get(BindingContext.VARIABLE, ktDeclaration);
              val type = variable?.type
              val declarationDescriptor = type?.constructor?.declarationDescriptor
              when(declarationDescriptor) {
                is ClassDescriptor -> {
                  println("ClassId.isLocal: ${declarationDescriptor.classId?.isLocal}")
                  println("Declaration Descriptor ${declarationDescriptor.fqNameSafe}")
                }
                else -> null
              }
            }
            else -> error("Unsupported declaration $ktDeclaration")          }
        }
      }

      return super.analysisCompleted(project, module, bindingTrace, files)
    }
  }
}
