package plugin

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.ir.builders.declarations.addGetter
import org.jetbrains.kotlin.ir.builders.declarations.buildProperty
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.toIrConst
import org.jetbrains.kotlin.name.Name

@OptIn(ExperimentalCompilerApi::class)
class CompilerVersionPluginRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String
        get() = "example.compiler-version"

    override val supportsK2: Boolean
        get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        IrGenerationExtension.registerExtension(CompilerVersionGenerationExtension())
    }
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
private class CompilerVersionGenerationExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val targetFile = moduleFragment.files.minByOrNull { it.fileEntry.name } ?: return
        if (targetFile.declarations.filterIsInstance<IrProperty>().any { it.name.asString() == GENERATED_PROPERTY_NAME }) {
            return
        }

        targetFile.declarations += pluginContext.irFactory.buildProperty {
            name = Name.identifier(GENERATED_PROPERTY_NAME)
        }.apply {
            parent = targetFile
            addGetter {
                returnType = pluginContext.irBuiltIns.stringType
            }.apply {
                val builder = pluginContext.irBuiltIns.createIrBuilder(symbol)
                body = builder.irBlockBody {
                    +irReturn(KotlinCompilerVersion.VERSION.toIrConst(pluginContext.irBuiltIns.stringType))
                }
            }
        }
    }
}

private const val GENERATED_PROPERTY_NAME = "generatedCompilerVersion"
