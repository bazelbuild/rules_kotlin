[![Build Status](https://badge.buildkite.com/72a7641b782f6f365efb775d7efb6b6ac4ea33f7db4ae7db55.svg)](https://buildkite.com/christian-gruber-open-source-stuffs/gruber-rules-kotlin-presubmit)

# A fork of the legacy branch of bazelbuild/rules_kotlin

Current version: ***`legacy_modded-0_26_1-01`***

# Announcements
* <b>June 12, 2019.</b> Kotlin 1.3 support, and release of `legacy_modded-0_26_1-01`
* <b>June 11, 2019.</b> Fix to kotlin worker to allow modern dagger to be used in kapt. (worker was leaking its dagger dep)
* <b>May 17, 2019.</b> More changes from upstream (mostly fixes for bazel version bump) and releases from the fork
* <b>April 1, 2019.</b> [Roadmap](https://github.com/bazelbuild/rules_kotlin/blob/master/ROADMAP.md) for rules_kotlin published.  The `cgruber` fork will continue until the legacy branch in the parent repo can be updated (or no one needs the fork)
* <b>February 20, 2019.</b> [Future directions](https://github.com/bazelbuild/rules_kotlin/issues/174) of rules_kotlin.
* <b>October 24, 2018.</b> Christian Gruber forks the main kotlin rules repository, adding in two fixes.
* <b>August 14, 2018.</b> Js support. No documentation yet but see the nested example workspace `examples/node`.
* <b>August 14, 2018.</b> Android support. No documentation but it's a simple integration. see 
  `kotlin/internal/jvm/android.bzl`.
* <b>Jun 29, 2018.</b> The commits from this date forward are compatible with bazel `>=0.14`. JDK9 host issues were 
  fixed as well some other deprecations. I recommend skipping `0.15.0` if you   are on a Mac. 
* <b>May 25, 2018.</b> Test "friend" support. A single friend dep can be provided to `kt_jvm_test` which allows the test
  to access internal members of the module under test.
* <b>February 15, 2018.</b> Toolchains for the JVM rules. Currently this allow tweaking: 
    * The JVM target (bytecode level).
    * API and Language levels.
    * Coroutines, enabled by default. 
* <b>February 9, 2018.</b> Annotation processing.
* <b>February 5, 2018. JVM rule name change:</b> the prefix has changed from `kotlin_` to `kt_jvm_`.

# Overview 

These rules were initially forked from [pubref/rules_kotlin](http://github.com/pubref/rules_kotlin), and then re-forked from [bazelbuild/rules_kotlin](http://github.com/bazelbuild/rules_kotlin)

Key changes:

* Replace the macros with three basic rules. `kt_jvm_binary`, `kt_jvm_library` and `kt_jvm_test`.
* Android rules. `kt_android_library` and `kt_android_binary`
* Friend support for tests (supports access to `internal` types and functions)
* Use a single `deps` attribute instead of `java_dep` and `dep`.
* Add support for the following standard java rules attributes:
  * `data`
  * `resource_jars`
  * `runtime_deps`
  * `resources`
  * `resources_strip_prefix`
  * `exports`
* Persistent worker support.
* Mixed-Mode compilation (compile Java and Kotlin in one pass).
* Configurable Kotlinc distribtution and verison
* Configurable Toolchain
* Kotlin 1.3 support

# Quick Guide

## WORKSPACE
In the project's `WORKSPACE`, declare the external repository and initialize the toolchains, like
this:

```build
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

rules_kotlin_version = "legacy-modded-0_26_1-01"
rules_kotlin_sha = "b943379a6b48156cb542cee4502a463a6e2edeb307d1a4cabd0309ca2bf3f10f"
http_archive(
    name = "io_bazel_rules_kotlin",
    urls = ["https://github.com/cgruber/rules_kotlin/archive/%s.zip" % rules_kotlin_version],
    type = "zip",
    strip_prefix = "rules_kotlin-%s" % rules_kotlin_version,
    sha256 = rules_kotlin_sha,
)

load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kotlin_repositories", "kt_register_toolchains")
kotlin_repositories() # if you want the default. Otherwise see custom kotlinc distribution below
kt_register_toolchains() # to use the default toolchain, otherwise see toolchains below
```

## BUILD files

In your project's `BUILD` files, load the kotlin rules and use them like so:

```
load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

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

```
load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "define_kt_toolchain")

define_kt_toolchain(
    name = "kotlin_toolchain",
    api_version = KOTLIN_LANGUAGE_LEVEL,  # "1.1", "1.2", or "1.3"
    jvm_target = JAVA_LANGUAGE_LEVEL, # "1.6" or "1.8"
    language_version = KOTLIN_LANGUAGE_LEVEL,  # "1.1", "1.2", or "1.3"
)
```

and then in your `WORKSPACE` file, instead of `kt_register_toolchains()` do

```
register_toolchains("//:kotlin_toolchain")
```

## Custom kotlinc distribution (and version)

To choose a different kotlinc distribution (only 1.3 variants supported), do the following
in your `WORKSPACE` file (or import from a `.bzl` file:

```
load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kotlin_repositories")

KOTLIN_VERSION = "1.3.31"
KOTLINC_RELEASE_SHA = "107325d56315af4f59ff28db6837d03c2660088e3efeb7d4e41f3e01bb848d6a"

KOTLINC_RELEASE = {
    "urls": [
        "https://github.com/JetBrains/kotlin/releases/download/v{v}/kotlin-compiler-{v}.zip".format(v = KOTLIN_VERSION),
    ],
    "sha256": KOTLINC_RELEASE_SHA,
}

kotlin_repositories(compiler_release = KOTLINC_RELEASE)
```

# License

This project is licensed under the [Apache 2.0 license](LICENSE), as are all contributions

# Contributing

See the [CONTRIBUTING](CONTRIBUTING.md) doc for information about how to contribute to
this project.

