# Copyright 2018 The Bazel Authors. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""Kotlin Rules

### Setup

Add the following snippet to your `WORKSPACE` file:

```bzl
git_repository(
    name = "io_bazel_rules_kotlin",
    remote = "https://github.com/bazelbuild/rules_kotlin.git",
    commit = "<COMMIT_HASH>",
)
load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kotlin_repositories", "kt_register_toolchains")
kotlin_repositories(kotlin_release_version = "1.2.21")
kt_register_toolchains()
```

To enable persistent worker support, add the following to the appropriate `bazelrc` file:

```
build --strategy=KotlinCompile=worker
test --strategy=KotlinCompile=worker
```


### Standard Libraries

The Kotlin libraries that are bundled in a kotlin release should be used with the rules, the mandatory standard libraries are added implicetly. After enabling
the repository the following Kotlin Libraries are also made available from the workspace `com_github_jetbrains_kotlin`:

* `kotlin-test`,
* `kotlin-reflect`.

So if you needed to add reflect as a dep use the following label `@com_github_jetbrains_kotlin//:kotlin-reflect`.

### Mixed Mode compilation

The JVM rules can compile both Java and Kotlin sources. The Java compiler wrapper is not optimized or persistent and does not have the features found in the
native java rules. This mode is usefull for migrating a package to Kotlin over time.

### Annotation Processing

