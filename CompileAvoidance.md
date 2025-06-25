[![Build Status](https://badge.buildkite.com/a8860e94a7378491ce8f50480e3605b49eb2558cfa851bbf9b.svg)](https://buildkite.com/bazel/kotlin-postsubmit)

# Compilation Avoidance

# ABI support

**rules_kotlin** now supports compilation avoidance through the use of ABI jars (also known as
interface or header jars) for classpath dependencies. This can have significant performance wins for
non-ABI affecting changes since down stream recompilation can be avoided in such cases. 

This feature can be enabled through the `experimental_use_abi_jars` flag in the tool chain as
follows

```python
load("//kotlin:core.bzl", "define_kt_toolchain")


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
load("//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "framework",
    tags = ["kt_abi_plugin_incompatible"],
)
```

Depending on your compiler version you may find non-public symbols in your ABI jar such as [internal declarations](https://youtrack.jetbrains.com/issue/KT-65690) or [effectively private classes](https://youtrack.jetbrains.com/issue/KT-64590). 
Exclusion of these symbols from the ABI jar can be enabled through the `experimental_treat_internal_as_private_in_abi_jars` and `experimental_remove_private_classes_in_abi_jars` flags in addition to `experimental_use_abi_jars` flag when defining the toolchain as follows

```python
load("//kotlin:core.bzl", "define_kt_toolchain")


define_kt_toolchain(
    name = "kotlin_toolchain",
    experimental_use_abi_jars = True,
    experimental_treat_internal_as_private_in_abi_jars = True,
    experimental_remove_private_classes_in_abi_jars = True,
)
```

In order to completely remove debug information from the ABI jar, the `experimental_remove_debug_info_in_abi_jars` flag can be used along with `experimental_use_abi_jars`

```python
load("//kotlin:core.bzl", "define_kt_toolchain")


define_kt_toolchain(
    name = "kotlin_toolchain",
    experimental_use_abi_jars = True,
    experimental_remove_debug_info_in_abi_jars = True,
)
```

If you encounter bugs in older compiler versions such as [KT-71525](https://youtrack.jetbrains.com/issue/KT-71525) then ABI generation with only public symbols can be disabled on a per target basis by setting the following tags

```python
load("//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "framework",
    tags = [
        "kt_treat_internal_as_private_in_abi_plugin_incompatible", 
        "kt_remove_private_classes_in_abi_plugin_incompatible", 
        "kt_remove_debug_info_in_abi_plugin_incompatible"
    ],
)
```
