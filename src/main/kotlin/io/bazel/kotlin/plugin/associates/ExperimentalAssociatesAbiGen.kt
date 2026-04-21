/*
 * Copyright 2024 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package io.bazel.kotlin.plugin.associates

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.jvm.abi.JvmAbiComponentRegistrar
import org.jetbrains.kotlin.jvm.abi.JvmAbiConfigurationKeys

@OptIn(ExperimentalCompilerApi::class)
class ExperimentalAssociatesAbiGenCommandLineProcessor : CommandLineProcessor {
  companion object {
    const val COMPILER_PLUGIN_ID =
      "io.bazel.kotlin.plugin.ExperimentalAssociatesAbiGen"

    val OUTPUT_DIR =
      CompilerConfigurationKey<String>(
        "associates abi output directory",
      )
    val REMOVE_DEBUG_INFO =
      CompilerConfigurationKey<Boolean>(
        "associates abi remove debug info",
      )
    val PRESERVE_DECLARATION_ORDER =
      CompilerConfigurationKey<Boolean>(
        "associates abi preserve declaration order",
      )
    val REMOVE_DATA_CLASS_COPY_IF_CONSTRUCTOR_IS_PRIVATE =
      CompilerConfigurationKey<Boolean>(
        "associates abi remove data class copy if constructor is private",
      )

    val OUTPUT_DIR_OPTION =
      CliOption(
        "outputDir",
        "<path>",
        "Output directory for associates ABI classes",
        required = true,
      )
    val REMOVE_DEBUG_INFO_OPTION =
      CliOption(
        "removeDebugInfo",
        "<boolean>",
        "Remove debug info",
        required = false,
      )
    val PRESERVE_DECLARATION_ORDER_OPTION =
      CliOption(
        "preserveDeclarationOrder",
        "<boolean>",
        "Preserve declaration order",
        required = false,
      )
    val REMOVE_DATA_CLASS_COPY_IF_CONSTRUCTOR_IS_PRIVATE_OPTION =
      CliOption(
        "removeDataClassCopyIfConstructorIsPrivate",
        "<boolean>",
        "Remove data class copy if constructor is private",
        required = false,
      )
  }

  override val pluginId: String = COMPILER_PLUGIN_ID

  override val pluginOptions: Collection<AbstractCliOption> =
    listOf(
      OUTPUT_DIR_OPTION,
      REMOVE_DEBUG_INFO_OPTION,
      PRESERVE_DECLARATION_ORDER_OPTION,
      REMOVE_DATA_CLASS_COPY_IF_CONSTRUCTOR_IS_PRIVATE_OPTION,
    )

  override fun processOption(
    option: AbstractCliOption,
    value: String,
    configuration: CompilerConfiguration,
  ) {
    when (option) {
      OUTPUT_DIR_OPTION ->
        configuration.put(OUTPUT_DIR, value)
      REMOVE_DEBUG_INFO_OPTION ->
        configuration.put(REMOVE_DEBUG_INFO, value == "true")
      PRESERVE_DECLARATION_ORDER_OPTION ->
        configuration.put(
          PRESERVE_DECLARATION_ORDER,
          value == "true",
        )
      REMOVE_DATA_CLASS_COPY_IF_CONSTRUCTOR_IS_PRIVATE_OPTION ->
        configuration.put(
          REMOVE_DATA_CLASS_COPY_IF_CONSTRUCTOR_IS_PRIVATE,
          value == "true",
        )
      else ->
        throw CliOptionProcessingException(
          "Unknown option: ${option.optionName}",
        )
    }
  }
}

@OptIn(ExperimentalCompilerApi::class)
class ExperimentalAssociatesAbiGenRegistrar : CompilerPluginRegistrar() {
  override val pluginId: String =
    ExperimentalAssociatesAbiGenCommandLineProcessor.COMPILER_PLUGIN_ID

  override val supportsK2: Boolean = true

  override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    val outputDir =
      configuration.get(
        ExperimentalAssociatesAbiGenCommandLineProcessor.OUTPUT_DIR,
      ) ?: return

    // Build a configuration with the keys
    // JvmAbiComponentRegistrar expects, hardcoding
    // removePrivateClasses=true and treatInternalAsPrivate=false
    // so that the associates ABI jar preserves internal visibility.
    val abiConfig = CompilerConfiguration()
    abiConfig.put(
      JvmAbiConfigurationKeys.OUTPUT_PATH,
      outputDir,
    )
    abiConfig.put(
      JvmAbiConfigurationKeys.REMOVE_PRIVATE_CLASSES,
      true,
    )
    abiConfig.put(
      JvmAbiConfigurationKeys.TREAT_INTERNAL_AS_PRIVATE,
      false,
    )
    abiConfig.put(
      JvmAbiConfigurationKeys.REMOVE_DEBUG_INFO,
      configuration.get(
        ExperimentalAssociatesAbiGenCommandLineProcessor
          .REMOVE_DEBUG_INFO,
      ) ?: false,
    )
    abiConfig.put(
      JvmAbiConfigurationKeys.PRESERVE_DECLARATION_ORDER,
      configuration.get(
        ExperimentalAssociatesAbiGenCommandLineProcessor
          .PRESERVE_DECLARATION_ORDER,
      ) ?: false,
    )
    abiConfig.put(
      JvmAbiConfigurationKeys
        .REMOVE_DATA_CLASS_COPY_IF_CONSTRUCTOR_IS_PRIVATE,
      configuration.get(
        ExperimentalAssociatesAbiGenCommandLineProcessor
          .REMOVE_DATA_CLASS_COPY_IF_CONSTRUCTOR_IS_PRIVATE,
      ) ?: false,
    )

    with(JvmAbiComponentRegistrar()) {
      registerExtensions(abiConfig)
    }
  }
}