Annotation processing works just as it does in Java, plugins are declared via a [`java_plugin`](https://docs.bazel.build/versions/master/be/java.html#java_plugin)
and may also be inherited from a `java_library` via the `exported_plugins` attribute. Annotation work in mixed-mode compilation and the Kotlin compiler take
care of processing both aspects.

An example which can be found under `//examples/dagger`:

```bzl
java_plugin(
    name = "dagger_plugin",
    deps = [
        "@dagger_compiler//jar",
        "@guava//jar",
        "@dagger_producers//jar",
        "@dagger//jar",
        "@javax_inject//jar"
    ],
    processor_class = "dagger.internal.codegen.ComponentProcessor"
)

java_library(
    name = "dagger_lib",
    exports = [
        "@javax_inject//jar",
        "@dagger//jar",
    ],
    exported_plugins = ["dagger_plugin"]
)

kt_jvm_binary(
    name = "dagger",
    srcs = glob(["src/**"]),
    main_class = "coffee.CoffeeApp",
    deps = [":dagger_lib"],
)
```
"""
# This file is the main import -- it shouldn't grow out of hand the reason it contains so much allready is due to the limitations of skydoc.

########################################################################################################################
# Common Definitions
########################################################################################################################
load("//kotlin/internal:defs.bzl", _KtJvmInfo="KtJvmInfo", _TOOLCHAIN_TYPE="TOOLCHAIN_TYPE")
# struct can't be used till skydoc is removed
load(
    "//kotlin/internal/jvm:plugins.bzl",
    _kt_jvm_plugin_aspect="kt_jvm_plugin_aspect",
)
# struct can't be used till skydoc is removed
load("//kotlin/internal/jvm:impl.bzl",
    _kt_jvm_binary_impl="kt_jvm_binary_impl",
    _kt_jvm_import_impl="kt_jvm_import_impl",
    _kt_jvm_junit_test_impl="kt_jvm_junit_test_impl",
    _kt_jvm_library_impl="kt_jvm_library_impl",
)


########################################################################################################################
# Rule Attributes
########################################################################################################################
_implicit_deps = {
    "_kotlin_toolchain": attr.label_list(
        default = [
            Label("@io_bazel_rules_kotlin//kotlin:kt_toolchain_ide_info"),
        ],
        cfg="host",
        allow_files = False,
    ),
    "_singlejar": attr.label(
        executable = True,
        cfg = "host",
        default = Label("@bazel_tools//tools/jdk:singlejar"),
        allow_files = True,
    ),
    "_zipper": attr.label(
        executable = True,
        cfg = "host",
        default = Label("@bazel_tools//tools/zip:zipper"),
        allow_files = True,
    ),
    "_java": attr.label(
        executable = True,
        cfg = "host",
        default = Label("@bazel_tools//tools/jdk:java"),
        allow_files = True,
    ),
    "_java_stub_template": attr.label(
        cfg = "host",
        default = Label("@kt_java_stub_template//file"),
    ),
}

_common_attr = dict(_implicit_deps.items() + {
    "srcs": attr.label_list(
        default = [],
        allow_files = [".srcjar", ".kt", ".java"],
    ),
    "deps": attr.label_list(
        aspects = [_kt_jvm_plugin_aspect],
        providers=[
            [JavaInfo],
        ],
        allow_files = False),
    "runtime_deps": attr.label_list(default = [], allow_files=False),
    "resources": attr.label_list(
        default = [],
        allow_files = True,
    ),
    "resource_strip_prefix": attr.string(default = ""),
    "resource_jars": attr.label_list(default = []),
    "data": attr.label_list(
        allow_files = True,
        cfg = "data",
    ),
    "plugins": attr.label_list(
        default = [],
        aspects = [_kt_jvm_plugin_aspect],
    ),
    "module_name": attr.string(
        mandatory = False
    ),
}.items())

_runnable_common_attr = dict(_common_attr.items() + {
    "jvm_flags": attr.string_list(
        default = [],
    ),
}.items())

########################################################################################################################
# Outputs: All the outputs produced by the various rules are modelled here.
########################################################################################################################
_common_outputs = dict(
    jar = "%{name}.jar",
    jdeps = "%{name}.jdeps",
    # The params file, declared here so that validate it can be validated for testing.
#    jar_2_params = "%{name}.jar-2.params",
    srcjar = "%{name}-sources.jar",
)

_binary_outputs = dict(_common_outputs.items() + {
}.items())



########################################################################################################################
# Simple Rules:
########################################################################################################################
kt_jvm_library = rule(
    attrs = dict(_common_attr.items() + {
        "exports": attr.label_list(default = [], providers=[JavaInfo]),
        "neverlink": attr.bool(default=False),
    }.items()),
    outputs = _common_outputs,
    toolchains = [_TOOLCHAIN_TYPE],
    implementation = _kt_jvm_library_impl,
    provides = [JavaInfo, _KtJvmInfo]
)

"""This rule compiles and links Kotlin and Java sources into a .jar file.
Args:
  srcs: The list of source files that are processed to create the target, this can contain both Java and Kotlin files. Java analysis occurs first so Kotlin
    classes may depend on Java classes in the same compilation unit.
  exports: Exported libraries.

    Deps listed here will be made available to other rules, as if the parents explicitly depended on these deps.
    This is not true for regular (non-exported) deps.
  resources: A list of data files to include in a Java jar.
  resource_strip_prefix: The path prefix to strip from Java resources, files residing under common prefix such as `src/main/resources` or `src/test/resources`
    will have stripping applied by convention.
  resource_jars: Set of archives containing Java resources. If specified, the contents of these jars are merged into the output jar.
  runtime_deps: Libraries to make available to the final binary or test at runtime only. Like ordinary deps, these will appear on the runtime classpath, but
    unlike them, not on the compile-time classpath.
  data: The list of files needed by this rule at runtime. See general comments about `data` at [Attributes common to all build rules](https://docs.bazel.build/versions/master/be/common-definitions.html#common-attributes).
  deps: A list of dependencies of this rule.See general comments about `deps` at [Attributes common to all build rules](https://docs.bazel.build/versions/master/be/common-definitions.html#common-attributes).
  module_name: The name of the module, if not provided the module name is derived from the label. --e.g., `//some/package/path:label_name` is translated to
    `some_package_path-label_name`.
  neverlink: If true only use this library for compilation and not at runtime.
"""

kt_jvm_binary = rule(
    attrs = dict(_runnable_common_attr.items() + {
        "main_class": attr.string(mandatory = True)
    }.items()),
    executable = True,
    outputs = _binary_outputs,
    toolchains = [_TOOLCHAIN_TYPE],
    implementation = _kt_jvm_binary_impl,
)

"""Builds a Java archive ("jar file"), plus a wrapper shell script with the same name as the rule. The wrapper shell script uses a classpath that includes,
among other things, a jar file for each library on which the binary depends.

**Note:** This rule does not have all of the features found in [`java_binary`](https://docs.bazel.build/versions/master/be/java.html#java_binary). It is
appropriate for building workspace utilities. `java_binary` should be preferred for release artefacts.

Args:
  main_class: Name of class with main() method to use as entry point.
  jvm_flags: A list of flags to embed in the wrapper script generated for running this binary. Note: does not yet support make variable substitution.
"""

kt_jvm_test = rule(
    attrs = dict(_runnable_common_attr.items() + {
        "_bazel_test_runner": attr.label(
            default = Label("@bazel_tools//tools/jdk:TestRunner_deploy.jar"),
            allow_files = True,
        ),
        "friends": attr.label_list(
            default = [],
            providers = [JavaInfo, _KtJvmInfo]
        ),
        "test_class": attr.string(),
        "main_class": attr.string(default="com.google.testing.junit.runner.BazelTestRunner"),
    }.items()),
    executable = True,
    outputs = _binary_outputs,
    test = True,
    toolchains = [_TOOLCHAIN_TYPE],
    implementation = _kt_jvm_junit_test_impl,
)

"""Setup a simple kotlin_test.

**Notes:**
* The kotlin test library is not added implicitly, it is available with the label `@com_github_jetbrains_kotlin//:kotlin-test`.

Args:
  test_class: The Java class to be loaded by the test runner.
  friends: A single Kotlin dep which allows the test code access to internal members. Currently uses the output jar of
    the module -- i.e., exported deps won't be included.
"""

kt_jvm_import = rule(
    attrs = {
        "jars": attr.label_list(
            allow_files = True,
            mandatory = True,
            cfg = "target",
        ),
        "srcjar": attr.label(
            allow_single_file = True,
            cfg = "target",
        ),
        "runtime_deps": attr.label_list(
            default = [],
            mandatory = False,
            providers = [JavaInfo]
        )
    },
    implementation = _kt_jvm_import_impl,
    provides = [JavaInfo, _KtJvmInfo]
)

# The pairing of src and class is used by intellij to attatch sources, this is picked up via the kt provider attribute.
#
# once current format and semantics are finalized add runtime_deps, exports, data, neverlink, testonly.
#   * runtime_deps should accept JavaInfo's (this includes KotlinInfo) and maven_jar filegroups.
#   * exports should only accept JavaInfo's (this include KotlinInfo) but not filegroup. The jars attribute takes care of importing the jars without generating
#     ijars.
"""(experimental) Import Kotlin jars.

**Status:** This rule is not a counterpart to `java_import`. The current purpose for this rule is to import a kotlin jar without creating ijars. It will
eventually [be replaced](https://github.com/bazelbuild/rules_kotlin/issues/4) with `java_import`. If there is a need for expanding this rule we can instead
create a utility macro that delegates to this.

## examples

```bzl
# Old style usage -- reference file groups, do not used this.
kt_jvm_import(
    name = "kodein",
    jars = [
        "@com_github_salomonbrys_kodein_kodein//jar:file",
        "@com_github_salomonbrys_kodein_kodein_core//jar:file"
    ]
)

# This style will pull in the transitive runtime dependencies of the targets as well.
kt_jvm_import(
    name = "kodein",
    jars = [
        "@com_github_salomonbrys_kodein_kodein//jar",
        "@com_github_salomonbrys_kodein_kodein_core//jar"
    ]
)

# Import a single kotlin jar.
kt_jvm_import(
    name = "kotlin-runtime",
    jars = ["lib/kotlin-runtime.jar"],
    srcjar = "lib/kotlin-runtime-sources.jar"
)
```

Args:
  jars: The jars listed here are equavalent to an export attribute. The label should be either to a single class jar, or multiple filegroup labels. When the
    labels is a file_provider it should follow the conventions used in repositories generated by the maven_jar rule  --i.e., the rule expects a file_provider
    with a single class jar and a single source jar. a source jar is recognized by the suffix `-sources.jar`.
  srcjar: The sources for the class jar. This should be set when importing a single class jar.
  runtime_deps: Additional runtime deps.
"""
