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

load(
    "//kotlin/internal:defs.bzl",
    _KT_COMPILER_REPO = "KT_COMPILER_REPO",
    _KtJvmInfo = "KtJvmInfo",
    _TOOLCHAIN_TYPE = "TOOLCHAIN_TYPE",
)
load(
    "//kotlin/internal/jvm:plugins.bzl",
    _kt_jvm_plugin_aspect = "kt_jvm_plugin_aspect",
)
load(
    "//kotlin/internal/jvm:impl.bzl",
    _kt_jvm_binary_impl = "kt_jvm_binary_impl",
    _kt_jvm_import_impl = "kt_jvm_import_impl",
    _kt_jvm_junit_test_impl = "kt_jvm_junit_test_impl",
    _kt_jvm_library_impl = "kt_jvm_library_impl",
)
load("//kotlin/internal/utils:utils.bzl", "utils")

_implicit_deps = {
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
    "_java_runtime": attr.label(
        default = Label("@bazel_tools//tools/jdk:current_java_runtime"),
    ),
    "_java_stub_template": attr.label(
        cfg = "host",
        default = Label("@kt_java_stub_template//file"),
    ),
    "_toolchain": attr.label(
        doc = """The Kotlin JVM Runtime. it's only purpose is to enable the Android native rules to discover the Kotlin
        runtime for dexing""",
        default = Label("@" + _KT_COMPILER_REPO + "//:kotlin-stdlib"),
        cfg = "target",
    ),
}

_common_attr = utils.add_dicts(
    _implicit_deps,
    {
        "srcs": attr.label_list(
            doc = """The list of source files that are processed to create the target, this can contain both Java and Kotlin
        files. Java analysis occurs first so Kotlin classes may depend on Java classes in the same compilation unit.""",
            default = [],
            allow_files = [".srcjar", ".kt", ".java"],
        ),
        "deps": attr.label_list(
            doc = """A list of dependencies of this rule.See general comments about `deps` at
        [Attributes common to all build rules](https://docs.bazel.build/versions/master/be/common-definitions.html#common-attributes).""",
            aspects = [_kt_jvm_plugin_aspect],
            providers = [
                [JavaInfo],
            ],
            allow_files = False,
        ),
        "runtime_deps": attr.label_list(
            doc = """Libraries to make available to the final binary or test at runtime only. Like ordinary deps, these will
        appear on the runtime classpath, but unlike them, not on the compile-time classpath.""",
            default = [],
            allow_files = False,
        ),
        "friend": attr.label(
            doc = """A single Kotlin dep which allows this code to access internal members of the given dependency.
             Currently uses the output jar of the module -- i.e., exported deps won't be included.""",
            providers = [JavaInfo, _KtJvmInfo],
        ),
        "resources": attr.label_list(
            doc = """A list of files that should be include in a Java jar.""",
            default = [],
            allow_files = True,
        ),
        "resource_strip_prefix": attr.string(
            doc = """The path prefix to strip from Java resources, files residing under common prefix such as
        `src/main/resources` or `src/test/resources` will have stripping applied by convention.""",
            default = "",
        ),
        "resource_jars": attr.label_list(
            doc = """Set of archives containing Java resources. If specified, the contents of these jars are merged into
        the output jar.""",
            default = [],
        ),
        "data": attr.label_list(
            doc = """The list of files needed by this rule at runtime. See general comments about `data` at
        [Attributes common to all build rules](https://docs.bazel.build/versions/master/be/common-definitions.html#common-attributes).""",
            allow_files = True,
        ),
        "plugins": attr.label_list(
            default = [],
            aspects = [_kt_jvm_plugin_aspect],
        ),
        "module_name": attr.string(
            doc = """The name of the module, if not provided the module name is derived from the label. --e.g.,
        `//some/package/path:label_name` is translated to
        `some_package_path-label_name`.""",
            mandatory = False,
        ),
    },
)

_lib_common_attr = utils.add_dicts(_common_attr, {
    "exports": attr.label_list(
        doc = """Exported libraries.

        Deps listed here will be made available to other rules, as if the parents explicitly depended on
        these deps. This is not true for regular (non-exported) deps.""",
        default = [],
        providers = [JavaInfo],
    ),
    "neverlink": attr.bool(
        doc = """If true only use this library for compilation and not at runtime.""",
        default = False,
    ),
})

_runnable_common_attr = utils.add_dicts(_common_attr, {
    "jvm_flags": attr.string_list(
        doc = """A list of flags to embed in the wrapper script generated for running this binary. Note: does not yet
        support make variable substitution.""",
        default = [],
    ),
})

