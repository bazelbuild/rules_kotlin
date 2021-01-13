package io.bazel.kotlin.plugin.jdeps

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension

class JdepsGenComponentRegistrar : ComponentRegistrar {

  override fun registerProjectComponents(
    project: MockProject,
    configuration: CompilerConfiguration
  ) {

    // Capture all types referenced by the compiler for this module and look up the jar from which
    // they were loaded from
    val extension = JdepsGenExtension(project, configuration)
    AnalysisHandlerExtension.registerExtension(project, extension)
    ClassBuilderInterceptorExtension.registerExtension(project, extension)
    StorageComponentContainerContributor.registerExtension(project, extension)
  }
}
