[![Build Status](https://badge.buildkite.com/a8860e94a7378491ce8f50480e3605b49eb2558cfa851bbf9b.svg)](https://buildkite.com/bazel/kotlin-postsubmit)

# Bazel Kotlin Rules

Current release: ***`TBD`***<br />
Main branch: `master`

# News!
* <b>Oct 5, 2019.</b> github.com/cgruber/rules_kotlin upstreamed into this repository. 

For older news, please see [Changelog](CHANGELOG.md)

# Overview 

**rules_kotlin** supports the basic paradigm of `*_binary`, `*_library`, `*_test` of other bazel 
language rules. It also supports `jvm`, `android`, and `js` flavors, with the prefix `kt_jvm`
and `kt_js`, and `kt_android` typically applied to the rules (the exception being 
`kt_android_local_test`, which doesn't exist. Use an `android_local_test` that takes a 
`kt_android_library` as a dependency).

Friend support for tests and other use-cases (supports access to `internal` types and functions)

Also, jvm rules support the following standard java rules attributes:
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
  * Configurable Kotlinc distribtution and version
  * Configurable Toolchain
  * Kotlin 1.3 support
  
Javascript is reported to work, but is not as well maintained (at present)

# Quick Guide

## WORKSPACE
In the project's `WORKSPACE`, declare the external repository and initialize the toolchains, like
this:

```build
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

rules_kotlin_version = "legacy-modded-1_0_0-01"
rules_kotlin_sha = "b7984b28e0a1e010e225a3ecdf0f49588b7b9365640af783bd01256585cbb3ae"
http_archive(
    name = "io_bazel_rules_kotlin",
    urls = ["https://github.com/bazelbuild/rules_kotlin/archive/%s.zip" % rules_kotlin_version],
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

KOTLIN_VERSION = "1.3.50"
KOTLINC_RELEASE_SHA = "69424091a6b7f52d93eed8bba2ace921b02b113dbb71388d704f8180a6bdc6ec"

KOTLINC_RELEASE = {
    "urls": [
        "https://github.com/JetBrains/kotlin/releases/download/v{v}/kotlin-compiler-{v}.zip".format(v = KOTLIN_VERSION),
    ],
    "sha256": KOTLINC_RELEASE_SHA,
}

kotlin_repositories(compiler_release = KOTLINC_RELEASE)
```

## Friends/Internal support

The rules support kotlin `internal` visibility (usually accessible only to the compilation unit)
by allowing a library or test to specify that it is friends with another library like so:

```
kt_jvm_library(
    name = "foo",
    srcs = glob(["*.kt"]),
    friend = "//some/other:target",
    # ...
)
```

> Note: declaring friends of a kt_android_library requires adding `_kt` to the target, e.g.
> `//some/other:target_kt`.  This is because the `kt_android_library` rule is a macro with a
> `kt_jvm_library` under the hood, and the surfaced rule is an android rule which doesn't have
> kotlin aware bazel providers. This will be fixed in the future.

This grants access to `internal` members of `//some/other:target`. A library can only be friends
with one other library, which must be a kotlin-aware target. It inherits the module name from that
library. Only one friend can be declared, because of the assumptions made here about module
membership.  Since friendship can be transitive (see below), this constrains the visibility so it
does not become an arbitrarily growing lattice of trust, defeating the purpose.

Very common use-cases for this are:

  * tests and test-libraries depending on internals of the system under test.
  * clusters of targets that represent one logical unit, with public, private,
     fake, testing-utilities, configuration, or other targets that comprise the
     whole unit.

Friendship has limited transitivity. Consider projects `C`, `B`, and `A` with a dependency
relationship `C->B->A`.  If `C` declares friendship in `B`, and `B` declares friendship with `A`,
then they are all treated as logically one module for `internal` purposes. However it doesn't
skip. Once the line of friendship is broken, a separate module is presumed by kotlinc. 

## Third party dependencies 
_(e.g. maven artifacts)_

Third party (external) artifacts can be brought in with systems such as [rules_jvm_external](https://github.com/bazelbuild/rules_jvm_external) or [bazel_maven_repository](https://github.com/square/bazel_maven_repository) or [bazel-deps](https://github.com/johnynek/bazel-deps), but make sure the version you use doesn't naively use java_import, as this will cause bazel to make an interface-only (ijar), or ABI jar, and the native ijar tool does not know about kotlin metadata with respect to inlined functions, and will remove method bodies inappropriately.  Recent versions of [rules_jvm_external] and [bazel_maven_repository] are known to work with kotlin.

# History

These rules were initially forked from [pubref/rules_kotlin](http://github.com/pubref/rules_kotlin), and then re-forked from [bazelbuild/rules_kotlin](http://github.com/bazelbuild/rules_kotlin). They were merged back into this repository in October, 2019.

# License

This project is licensed under the [Apache 2.0 license](LICENSE), as are all contributions

# Contributing

See the [CONTRIBUTING](CONTRIBUTING.md) doc for information about how to contribute to
this project.

