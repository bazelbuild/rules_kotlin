package io.bazel.kotlin.builder.mode.jvm.actions

import com.google.devtools.build.lib.view.proto.Deps
import io.bazel.kotlin.builder.BuildAction
import io.bazel.kotlin.builder.Context
import io.bazel.kotlin.builder.KotlinToolchain
import io.bazel.kotlin.builder.model.CompileDependencies
import io.bazel.kotlin.builder.model.Metas
import io.bazel.kotlin.builder.utils.Console
import java.nio.file.Files
import java.nio.file.Paths

class StrictDepValidation(toolchain: KotlinToolchain): BuildAction("validate deps", toolchain) {
    override fun invoke(ctx: Context): Int {
        val jdeps = Metas.JDEPS[ctx]
        val dependencies = CompileDependencies[ctx]

        val usedIndirectDependencies = jdeps.dependencyList.asSequence()
                .filter { it.kind == Deps.Dependency.Kind.EXPLICIT }
                .map { dependencies.indirectDependencies[it.path] }
                .filterNotNull()
                .toSet() // A target might be exporting multiple jars.

        if(usedIndirectDependencies.isNotEmpty()) {
            val msg = """error: Transitive dependencies are being used directly

  ${Console.purple("** Please add the following dependencies:")}
    ${usedIndirectDependencies.joinToString(" ")} to ${ctx.flags.label}
  ${Console.purple("** You can use the following buildozer command:")}
    buildozer 'add deps ${usedIndirectDependencies.joinToString(" ")}' ${ctx.flags.label}
"""
            println(msg)
            Files.delete(Paths.get(ctx.flags.outputClassJar))
            return 1
        }
        return 0
    }
}