_common_outputs = dict(
    jar = "%{name}.jar",
    jdeps = "%{name}.jdeps",
    # The params file, declared here so that validate it can be validated for testing.
    #    jar_2_params = "%{name}.jar-2.params",
    srcjar = "%{name}-sources.jar",
)

kt_jvm_library = rule(
    doc = """This rule compiles and links Kotlin and Java sources into a .jar file.""",
    attrs = _lib_common_attr,
    outputs = _common_outputs,
    toolchains = [_TOOLCHAIN_TYPE],
    implementation = _kt_jvm_library_impl,
    provides = [JavaInfo, _KtJvmInfo],
)

kt_jvm_binary = rule(
    doc = """Builds a Java archive ("jar file"), plus a wrapper shell script with the same name as the rule. The wrapper
    shell script uses a classpath that includes, among other things, a jar file for each library on which the binary
    depends.

    **Note:** This rule does not have all of the features found in [`java_binary`](https://docs.bazel.build/versions/master/be/java.html#java_binary).
    It is appropriate for building workspace utilities. `java_binary` should be preferred for release artefacts.
    """,
    attrs = dict(_runnable_common_attr.items() + {
        "main_class": attr.string(
            doc = """Name of class with main() method to use as entry point.""",
            mandatory = True,
        ),
    }.items()),
    executable = True,
    outputs = _common_outputs,
    toolchains = [_TOOLCHAIN_TYPE],
    implementation = _kt_jvm_binary_impl,
)

kt_jvm_test = rule(
    doc = """Setup a simple kotlin_test.

    **Notes:**
    * The kotlin test library is not added implicitly, it is available with the label
    `@com_github_jetbrains_kotlin//:kotlin-test`.
    """,
    attrs = utils.add_dicts(_runnable_common_attr, {
        "_bazel_test_runner": attr.label(
            default = Label("@bazel_tools//tools/jdk:TestRunner_deploy.jar"),
            allow_files = True,
        ),
        "friends": attr.label_list(
            doc = """A single Kotlin dep which allows this code to access internal members of the given dependency.
            Currently uses the output jar of the module -- i.e., exported deps won't be included.

            DEPRECATED - PLEASE USE `friend=` instead.""",
            default = [],
            providers = [JavaInfo, _KtJvmInfo],
        ),
        "test_class": attr.string(
            doc = "The Java class to be loaded by the test runner.",
            default = "",
        ),
        "main_class": attr.string(default = "com.google.testing.junit.runner.BazelTestRunner"),
    }),
    executable = True,
    outputs = _common_outputs,
    test = True,
    toolchains = [_TOOLCHAIN_TYPE],
    implementation = _kt_jvm_junit_test_impl,
)

kt_jvm_import = rule(
    doc = """Import Kotlin jars.

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
         name = "kotlin-stdlib",
         jars = ["lib/kotlin-stdlib.jar"],
         srcjar = "lib/kotlin-stdlib-sources.jar"
     )
     ```
    """,
    attrs = {
        "jars": attr.label_list(
            doc = """The jars listed here are equavalent to an export attribute. The label should be either to a single
            class jar, or one or more filegroup labels.  The filegroups, when resolved, must contain  only one jar
            containing classes, and (optionally) one peer file containing sources, named `<jarname>-sources.jar`.

            DEPRECATED - please use `jar` and `srcjar` attributes.""",
            allow_files = True,
            cfg = "target",
        ),
        "jar": attr.label(
            doc = """The jar listed here is equivalent to an export attribute.""",
            allow_single_file = True,
            cfg = "target",
        ),
        "srcjar": attr.label(
            doc = """The sources for the class jar.""",
            allow_single_file = True,
            cfg = "target",
        ),
        "runtime_deps": attr.label_list(
            doc = """Additional runtime deps.""",
            default = [],
            mandatory = False,
            providers = [JavaInfo],
        ),
        "exports": attr.label_list(
            doc = """Exported libraries.

            Deps listed here will be made available to other rules, as if the parents explicitly depended on
            these deps. This is not true for regular (non-exported) deps.""",
            default = [],
            providers = [JavaInfo],
        ),
        "neverlink": attr.bool(
            doc = """If true only use this library for compilation and not at runtime.""",
            default = False,
        ),
    },
    implementation = _kt_jvm_import_impl,
    provides = [JavaInfo, _KtJvmInfo],
)
