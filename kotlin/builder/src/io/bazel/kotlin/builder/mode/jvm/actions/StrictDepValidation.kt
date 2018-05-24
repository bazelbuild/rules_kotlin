package io.bazel.kotlin.builder.mode.jvm.actions

import com.google.devtools.build.lib.view.proto.Deps
import com.google.inject.ImplementedBy
import io.bazel.kotlin.builder.utils.Console
import io.bazel.kotlin.model.KotlinModel
import java.nio.file.Files
import java.nio.file.Paths

@ImplementedBy(DefaultStrictDepsValidator::class)
interface StrictDepsValidator {
    fun validateDeps(command: KotlinModel.BuilderCommand, jdeps: Deps.Dependencies): Int
}


private class DefaultStrictDepsValidator: StrictDepsValidator {
    override fun validateDeps(command: KotlinModel.BuilderCommand, jdeps: Deps.Dependencies): Int {
        val usedIndirectDependencies = jdeps.dependencyList.asSequence()
                .filter { it.kind == Deps.Dependency.Kind.EXPLICIT }
                .mapNotNull { command.inputs.indirectDependenciesMap[it.path] }
                .toSet() // A target might be exporting multiple jars.

        if(usedIndirectDependencies.isNotEmpty()) {
            val msg = """error: Transitive dependencies are being used directly

  ${Console.purple("** Please add the following dependencies:")}
    ${usedIndirectDependencies.joinToString(" ")} to ${command.info.label}
  ${Console.purple("** You can use the following buildozer command:")}
    buildozer 'add deps ${usedIndirectDependencies.joinToString(" ")}' ${command.info.label}
"""
            println(msg)
            // TODO should the file be deleted here, a compile error should be enough ?
            Files.delete(Paths.get(command.outputs.output))
            return 1
        }
        return 0
    }
}