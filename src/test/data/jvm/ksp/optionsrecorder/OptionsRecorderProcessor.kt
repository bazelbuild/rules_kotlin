package com.example.optionsrecorder

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated

/**
 * A minimal KSP processor that writes the options it received into a generated Kotlin file.
 * Used by integration tests to verify that options are passed through correctly.
 */
class OptionsRecorderProcessor(
    private val codeGenerator: CodeGenerator,
    private val options: Map<String, String>,
) : SymbolProcessor {
    private var done = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (done) return emptyList()
        done = true

        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(false),
            packageName = "generated",
            fileName = "RecordedOptions",
        )
        file.bufferedWriter().use { writer ->
            writer.write("package generated\n\n")
            writer.write("object RecordedOptions {\n")
            writer.write("    val options: Map<String, String> = mapOf(\n")
            options.forEach { (k, v) ->
                writer.write("        \"$k\" to \"$v\",\n")
            }
            writer.write("    )\n")
            writer.write("}\n")
        }
        return emptyList()
    }
}
