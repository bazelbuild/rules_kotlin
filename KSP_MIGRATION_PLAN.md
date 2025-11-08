# KSP Migration Plan: Moving to Generic Annotation Processing Infrastructure

**Status:** Planning Phase
**Created:** 2025-11-08
**Target Completion:** 8 weeks from start

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Background](#background)
3. [Current State Analysis](#current-state-analysis)
4. [Comparison with KAPT Migration](#comparison-with-kapt-migration)
5. [Migration Phases](#migration-phases)
6. [Technical Challenges](#technical-challenges)
7. [Testing Strategy](#testing-strategy)
8. [Success Criteria](#success-criteria)
9. [Rollback Plan](#rollback-plan)
10. [Open Questions](#open-questions)

---

## Executive Summary

This document outlines a plan to migrate KSP (Kotlin Symbol Processing) to use the same generic annotation processing infrastructure that was recently implemented for KAPT. The migration will happen in three phases over approximately 8 weeks, ensuring backward compatibility and thorough testing at each stage.

**Key Goals:**
- Remove hardcoded KSP logic from the builder layer
- Unify KAPT and KSP under generic annotation processing framework
- Make KSP plugin resolution happen in Starlark layer
- Maintain full backward compatibility during migration

**Key Challenges:**
- KSP uses TWO plugin jars (API + CommandLine) instead of one
- KSP requires version override for Kotlin 2.0+ compatibility
- KSP has custom `KspPluginInfo` provider with special behavior
- More complex configuration than KAPT (10+ options)

---

## Background

### Why Migrate KSP?

After completing the KAPT migration, we identified that KSP follows identical patterns to what KAPT had before its migration:
- Hardcoded plugin references in `InternalCompilerPlugins`
- Dedicated execution functions (`runKspPlugin`)
- Plugin-specific configuration building
- Special-case handling in compilation flow

The recent KAPT migration demonstrated that a generic annotation processing framework provides:
- **Cleaner architecture**: No special cases for specific processors
- **Better separation of concerns**: Config built in Starlark, execution in builder
- **More flexibility**: Users can customize processor behavior per target
- **Easier maintenance**: Single code path for all annotation processors

### KAPT Migration Context

The KAPT migration was completed in three phases:
1. **Phase 1**: Added generic `AnnotationProcessingConfig` protobuf message
2. **Phase 2**: Removed KAPT from `InternalCompilerPlugins` and moved to Starlark
3. **Phase 3**: Deleted all legacy KAPT-specific code

This KSP migration will follow a similar pattern, with adjustments for KSP's unique characteristics.

---

## Current State Analysis

### 1. Hardcoded References in Builder Layer

#### InternalCompilerPlugins
**Location:** `src/main/kotlin/io/bazel/kotlin/builder/tasks/jvm/InternalCompilerPlugins.kt`

```kotlin
class InternalCompilerPlugins constructor(
  val jvmAbiGen: KotlinToolchain.CompilerPlugin,
  val skipCodeGen: KotlinToolchain.CompilerPlugin,
  val jdeps: KotlinToolchain.CompilerPlugin,
  val kspSymbolProcessingApi: KotlinToolchain.CompilerPlugin,      // ❌ Hardcoded
  val kspSymbolProcessingCommandLine: KotlinToolchain.CompilerPlugin, // ❌ Hardcoded
)
```

**Issue:** KSP plugins are treated as "internal" when they should be external, user-configurable.

#### KotlinToolchain
**Location:** `src/main/kotlin/io/bazel/kotlin/builder/toolchain/KotlinToolchain.kt`

```kotlin
// Lines 78-90: Hardcoded KSP plugin paths
private val KSP_SYMBOL_PROCESSING_API by lazy {
  BazelRunFiles.resolveVerifiedFromProperty(
    "@com_github_google_ksp...symbol-processing-api",
  ).toPath()
}

private val KSP_SYMBOL_PROCESSING_CMDLINE by lazy {
  BazelRunFiles.resolveVerifiedFromProperty(
    "@com_github_google_ksp...symbol-processing-cmdline",
  ).toPath()
}

// Lines 204-213: Added to toolchain
kspSymbolProcessingApi = CompilerPlugin(
  kspSymbolProcessingApi.absolutePath,
  "com.google.devtools.ksp.symbol-processing",
),
kspSymbolProcessingCommandLine = CompilerPlugin(
  kspSymbolProcessingCommandLine.absolutePath,
  "com.google.devtools.ksp.symbol-processing",
)
```

**Issue:** Plugin jars are resolved in Java code rather than Starlark layer.

### 2. Dedicated KSP Functions

#### CompilationTask.kt
**Location:** `src/main/kotlin/io/bazel/kotlin/builder/tasks/jvm/CompilationTask.kt`

```kotlin
// Lines 142-185: KSP-specific args builder
internal fun JvmCompilationTask.kspArgs(plugins: InternalCompilerPlugins): CompilationArgs {
  return CompilationArgs().apply {
    xFlag("plugin", plugins.kspSymbolProcessingApi.jarPath)
    xFlag("plugin", plugins.kspSymbolProcessingCommandLine.jarPath)

    plugin("com.google.devtools.ksp.symbol-processing") {
      // ~40 lines of KSP-specific configuration
      flag("-Xallow-no-source-files")
      flag("apclasspath", inputs.processorpathsList.joinToString(File.pathSeparator))
      flag("projectBaseDir", directories.incrementalData)
      flag("classOutputDir", directories.generatedClasses)
      flag("javaOutputDir", directories.generatedJavaSources)
      flag("kotlinOutputDir", directories.generatedSources)
      flag("resourceOutputDir", directories.generatedSources)
      flag("kspOutputDir", directories.incrementalData)
      flag("cachesDir", directories.incrementalData)
      flag("withCompilation", "false")
      flag("incremental", "false")
      flag("returnOkOnError", "false")
    }
  }
}

// Lines 373-408: Dedicated KSP plugin runner
private fun JvmCompilationTask.runKspPlugin(
  context: CompilationTaskContext,
  plugins: InternalCompilerPlugins,
  compiler: KotlinToolchain.KotlincInvoker
): JvmCompilationTask {
  val processorNames = inputs.processorsList.joinToString(", ")

  return context.execute("Ksp ($processorNames)") {
    val kspArgs = kspArgs(plugins)
    val args = baseArgs()
      .plus(plugins(options = emptyList(), classpath = emptyList()))
      .plus(kspArgs)
      .flag("-d", directories.generatedClasses)
      .values(inputs.kotlinSourcesList)
      .values(inputs.javaSourcesList)
      .list()

    context.executeCompilerTask(
      args,
      compiler::compile,
      printOnSuccess = context.whenTracing { true } == true,
    ).let { outputLines ->
      context.whenTracing {
        context.printCompilerOutput(listOf("ksp output:") + outputLines)
      }
      return@let expandWithGeneratedSources()
    }
  }
}

// Lines 365-371: KSP-specific dispatch in runPlugins()
if (!outputs.generatedKspSrcJar.isNullOrEmpty()) {
  return runKspPlugin(context, plugins, compiler)
}

// Lines 410-413: Version compatibility hack
private fun kspKotlinToolchainVersion(version: String): String {
  return if (version.toFloat() >= 2.0) "1.9" else version
}
```

**Pattern:** Same as KAPT before migration - dedicated args builder, dedicated runner, special dispatch.

### 3. Starlark Layer

#### Dedicated KSP Builder Action
**Location:** `kotlin/internal/jvm/compile.bzl`

```starlark
# Lines 406-443
def _run_ksp_builder_actions(
    ctx,
    rule_kind,
    toolchains,
    srcs,
    compile_deps,
    deps_artifacts,
    annotation_processors,  # Actually ksp_annotation_processors
    transitive_runtime_jars,
    plugins
):
    """Runs KSP (Kotlin Symbol Processing) builder action."""

    ksp_generated_java_srcjar = ctx.actions.declare_file(
        "%s-ksp-generated-src.jar" % ctx.label.name,
    )
    ksp_generated_classes_jar = ctx.actions.declare_file(
        "%s-ksp-generated-class.jar" % ctx.label.name,
    )

    _run_kt_builder_action(
        ctx = ctx,
        rule_kind = rule_kind,
        toolchains = toolchains,
        srcs = srcs,
        generated_src_jars = [],
        compile_deps = compile_deps,
        deps_artifacts = deps_artifacts,
        annotation_processors = annotation_processors,
        transitive_runtime_jars = transitive_runtime_jars,
        plugins = plugins,
        outputs = {
            "ksp_generated_classes_jar": ksp_generated_classes_jar,
            "ksp_generated_java_srcjar": ksp_generated_java_srcjar,
        },
        build_kotlin = False,  # Important: KSP doesn't run Kotlin compilation
        mnemonic = "KotlinKsp",
    )

    return struct(
        ksp_generated_class_jar = ksp_generated_classes_jar,
        ksp_generated_src_jar = ksp_generated_java_srcjar,
    )
```

#### KSP-Specific Execution Flow
**Location:** `kotlin/internal/jvm/compile.bzl`

```starlark
# Lines 724-725: Separate processor collection
annotation_processors = _plugin_mappers.targets_to_annotation_processors(ctx.attr.plugins + ctx.attr.deps)
ksp_annotation_processors = _plugin_mappers.targets_to_ksp_annotation_processors(ctx.attr.plugins + ctx.attr.deps)

# Lines 905-922: KSP execution
if has_kt_sources and ksp_annotation_processors:
    ksp_outputs = _run_ksp_builder_actions(
        ctx = ctx,
        rule_kind = ctx.attr._rule_kind,
        toolchains = toolchains,
        srcs = srcs,
        compile_deps = compile_deps,
        deps_artifacts = deps_artifacts,
        annotation_processors = ksp_annotation_processors,
        transitive_runtime_jars = transitive_runtime_jars,
        plugins = plugins,
    )
    ksp_generated_class_jar = ksp_outputs.ksp_generated_class_jar
    ksp_generated_src_jar = ksp_outputs.ksp_generated_src_jar

    output_jars.append(ksp_generated_class_jar)
    generated_ksp_src_jars.append(ksp_generated_src_jar)
```

**Issue:** Separate execution path from KAPT and main compilation.

### 4. KSP Plugin System

#### KspPluginInfo Provider
**Location:** `src/main/starlark/core/plugin/providers.bzl`

```starlark
KspPluginInfo = provider(
    fields = {
        "generates_java": "Runs Java compilation action for this plugin",
        "plugins": "List of JavaPluginInfo providers for the plugins to run with KSP",
    },
)
```

**Special Feature:** `generates_java` flag controls whether Java compilation runs after KSP.

#### Dedicated Mapper
**Location:** `kotlin/internal/jvm/plugins.bzl`

```starlark
# Lines 52-58
def _targets_to_ksp_annotation_processors(targets):
    """Extract KSP processors from targets."""
    plugins = []
    for t in targets:
        if _KspPluginInfo in t:
            for plugin in t[_KspPluginInfo].plugins:
                plugins.append(plugin.plugins)
    return depset(plugins)
```

### 5. Protobuf Model

#### KSP-Specific Outputs
**Location:** `src/main/protobuf/kotlin_model.proto`

```protobuf
message Outputs {
  string jar = 1;
  string jdeps = 2;
  string srcjar = 3;
  string abijar = 4;
  string generated_java_src_jar = 5;
  string generated_java_stub_jar = 6;
  string generated_class_jar = 7;

  // KSP-specific outputs
  string generated_ksp_src_jar = 8;      // ❌ KSP-specific
  string generated_ksp_classes_jar = 9;  // ❌ KSP-specific
}
```

**Note:** These may be generalizable to work with `generated_java_src_jar` and `generated_class_jar`.

---

## Comparison with KAPT Migration

### Similarities (Easy Migration Path)

| Aspect | KAPT | KSP | Migration Difficulty |
|--------|------|-----|---------------------|
| **Hardcoded in InternalCompilerPlugins** | ✅ Yes | ✅ Yes | Easy |
| **Dedicated args builder** | ✅ `kaptArgs()` | ✅ `kspArgs()` | Easy |
| **Dedicated runner function** | ✅ `runKaptPlugin()` | ✅ `runKspPlugin()` | Easy |
| **Special dispatch logic** | ✅ Check output jar | ✅ Check output jar | Easy |
| **Starlark builder action** | ✅ Has own function | ✅ Has own function | Easy |
| **Runs in stubs phase** | ✅ Yes | ✅ Yes | Easy |

### Key Differences (Complications)

| Aspect | KAPT | KSP | Migration Impact |
|--------|------|-----|-----------------|
| **Number of plugin jars** | 1 jar | 2 jars (API + CommandLine) | Medium - Change signatures to accept `List<String>` |
| **Custom provider** | ❌ Uses JavaPluginInfo | ✅ KspPluginInfo | Low - Keep provider, may merge later |
| **Version compatibility** | ✅ Works with all versions | ❌ Needs override for 2.0+ | Low - Preserve version logic |
| **Configuration options** | ~6 options | ~10 options | Low - Just more configuration |
| **Java generation flag** | N/A | ✅ `generates_java` | Medium - Preserve this behavior |
| **Output handling** | Standard jars | Custom KSP jars | Low - May generalize later |

---

## Migration Phases

### Phase 1: Add Generic Path (2-3 weeks)

**Goal:** Make KSP work through the generic `AnnotationProcessingConfig` while keeping backward compatibility.

#### 1.1: Extend Builder to Support KSP Configuration

**File:** `src/main/kotlin/io/bazel/kotlin/builder/tasks/jvm/CompilationTask.kt`

**Key Changes:**

1. **Update `annotationProcessingArgs` signature to accept multiple jars:**

```kotlin
internal fun JvmCompilationTask.annotationProcessingArgs(
  context: CompilationTaskContext,
  config: JvmCompilationTask.Inputs.AnnotationProcessingConfig,
  pluginJars: List<String>,  // CHANGED: List instead of single String
): CompilationArgs {
  return CompilationArgs().apply {
    // Add all plugin jars (for KSP: both API and CommandLine)
    pluginJars.forEach { jar ->
      xFlag("plugin", jar)
    }

    when (config.pluginId) {
      "org.jetbrains.kotlin.kapt3" -> {
        buildKaptOptions(config, directories, context)
      }
      "com.google.devtools.ksp.symbol-processing" -> {
        // NEW: KSP configuration
        buildKspOptions(config, directories, context, info)
      }
      else -> error("Unknown annotation processing plugin: ${config.pluginId}")
    }
  }
}
```

2. **Add KSP-specific options builder:**

```kotlin
private fun CompilationArgs.buildKspOptions(
  config: JvmCompilationTask.Inputs.AnnotationProcessingConfig,
  directories: Directories,
  context: CompilationTaskContext,
  info: CompilationTaskInfo,
) {
  plugin("com.google.devtools.ksp.symbol-processing") {
    flag("-Xallow-no-source-files")

    // Required KSP configuration
    flag("apclasspath", config.processorpathsList.joinToString(File.pathSeparator))
    flag("projectBaseDir", directories.incrementalData)
    flag("incremental", "false")

    // Output directories
    flag("classOutputDir", directories.generatedClasses)
    flag("javaOutputDir", directories.generatedJavaSources)
    flag("kotlinOutputDir", directories.generatedSources)
    flag("resourceOutputDir", directories.generatedSources)
    flag("kspOutputDir", directories.incrementalData)
    flag("cachesDir", directories.incrementalData)

    // Important: Prevent KSP from running its own compilation
    flag("withCompilation", "false")
    flag("returnOkOnError", "false")

    // Custom options from user (e.g., allWarningsAsErrors)
    config.optionsMap.forEach { (key, value) ->
      flag(key, value)
    }
  }
}
```

3. **Update `runPlugins` to dispatch to generic path:**

```kotlin
internal fun JvmCompilationTask.runPlugins(
  context: CompilationTaskContext,
  plugins: InternalCompilerPlugins,
  compiler: KotlinToolchain.KotlincInvoker,
): JvmCompilationTask {
  // Early exit if no annotation processing
  if ((inputs.processorsList.isEmpty() && inputs.stubsPluginClasspathList.isEmpty()) ||
      inputs.kotlinSourcesList.isEmpty()) {
    return this
  }

  // NEW PATH: Generic annotation processing (KAPT + KSP)
  if (inputs.hasAnnotationProcessing()) {
    val config = inputs.annotationProcessing

    // Find plugin jars from classpath
    val pluginJars = when (config.pluginId) {
      "org.jetbrains.kotlin.kapt3" -> {
        listOfNotNull(
          findAnnotationProcessingPluginJar(config.pluginId, inputs.stubsPluginClasspathList)
        )
      }
      "com.google.devtools.ksp.symbol-processing" -> {
        // KSP needs BOTH API and CommandLine jars
        listOfNotNull(
          inputs.stubsPluginClasspathList.firstOrNull { "symbol-processing-api" in it },
          inputs.stubsPluginClasspathList.firstOrNull { "symbol-processing-cmdline" in it }
        )
      }
      else -> error("Unknown plugin: ${config.pluginId}")
    }

    if (pluginJars.isEmpty()) {
      error("Could not find plugin jars for ${config.pluginId}")
    }

    // Apply version override for KSP (doesn't support Kotlin 2.0+)
    val overrides = if (config.pluginId == "com.google.devtools.ksp.symbol-processing") {
      mapOf(
        API_VERSION_ARG to kspKotlinToolchainVersion(info.toolchainInfo.common.apiVersion),
        LANGUAGE_VERSION_ARG to kspKotlinToolchainVersion(info.toolchainInfo.common.languageVersion)
      )
    } else {
      emptyMap()
    }

    return runAnnotationProcessingPlugin(context, config, pluginJars, compiler, overrides)
  }

  // LEGACY PATH: Keep for backward compatibility during Phase 1
  if (!outputs.generatedKspSrcJar.isNullOrEmpty()) {
    return runKspPlugin(context, plugins, compiler)
  }

  return this
}
```

4. **Update `runAnnotationProcessingPlugin` signature:**

```kotlin
private fun JvmCompilationTask.runAnnotationProcessingPlugin(
  context: CompilationTaskContext,
  config: JvmCompilationTask.Inputs.AnnotationProcessingConfig,
  pluginJars: List<String>,  // CHANGED: List instead of single String
  compiler: KotlinToolchain.KotlincInvoker,
  overrides: Map<String, String> = emptyMap(),  // NEW: For version overrides
): JvmCompilationTask {
  val processorNames = config.processorsList.joinToString(", ")

  return context.execute("${config.pluginId} ($processorNames)") {
    val args = baseArgs(overrides)  // Apply overrides here
      .plus(plugins(
        options = inputs.stubsPluginOptionsList.filterNot { o ->
          o.startsWith(config.pluginId)
        },
        // Filter out plugin jars to avoid duplicate -Xplugin
        classpath = inputs.stubsPluginClasspathList.filterNot { it in pluginJars },
      ))
      .plus(annotationProcessingArgs(context, config, pluginJars))
      .flag("-d", directories.generatedClasses)
      .values(inputs.kotlinSourcesList)
      .values(inputs.javaSourcesList)
      .list()

    context.executeCompilerTask(
      args,
      compiler::compile,
      printOnSuccess = context.whenTracing { true } == true,
    ).let { outputLines ->
      context.whenTracing {
        context.printCompilerOutput(listOf("${config.pluginId} output:") + outputLines)
      }
      return@let expandWithGeneratedSources()
    }
  }
}
```

#### 1.2: Update Starlark to Build KSP Config

**File:** `kotlin/internal/jvm/compile.bzl`

**Key Changes:**

1. **Extend `_build_annotation_processing_config` to detect KSP:**

```starlark
def _build_annotation_processing_config(plugins, annotation_processors):
    """Extract annotation processing config from plugins.

    Now supports both KAPT and KSP.
    """
    if not annotation_processors:
        return None

    # Check for KAPT
    kapt_id = "org.jetbrains.kotlin.kapt3"
    has_kapt = any([
        kapt_id in str(jar) or "kotlin-annotation-processing" in str(jar)
        for jar in plugins.stubs_phase.classpath.to_list()
    ])

    if has_kapt:
        # Build KAPT config (existing logic)
        processors = []
        processorpath = []
        options = {}

        for processor in annotation_processors:
            processors.extend(processor.processor_classes.to_list())
            processorpath.extend([j.path for j in processor.processor_jars.to_list()])

        for opt in plugins.stubs_phase.options:
            if opt.id == kapt_id and opt.value.startswith("apoption="):
                apoption_part = opt.value[len("apoption="):]
                parts = apoption_part.split(":", 1)
                if len(parts) == 2:
                    options[parts[0]] = parts[1]

        return struct(
            plugin_id = kapt_id,
            processors = processors,
            processorpath = processorpath,
            options = options,
            apt_mode = "stubsAndApt",
        )

    # NEW: Check for KSP
    ksp_id = "com.google.devtools.ksp.symbol-processing"
    has_ksp = any([
        "symbol-processing" in str(jar)
        for jar in plugins.stubs_phase.classpath.to_list()
    ])

    if has_ksp:
        # Build KSP config
        processors = []
        processorpath = []
        ksp_options = {}

        for processor in annotation_processors:
            processors.extend(processor.processor_classes.to_list())
            processorpath.extend([j.path for j in processor.processor_jars.to_list()])

        # Extract KSP-specific options
        for opt in plugins.stubs_phase.options:
            if opt.id == ksp_id:
                # KSP options are in format "key=value"
                if "=" in opt.value:
                    key, val = opt.value.split("=", 1)
                    ksp_options[key] = val

        return struct(
            plugin_id = ksp_id,
            processors = processors,
            processorpath = processorpath,
            options = ksp_options,
            apt_mode = "",  # KSP doesn't use apt_mode
        )

    return None
```

2. **Pass KSP config in `_run_kt_builder_action`:**

```starlark
def _run_kt_builder_action(
    ctx,
    rule_kind,
    # ... other params ...
    annotation_processors = [],
    ksp_annotation_processors = [],  # Keep separate for now
    # ... other params ...
):
    # Combine all annotation processors
    all_processors = annotation_processors + ksp_annotation_processors

    # Build unified annotation processing config
    ap_config = _build_annotation_processing_config(
        plugins = plugins,
        annotation_processors = all_processors,
    )

    if ap_config:
        args.add("--annotation_processing_plugin_id", ap_config.plugin_id)
        args.add_all("--annotation_processing_processors", ap_config.processors, omit_if_empty = True)
        args.add_all("--annotation_processing_processorpath", ap_config.processorpath, omit_if_empty = True)
        if ap_config.options:
            args.add_all(
                "--annotation_processing_options",
                ["%s=%s" % (k, v) for k, v in ap_config.options.items()],
            )
        if ap_config.apt_mode:
            args.add("--annotation_processing_apt_mode", ap_config.apt_mode)
```

#### 1.3: Testing Phase 1

**Test Strategy:**

1. **Unit Tests:**
   - Test `buildKspOptions()` function
   - Test multi-jar plugin handling
   - Test version override logic
   - Verify KSP config building in Starlark

2. **Integration Tests:**
   - Use existing KSP test data: `src/test/data/jvm/ksp/`
   - Test bytecode generator example
   - Test KSP with Java generation

3. **Backward Compatibility:**
   - Ensure legacy `runKspPlugin()` path still works
   - Test both new and old paths side by side
   - Verify no breaking changes to public API

**Test Commands:**

```bash
# Test new generic path
bazel test //src/test/kotlin/io/bazel/kotlin/builder/tasks:KotlinBuilderJvmKspTest

# Test existing KSP functionality
bazel build //src/test/data/jvm/ksp:bytecodegenerator

# Test legacy path still works
bazel test //src/test/data/jvm/ksp/...

# Full test suite
bazel test //src/...
```

**Success Criteria:**
- ✅ All existing tests pass
- ✅ New tests added for generic KSP path
- ✅ Both new and legacy paths work
- ✅ No performance regression

---

### Phase 2: Remove Hardcoding (2-3 weeks)

**Goal:** Move KSP plugin jar resolution to Starlark layer and remove hardcoded references.

#### 2.1: Remove KSP from InternalCompilerPlugins

**File:** `src/main/kotlin/io/bazel/kotlin/builder/tasks/jvm/InternalCompilerPlugins.kt`

```kotlin
class InternalCompilerPlugins constructor(
  val jvmAbiGen: KotlinToolchain.CompilerPlugin,
  val skipCodeGen: KotlinToolchain.CompilerPlugin,
  val jdeps: KotlinToolchain.CompilerPlugin,
  // REMOVED: val kspSymbolProcessingApi: KotlinToolchain.CompilerPlugin,
  // REMOVED: val kspSymbolProcessingCommandLine: KotlinToolchain.CompilerPlugin,
)
```

**File:** `src/main/kotlin/io/bazel/kotlin/builder/KotlinBuilderComponent.java`

```java
public InternalCompilerPlugins provideInternalPlugins(KotlinToolchain toolchain) {
    return new InternalCompilerPlugins(
            toolchain.getJvmAbiGen(),
            toolchain.getSkipCodeGen(),
            toolchain.getJdepsGen()
            // REMOVED: toolchain.getKspSymbolProcessingApi(),
            // REMOVED: toolchain.getKspSymbolProcessingCommandLine()
    );
}
```

#### 2.2: Remove KSP from KotlinToolchain

**File:** `src/main/kotlin/io/bazel/kotlin/builder/toolchain/KotlinToolchain.kt`

**Remove lazy properties:**

```kotlin
// DELETE:
// private val KSP_SYMBOL_PROCESSING_API by lazy { ... }
// private val KSP_SYMBOL_PROCESSING_CMDLINE by lazy { ... }
```

**Remove from constructor parameters:**

```kotlin
@JvmStatic
fun createToolchain(
  javaHome: Path,
  kotlinCompiler: File,
  buildToolsRuntime: File,
  compilerPluginJar: File,
  jvmAbiGenPluginJar: File,
  skipCodeGenPluginJar: File,
  jdepsGenPluginJar: File,
  // REMOVED: kspSymbolProcessingApi: File,
  // REMOVED: kspSymbolProcessingCommandLine: File,
  kotlinxSerializationCoreJvm: File,
  kotlinxSerializationJson: File,
  kotlinxSerializationJsonJvm: File,
): KotlinToolchain = createToolchain(
  // ... parameters ...
  // REMOVED: kspSymbolProcessingApi,
  // REMOVED: kspSymbolProcessingCommandLine,
)
```

**Remove from preloaded jars:**

```kotlin
private val preloadedJars = listOf(
  jvmAbiGenPlugin.jarPath,
  skipCodeGenPlugin.jarPath,
  // REMOVED: kspSymbolProcessingApi.jarPath,
  // REMOVED: kspSymbolProcessingCommandLine.jarPath,
)
```

#### 2.3: Simplify Plugin Dispatch

**File:** `src/main/kotlin/io/bazel/kotlin/builder/tasks/jvm/CompilationTask.kt`

**Remove dependency on InternalCompilerPlugins for KSP:**

```kotlin
internal fun JvmCompilationTask.runPlugins(
  context: CompilationTaskContext,
  plugins: InternalCompilerPlugins,  // Still needed for jvm-abi-gen, etc.
  compiler: KotlinToolchain.KotlincInvoker,
): JvmCompilationTask {
  if (!inputs.hasAnnotationProcessing()) {
    return this
  }

  val config = inputs.annotationProcessing

  // Plugin jars now come entirely from stubs_plugin_classpath (provided by Starlark)
  val pluginJars = findAnnotationProcessingPluginJars(
    config.pluginId,
    inputs.stubsPluginClasspathList
  )

  if (pluginJars.isEmpty()) {
    error("No plugin jars found for ${config.pluginId} in classpath")
  }

  // Determine version overrides
  val overrides = if (config.pluginId == "com.google.devtools.ksp.symbol-processing") {
    mapOf(
      API_VERSION_ARG to kspKotlinToolchainVersion(info.toolchainInfo.common.apiVersion),
      LANGUAGE_VERSION_ARG to kspKotlinToolchainVersion(info.toolchainInfo.common.languageVersion)
    )
  } else {
    emptyMap()
  }

  return runAnnotationProcessingPlugin(context, config, pluginJars, compiler, overrides)
}
```

**Add generic plugin jar finder:**

```kotlin
private fun findAnnotationProcessingPluginJars(
  pluginId: String,
  classpath: List<String>,
): List<String> {
  return when (pluginId) {
    "org.jetbrains.kotlin.kapt3" -> {
      listOfNotNull(
        classpath.firstOrNull { "kotlin-annotation-processing" in it || "kapt" in it }
      )
    }
    "com.google.devtools.ksp.symbol-processing" -> {
      // KSP requires both API and CommandLine jars
      listOfNotNull(
        classpath.firstOrNull { "symbol-processing-api" in it },
        classpath.firstOrNull { "symbol-processing-cmdline" in it }
      )
    }
    else -> {
      // Generic: try to find jar by plugin ID
      classpath.filter { pluginId.substringAfterLast(".") in it.lowercase() }
    }
  }
}
```

#### 2.4: Add KSP Plugin to Implicit Attributes

**File:** `kotlin/internal/jvm/jvm.bzl`

```starlark
_common_attr = {
    # ... existing attributes ...

    # Implicit KAPT plugin
    "_kapt_plugin": attr.label(
        default = Label("//kotlin/compiler:kapt"),
        cfg = "exec",
        providers = [_KtCompilerPluginInfo],
    ),

    # NEW: Implicit KSP plugin
    "_ksp_plugin": attr.label(
        default = Label("//kotlin/compiler:ksp"),
        cfg = "exec",
        providers = [_KtCompilerPluginInfo],
    ),
}
```

**File:** `kotlin/internal/jvm/compile.bzl`

```starlark
# Add automatic KSP plugin when there are KSP processors
all_plugins = ctx.attr.plugins + _exported_plugins(deps = ctx.attr.deps)

# Check if KSP plugin is already present
ksp_id = "com.google.devtools.ksp.symbol-processing"
has_ksp = any([
    _KtCompilerPluginInfo in p and p[_KtCompilerPluginInfo].id == ksp_id
    for p in all_plugins
])

if ksp_annotation_processors and not has_ksp:
    # Add implicit KSP plugin
    all_plugins = all_plugins + [ctx.attr._ksp_plugin]

plugins = _new_plugins_from(all_plugins)
```

#### 2.5: Create KSP Plugin Definition

**File:** `kotlin/compiler/ksp.bzl` (new file)

```starlark
"""KSP (Kotlin Symbol Processing) compiler plugin definition."""

load("@rules_java//java:defs.bzl", "java_import")
load("//kotlin/internal:defs.bzl", "KtCompilerPluginInfo")

def _ksp_compiler_plugin_impl(ctx):
    """Provides KSP plugin with both API and CommandLine jars."""

    # Get both KSP jars from compiler distribution
    ksp_api_jar = ctx.file.ksp_api_jar
    ksp_cmdline_jar = ctx.file.ksp_cmdline_jar

    return [
        KtCompilerPluginInfo(
            id = "com.google.devtools.ksp.symbol-processing",
            classpath = depset([ksp_api_jar, ksp_cmdline_jar]),
            options = [],
            stubs = True,  # Runs in stubs phase
            compile = False,  # Doesn't run in compile phase
        ),
    ]

ksp_compiler_plugin = rule(
    implementation = _ksp_compiler_plugin_impl,
    attrs = {
        "ksp_api_jar": attr.label(
            default = Label("//kotlin/compiler:symbol-processing-api"),
            allow_single_file = [".jar"],
        ),
        "ksp_cmdline_jar": attr.label(
            default = Label("//kotlin/compiler:symbol-processing-cmdline"),
            allow_single_file = [".jar"],
        ),
    },
)
```

**File:** `kotlin/compiler/BUILD.bazel`

```starlark
# Add KSP plugin definition
load(":ksp.bzl", "ksp_compiler_plugin")

ksp_compiler_plugin(
    name = "ksp",
    visibility = ["//visibility:public"],
)
```

#### 2.6: Testing Phase 2

**Test Strategy:**

1. **Verify no hardcoded references:**
   ```bash
   # Should return no results
   rg "plugins\.ksp" src/main/kotlin/io/bazel/kotlin/builder/
   rg "KSP_SYMBOL_PROCESSING" src/main/kotlin/io/bazel/kotlin/builder/
   ```

2. **Integration tests:**
   ```bash
   bazel test //src/test/data/jvm/ksp/...
   bazel build //examples/ksp/...
   ```

3. **Verify plugin resolution:**
   ```bash
   # Build with trace to see plugin jars loaded
   bazel build --define=kt_trace=1 //src/test/data/jvm/ksp:bytecodegenerator
   ```

**Success Criteria:**
- ✅ KSP removed from InternalCompilerPlugins
- ✅ KSP removed from KotlinToolchain
- ✅ Plugin jars resolved in Starlark layer
- ✅ All tests pass without hardcoded KSP references
- ✅ Implicit KSP plugin added when needed

---

### Phase 3: Cleanup and Remove Legacy (1-2 weeks)

**Goal:** Remove all KSP-specific functions and deprecated code paths.

#### 3.1: Remove Deprecated Functions

**File:** `src/main/kotlin/io/bazel/kotlin/builder/tasks/jvm/CompilationTask.kt`

```kotlin
// DELETE entire functions:
// internal fun JvmCompilationTask.kspArgs(plugins: InternalCompilerPlugins)
// private fun JvmCompilationTask.runKspPlugin(context, plugins, compiler)

// DECISION: Keep or integrate kspKotlinToolchainVersion()?
// Option A: Keep as internal utility for version override
// Option B: Move into generic annotation processing as "version_override" config
```

**Recommendation:** Keep `kspKotlinToolchainVersion()` but rename to something more generic:

```kotlin
private fun getPluginKotlinVersion(
  pluginId: String,
  requestedVersion: String
): String {
  return when (pluginId) {
    "com.google.devtools.ksp.symbol-processing" -> {
      // KSP doesn't support Kotlin 2.0+
      if (requestedVersion.toFloat() >= 2.0) "1.9" else requestedVersion
    }
    else -> requestedVersion
  }
}
```

#### 3.2: Evaluate KspPluginInfo

**Decision Point:** Keep or merge `KspPluginInfo`?

**Option A: Keep KspPluginInfo (Recommended)**

```starlark
# Rationale:
# - generates_java flag is useful behavior
# - Wraps JavaPluginInfo cleanly for KSP processors
# - Not causing maintenance burden
# - May be useful for future processors

# Keep as-is in src/main/starlark/core/plugin/providers.bzl
```

**Option B: Merge into KtCompilerPluginInfo (Aggressive)**

```starlark
# Add to KtCompilerPluginInfo
KtCompilerPluginInfo = provider(
    fields = {
        "id": "Plugin ID",
        "classpath": "Plugin jars",
        "options": "Plugin options",
        "stubs": "Run in stubs phase",
        "compile": "Run in compile phase",
        "generates_java": "Whether plugin generates Java sources",  # NEW
    },
)

# Remove KspPluginInfo entirely
# Update all KSP plugin definitions to use KtCompilerPluginInfo
```

**Recommendation:** Keep `KspPluginInfo` for now. Can revisit in future major version.

#### 3.3: Consider Generalizing KSP Outputs

**Current state:**

```protobuf
message Outputs {
  string generated_ksp_src_jar = 8;
  string generated_ksp_classes_jar = 9;
}
```

**Options:**

**Option A: Keep KSP-specific fields (Conservative)**
- Maintains backward compatibility
- Clear separation of KSP outputs
- No migration needed for existing code

**Option B: Generalize outputs (Aggressive)**
```protobuf
message Outputs {
  // DEPRECATED: generated_ksp_src_jar
  // DEPRECATED: generated_ksp_classes_jar

  // Use instead:
  string generated_java_src_jar = 5;      // For KSP-generated Java
  string generated_class_jar = 7;         // For KSP-generated classes
}
```

**Recommendation:** Keep KSP-specific fields for now. Deprecate but don't remove for backward compatibility.

#### 3.4: Clean Up Starlark Layer

**File:** `kotlin/internal/jvm/compile.bzl`

**Remove KSP-specific execution:**

```starlark
# DELETE: _run_ksp_builder_actions() function

# SIMPLIFY: Merge annotation processing flow
def _run_kt_java_builder_actions(...):
    # Unified handling for KAPT, KSP, and future processors
    if has_kt_sources and (annotation_processors or ksp_annotation_processors):
        all_processors = annotation_processors + ksp_annotation_processors

        # Single unified annotation processing action
        generated_sources = _run_annotation_processing(
            ctx = ctx,
            processors = all_processors,
            # ... unified parameters ...
        )

        # Handle outputs generically
        output_jars.extend(generated_sources.jars)
        generated_src_jars.extend(generated_sources.srcjars)
```

**File:** `kotlin/internal/jvm/plugins.bzl`

**Simplify or remove KSP-specific mapper:**

```starlark
# Option A: Keep separate mapper for clarity
def _targets_to_ksp_annotation_processors(targets):
    # ... existing logic ...

# Option B: Merge into single processor mapper
def _targets_to_annotation_processors(targets):
    """Extract all annotation processors (KAPT, KSP, etc.)."""
    processors = []
    for t in targets:
        if JavaPluginInfo in t:
            processors.append(t[JavaPluginInfo])
        if _KspPluginInfo in t:
            for plugin in t[_KspPluginInfo].plugins:
                processors.append(plugin)
    return depset(processors)
```

**Recommendation:** Keep separate mapper for now. Makes KSP handling explicit.

#### 3.5: Update Documentation

**Files to update:**

1. **README.md**
   - Update KSP usage examples
   - Document migration from old to new API
   - Add section on generic annotation processing

2. **CLAUDE.md**
   - Update architecture section
   - Document unified annotation processing flow
   - Remove KSP-specific sections

3. **docs/kotlin.md**
   - Update KSP rule documentation
   - Add examples of `kt_ksp_plugin` usage
   - Document configuration options

4. **Migration Guide** (new file: `KSP_MIGRATION_GUIDE.md`)
   ```markdown
   # Migrating to Generic KSP Implementation

   ## What Changed
   - KSP now uses generic annotation processing infrastructure
   - No code changes required for most users
   - Advanced users: see new configuration options

   ## Breaking Changes
   - None for standard usage
   - If you were using internal APIs: see "Advanced Migration"

   ## New Features
   - Can customize KSP configuration per target
   - Better integration with other annotation processors
   - Clearer error messages
   ```

5. **Code Comments**
   - Remove "Phase 1", "Phase 2", "Phase 3" comments from code
   - Add architectural documentation to key functions
   - Document why certain decisions were made (e.g., keeping KspPluginInfo)

#### 3.6: Final Testing

**Comprehensive test suite:**

```bash
# Full test suite
bazel test //:all_tests

# Verify no performance regression
bazel test //src/test/kotlin/io/bazel/kotlin:performance_tests --test_output=all

# KSP examples
bazel build //examples/ksp/...

# Integration with other features
bazel test //src/test/data/jvm/kapt/...  # Ensure KAPT still works
bazel test //src/test/data/jvm/ksp/...   # Ensure KSP works

# Edge cases
bazel test //src/test/kotlin/io/bazel/kotlin:KotlinJvmAssociatesBasicVisibilityTest
bazel test //src/test/kotlin/io/bazel/kotlin:KotlinJvmKspAssertionTest
```

**Performance benchmarks:**

```bash
# Before migration baseline
bazel build --define=kt_timings=1 //large/ksp:target

# After migration
bazel build --define=kt_timings=1 //large/ksp:target

# Compare timing results - should be within 5%
```

**Success Criteria:**
- ✅ All KSP-specific functions removed (except version override)
- ✅ All tests pass
- ✅ No performance regression (<5% variance)
- ✅ Documentation updated
- ✅ Clean code structure (no legacy branches)

---

## Technical Challenges

### Challenge 1: Multiple Plugin Jars

**Problem:** KSP needs both API and CommandLine jars, but the generic system was designed for single-jar plugins (like KAPT).

**Impact:** Medium

**Solution:**

1. Change `annotationProcessingArgs()` signature to accept `List<String>` instead of `String`
2. Update `runAnnotationProcessingPlugin()` to accept multiple jars
3. Modify plugin jar finder to return lists
4. Test thoroughly with both KAPT (1 jar) and KSP (2 jars)

**Code changes:**

```kotlin
// Before (single jar)
internal fun JvmCompilationTask.annotationProcessingArgs(
  config: ...,
  pluginJar: String,  // ❌ Single string
): CompilationArgs

// After (multiple jars)
internal fun JvmCompilationTask.annotationProcessingArgs(
  config: ...,
  pluginJars: List<String>,  // ✅ List of strings
): CompilationArgs {
  return CompilationArgs().apply {
    pluginJars.forEach { jar ->
      xFlag("plugin", jar)
    }
    // ... rest of configuration
  }
}
```

**Testing:**

```kotlin
@Test
fun testMultiplePluginJars() {
  val jars = listOf("/path/to/api.jar", "/path/to/cmdline.jar")
  val args = task.annotationProcessingArgs(config, jars)
  // Verify both -Xplugin flags present
}
```

### Challenge 2: Version Compatibility

**Problem:** KSP doesn't support Kotlin 2.0+, needs to override language/API version to 1.9.

**Impact:** Low

**Solution:**

Keep `kspKotlinToolchainVersion()` function and apply version override in `runAnnotationProcessingPlugin()`:

```kotlin
private fun kspKotlinToolchainVersion(version: String): String {
  return if (version.toFloat() >= 2.0) "1.9" else version
}

// Apply in runPlugins():
val overrides = if (config.pluginId == "com.google.devtools.ksp.symbol-processing") {
  mapOf(
    API_VERSION_ARG to kspKotlinToolchainVersion(info.toolchainInfo.common.apiVersion),
    LANGUAGE_VERSION_ARG to kspKotlinToolchainVersion(info.toolchainInfo.common.languageVersion)
  )
} else {
  emptyMap()
}

return runAnnotationProcessingPlugin(context, config, pluginJars, compiler, overrides)
```

**Testing:**

```kotlin
@Test
fun testKspVersionOverride() {
  val info = CompilationTaskInfo.newBuilder()
    .setToolchainInfo(
      KotlinToolchainInfo.newBuilder()
        .setCommon(
          KotlinToolchainInfo.Common.newBuilder()
            .setApiVersion("2.0")
            .setLanguageVersion("2.0")
        )
    )

  val config = AnnotationProcessingConfig.newBuilder()
    .setPluginId("com.google.devtools.ksp.symbol-processing")
    .build()

  // Verify API and language version overridden to 1.9
}
```

### Challenge 3: KspPluginInfo Provider

**Problem:** KSP has its own custom provider with `generates_java` flag. Not clear if this should be removed or kept.

**Impact:** Medium

**Options:**

**Option A: Keep KspPluginInfo (Recommended)**
- **Pros:**
  - `generates_java` flag is useful behavior
  - Wraps JavaPluginInfo cleanly
  - Not causing maintenance issues
- **Cons:**
  - Slight complexity having multiple providers

**Option B: Merge into KtCompilerPluginInfo**
- **Pros:**
  - Single unified provider
  - Cleaner architecture
- **Cons:**
  - More work to migrate
  - May not be applicable to all compiler plugins

**Recommendation:** Keep `KspPluginInfo` for now. The `generates_java` flag is valuable and merging provides minimal benefit.

**Decision deferred to Phase 3.**

### Challenge 4: Configuration Complexity

**Problem:** KSP has 10+ configuration options, more complex than KAPT's 6 options.

**Impact:** Low

**Solution:**

Create dedicated `buildKspOptions()` function with all options documented:

```kotlin
private fun CompilationArgs.buildKspOptions(
  config: JvmCompilationTask.Inputs.AnnotationProcessingConfig,
  directories: Directories,
  context: CompilationTaskContext,
  info: CompilationTaskInfo,
) {
  plugin("com.google.devtools.ksp.symbol-processing") {
    flag("-Xallow-no-source-files")

    // Processor classpath (required)
    flag("apclasspath", config.processorpathsList.joinToString(File.pathSeparator))

    // Project base directory (required for incremental, but disabled)
    flag("projectBaseDir", directories.incrementalData)

    // Incremental compilation (disabled - not stable)
    flag("incremental", "false")

    // Output directories (required)
    flag("classOutputDir", directories.generatedClasses)
    flag("javaOutputDir", directories.generatedJavaSources)
    flag("kotlinOutputDir", directories.generatedSources)
    flag("resourceOutputDir", directories.generatedSources)
    flag("kspOutputDir", directories.incrementalData)

    // Cache directory (required)
    flag("cachesDir", directories.incrementalData)

    // Compilation control (critical - prevent KSP from running own compilation)
    flag("withCompilation", "false")
    flag("returnOkOnError", "false")

    // Custom options from user
    config.optionsMap.forEach { (key, value) ->
      flag(key, value)
    }
  }
}
```

**Documentation:**

```kotlin
/**
 * Builds KSP-specific compiler plugin options.
 *
 * Key configuration points:
 * - apclasspath: Annotation processor classpath
 * - withCompilation: Must be "false" to prevent KSP from running its own compilation
 * - incremental: Currently disabled (not stable)
 * - Output directories: Must match Bazel's directory structure
 *
 * Custom options can be passed via config.optionsMap.
 */
```

### Challenge 5: Backward Compatibility

**Problem:** Need to support both old and new paths during Phase 1-2 without breaking existing users.

**Impact:** High

**Solution:**

Dual-path approach during Phases 1-2:

```kotlin
internal fun JvmCompilationTask.runPlugins(...): JvmCompilationTask {
  // NEW PATH: Try generic annotation processing first
  if (inputs.hasAnnotationProcessing()) {
    // Use new unified path
    return runAnnotationProcessingPlugin(...)
  }

  // LEGACY PATH: Fall back to old KSP-specific path
  if (!outputs.generatedKspSrcJar.isNullOrEmpty()) {
    return runKspPlugin(context, plugins, compiler)
  }

  return this
}
```

**Testing strategy:**

```bash
# Test new path
bazel test --define=use_new_ksp=true //src/test/data/jvm/ksp/...

# Test legacy path
bazel test --define=use_new_ksp=false //src/test/data/jvm/ksp/...

# Test both paths work
bazel test //src/test/data/jvm/ksp/...
```

**Gradual rollout:**

1. Phase 1: Both paths available, new path behind feature flag
2. Phase 2: New path default, legacy path available as fallback
3. Phase 3: Remove legacy path entirely

---

## Testing Strategy

### Unit Tests

**New Tests to Add:**

1. **`KotlinBuilderJvmKspTest.java`** (update existing)
   ```java
   @Test
   public void testKspGenericPath() {
     // Test KSP through generic annotation processing
   }

   @Test
   public void testKspMultipleJars() {
     // Test that both API and CommandLine jars are loaded
   }

   @Test
   public void testKspVersionOverride() {
     // Test Kotlin 2.0+ version override to 1.9
   }
   ```

2. **`AnnotationProcessingConfigTest.kt`** (new)
   ```kotlin
   @Test
   fun `buildKspOptions creates correct configuration`() {
     // Verify all KSP options are present
   }

   @Test
   fun `findAnnotationProcessingPluginJars finds both KSP jars`() {
     // Verify multi-jar detection
   }
   ```

3. **`CompilationTaskTest.kt`** (update existing)
   ```kotlin
   @Test
   fun `runPlugins dispatches to generic path for KSP`() {
     // Verify KSP uses generic runAnnotationProcessingPlugin()
   }
   ```

### Integration Tests

**Existing Tests to Verify:**

```bash
# Basic KSP functionality
bazel test //src/test/data/jvm/ksp:bytecodegenerator

# KSP with multiple processors
bazel test //src/test/kotlin/io/bazel/kotlin:KotlinJvmKspAssertionTest

# KSP with Java generation
bazel test //src/test/data/jvm/ksp:generates_java_test
```

**New Integration Tests:**

1. **KSP with custom options:**
   ```starlark
   kt_jvm_library(
     name = "ksp_with_options",
     srcs = ["Test.kt"],
     plugins = [
       kt_ksp_plugin(
         processor = "@maven//:processor",
         options = {
           "allWarningsAsErrors": "true",
           "customOption": "value",
         },
       ),
     ],
   )
   ```

2. **Mixed KAPT and KSP:**
   ```starlark
   kt_jvm_library(
     name = "mixed_processors",
     srcs = ["Test.kt"],
     plugins = [
       "//kotlin/compiler:kapt",
       "//kotlin/compiler:ksp",
     ],
   )
   ```

### Compatibility Tests

**Phase 1-2: Dual Path Testing**

```bash
# Test new path explicitly
bazel test --test_env=USE_GENERIC_KSP=1 //src/test/data/jvm/ksp/...

# Test legacy path still works
bazel test --test_env=USE_LEGACY_KSP=1 //src/test/data/jvm/ksp/...

# Test automatic selection
bazel test //src/test/data/jvm/ksp/...
```

**Phase 3: Regression Testing**

```bash
# Ensure no functionality lost
bazel test //src/test/data/jvm/ksp/...

# Ensure KAPT still works
bazel test //src/test/data/jvm/kapt/...

# Full test suite
bazel test //:all_tests
```

### Performance Tests

**Benchmark Setup:**

```bash
# Create large KSP test target with 100+ files
bazel build //benchmarks/ksp:large_target

# Measure compilation time before migration
bazel build --define=kt_timings=1 //benchmarks/ksp:large_target
# Record: Compilation took 15.3s

# Measure compilation time after migration
bazel build --define=kt_timings=1 //benchmarks/ksp:large_target
# Verify: Compilation took 15.5s (within 5% tolerance)
```

**Performance Criteria:**

- ✅ Compilation time within 5% of baseline
- ✅ Memory usage within 10% of baseline
- ✅ No increase in action count
- ✅ Worker reuse rate unchanged

### Test Matrix

| Feature | Phase 1 | Phase 2 | Phase 3 |
|---------|---------|---------|---------|
| Basic KSP | ✅ New + Legacy | ✅ New only | ✅ New only |
| Multi-jar plugins | ✅ Tested | ✅ Tested | ✅ Tested |
| Version override | ✅ Tested | ✅ Tested | ✅ Tested |
| Custom options | ✅ Tested | ✅ Tested | ✅ Tested |
| Java generation | ✅ Tested | ✅ Tested | ✅ Tested |
| Mixed processors | ❌ Not yet | ✅ Tested | ✅ Tested |
| Performance | ✅ Baseline | ✅ Verified | ✅ Verified |

---

## Success Criteria

### Phase 1 Success Criteria

- ✅ KSP works through generic annotation processing path
- ✅ Legacy KSP path still works (backward compatibility)
- ✅ All existing tests pass (no regressions)
- ✅ New tests added for generic KSP path
- ✅ Both KAPT and KSP use same `AnnotationProcessingConfig`
- ✅ Documentation updated with Phase 1 status
- ✅ Performance within 5% of baseline

### Phase 2 Success Criteria

- ✅ KSP removed from `InternalCompilerPlugins`
- ✅ KSP removed from `KotlinToolchain`
- ✅ Plugin jars resolved in Starlark layer
- ✅ All tests pass without hardcoded KSP references
- ✅ Implicit KSP plugin added when needed
- ✅ No usage of legacy `runKspPlugin()` in production code
- ✅ Documentation updated with Phase 2 status

### Phase 3 Success Criteria

- ✅ All KSP-specific functions removed (except version override utility)
- ✅ Legacy code paths deleted
- ✅ Documentation fully updated (README, CLAUDE.md, docs/)
- ✅ Migration guide created
- ✅ Full test suite passes (67/67 tests)
- ✅ No performance regression (<5% variance)
- ✅ Clean code structure (no "Phase X" comments)
- ✅ Examples updated and working

### Overall Migration Success

The migration is considered complete when:

1. **Architecture:**
   - ✅ KSP uses generic annotation processing infrastructure
   - ✅ No KSP-specific hardcoded logic in builder
   - ✅ Plugin resolution happens in Starlark layer
   - ✅ Unified code path for KAPT and KSP

2. **Code Quality:**
   - ✅ No duplicate code between KAPT and KSP
   - ✅ Clear separation of concerns
   - ✅ Well-documented functions
   - ✅ No legacy comments or dead code

3. **Testing:**
   - ✅ 100% test pass rate
   - ✅ No performance degradation
   - ✅ Comprehensive test coverage

4. **Documentation:**
   - ✅ Architecture documented in CLAUDE.md
   - ✅ User-facing docs updated
   - ✅ Migration guide available
   - ✅ Code comments explain design decisions

---

## Rollback Plan

### Phase 1 Rollback

**Trigger:** New generic path has critical bugs

**Action:**
```kotlin
// Add feature flag check
internal fun JvmCompilationTask.runPlugins(...): JvmCompilationTask {
  if (System.getProperty("use_legacy_ksp", "false") == "true") {
    // Force legacy path
    if (!outputs.generatedKspSrcJar.isNullOrEmpty()) {
      return runKspPlugin(context, plugins, compiler)
    }
  }

  // Try new path first
  if (inputs.hasAnnotationProcessing()) {
    return runAnnotationProcessingPlugin(...)
  }

  // Fall back to legacy path
  if (!outputs.generatedKspSrcJar.isNullOrEmpty()) {
    return runKspPlugin(context, plugins, compiler)
  }

  return this
}
```

**User Impact:** None - automatic fallback to legacy path

**Recovery Time:** Immediate (feature flag)

### Phase 2 Rollback

**Trigger:** Plugin resolution in Starlark fails

**Action:**
1. Revert `InternalCompilerPlugins` changes
2. Restore KSP fields temporarily
3. Revert `KotlinToolchain` changes
4. Use Phase 1 generic path until fixed

**Files to revert:**
- `src/main/kotlin/io/bazel/kotlin/builder/tasks/jvm/InternalCompilerPlugins.kt`
- `src/main/kotlin/io/bazel/kotlin/builder/toolchain/KotlinToolchain.kt`
- `src/main/kotlin/io/bazel/kotlin/builder/KotlinBuilderComponent.java`

**User Impact:** Low - Phase 1 path still available

**Recovery Time:** 1-2 hours (targeted revert)

### Phase 3 Rollback

**Trigger:** Critical functionality lost in cleanup

**Action:**
1. Restore deleted functions from git history
2. Re-add legacy paths
3. Keep running on Phase 2 code until issue resolved

**Commands:**
```bash
# Restore specific functions
git show HEAD~1:src/main/kotlin/.../CompilationTask.kt > restored.kt
# Cherry-pick specific functions back

# Rebuild and test
bazel test //src/test/data/jvm/ksp/...
```

**User Impact:** None if caught in testing, low if in production

**Recovery Time:** 2-4 hours (restore from git + testing)

### Rollback Decision Matrix

| Severity | Phase 1 | Phase 2 | Phase 3 |
|----------|---------|---------|---------|
| **P0: Total KSP failure** | Feature flag → Legacy | Revert to Phase 1 | Revert to Phase 2 |
| **P1: Performance degradation >20%** | Feature flag → Legacy | Revert to Phase 1 | Revert to Phase 2 |
| **P2: Some KSP features broken** | Debug new path | Debug new path | Restore specific functions |
| **P3: Edge case issues** | Fix forward | Fix forward | Fix forward |

---

## Open Questions

### Q1: Should KspPluginInfo be removed?

**Current State:** KSP uses custom `KspPluginInfo` provider with `generates_java` flag

**Options:**
- **A: Keep KspPluginInfo** (Recommended)
  - Pros: `generates_java` is useful, no migration needed
  - Cons: Slight added complexity

- **B: Merge into KtCompilerPluginInfo**
  - Pros: Unified provider system
  - Cons: More work, may not fit all plugins

**Recommendation:** Keep `KspPluginInfo` for now. Revisit in future if it causes maintenance issues.

**Decision deadline:** Phase 3

### Q2: How to handle KSP version upgrades?

**Current State:** KSP version is hardcoded in toolchain

**Options:**
- **A: Move to toolchain configuration** (Recommended)
  ```starlark
  define_kt_toolchain(
    name = "my_toolchain",
    ksp_version = "1.9.20-1.0.14",
  )
  ```

- **B: Make user-overridable per target**
  ```starlark
  kt_jvm_library(
    name = "lib",
    plugins = [
      kt_ksp_plugin(
        version = "1.9.20-1.0.14",
        processor = "...",
      ),
    ],
  )
  ```

**Recommendation:** Option A - toolchain configuration. Per-target override can be added later if needed.

**Decision deadline:** Phase 2

### Q3: What about custom KSP processors?

**Current State:** Users define KSP processors using `kt_ksp_plugin`

**Question:** Will custom processors work automatically after migration?

**Answer:** Yes, should work automatically once generic path is implemented. The generic system processes any annotation processor, not just KAPT.

**Action:** Document usage in migration guide

**Decision:** No changes needed

### Q4: Should KSP outputs be generalized?

**Current State:**
```protobuf
string generated_ksp_src_jar = 8;
string generated_ksp_classes_jar = 9;
```

**Options:**
- **A: Keep KSP-specific fields** (Recommended)
  - Pros: Backward compatible, clear separation
  - Cons: Slightly redundant

- **B: Generalize to use existing fields**
  ```protobuf
  string generated_java_src_jar = 5;  // Use for KSP Java sources
  string generated_class_jar = 7;     // Use for KSP classes
  ```
  - Pros: More generic, fewer fields
  - Cons: Breaking change, migration needed

**Recommendation:** Keep KSP-specific fields for now. Deprecate but don't remove. Can generalize in future major version.

**Decision deadline:** Phase 3

### Q5: How to handle KSP + KAPT in same target?

**Current State:** Not explicitly supported

**Question:** Should we support running both KAPT and KSP in the same target?

**Options:**
- **A: Allow both** (Recommended)
  ```starlark
  kt_jvm_library(
    name = "lib",
    plugins = [
      "//kotlin/compiler:kapt",  # For Dagger
      "//kotlin/compiler:ksp",   # For Room
    ],
  )
  ```
  - Pros: Maximum flexibility
  - Cons: Complexity, execution order matters

- **B: Disallow mixing**
  - Pros: Simpler, clearer error messages
  - Cons: Users may legitimately need both

**Recommendation:** Allow both but document execution order and potential issues.

**Action Required:**
- Test mixed KAPT + KSP in Phase 2
- Document execution order (KAPT runs first, then KSP)
- Warn if both are present?

**Decision deadline:** Phase 2

### Q6: Performance monitoring approach?

**Question:** How should we monitor performance during migration?

**Options:**
- **A: Manual benchmarks**
  - Run before/after on large targets
  - Document in migration log

- **B: Automated performance tests**
  - Add to CI pipeline
  - Fail if >5% regression

**Recommendation:** Option A for Phases 1-2, Option B for Phase 3

**Action:** Create benchmark targets in Phase 1

---

## Related Work

### KAPT Migration (Completed)

The KAPT migration provides the template for this KSP migration:

**Phase 1 (Completed):**
- Added `AnnotationProcessingConfig` protobuf message
- Created `runAnnotationProcessingPlugin()` function
- Made KAPT work through generic path
- Kept legacy path for backward compatibility

**Phase 2 (Completed):**
- Removed KAPT from `InternalCompilerPlugins`
- Removed KAPT from `KotlinToolchain`
- Moved KAPT plugin resolution to Starlark

**Phase 3 (Completed):**
- Deleted `kaptArgs()` function
- Deleted `runKaptPlugin()` function
- Removed all KAPT-specific code
- Updated documentation

**Lessons Learned:**
- ✅ Phased approach works well
- ✅ Dual path during transition is essential
- ✅ Comprehensive testing catches edge cases
- ✅ Documentation updates should happen continuously
- ⚠️ Need better communication with users during migration
- ⚠️ Performance testing should be automated

### Issue #1311: Generic Annotation Processing

**Context:** User request for generic annotation processing system

**Status:** Partially addressed by KAPT migration, will be completed by KSP migration

**Remaining work:**
- KSP migration (this document)
- Support for other annotation processors (future)
- Unified plugin configuration API (future)

### Related Documentation

**Files to reference:**
- `KAPT_MIGRATION_DESIGN.md` - Original KAPT migration plan
- `KAPT_MIGRATION_PHASE1_COMPLETE.md` - Phase 1 completion report
- `KAPT_MIGRATION_PHASE3_COMPLETE.md` - Phase 3 completion report (just created)
- `CLAUDE.md` - Architecture documentation (lines 200-300)

**Files to update:**
- `CLAUDE.md` - Add KSP migration to architecture section
- `README.md` - Update KSP examples
- `docs/kotlin.md` - Update KSP rule documentation

---

## Timeline and Milestones

### Week 1-3: Phase 1 Implementation
- **Week 1:**
  - Day 1-2: Implement `buildKspOptions()` function
  - Day 3-4: Update `runPlugins()` dispatcher
  - Day 5: Initial testing

- **Week 2:**
  - Day 1-2: Update Starlark `_build_annotation_processing_config()`
  - Day 3-4: Integration testing
  - Day 5: Bug fixes

- **Week 3:**
  - Day 1-3: Comprehensive testing (unit + integration)
  - Day 4: Performance testing
  - Day 5: Documentation and code review

**Milestone 1:** KSP works through generic path ✅

### Week 4-6: Phase 2 Implementation
- **Week 4:**
  - Day 1-2: Remove KSP from InternalCompilerPlugins
  - Day 3-4: Remove KSP from KotlinToolchain
  - Day 5: Update tests

- **Week 5:**
  - Day 1-2: Create KSP plugin definition in Starlark
  - Day 3-4: Add implicit KSP plugin support
  - Day 5: Testing and validation

- **Week 6:**
  - Day 1-3: Integration testing without hardcoded KSP
  - Day 4: Performance verification
  - Day 5: Documentation and code review

**Milestone 2:** KSP no longer hardcoded ✅

### Week 7-8: Phase 3 Cleanup
- **Week 7:**
  - Day 1-2: Remove deprecated functions
  - Day 3: Evaluate KspPluginInfo decision
  - Day 4-5: Clean up Starlark layer

- **Week 8:**
  - Day 1-2: Update all documentation
  - Day 3: Create migration guide
  - Day 4: Final comprehensive testing
  - Day 5: Code review and release preparation

**Milestone 3:** Migration complete ✅

### Post-Migration (Week 9+)
- Monitor production usage
- Gather user feedback
- Address any issues
- Consider future improvements

---

## Metrics for Success

### Code Metrics

| Metric | Before | Target | Actual |
|--------|--------|--------|--------|
| Lines of KSP-specific code | ~250 | ~50 | TBD |
| Duplicate KAPT/KSP code | ~100 | 0 | TBD |
| Number of KSP dispatch points | 3 | 1 | TBD |
| Test coverage (KSP) | 75% | 90% | TBD |

### Performance Metrics

| Metric | Baseline | Tolerance | Target |
|--------|----------|-----------|--------|
| Compilation time (small) | 2.5s | ±5% | 2.4-2.6s |
| Compilation time (large) | 15.3s | ±5% | 14.5-16.1s |
| Memory usage | 512MB | ±10% | 460-563MB |
| Action count | 42 | ±0% | 42 |
| Worker reuse rate | 85% | ±5% | 80-90% |

### Quality Metrics

| Metric | Target |
|--------|--------|
| Test pass rate | 100% |
| Code review approval | Required |
| Documentation coverage | 100% |
| No hardcoded references | 0 |
| No deprecated code | 0 |

---

## Approval and Sign-off

### Phase 1 Approval Criteria
- [ ] Technical design reviewed
- [ ] Implementation complete
- [ ] All tests passing
- [ ] Performance verified
- [ ] Documentation updated

**Approvers:** TBD

### Phase 2 Approval Criteria
- [ ] InternalCompilerPlugins refactored
- [ ] KotlinToolchain refactored
- [ ] All tests passing
- [ ] No hardcoded references
- [ ] Documentation updated

**Approvers:** TBD

### Phase 3 Approval Criteria
- [ ] All legacy code removed
- [ ] Full test suite passing
- [ ] Documentation complete
- [ ] Migration guide created
- [ ] Performance benchmarks passed

**Approvers:** TBD

---

## Appendices

### Appendix A: Key File Locations

**Builder Layer:**
- `src/main/kotlin/io/bazel/kotlin/builder/tasks/jvm/CompilationTask.kt` - Main compilation logic
- `src/main/kotlin/io/bazel/kotlin/builder/tasks/jvm/InternalCompilerPlugins.kt` - Internal plugin references
- `src/main/kotlin/io/bazel/kotlin/builder/toolchain/KotlinToolchain.kt` - Toolchain management
- `src/main/kotlin/io/bazel/kotlin/builder/KotlinBuilderComponent.java` - Dagger component

**Starlark Layer:**
- `kotlin/internal/jvm/compile.bzl` - Compilation orchestration
- `kotlin/internal/jvm/jvm.bzl` - Rule definitions
- `kotlin/internal/jvm/plugins.bzl` - Plugin mappers
- `kotlin/compiler/ksp.bzl` - KSP plugin definition (to be created)

**Protobuf:**
- `src/main/protobuf/kotlin_model.proto` - Data model

**Tests:**
- `src/test/kotlin/io/bazel/kotlin/builder/tasks/jvm/KotlinBuilderJvmKspTest.java` - Unit tests
- `src/test/data/jvm/ksp/` - Integration test data

### Appendix B: Git Commit Strategy

**Phase 1 Commits:**
```
1. "ksp: Add buildKspOptions() function"
2. "ksp: Update runPlugins() to dispatch KSP to generic path"
3. "ksp: Update Starlark to build KSP config"
4. "ksp: Add tests for generic KSP path"
5. "ksp: Update documentation for Phase 1"
```

**Phase 2 Commits:**
```
6. "ksp: Remove from InternalCompilerPlugins"
7. "ksp: Remove from KotlinToolchain"
8. "ksp: Create ksp.bzl plugin definition"
9. "ksp: Add implicit KSP plugin support"
10. "ksp: Update tests for Phase 2"
```

**Phase 3 Commits:**
```
11. "ksp: Remove kspArgs() and runKspPlugin() functions"
12. "ksp: Clean up Starlark layer"
13. "ksp: Update all documentation"
14. "ksp: Add migration guide"
15. "ksp: Remove Phase comments and finalize"
```

### Appendix C: Testing Checklist

**Phase 1 Testing:**
- [ ] `buildKspOptions()` unit tests
- [ ] Multi-jar plugin tests
- [ ] Version override tests
- [ ] Generic path integration tests
- [ ] Legacy path still works
- [ ] Performance baseline

**Phase 2 Testing:**
- [ ] No InternalCompilerPlugins.ksp* references
- [ ] Plugin jars loaded from Starlark
- [ ] Implicit plugin addition works
- [ ] All existing tests pass
- [ ] Performance maintained

**Phase 3 Testing:**
- [ ] All KSP tests pass
- [ ] All KAPT tests pass
- [ ] Mixed processors work
- [ ] Examples build
- [ ] Performance benchmarks pass
- [ ] Full test suite passes (67/67)

### Appendix D: Communication Plan

**Internal Communication:**
- Weekly status updates during migration
- Demo sessions after each phase
- Code review notifications

**External Communication:**
- GitHub issue updates (#1311)
- Release notes for each phase
- Migration guide for users
- Blog post after completion (optional)

---

**Document Version:** 1.0
**Last Updated:** 2025-11-08
**Next Review:** After Phase 1 completion
