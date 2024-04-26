package io.bazel.kotlin.plugin.jdeps.k2

import io.bazel.kotlin.plugin.jdeps.BaseJdepsGenExtension
import org.jetbrains.kotlin.codegen.ClassFileFactory
import org.jetbrains.kotlin.codegen.extensions.ClassFileFactoryFinalizerExtension
import org.jetbrains.kotlin.config.CompilerConfiguration

internal class JdepsGenExtension2(
  configuration: CompilerConfiguration,
) : BaseJdepsGenExtension(configuration), ClassFileFactoryFinalizerExtension {
  override fun finalizeClassFactory(factory: ClassFileFactory) {
    onAnalysisCompleted(
      ClassUsageRecorder.getExplicitClassesCanonicalPaths(),
      ClassUsageRecorder.getImplicitClassesCanonicalPaths(),
    )
  }
}
