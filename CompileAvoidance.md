[![Build Status](https://badge.buildkite.com/a8860e94a7378491ce8f50480e3605b49eb2558cfa851bbf9b.svg)](https://buildkite.com/bazel/kotlin-postsubmit)

# Compilation Avoidance

# ABI support

**rules_kotlin** now supports compilation avoidance through the use of ABI jars (also known as
interface or header jars) for classpath dependencies. This can have significant performance wins for
non-ABI affecting changes since down stream recompilation can be avoided in such cases. 

This feature can be enabled through the `experimental_use_abi_jars` flag in the tool chain as
follows

```python
load("//kotlin:kotlin.bzl", "define_kt_toolchain")


define_kt_toolchain(
    name = "kotlin_toolchain",
    experimental_use_abi_jars = True,
)
```

This feature is implemented using the Jetbrains Kotlin JvmABI compiler plugin for generating headers
for Kotlin code. Unfortunately there are some known bugs with this plugin that affect less than 1%
of targets.
* https://youtrack.jetbrains.com/issue/KT-40340
* https://youtrack.jetbrains.com/issue/KT-40133

If you encounter such bugs, ABI generation can be disabled on a per target basis by setting the
following tag

```python
load("//kotlin:kotlin.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "framework",
    tags = ["kt_abi_plugin_incompatible"],
)
```
