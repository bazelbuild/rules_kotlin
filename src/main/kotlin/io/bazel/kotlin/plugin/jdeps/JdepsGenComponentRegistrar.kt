package io.bazel.kotlin.plugin.jdeps

import com.google.devtools.build.lib.view.proto.Deps
import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
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
    val extension = JdepsGenExtension(project, configuration)
    AnalysisHandlerExtension.registerExtension(project, extension)
    ClassBuilderInterceptorExtension.registerExtension(project, extension)
  }
}
