# Bazel Kotlin Rules

[![Build Status](https://badge.buildkite.com/a8860e94a7378491ce8f50480e3605b49eb2558cfa851bbf9b.svg)](https://buildkite.com/bazel/kotlin-postsubmit)
<a href="https://github.com/bazelbuild/rules_kotlin/tree/master"><img src="https://img.shields.io/badge/Main%20Branch-master-blue.svg"/></a>
###  Releases

<a href="https://github.com/bazelbuild/rules_kotlin/releases/latest"><img src="https://img.shields.io/github/v/release/bazelbuild/rules_kotlin?display_name=tag&label=Latest%20Stable%20Release"/></a>
<br/>
<a href="https://github.com/bazelbuild/rules_kotlin/releases/"><img src="https://img.shields.io/github/v/release/bazelbuild/rules_kotlin?display_name=tag&include_prereleases&label=Latest%20Release%20Candidate"/></a>

For more information about release and changelogs please see [Changelog](CHANGELOG.md) or refer to the [Github Releases](https://github.com/bazelbuild/rules_kotlin/releases) page.

# `rules_kotlin` Bazel Compatibility
- HEAD: `6`, `7`
- [Release 1.9](https://github.com/bazelbuild/rules_kotlin/releases/tag/v1.9.1): `6`, `7`
- [Release 1.8](https://github.com/bazelbuild/rules_kotlin/releases/tag/v1.8.1): `5`, `6`
- [Release 1.7](https://github.com/bazelbuild/rules_kotlin/releases/tag/v1.7.1): `4`, `5`, `6`

# Overview 

**rules_kotlin** supports the basic paradigm of `*_binary`, `*_library`, `*_test` of other Bazel 
language rules. It also supports `jvm`, `android`, and `js` flavors, with the prefix `kt_jvm`
and `kt_android` typically applied to the rules.

Support for kotlin's -Xfriend-paths via the `associates=` attribute in the jvm allow access to
`internal` members.

Also, `kt_jvm_*` rules support the following standard `java_*` rules attributes:
  * `data`
  * `resource_jars`
  * `runtime_deps`
  * `resources`
  * `resources_strip_prefix`
  * `exports`
  
Android rules also support custom_package for `R.java` generation, `manifest=`, `resource_files`, etc.

Other features:
  * Persistent worker support.
  * Mixed-Mode compilation (compile Java and Kotlin in one pass).
  * Configurable Kotlinc distribution and version
  * Configurable Toolchain
  * Support for all recent major Kotlin releases.
  
Javascript is reported to work, but is not as well maintained (at present)

# Documentation

Generated API documentation is available at
[https://bazelbuild.github.io/rules_kotlin/kotlin](https://bazelbuild.github.io/rules_kotlin/kotlin).

# Quick Guide

## `WORKSPACE`
In the project's `WORKSPACE`, declare the external repository and initialize the toolchains, like
this:

```python
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

rules_kotlin_version = "1.9.0"
rules_kotlin_sha = "5766f1e599acf551aa56f49dab9ab9108269b03c557496c54acaf41f98e2b8d6"
http_archive(
    name = "rules_kotlin",
    urls = ["https://github.com/bazelbuild/rules_kotlin/releases/download/v%s/rules_kotlin-v%s.tar.gz" % (rules_kotlin_version, rules_kotlin_version)],
    sha256 = rules_kotlin_sha,
)

load("@rules_kotlin//kotlin:repositories.bzl", "kotlin_repositories")
kotlin_repositories() # if you want the default. Otherwise see custom kotlinc distribution below

load("@rules_kotlin//kotlin:core.bzl", "kt_register_toolchains")
kt_register_toolchains() # to use the default toolchain, otherwise see toolchains below
```

## `BUILD` files

In your project's `BUILD` files, load the Kotlin rules and use them like so:

```python
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "package_name",
    srcs = glob(["*.kt"]),
    deps = [
        "//path/to/dependency",
    ],
)
```

## Custom toolchain

To enable a custom toolchain (to configure language level, etc.)
do the following.  In a `<workspace>/BUILD.bazel` file define the following:

```python
load("@rules_kotlin//kotlin:core.bzl", "define_kt_toolchain")

define_kt_toolchain(
    name = "kotlin_toolchain",
    api_version = KOTLIN_LANGUAGE_LEVEL,  # "1.1", "1.2", "1.3", "1.4", "1.5" "1.6", "1.7", "1.8", or "1.9"
    jvm_target = JAVA_LANGUAGE_LEVEL, # "1.6", "1.8", "9", "10", "11", "12", "13", "15", "16", "17", "18", "19", "20" or "21"
    language_version = KOTLIN_LANGUAGE_LEVEL,  # "1.1", "1.2", "1.3", "1.4", "1.5" "1.6", "1.7", "1.8", or "1.9"
)
```

and then in your `WORKSPACE` file, instead of `kt_register_toolchains()` do

```python
register_toolchains("//:kotlin_toolchain")
```

## Custom `kotlinc` distribution (and version)

To choose a different `kotlinc` distribution (1.3 and 1.4 variants supported), do the following
in your `WORKSPACE` file (or import from a `.bzl` file:

```python
load("@rules_kotlin//kotlin:repositories.bzl", "kotlin_repositories", "kotlinc_version")

kotlin_repositories(
    compiler_release = kotlinc_version(
        release = "1.6.21", # just the numeric version
        sha256 = "632166fed89f3f430482f5aa07f2e20b923b72ef688c8f5a7df3aa1502c6d8ba"
    )
)
```

## Third party dependencies 
_(e.g. Maven artifacts)_

Third party (external) artifacts can be brought in with systems such as [`rules_jvm_external`](https://github.com/bazelbuild/rules_jvm_external) or [`bazel_maven_repository`](https://github.com/square/bazel_maven_repository) or [`bazel-deps`](https://github.com/johnynek/bazel-deps), but make sure the version you use doesn't naively use `java_import`, as this will cause bazel to make an interface-only (`ijar`), or ABI jar, and the native `ijar` tool does not know about kotlin metadata with respect to inlined functions, and will remove method bodies inappropriately.  Recent versions of `rules_jvm_external` and `bazel_maven_repository` are known to work with Kotlin.

# Development Setup Guide
As of 1.5.0, to use the rules directly from the rules_kotlin workspace (i.e. not the release artifact)
 require the use of `release_archive` repository. This repository will build and configure the current
workspace to use `rules_kotlin` in the same manner as the released binary artifact.

In the project's `WORKSPACE`, change the setup:

```python

# Use local check-out of repo rules (or a commit-archive from github via http_archive or git_repository)
local_repository(
    name = "rules_kotlin",
    path = "../path/to/rules_kotlin_clone/",
)

load("@rules_kotlin//kotlin:dependencies.bzl", "kt_download_local_dev_dependencies")

kt_download_local_dev_dependencies()

load("@rules_kotlin//kotlin:repositories.bzl", "kotlin_repositories", "versions")

kotlin_repositories()

load("@rules_kotlin//kotlin:core.bzl", "kt_register_toolchains")

kt_register_toolchains()
```

To use rules_kotlin from head without cloning the repository, (caveat emptor, of course), change the
rules to this:

```python
# Download master or specific revisions
http_archive(
    name = "rules_kotlin",
    strip_prefix = "rules_kotlin-master",
    urls = ["https://github.com/bazelbuild/rules_kotlin/archive/refs/heads/master.zip"],
)

load("@rules_kotlin//kotlin:repositories.bzl", "kotlin_repositories")

kotlin_repositories()

load("@rules_kotlin//kotlin:core.bzl", "kt_register_toolchains")

kt_register_toolchains()
```

# Debugging native actions

To attach debugger and step through native action code when using local checkout of rules_kotlin repo :

1. Open `rules_kotlin` project in Android Studio, using existing `.bazelproject` file as project view.
2. In terminal, build the kotlin target you want to debug, using the subcommand option, ex: `bazel build //lib/mylib:main_kt -s`. You can also use `bazel aquery` to get this info.
3. Locate the subcommand for the kotlin action you want to debug, let's say `KotlinCompile`. Note: If you don't see it, target rebuild may have been skipped (in this case `touch` one of the source .kt file to trigger rebuild).
4. Export `REPOSITORY_NAME` as specified in action env, ex : `export REPOSITORY_NAME=rules_kotlin`
5. Copy the command line, ex : `bazel-out/darwin_arm64-opt-exec-2B5CBBC6/bin/external/rules_kotlin/src/main/kotlin/build '--flagfile=bazel-out/darwin_arm64-fastbuild/bin/lib/mylib/main_kt-kt.jar-0.params'`
6. Change directory into the [execRoot](https://bazel.build/remote/output-directories#layout-diagram), normally `bazel-MYPROJECT`, available via `bazel info | grep execution_root`.
7. Add `--debug=5005` to command line to make the action wait for a debugger to attach, ex: `bazel-out/darwin_arm64-opt-exec-2B5CBBC6/bin/external/rules_kotlin/src/main/kotlin/build --debug=5005 '--flagfile=bazel-out/darwin_arm64-fastbuild/bin/lib/mylib/main_kt-kt.jar-0.params'`. Note: if command invokes `java` toolchain directly, use `-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005` instead.
8. You should see in output that action is waiting for debugger. Use a default `Remote JVM Debug` configuration in Android Studio, set breakpoint in kotlin action java/kt code, and attach debugger. Breakpoints should be hit.

# Kotlin and Java compiler flags

The `kt_kotlinc_options` and `kt_javac_options` rules allows passing compiler flags to kotlinc and javac.

Note: Not all compiler flags are supported in all language versions. When this happens, the rules will fail.

For example you can define global compiler flags by doing: 
```python
load("@rules_kotlin//kotlin:core.bzl", "kt_kotlinc_options", "kt_javac_options", "define_kt_toolchain")

kt_kotlinc_options(
    name = "kt_kotlinc_options",
    x_no_param_assertions = True,
    jvm_target = "1.8",
)

kt_javac_options(
    name = "kt_javac_options",
    warn = "off",
)

define_kt_toolchain(
    name = "kotlin_toolchain",
    kotlinc_options = "//:kt_kotlinc_options",
    javac_options = "//:kt_javac_options",
)
```

You can optionally override compiler flags at the target level by providing an alternative set of `kt_kotlinc_options` or `kt_javac_options` in your target definitions.

Compiler flags that are passed to the rule definitions will be taken over the toolchain definition.

Example:
```python
load("@rules_kotlin//kotlin:core.bzl", "kt_kotlinc_options", "kt_javac_options")
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_kotlinc_options(
    name = "kt_kotlinc_options_for_package_name",
    x_no_param_assertions = True,
    x_optin = [
        "kotlin.Experimental",
        "kotlin.ExperimentalStdlibApi",
    ],
)

kt_javac_options(
    name = "kt_javac_options_for_package_name",
    warn = "off"
)

kt_jvm_library(
    name = "package_name",
    srcs = glob(["*.kt"]),
    kotlinc_opts = "//:kt_kotlinc_options_for_package_name",
    javac_opts = "//:kt_javac_options_for_package_name",
    deps = ["//path/to/dependency"],
)
```

Additionally, you can add options for both tracing and timing of the bazel build using the `kt_trace` and `kt_timings` flags, for example:
* `bazel build --define=kt_trace=1`
* `bazel build --define=kt_timings=1`

`kt_trace=1` will allow you to inspect the full kotlinc commandline invocation, while `kt_timings=1` will report the high level time taken for each step.

# KSP (Kotlin Symbol Processing)

KSP is officially supported as of `rules_kotlin` 1.8 and can be declared using the new
`kt_ksp_plugin` rule. 

Note:
- KSP is [not yet thread safe](https://github.com/google/ksp/issues/1385) and will likely fail if you are using it in a build that has multiplex workers enabled. To work around this add the following flag to your Bazelrc:
  ```
  build --experimental_worker_max_multiplex_instances=KotlinKsp=1
  ```

```python
load("@rules_kotlin//kotlin:core.bzl", "kt_ksp_plugin")
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_ksp_plugin(
    name = "moshi-kotlin-codegen",
    processor_class = "com.squareup.moshi.kotlin.codegen.ksp.JsonClassSymbolProcessorProvider",
    deps = [
        "@maven//:com_squareup_moshi_moshi",
        "@maven//:com_squareup_moshi_moshi_kotlin",
        "@maven//:com_squareup_moshi_moshi_kotlin_codegen",
    ],
)

kt_jvm_library(
    name = "lib",
    srcs = glob(["*.kt"]),
    plugins = ["//:moshi-kotlin-codegen"],
)
```

To choose a different `ksp_version` distribution,
do the following in your `WORKSPACE` file (or import from a `.bzl` file):

```python
load("@rules_kotlin//kotlin:repositories.bzl", "kotlin_repositories", "ksp_version")

kotlin_repositories(
    ksp_compiler_release = ksp_version(
        release = "1.8.22-1.0.11",
        sha256 = "2ce5a08fddd20ef07ac051615905453fe08c3ba3ce5afa5dc43a9b77aa64507d",
    ),
)
```

# Kotlin compiler plugins

The `kt_compiler_plugin` rule allows running Kotlin compiler plugins, such as no-arg, sam-with-receiver and allopen.

For example, you can add allopen to your project like this:
```python
load("@rules_kotlin//kotlin:core.bzl", "kt_compiler_plugin")
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_compiler_plugin(
    name = "open_for_testing_plugin",
    id = "org.jetbrains.kotlin.allopen",
    options = {
        "annotation": "plugin.allopen.OpenForTesting",
    },
    deps = [
        "//kotlin/compiler:allopen-compiler-plugin",
    ],
)

kt_jvm_library(
    name = "user",
    srcs = ["User.kt"], # The User class is annotated with OpenForTesting
    plugins = [
        ":open_for_testing_plugin",
    ],
    deps = [
        ":open_for_testing", # This contains the annotation (plugin.allopen.OpenForTesting)
    ],
)
```

Full examples of using compiler plugins can be found [here](examples/plugin).

## Creating Rule Releases

A new release can be published by just pushing a tag.

Once the tag is pushed, GitHub Actions will build, test, and publish a release to both GitHub releases and the BCR.

A tag can be created and pushed by doing the following:
```
git tag v4.13
git push origin v4.13
```


## Examples

Examples can be found in the [examples directory](https://github.com/bazelbuild/rules_kotlin/tree/master/examples), including usage with Android, Dagger, Node-JS, Kotlin compiler plugins, etc.

# History

These rules were initially forked from [pubref/rules_kotlin](http://github.com/pubref/rules_kotlin), and then re-forked from [bazelbuild/rules_kotlin](http://github.com/bazelbuild/rules_kotlin). They were merged back into this repository in October, 2019.

# License

This project is licensed under the [Apache 2.0 license](LICENSE), as are all contributions

# Contributing

See the [CONTRIBUTING](CONTRIBUTING.md) doc for information about how to contribute to
this project.
