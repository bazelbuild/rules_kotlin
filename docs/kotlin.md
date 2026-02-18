<!-- Generated with Stardoc: http://skydoc.bazel.build -->



<a id="kt_javac_options"></a>

## kt_javac_options

<pre>
load("@rules_kotlin//kotlin:jvm.bzl", "kt_javac_options")

kt_javac_options(<a href="#kt_javac_options-name">name</a>, <a href="#kt_javac_options-add_exports">add_exports</a>, <a href="#kt_javac_options-no_proc">no_proc</a>, <a href="#kt_javac_options-release">release</a>, <a href="#kt_javac_options-warn">warn</a>, <a href="#kt_javac_options-x_ep_disable_all_checks">x_ep_disable_all_checks</a>,
                 <a href="#kt_javac_options-x_explicit_api_mode">x_explicit_api_mode</a>, <a href="#kt_javac_options-x_lint">x_lint</a>, <a href="#kt_javac_options-xd_suppress_notes">xd_suppress_notes</a>)
</pre>

Define java compiler options for `kt_jvm_*` rules with java sources.

**ATTRIBUTES**


| Name  | Description | Type | Mandatory | Default |
| :------------- | :------------- | :------------- | :------------- | :------------- |
| <a id="kt_javac_options-name"></a>name |  A unique name for this target.   | <a href="https://bazel.build/concepts/labels#target-names">Name</a> | required |  |
| <a id="kt_javac_options-add_exports"></a>add_exports |  Export internal jdk apis   | List of strings | optional |  `[]`  |
| <a id="kt_javac_options-no_proc"></a>no_proc |  Disable annotation processing with -proc:none   | Boolean | optional |  `False`  |
| <a id="kt_javac_options-release"></a>release |  Compile for the specified Java SE release   | String | optional |  `"default"`  |
| <a id="kt_javac_options-warn"></a>warn |  Control warning behaviour.   | String | optional |  `"report"`  |
| <a id="kt_javac_options-x_ep_disable_all_checks"></a>x_ep_disable_all_checks |  See javac -XepDisableAllChecks documentation   | Boolean | optional |  `False`  |
| <a id="kt_javac_options-x_explicit_api_mode"></a>x_explicit_api_mode |  Enable explicit API mode for Kotlin libraries.   | String | optional |  `"off"`  |
| <a id="kt_javac_options-x_lint"></a>x_lint |  See javac -Xlint: documentation   | List of strings | optional |  `[]`  |
| <a id="kt_javac_options-xd_suppress_notes"></a>xd_suppress_notes |  See javac -XDsuppressNotes documentation   | Boolean | optional |  `False`  |


<a id="kt_jvm_binary"></a>

## kt_jvm_binary

<pre>
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_binary")

kt_jvm_binary(<a href="#kt_jvm_binary-name">name</a>, <a href="#kt_jvm_binary-deps">deps</a>, <a href="#kt_jvm_binary-srcs">srcs</a>, <a href="#kt_jvm_binary-data">data</a>, <a href="#kt_jvm_binary-resources">resources</a>, <a href="#kt_jvm_binary-associates">associates</a>, <a href="#kt_jvm_binary-env">env</a>, <a href="#kt_jvm_binary-env_inherit">env_inherit</a>, <a href="#kt_jvm_binary-java_stub_template">java_stub_template</a>,
              <a href="#kt_jvm_binary-javac_opts">javac_opts</a>, <a href="#kt_jvm_binary-jvm_flags">jvm_flags</a>, <a href="#kt_jvm_binary-kotlinc_opts">kotlinc_opts</a>, <a href="#kt_jvm_binary-main_class">main_class</a>, <a href="#kt_jvm_binary-module_name">module_name</a>, <a href="#kt_jvm_binary-plugins">plugins</a>, <a href="#kt_jvm_binary-resource_jars">resource_jars</a>,
              <a href="#kt_jvm_binary-resource_strip_prefix">resource_strip_prefix</a>, <a href="#kt_jvm_binary-runtime_deps">runtime_deps</a>)
</pre>

Builds a Java archive ("jar file"), plus a wrapper shell script with the same name as the rule. The wrapper
shell script uses a classpath that includes, among other things, a jar file for each library on which the binary
depends.

**Note:** This rule does not have all of the features found in [`java_binary`](https://docs.bazel.build/versions/master/be/java.html#java_binary).
It is appropriate for building workspace utilities. `java_binary` should be preferred for release artefacts.

**ATTRIBUTES**


| Name  | Description | Type | Mandatory | Default |
| :------------- | :------------- | :------------- | :------------- | :------------- |
| <a id="kt_jvm_binary-name"></a>name |  A unique name for this target.   | <a href="https://bazel.build/concepts/labels#target-names">Name</a> | required |  |
| <a id="kt_jvm_binary-deps"></a>deps |  A list of dependencies of this rule.See general comments about `deps` at [Attributes common to all build rules](https://docs.bazel.build/versions/master/be/common-definitions.html#common-attributes).   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |
| <a id="kt_jvm_binary-srcs"></a>srcs |  The list of source files that are processed to create the target, this can contain both Java and Kotlin files. Java analysis occurs first so Kotlin classes may depend on Java classes in the same compilation unit.   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |
| <a id="kt_jvm_binary-data"></a>data |  The list of files needed by this rule at runtime. See general comments about `data` at [Attributes common to all build rules](https://docs.bazel.build/versions/master/be/common-definitions.html#common-attributes).   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |
| <a id="kt_jvm_binary-resources"></a>resources |  A list of files that should be include in a Java jar.   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |
| <a id="kt_jvm_binary-associates"></a>associates |  Kotlin deps who should be considered part of the same module/compilation-unit for the purposes of "internal" access. Such deps must all share the same module space and so a target cannot associate to two deps from two different modules.   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |
| <a id="kt_jvm_binary-env"></a>env |  Environment variables to set when this binary is executed with `bazel run`. Note: for Starlark rules, values are used as-is (no automatic $(location) / make variable expansion).   | <a href="https://bazel.build/rules/lib/dict">Dictionary: String -> String</a> | optional |  `{}`  |
| <a id="kt_jvm_binary-env_inherit"></a>env_inherit |  Names of environment variables to inherit from the shell when executed with `bazel run`.   | List of strings | optional |  `[]`  |
| <a id="kt_jvm_binary-java_stub_template"></a>java_stub_template |  -   | <a href="https://bazel.build/concepts/labels">Label</a> | optional |  `"@rules_kotlin//third_party:java_stub_template.txt"`  |
| <a id="kt_jvm_binary-javac_opts"></a>javac_opts |  Javac options to be used when compiling this target. These opts if provided will be used instead of the ones provided to the toolchain.   | <a href="https://bazel.build/concepts/labels">Label</a> | optional |  `None`  |
| <a id="kt_jvm_binary-jvm_flags"></a>jvm_flags |  A list of flags to embed in the wrapper script generated for running this binary. Note: does not yet support make variable substitution.   | List of strings | optional |  `[]`  |
| <a id="kt_jvm_binary-kotlinc_opts"></a>kotlinc_opts |  Kotlinc options to be used when compiling this target. These opts if provided will be used instead of the ones provided to the toolchain. Toolchain-managed settings (for example `api_version` and `language_version`) are not overridden here.   | <a href="https://bazel.build/concepts/labels">Label</a> | optional |  `None`  |
| <a id="kt_jvm_binary-main_class"></a>main_class |  Name of class with main() method to use as entry point.   | String | required |  |
| <a id="kt_jvm_binary-module_name"></a>module_name |  The name of the module, if not provided the module name is derived from the label. --e.g., `//some/package/path:label_name` is translated to `some_package_path-label_name`.   | String | optional |  `""`  |
| <a id="kt_jvm_binary-plugins"></a>plugins |  -   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |
| <a id="kt_jvm_binary-resource_jars"></a>resource_jars |  Set of archives containing Java resources. If specified, the contents of these jars are merged into the output jar.   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |
| <a id="kt_jvm_binary-resource_strip_prefix"></a>resource_strip_prefix |  The path prefix to strip from Java resources. Should be a label pointing to a directory. Files residing under common prefix such as `src/main/resources` or `src/test/resources` or `kotlin` will have stripping applied by convention if this is not specified.   | <a href="https://bazel.build/concepts/labels">Label</a> | optional |  `None`  |
| <a id="kt_jvm_binary-runtime_deps"></a>runtime_deps |  Libraries to make available to the final binary or test at runtime only. Like ordinary deps, these will appear on the runtime classpath, but unlike them, not on the compile-time classpath.   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |


<a id="kt_jvm_import"></a>

## kt_jvm_import

<pre>
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_import")

kt_jvm_import(<a href="#kt_jvm_import-name">name</a>, <a href="#kt_jvm_import-deps">deps</a>, <a href="#kt_jvm_import-exported_compiler_plugins">exported_compiler_plugins</a>, <a href="#kt_jvm_import-exports">exports</a>, <a href="#kt_jvm_import-jar">jar</a>, <a href="#kt_jvm_import-jars">jars</a>, <a href="#kt_jvm_import-neverlink">neverlink</a>, <a href="#kt_jvm_import-runtime_deps">runtime_deps</a>,
              <a href="#kt_jvm_import-srcjar">srcjar</a>)
</pre>

Import Kotlin jars.

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

**ATTRIBUTES**


| Name  | Description | Type | Mandatory | Default |
| :------------- | :------------- | :------------- | :------------- | :------------- |
| <a id="kt_jvm_import-name"></a>name |  A unique name for this target.   | <a href="https://bazel.build/concepts/labels#target-names">Name</a> | required |  |
| <a id="kt_jvm_import-deps"></a>deps |  Compile and runtime dependencies   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |
| <a id="kt_jvm_import-exported_compiler_plugins"></a>exported_compiler_plugins |  Exported compiler plugins.<br><br>Compiler plugins listed here will be treated as if they were added in the plugins attribute of any targets that directly depend on this target. Like java_plugins' exported_plugins, this is not transitive   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |
| <a id="kt_jvm_import-exports"></a>exports |  Exported libraries.<br><br>Deps listed here will be made available to other rules, as if the parents explicitly depended on these deps. This is not true for regular (non-exported) deps.   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |
| <a id="kt_jvm_import-jar"></a>jar |  The jar listed here is equivalent to an export attribute.   | <a href="https://bazel.build/concepts/labels">Label</a> | optional |  `None`  |
| <a id="kt_jvm_import-jars"></a>jars |  The jars listed here are equavalent to an export attribute. The label should be either to a single class jar, or one or more filegroup labels.  The filegroups, when resolved, must contain  only one jar containing classes, and (optionally) one peer file containing sources, named `<jarname>-sources.jar`.<br><br>DEPRECATED - please use `jar` and `srcjar` attributes.   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |
| <a id="kt_jvm_import-neverlink"></a>neverlink |  If true only use this library for compilation and not at runtime.   | Boolean | optional |  `False`  |
| <a id="kt_jvm_import-runtime_deps"></a>runtime_deps |  Additional runtime deps.   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |
| <a id="kt_jvm_import-srcjar"></a>srcjar |  The sources for the class jar.   | <a href="https://bazel.build/concepts/labels">Label</a> | optional |  `"@rules_kotlin//third_party:empty.jar"`  |


<a id="kt_jvm_library"></a>

## kt_jvm_library

<pre>
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(<a href="#kt_jvm_library-name">name</a>, <a href="#kt_jvm_library-deps">deps</a>, <a href="#kt_jvm_library-srcs">srcs</a>, <a href="#kt_jvm_library-data">data</a>, <a href="#kt_jvm_library-resources">resources</a>, <a href="#kt_jvm_library-associates">associates</a>, <a href="#kt_jvm_library-exported_compiler_plugins">exported_compiler_plugins</a>, <a href="#kt_jvm_library-exports">exports</a>,
               <a href="#kt_jvm_library-java_stub_template">java_stub_template</a>, <a href="#kt_jvm_library-javac_opts">javac_opts</a>, <a href="#kt_jvm_library-kotlinc_opts">kotlinc_opts</a>, <a href="#kt_jvm_library-module_name">module_name</a>, <a href="#kt_jvm_library-neverlink">neverlink</a>, <a href="#kt_jvm_library-plugins">plugins</a>,
               <a href="#kt_jvm_library-resource_jars">resource_jars</a>, <a href="#kt_jvm_library-resource_strip_prefix">resource_strip_prefix</a>, <a href="#kt_jvm_library-runtime_deps">runtime_deps</a>)
</pre>

This rule compiles and links Kotlin and Java sources into a .jar file.

**ATTRIBUTES**


| Name  | Description | Type | Mandatory | Default |
| :------------- | :------------- | :------------- | :------------- | :------------- |
| <a id="kt_jvm_library-name"></a>name |  A unique name for this target.   | <a href="https://bazel.build/concepts/labels#target-names">Name</a> | required |  |
| <a id="kt_jvm_library-deps"></a>deps |  A list of dependencies of this rule.See general comments about `deps` at [Attributes common to all build rules](https://docs.bazel.build/versions/master/be/common-definitions.html#common-attributes).   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |
| <a id="kt_jvm_library-srcs"></a>srcs |  The list of source files that are processed to create the target, this can contain both Java and Kotlin files. Java analysis occurs first so Kotlin classes may depend on Java classes in the same compilation unit.   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |
| <a id="kt_jvm_library-data"></a>data |  The list of files needed by this rule at runtime. See general comments about `data` at [Attributes common to all build rules](https://docs.bazel.build/versions/master/be/common-definitions.html#common-attributes).   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |
| <a id="kt_jvm_library-resources"></a>resources |  A list of files that should be include in a Java jar.   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |
| <a id="kt_jvm_library-associates"></a>associates |  Kotlin deps who should be considered part of the same module/compilation-unit for the purposes of "internal" access. Such deps must all share the same module space and so a target cannot associate to two deps from two different modules.   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |
| <a id="kt_jvm_library-exported_compiler_plugins"></a>exported_compiler_plugins |  Exported compiler plugins.<br><br>Compiler plugins listed here will be treated as if they were added in the plugins attribute of any targets that directly depend on this target. Like `java_plugin`s exported_plugins, this is not transitive   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |
| <a id="kt_jvm_library-exports"></a>exports |  Exported libraries.<br><br>Deps listed here will be made available to other rules, as if the parents explicitly depended on these deps. This is not true for regular (non-exported) deps.   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |
| <a id="kt_jvm_library-java_stub_template"></a>java_stub_template |  -   | <a href="https://bazel.build/concepts/labels">Label</a> | optional |  `"@rules_kotlin//third_party:java_stub_template.txt"`  |
| <a id="kt_jvm_library-javac_opts"></a>javac_opts |  Javac options to be used when compiling this target. These opts if provided will be used instead of the ones provided to the toolchain.   | <a href="https://bazel.build/concepts/labels">Label</a> | optional |  `None`  |
| <a id="kt_jvm_library-kotlinc_opts"></a>kotlinc_opts |  Kotlinc options to be used when compiling this target. These opts if provided will be used instead of the ones provided to the toolchain. Toolchain-managed settings (for example `api_version` and `language_version`) are not overridden here.   | <a href="https://bazel.build/concepts/labels">Label</a> | optional |  `None`  |
| <a id="kt_jvm_library-module_name"></a>module_name |  The name of the module, if not provided the module name is derived from the label. --e.g., `//some/package/path:label_name` is translated to `some_package_path-label_name`.   | String | optional |  `""`  |
| <a id="kt_jvm_library-neverlink"></a>neverlink |  If true only use this library for compilation and not at runtime.   | Boolean | optional |  `False`  |
| <a id="kt_jvm_library-plugins"></a>plugins |  -   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |
| <a id="kt_jvm_library-resource_jars"></a>resource_jars |  Set of archives containing Java resources. If specified, the contents of these jars are merged into the output jar.   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |
| <a id="kt_jvm_library-resource_strip_prefix"></a>resource_strip_prefix |  The path prefix to strip from Java resources. Should be a label pointing to a directory. Files residing under common prefix such as `src/main/resources` or `src/test/resources` or `kotlin` will have stripping applied by convention if this is not specified.   | <a href="https://bazel.build/concepts/labels">Label</a> | optional |  `None`  |
| <a id="kt_jvm_library-runtime_deps"></a>runtime_deps |  Libraries to make available to the final binary or test at runtime only. Like ordinary deps, these will appear on the runtime classpath, but unlike them, not on the compile-time classpath.   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |


<a id="kt_jvm_test"></a>

## kt_jvm_test

<pre>
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_test")

kt_jvm_test(<a href="#kt_jvm_test-name">name</a>, <a href="#kt_jvm_test-deps">deps</a>, <a href="#kt_jvm_test-srcs">srcs</a>, <a href="#kt_jvm_test-data">data</a>, <a href="#kt_jvm_test-resources">resources</a>, <a href="#kt_jvm_test-associates">associates</a>, <a href="#kt_jvm_test-env">env</a>, <a href="#kt_jvm_test-env_inherit">env_inherit</a>, <a href="#kt_jvm_test-java_stub_template">java_stub_template</a>,
            <a href="#kt_jvm_test-javac_opts">javac_opts</a>, <a href="#kt_jvm_test-jvm_flags">jvm_flags</a>, <a href="#kt_jvm_test-kotlinc_opts">kotlinc_opts</a>, <a href="#kt_jvm_test-main_class">main_class</a>, <a href="#kt_jvm_test-module_name">module_name</a>, <a href="#kt_jvm_test-plugins">plugins</a>, <a href="#kt_jvm_test-resource_jars">resource_jars</a>,
            <a href="#kt_jvm_test-resource_strip_prefix">resource_strip_prefix</a>, <a href="#kt_jvm_test-runtime_deps">runtime_deps</a>, <a href="#kt_jvm_test-test_class">test_class</a>)
</pre>

Setup a simple kotlin_test.

**Notes:**
* The kotlin test library is not added implicitly, it is available with the label
`@rules_kotlin//kotlin/compiler:kotlin-test`.

**ATTRIBUTES**


| Name  | Description | Type | Mandatory | Default |
| :------------- | :------------- | :------------- | :------------- | :------------- |
| <a id="kt_jvm_test-name"></a>name |  A unique name for this target.   | <a href="https://bazel.build/concepts/labels#target-names">Name</a> | required |  |
| <a id="kt_jvm_test-deps"></a>deps |  A list of dependencies of this rule.See general comments about `deps` at [Attributes common to all build rules](https://docs.bazel.build/versions/master/be/common-definitions.html#common-attributes).   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |
| <a id="kt_jvm_test-srcs"></a>srcs |  The list of source files that are processed to create the target, this can contain both Java and Kotlin files. Java analysis occurs first so Kotlin classes may depend on Java classes in the same compilation unit.   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |
| <a id="kt_jvm_test-data"></a>data |  The list of files needed by this rule at runtime. See general comments about `data` at [Attributes common to all build rules](https://docs.bazel.build/versions/master/be/common-definitions.html#common-attributes).   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |
| <a id="kt_jvm_test-resources"></a>resources |  A list of files that should be include in a Java jar.   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |
| <a id="kt_jvm_test-associates"></a>associates |  Kotlin deps who should be considered part of the same module/compilation-unit for the purposes of "internal" access. Such deps must all share the same module space and so a target cannot associate to two deps from two different modules.   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |
| <a id="kt_jvm_test-env"></a>env |  Specifies additional environment variables to set when the target is executed by bazel test.   | <a href="https://bazel.build/rules/lib/dict">Dictionary: String -> String</a> | optional |  `{}`  |
| <a id="kt_jvm_test-env_inherit"></a>env_inherit |  Environment variables to inherit from the external environment.   | List of strings | optional |  `[]`  |
| <a id="kt_jvm_test-java_stub_template"></a>java_stub_template |  -   | <a href="https://bazel.build/concepts/labels">Label</a> | optional |  `"@rules_kotlin//third_party:java_stub_template.txt"`  |
| <a id="kt_jvm_test-javac_opts"></a>javac_opts |  Javac options to be used when compiling this target. These opts if provided will be used instead of the ones provided to the toolchain.   | <a href="https://bazel.build/concepts/labels">Label</a> | optional |  `None`  |
| <a id="kt_jvm_test-jvm_flags"></a>jvm_flags |  A list of flags to embed in the wrapper script generated for running this binary. Note: does not yet support make variable substitution.   | List of strings | optional |  `[]`  |
| <a id="kt_jvm_test-kotlinc_opts"></a>kotlinc_opts |  Kotlinc options to be used when compiling this target. These opts if provided will be used instead of the ones provided to the toolchain. Toolchain-managed settings (for example `api_version` and `language_version`) are not overridden here.   | <a href="https://bazel.build/concepts/labels">Label</a> | optional |  `None`  |
| <a id="kt_jvm_test-main_class"></a>main_class |  -   | String | optional |  `"com.google.testing.junit.runner.BazelTestRunner"`  |
| <a id="kt_jvm_test-module_name"></a>module_name |  The name of the module, if not provided the module name is derived from the label. --e.g., `//some/package/path:label_name` is translated to `some_package_path-label_name`.   | String | optional |  `""`  |
| <a id="kt_jvm_test-plugins"></a>plugins |  -   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |
| <a id="kt_jvm_test-resource_jars"></a>resource_jars |  Set of archives containing Java resources. If specified, the contents of these jars are merged into the output jar.   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |
| <a id="kt_jvm_test-resource_strip_prefix"></a>resource_strip_prefix |  The path prefix to strip from Java resources. Should be a label pointing to a directory. Files residing under common prefix such as `src/main/resources` or `src/test/resources` or `kotlin` will have stripping applied by convention if this is not specified.   | <a href="https://bazel.build/concepts/labels">Label</a> | optional |  `None`  |
| <a id="kt_jvm_test-runtime_deps"></a>runtime_deps |  Libraries to make available to the final binary or test at runtime only. Like ordinary deps, these will appear on the runtime classpath, but unlike them, not on the compile-time classpath.   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |
| <a id="kt_jvm_test-test_class"></a>test_class |  The Java class to be loaded by the test runner.   | String | optional |  `""`  |


<!-- Generated with Stardoc: http://skydoc.bazel.build -->



<a id="ktlint_config"></a>

## ktlint_config

<pre>
load("@rules_kotlin//kotlin:lint.bzl", "ktlint_config")

ktlint_config(<a href="#ktlint_config-name">name</a>, <a href="#ktlint_config-android_rules_enabled">android_rules_enabled</a>, <a href="#ktlint_config-editorconfig">editorconfig</a>, <a href="#ktlint_config-experimental_rules_enabled">experimental_rules_enabled</a>)
</pre>

Used to configure ktlint.

`ktlint` can be configured to use a `.editorconfig`, as documented at
https://github.com/pinterest/ktlint/#editorconfig

**ATTRIBUTES**


| Name  | Description | Type | Mandatory | Default |
| :------------- | :------------- | :------------- | :------------- | :------------- |
| <a id="ktlint_config-name"></a>name |  A unique name for this target.   | <a href="https://bazel.build/concepts/labels#target-names">Name</a> | required |  |
| <a id="ktlint_config-android_rules_enabled"></a>android_rules_enabled |  Turn on Android Kotlin Style Guide compatibility   | Boolean | optional |  `False`  |
| <a id="ktlint_config-editorconfig"></a>editorconfig |  Editor config file to use   | <a href="https://bazel.build/concepts/labels">Label</a> | optional |  `None`  |
| <a id="ktlint_config-experimental_rules_enabled"></a>experimental_rules_enabled |  Turn on experimental rules (ktlint-ruleset-experimental)   | Boolean | optional |  `False`  |


<a id="ktlint_fix"></a>

## ktlint_fix

<pre>
load("@rules_kotlin//kotlin:lint.bzl", "ktlint_fix")

ktlint_fix(<a href="#ktlint_fix-name">name</a>, <a href="#ktlint_fix-srcs">srcs</a>, <a href="#ktlint_fix-config">config</a>)
</pre>

Lint Kotlin files and automatically fix them as needed

**ATTRIBUTES**


| Name  | Description | Type | Mandatory | Default |
| :------------- | :------------- | :------------- | :------------- | :------------- |
| <a id="ktlint_fix-name"></a>name |  A unique name for this target.   | <a href="https://bazel.build/concepts/labels#target-names">Name</a> | required |  |
| <a id="ktlint_fix-srcs"></a>srcs |  Source files to review and fix   | <a href="https://bazel.build/concepts/labels">List of labels</a> | required |  |
| <a id="ktlint_fix-config"></a>config |  ktlint_config to use   | <a href="https://bazel.build/concepts/labels">Label</a> | optional |  `None`  |


<a id="ktlint_test"></a>

## ktlint_test

<pre>
load("@rules_kotlin//kotlin:lint.bzl", "ktlint_test")

ktlint_test(<a href="#ktlint_test-name">name</a>, <a href="#ktlint_test-srcs">srcs</a>, <a href="#ktlint_test-config">config</a>)
</pre>

Lint Kotlin files, and fail if the linter raises errors.

**ATTRIBUTES**


| Name  | Description | Type | Mandatory | Default |
| :------------- | :------------- | :------------- | :------------- | :------------- |
| <a id="ktlint_test-name"></a>name |  A unique name for this target.   | <a href="https://bazel.build/concepts/labels#target-names">Name</a> | required |  |
| <a id="ktlint_test-srcs"></a>srcs |  Source files to lint   | <a href="https://bazel.build/concepts/labels">List of labels</a> | required |  |
| <a id="ktlint_test-config"></a>config |  ktlint_config to use   | <a href="https://bazel.build/concepts/labels">Label</a> | optional |  `None`  |


<!-- Generated with Stardoc: http://skydoc.bazel.build -->



<a id="kt_compiler_plugin"></a>

## kt_compiler_plugin

<pre>
load("@rules_kotlin//kotlin:core.bzl", "kt_compiler_plugin")

kt_compiler_plugin(<a href="#kt_compiler_plugin-name">name</a>, <a href="#kt_compiler_plugin-deps">deps</a>, <a href="#kt_compiler_plugin-data">data</a>, <a href="#kt_compiler_plugin-compile_phase">compile_phase</a>, <a href="#kt_compiler_plugin-id">id</a>, <a href="#kt_compiler_plugin-options">options</a>, <a href="#kt_compiler_plugin-stubs_phase">stubs_phase</a>)
</pre>

Define a plugin for the Kotlin compiler to run. The plugin can then be referenced in the `plugins` attribute
of the `kt_jvm_*` rules.

An example can be found under `//examples/plugin`:

```bzl
kt_compiler_plugin(
    name = "open_for_testing_plugin",
    id = "org.jetbrains.kotlin.allopen",
    options = {
        "annotation": ["plugin.OpenForTesting"],
    },
    deps = [
        "//kotlin/compiler:allopen-compiler-plugin",
    ],
)

kt_jvm_library(
    name = "open_for_testing",
    srcs = ["OpenForTesting.kt"],
)

kt_jvm_library(
    name = "user",
    srcs = ["User.kt"],
    plugins = [":open_for_testing_plugin"],
    deps = [
        ":open_for_testing",
    ],
)
```

**ATTRIBUTES**


| Name  | Description | Type | Mandatory | Default |
| :------------- | :------------- | :------------- | :------------- | :------------- |
| <a id="kt_compiler_plugin-name"></a>name |  A unique name for this target.   | <a href="https://bazel.build/concepts/labels#target-names">Name</a> | required |  |
| <a id="kt_compiler_plugin-deps"></a>deps |  The list of libraries to be added to the compiler's plugin classpath   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |
| <a id="kt_compiler_plugin-data"></a>data |  The list of data files to be used by compiler's plugin   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |
| <a id="kt_compiler_plugin-compile_phase"></a>compile_phase |  Runs the compiler plugin during kotlin compilation. Known examples: `allopen`, `sam_with_reciever`   | Boolean | optional |  `True`  |
| <a id="kt_compiler_plugin-id"></a>id |  The ID of the plugin   | String | required |  |
| <a id="kt_compiler_plugin-options"></a>options |  Dictionary of options to be passed to the plugin. Each option key can have multiple values. Supports the following template values:<br><br>- `{generatedClasses}`: directory for generated class output - `{temp}`: temporary directory, discarded between invocations - `{generatedSources}`:  directory for generated source output - `{classpath}` : replaced with a list of jars separated by the filesystem appropriate separator.   | <a href="https://bazel.build/rules/lib/dict">Dictionary: String -> List of strings</a> | optional |  `{}`  |
| <a id="kt_compiler_plugin-stubs_phase"></a>stubs_phase |  Runs the compiler plugin in kapt stub generation.   | Boolean | optional |  `True`  |


<a id="kt_javac_options"></a>

## kt_javac_options

<pre>
load("@rules_kotlin//kotlin:core.bzl", "kt_javac_options")

kt_javac_options(<a href="#kt_javac_options-name">name</a>, <a href="#kt_javac_options-add_exports">add_exports</a>, <a href="#kt_javac_options-no_proc">no_proc</a>, <a href="#kt_javac_options-release">release</a>, <a href="#kt_javac_options-warn">warn</a>, <a href="#kt_javac_options-x_ep_disable_all_checks">x_ep_disable_all_checks</a>,
                 <a href="#kt_javac_options-x_explicit_api_mode">x_explicit_api_mode</a>, <a href="#kt_javac_options-x_lint">x_lint</a>, <a href="#kt_javac_options-xd_suppress_notes">xd_suppress_notes</a>)
</pre>

Define java compiler options for `kt_jvm_*` rules with java sources.

**ATTRIBUTES**


| Name  | Description | Type | Mandatory | Default |
| :------------- | :------------- | :------------- | :------------- | :------------- |
| <a id="kt_javac_options-name"></a>name |  A unique name for this target.   | <a href="https://bazel.build/concepts/labels#target-names">Name</a> | required |  |
| <a id="kt_javac_options-add_exports"></a>add_exports |  Export internal jdk apis   | List of strings | optional |  `[]`  |
| <a id="kt_javac_options-no_proc"></a>no_proc |  Disable annotation processing with -proc:none   | Boolean | optional |  `False`  |
| <a id="kt_javac_options-release"></a>release |  Compile for the specified Java SE release   | String | optional |  `"default"`  |
| <a id="kt_javac_options-warn"></a>warn |  Control warning behaviour.   | String | optional |  `"report"`  |
| <a id="kt_javac_options-x_ep_disable_all_checks"></a>x_ep_disable_all_checks |  See javac -XepDisableAllChecks documentation   | Boolean | optional |  `False`  |
| <a id="kt_javac_options-x_explicit_api_mode"></a>x_explicit_api_mode |  Enable explicit API mode for Kotlin libraries.   | String | optional |  `"off"`  |
| <a id="kt_javac_options-x_lint"></a>x_lint |  See javac -Xlint: documentation   | List of strings | optional |  `[]`  |
| <a id="kt_javac_options-xd_suppress_notes"></a>xd_suppress_notes |  See javac -XDsuppressNotes documentation   | Boolean | optional |  `False`  |


<a id="kt_kotlinc_options"></a>

## kt_kotlinc_options

<pre>
load("@rules_kotlin//kotlin:core.bzl", "kt_kotlinc_options")

kt_kotlinc_options(<a href="#kt_kotlinc_options-name">name</a>, <a href="#kt_kotlinc_options-include_stdlibs">include_stdlibs</a>, <a href="#kt_kotlinc_options-java_parameters">java_parameters</a>, <a href="#kt_kotlinc_options-jvm_default">jvm_default</a>, <a href="#kt_kotlinc_options-jvm_target">jvm_target</a>, <a href="#kt_kotlinc_options-opt_in">opt_in</a>,
                   <a href="#kt_kotlinc_options-progressive">progressive</a>, <a href="#kt_kotlinc_options-verbose">verbose</a>, <a href="#kt_kotlinc_options-warn">warn</a>, <a href="#kt_kotlinc_options-wextra">wextra</a>, <a href="#kt_kotlinc_options-x_abi_stability">x_abi_stability</a>,
                   <a href="#kt_kotlinc_options-x_allow_condition_implies_returns_contracts">x_allow_condition_implies_returns_contracts</a>, <a href="#kt_kotlinc_options-x_allow_contracts_on_more_functions">x_allow_contracts_on_more_functions</a>,
                   <a href="#kt_kotlinc_options-x_allow_holdsin_contract">x_allow_holdsin_contract</a>, <a href="#kt_kotlinc_options-x_allow_kotlin_package">x_allow_kotlin_package</a>, <a href="#kt_kotlinc_options-x_allow_reified_type_in_catch">x_allow_reified_type_in_catch</a>,
                   <a href="#kt_kotlinc_options-x_allow_unstable_dependencies">x_allow_unstable_dependencies</a>, <a href="#kt_kotlinc_options-x_annotation_default_target">x_annotation_default_target</a>,
                   <a href="#kt_kotlinc_options-x_annotation_target_all">x_annotation_target_all</a>, <a href="#kt_kotlinc_options-x_annotations_in_metadata">x_annotations_in_metadata</a>, <a href="#kt_kotlinc_options-x_assertions">x_assertions</a>,
                   <a href="#kt_kotlinc_options-x_backend_threads">x_backend_threads</a>, <a href="#kt_kotlinc_options-x_check_phase_conditions">x_check_phase_conditions</a>, <a href="#kt_kotlinc_options-x_compiler_plugin_order">x_compiler_plugin_order</a>,
                   <a href="#kt_kotlinc_options-x_consistent_data_class_copy_visibility">x_consistent_data_class_copy_visibility</a>, <a href="#kt_kotlinc_options-x_context_parameters">x_context_parameters</a>, <a href="#kt_kotlinc_options-x_context_receivers">x_context_receivers</a>,
                   <a href="#kt_kotlinc_options-x_context_sensitive_resolution">x_context_sensitive_resolution</a>, <a href="#kt_kotlinc_options-x_data_flow_based_exhaustiveness">x_data_flow_based_exhaustiveness</a>, <a href="#kt_kotlinc_options-x_debug">x_debug</a>,
                   <a href="#kt_kotlinc_options-x_detailed_perf">x_detailed_perf</a>, <a href="#kt_kotlinc_options-x_direct_java_actualization">x_direct_java_actualization</a>, <a href="#kt_kotlinc_options-x_disable_phases">x_disable_phases</a>,
                   <a href="#kt_kotlinc_options-x_dont_warn_on_error_suppression">x_dont_warn_on_error_suppression</a>, <a href="#kt_kotlinc_options-x_emit_jvm_type_annotations">x_emit_jvm_type_annotations</a>,
                   <a href="#kt_kotlinc_options-x_enable_incremental_compilation">x_enable_incremental_compilation</a>, <a href="#kt_kotlinc_options-x_enhance_type_parameter_types_to_def_not_null">x_enhance_type_parameter_types_to_def_not_null</a>,
                   <a href="#kt_kotlinc_options-x_enhanced_coroutines_debugging">x_enhanced_coroutines_debugging</a>, <a href="#kt_kotlinc_options-x_expect_actual_classes">x_expect_actual_classes</a>, <a href="#kt_kotlinc_options-x_explicit_api">x_explicit_api</a>,
                   <a href="#kt_kotlinc_options-x_explicit_backing_fields">x_explicit_backing_fields</a>, <a href="#kt_kotlinc_options-x_generate_strict_metadata_version">x_generate_strict_metadata_version</a>,
                   <a href="#kt_kotlinc_options-x_ignore_const_optimization_errors">x_ignore_const_optimization_errors</a>, <a href="#kt_kotlinc_options-x_indy_allow_annotated_lambdas">x_indy_allow_annotated_lambdas</a>,
                   <a href="#kt_kotlinc_options-x_inline_classes">x_inline_classes</a>, <a href="#kt_kotlinc_options-x_ir_do_not_clear_binding_context">x_ir_do_not_clear_binding_context</a>, <a href="#kt_kotlinc_options-x_jdk_release">x_jdk_release</a>,
                   <a href="#kt_kotlinc_options-x_jspecify_annotations">x_jspecify_annotations</a>, <a href="#kt_kotlinc_options-x_jsr305">x_jsr305</a>, <a href="#kt_kotlinc_options-x_jvm_default">x_jvm_default</a>, <a href="#kt_kotlinc_options-x_jvm_enable_preview">x_jvm_enable_preview</a>,
                   <a href="#kt_kotlinc_options-x_jvm_expose_boxed">x_jvm_expose_boxed</a>, <a href="#kt_kotlinc_options-x_lambdas">x_lambdas</a>, <a href="#kt_kotlinc_options-x_link_via_signatures">x_link_via_signatures</a>, <a href="#kt_kotlinc_options-x_list_phases">x_list_phases</a>,
                   <a href="#kt_kotlinc_options-x_metadata_klib">x_metadata_klib</a>, <a href="#kt_kotlinc_options-x_metadata_version">x_metadata_version</a>, <a href="#kt_kotlinc_options-x_multi_dollar_interpolation">x_multi_dollar_interpolation</a>,
                   <a href="#kt_kotlinc_options-x_multi_platform">x_multi_platform</a>, <a href="#kt_kotlinc_options-x_multifile_parts_inherit">x_multifile_parts_inherit</a>, <a href="#kt_kotlinc_options-x_name_based_destructuring">x_name_based_destructuring</a>,
                   <a href="#kt_kotlinc_options-x_nested_type_aliases">x_nested_type_aliases</a>, <a href="#kt_kotlinc_options-x_new_inference">x_new_inference</a>, <a href="#kt_kotlinc_options-x_no_call_assertions">x_no_call_assertions</a>, <a href="#kt_kotlinc_options-x_no_check_actual">x_no_check_actual</a>,
                   <a href="#kt_kotlinc_options-x_no_inline">x_no_inline</a>, <a href="#kt_kotlinc_options-x_no_new_java_annotation_targets">x_no_new_java_annotation_targets</a>, <a href="#kt_kotlinc_options-x_no_optimize">x_no_optimize</a>,
                   <a href="#kt_kotlinc_options-x_no_param_assertions">x_no_param_assertions</a>, <a href="#kt_kotlinc_options-x_no_receiver_assertions">x_no_receiver_assertions</a>, <a href="#kt_kotlinc_options-x_no_unified_null_checks">x_no_unified_null_checks</a>,
                   <a href="#kt_kotlinc_options-x_non_local_break_continue">x_non_local_break_continue</a>, <a href="#kt_kotlinc_options-x_nullability_annotations">x_nullability_annotations</a>, <a href="#kt_kotlinc_options-x_phases_to_dump">x_phases_to_dump</a>,
                   <a href="#kt_kotlinc_options-x_phases_to_dump_after">x_phases_to_dump_after</a>, <a href="#kt_kotlinc_options-x_phases_to_dump_before">x_phases_to_dump_before</a>, <a href="#kt_kotlinc_options-x_phases_to_validate">x_phases_to_validate</a>,
                   <a href="#kt_kotlinc_options-x_phases_to_validate_after">x_phases_to_validate_after</a>, <a href="#kt_kotlinc_options-x_phases_to_validate_before">x_phases_to_validate_before</a>, <a href="#kt_kotlinc_options-x_profile_phases">x_profile_phases</a>,
                   <a href="#kt_kotlinc_options-x_render_internal_diagnostic_names">x_render_internal_diagnostic_names</a>, <a href="#kt_kotlinc_options-x_report_all_warnings">x_report_all_warnings</a>, <a href="#kt_kotlinc_options-x_report_output_files">x_report_output_files</a>,
                   <a href="#kt_kotlinc_options-x_report_perf">x_report_perf</a>, <a href="#kt_kotlinc_options-x_return_value_checker">x_return_value_checker</a>, <a href="#kt_kotlinc_options-x_sam_conversions">x_sam_conversions</a>, <a href="#kt_kotlinc_options-x_sanitize_parentheses">x_sanitize_parentheses</a>,
                   <a href="#kt_kotlinc_options-x_separate_kmp_compilation">x_separate_kmp_compilation</a>, <a href="#kt_kotlinc_options-x_serialize_ir">x_serialize_ir</a>, <a href="#kt_kotlinc_options-x_skip_metadata_version_check">x_skip_metadata_version_check</a>,
                   <a href="#kt_kotlinc_options-x_skip_prerelease_check">x_skip_prerelease_check</a>, <a href="#kt_kotlinc_options-x_string_concat">x_string_concat</a>,
                   <a href="#kt_kotlinc_options-x_support_compatqual_checker_framework_annotations">x_support_compatqual_checker_framework_annotations</a>,
                   <a href="#kt_kotlinc_options-x_suppress_api_version_greater_than_language_version_error">x_suppress_api_version_greater_than_language_version_error</a>,
                   <a href="#kt_kotlinc_options-x_suppress_deprecated_jvm_target_warning">x_suppress_deprecated_jvm_target_warning</a>, <a href="#kt_kotlinc_options-x_suppress_missing_builtins_error">x_suppress_missing_builtins_error</a>,
                   <a href="#kt_kotlinc_options-x_suppress_version_warnings">x_suppress_version_warnings</a>, <a href="#kt_kotlinc_options-x_suppress_warning">x_suppress_warning</a>,
                   <a href="#kt_kotlinc_options-x_type_enhancement_improvements_strict_mode">x_type_enhancement_improvements_strict_mode</a>, <a href="#kt_kotlinc_options-x_unrestricted_builder_inference">x_unrestricted_builder_inference</a>,
                   <a href="#kt_kotlinc_options-x_use_14_inline_classes_mangling_scheme">x_use_14_inline_classes_mangling_scheme</a>, <a href="#kt_kotlinc_options-x_use_fast_jar_file_system">x_use_fast_jar_file_system</a>,
                   <a href="#kt_kotlinc_options-x_use_fir_experimental_checkers">x_use_fir_experimental_checkers</a>, <a href="#kt_kotlinc_options-x_use_fir_ic">x_use_fir_ic</a>, <a href="#kt_kotlinc_options-x_use_fir_lt">x_use_fir_lt</a>,
                   <a href="#kt_kotlinc_options-x_use_inline_scopes_numbers">x_use_inline_scopes_numbers</a>, <a href="#kt_kotlinc_options-x_use_old_class_files_reading">x_use_old_class_files_reading</a>, <a href="#kt_kotlinc_options-x_use_type_table">x_use_type_table</a>,
                   <a href="#kt_kotlinc_options-x_validate_bytecode">x_validate_bytecode</a>, <a href="#kt_kotlinc_options-x_value_classes">x_value_classes</a>, <a href="#kt_kotlinc_options-x_verbose_phases">x_verbose_phases</a>, <a href="#kt_kotlinc_options-x_verify_ir">x_verify_ir</a>,
                   <a href="#kt_kotlinc_options-x_verify_ir_visibility">x_verify_ir_visibility</a>, <a href="#kt_kotlinc_options-x_warning_level">x_warning_level</a>, <a href="#kt_kotlinc_options-x_when_expressions">x_when_expressions</a>, <a href="#kt_kotlinc_options-x_when_guards">x_when_guards</a>,
                   <a href="#kt_kotlinc_options-x_xdebug_level_compiler_checks">x_xdebug_level_compiler_checks</a>, <a href="#kt_kotlinc_options-x_xdump_model">x_xdump_model</a>, <a href="#kt_kotlinc_options-x_xexplicit_return_types">x_xexplicit_return_types</a>,
                   <a href="#kt_kotlinc_options-x_xlanguage">x_xlanguage</a>, <a href="#kt_kotlinc_options-x_xlenient_mode">x_xlenient_mode</a>)
</pre>

Define kotlin compiler options.

For string attributes, the default empty string (`""`) means "unset", so the corresponding compiler flag is not emitted.

**ATTRIBUTES**


| Name  | Description | Type | Mandatory | Default |
| :------------- | :------------- | :------------- | :------------- | :------------- |
| <a id="kt_kotlinc_options-name"></a>name |  A unique name for this target.   | <a href="https://bazel.build/concepts/labels#target-names">Name</a> | required |  |
| <a id="kt_kotlinc_options-include_stdlibs"></a>include_stdlibs |  Don't automatically include the Kotlin standard libraries into the classpath (stdlib and reflect).   | String | optional |  `"all"`  |
| <a id="kt_kotlinc_options-java_parameters"></a>java_parameters |  Generate metadata for Java 1.8 reflection on method parameters.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-jvm_default"></a>jvm_default |  Emit JVM default methods for interface declarations with bodies. The default is 'enable'. -jvm-default=enable              Generate default methods for non-abstract interface declarations, as well as 'DefaultImpls' classes with                                  static methods for compatibility with code compiled in the 'disable' mode.                                  This is the default behavior since language version 2.2. -jvm-default=no-compatibility    Generate default methods for non-abstract interface declarations. Do not generate 'DefaultImpls' classes. -jvm-default=disable             Do not generate JVM default methods. This is the default behavior up to language version 2.1.   | String | optional |  `""`  |
| <a id="kt_kotlinc_options-jvm_target"></a>jvm_target |  The target version of the generated JVM bytecode (1.8 and 9–25), with 1.8 as the default.   | String | optional |  `""`  |
| <a id="kt_kotlinc_options-opt_in"></a>opt_in |  Enable API usages that require opt-in with an opt-in requirement marker with the given fully qualified name.   | List of strings | optional |  `[]`  |
| <a id="kt_kotlinc_options-progressive"></a>progressive |  Enable progressive compiler mode. In this mode, deprecations and bug fixes for unstable code take effect immediately instead of going through a graceful migration cycle. Code written in progressive mode is backward compatible; however, code written without progressive mode enabled may cause compilation errors in progressive mode.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-verbose"></a>verbose |  Enable verbose logging output.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-warn"></a>warn |  Control warning behaviour.   | String | optional |  `"report"`  |
| <a id="kt_kotlinc_options-wextra"></a>wextra |  Enable extra checkers for K2.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_abi_stability"></a>x_abi_stability |  When using unstable compiler features such as FIR, use 'stable' to mark generated class files as stable to prevent diagnostics from being reported when using stable compilers at the call site. When using the JVM IR backend, conversely, use 'unstable' to mark generated class files as unstable to force diagnostics to be reported.   | String | optional |  `""`  |
| <a id="kt_kotlinc_options-x_allow_condition_implies_returns_contracts"></a>x_allow_condition_implies_returns_contracts |  Allow contracts that specify a limited conditional returns postcondition.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_allow_contracts_on_more_functions"></a>x_allow_contracts_on_more_functions |  Allow contracts on some operators and accessors, and allow checks for erased types.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_allow_holdsin_contract"></a>x_allow_holdsin_contract |  Allow contracts that specify a condition that holds true inside a lambda argument.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_allow_kotlin_package"></a>x_allow_kotlin_package |  Allow compiling code in the 'kotlin' package, and allow not requiring 'kotlin.stdlib' in 'module-info'.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_allow_reified_type_in_catch"></a>x_allow_reified_type_in_catch |  Allow 'catch' parameters to have reified types.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_allow_unstable_dependencies"></a>x_allow_unstable_dependencies |  Do not report errors on classes in dependencies that were compiled by an unstable version of the Kotlin compiler.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_annotation_default_target"></a>x_annotation_default_target |  Change the default annotation targets for constructor properties: -Xannotation-default-target=first-only:      use the first of the following allowed targets: '@param:', '@property:', '@field:'; -Xannotation-default-target=first-only-warn: same as first-only, and raise warnings when both '@param:' and either '@property:' or '@field:' are allowed; -Xannotation-default-target=param-property:  use '@param:' target if applicable, and also use the first of either '@property:' or '@field:'; default: 'first-only-warn' in language version 2.2+, 'first-only' in version 2.1 and before.   | String | optional |  `""`  |
| <a id="kt_kotlinc_options-x_annotation_target_all"></a>x_annotation_target_all |  Enable experimental language support for @all: annotation use-site target.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_annotations_in_metadata"></a>x_annotations_in_metadata |  Write annotations on declarations into the metadata (in addition to the JVM bytecode), and read annotations from the metadata if they are present.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_assertions"></a>x_assertions |  'kotlin.assert' call behavior: -Xassertions=always-enable:  enable, ignore JVM assertion settings; -Xassertions=always-disable: disable, ignore JVM assertion settings; -Xassertions=jvm:            enable, depend on JVM assertion settings; -Xassertions=legacy:         calculate the condition on each call, the behavior depends on JVM assertion settings in the kotlin package; default: legacy   | String | optional |  `""`  |
| <a id="kt_kotlinc_options-x_backend_threads"></a>x_backend_threads |  Run codegen phase in N parallel threads. 0 means use one thread per processor core. The default value is 1.   | Integer | optional |  `1`  |
| <a id="kt_kotlinc_options-x_check_phase_conditions"></a>x_check_phase_conditions |  Check pre- and postconditions of IR lowering phases.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_compiler_plugin_order"></a>x_compiler_plugin_order |  Specify an execution order constraint for compiler plugins. Order constraint can be specified using the 'pluginId' of compiler plugins. The first specified plugin will be executed before the second plugin. Multiple constraints can be specified by repeating this option. Cycles in constraints will cause an error.   | List of strings | optional |  `[]`  |
| <a id="kt_kotlinc_options-x_consistent_data_class_copy_visibility"></a>x_consistent_data_class_copy_visibility |  The effect of this compiler flag is the same as applying @ConsistentCopyVisibility annotation to all data classes in the module. See https://youtrack.jetbrains.com/issue/KT-11914   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_context_parameters"></a>x_context_parameters |  Enable experimental context parameters.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_context_receivers"></a>x_context_receivers |  Enable experimental context receivers.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_context_sensitive_resolution"></a>x_context_sensitive_resolution |  Enable experimental context-sensitive resolution.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_data_flow_based_exhaustiveness"></a>x_data_flow_based_exhaustiveness |  Enable `when` exhaustiveness improvements that rely on data-flow analysis.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_debug"></a>x_debug |  Enable debug mode for compilation. Currently this includes spilling all variables in a suspending context regardless of whether they are alive. If API Level >= 2.2 -- no-op.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_detailed_perf"></a>x_detailed_perf |  Enable more detailed performance statistics (Experimental). For Native, the performance report includes execution time and lines processed per second for every individual lowering. For WASM and JS, the performance report includes execution time and lines per second for each lowering of the first stage of compilation.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_direct_java_actualization"></a>x_direct_java_actualization |  Enable experimental direct Java actualization support.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_disable_phases"></a>x_disable_phases |  Disable backend phases.   | List of strings | optional |  `[]`  |
| <a id="kt_kotlinc_options-x_dont_warn_on_error_suppression"></a>x_dont_warn_on_error_suppression |  Don't report warnings when errors are suppressed. This only affects K2.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_emit_jvm_type_annotations"></a>x_emit_jvm_type_annotations |  Emit JVM type annotations in bytecode.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_enable_incremental_compilation"></a>x_enable_incremental_compilation |  Enable incremental compilation.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_enhance_type_parameter_types_to_def_not_null"></a>x_enhance_type_parameter_types_to_def_not_null |  Enhance not-null-annotated type parameter types to definitely-non-nullable types ('@NotNull T' => 'T & Any').   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_enhanced_coroutines_debugging"></a>x_enhanced_coroutines_debugging |  Generate additional linenumber instruction for compiler-generated code inside suspend functions and lambdas to distinguish them from user code by debugger.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_expect_actual_classes"></a>x_expect_actual_classes |  'expect'/'actual' classes (including interfaces, objects, annotations, enums, and 'actual' typealiases) are in Beta. Kotlin reports a warning every time you use one of them. You can use this flag to mute the warning.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_explicit_api"></a>x_explicit_api |  Force the compiler to report errors on all public API declarations without an explicit visibility or a return type. Use the 'warning' level to issue warnings instead of errors.   | String | optional |  `""`  |
| <a id="kt_kotlinc_options-x_explicit_backing_fields"></a>x_explicit_backing_fields |  Enable experimental language support for explicit backing fields.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_generate_strict_metadata_version"></a>x_generate_strict_metadata_version |  Generate metadata with strict version semantics (see the KDoc entry on 'Metadata.extraInt').   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_ignore_const_optimization_errors"></a>x_ignore_const_optimization_errors |  Ignore all compilation exceptions while optimizing some constant expressions.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_indy_allow_annotated_lambdas"></a>x_indy_allow_annotated_lambdas |  Allow using 'invokedynamic' for lambda expressions with annotations   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_inline_classes"></a>x_inline_classes |  Enable experimental inline classes.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_ir_do_not_clear_binding_context"></a>x_ir_do_not_clear_binding_context |  When using the IR backend, do not clear BindingContext between 'psi2ir' and lowerings.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_jdk_release"></a>x_jdk_release |  Compile against the specified JDK API version, similarly to javac's '-release'. This requires JDK 9 or newer. The supported versions depend on the JDK used; for JDK 17+, the supported versions are 1.8 and 9–25. This also sets the value of '-jvm-target' to be equal to the selected JDK version.   | String | optional |  `""`  |
| <a id="kt_kotlinc_options-x_jspecify_annotations"></a>x_jspecify_annotations |  Specify the behavior of 'jspecify' annotations. The default value is 'strict'.   | String | optional |  `""`  |
| <a id="kt_kotlinc_options-x_jsr305"></a>x_jsr305 |  Specify the behavior of 'JSR-305' nullability annotations: -Xjsr305={ignore/strict/warn}                   global (all non-@UnderMigration annotations) -Xjsr305=under-migration:{ignore/strict/warn}   all @UnderMigration annotations -Xjsr305=@<fq.name>:{ignore/strict/warn}        annotation with the given fully qualified class name Modes: * ignore * strict (experimental; treat like other supported nullability annotations) * warn (report a warning)   | List of strings | optional |  `[]`  |
| <a id="kt_kotlinc_options-x_jvm_default"></a>x_jvm_default |  DEPRECATED: Use -jvm-default instead.<br><br>This option is deprecated. Migrate to -jvm-default as follows: -Xjvm-default=disable            -> -jvm-default=disable -Xjvm-default=all-compatibility  -> -jvm-default=enable -Xjvm-default=all                -> -jvm-default=no-compatibility   | String | optional |  `""`  |
| <a id="kt_kotlinc_options-x_jvm_enable_preview"></a>x_jvm_enable_preview |  Allow using Java features that are in the preview phase. This works like '--enable-preview' in Java. All class files are marked as compiled with preview features, meaning it won't be possible to use them in release environments.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_jvm_expose_boxed"></a>x_jvm_expose_boxed |  Expose inline classes and functions, accepting and returning them, to Java.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_lambdas"></a>x_lambdas |  Select the code generation scheme for lambdas. -Xlambdas=indy                  Generate lambdas using 'invokedynamic' with 'LambdaMetafactory.metafactory'.                                 A lambda object created using 'LambdaMetafactory.metafactory' will have a different 'toString()'. -Xlambdas=class                 Generate lambdas as explicit classes. The default value is 'indy' if language version is 2.0+, and 'class' otherwise.   | String | optional |  `""`  |
| <a id="kt_kotlinc_options-x_link_via_signatures"></a>x_link_via_signatures |  Link JVM IR symbols via signatures instead of descriptors. This mode is slower, but it can be useful for troubleshooting problems with the JVM IR backend. This option is deprecated and will be deleted in future versions. It has no effect when -language-version is 2.0 or higher.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_list_phases"></a>x_list_phases |  List backend phases.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_metadata_klib"></a>x_metadata_klib |  Produce a klib that only contains the metadata of declarations.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_metadata_version"></a>x_metadata_version |  Change the metadata version of the generated binary files.   | String | optional |  `""`  |
| <a id="kt_kotlinc_options-x_multi_dollar_interpolation"></a>x_multi_dollar_interpolation |  Enable experimental multi-dollar interpolation.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_multi_platform"></a>x_multi_platform |  Enable language support for multiplatform projects.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_multifile_parts_inherit"></a>x_multifile_parts_inherit |  Compile multifile classes as a hierarchy of parts and a facade.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_name_based_destructuring"></a>x_name_based_destructuring |  Enables the following destructuring features: -Xname-based-destructuring=only-syntax:   Enables syntax for positional destructuring with square brackets and the full form of name-based destructuring with parentheses; -Xname-based-destructuring=name-mismatch: Reports warnings when short form positional destructuring of data classes uses names that don't match the property names; -Xname-based-destructuring=complete:      Enables short-form name-based destructuring with parentheses;   | String | optional |  `""`  |
| <a id="kt_kotlinc_options-x_nested_type_aliases"></a>x_nested_type_aliases |  Enable experimental language support for nested type aliases.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_new_inference"></a>x_new_inference |  Enable the new experimental generic type inference algorithm.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_no_call_assertions"></a>x_no_call_assertions |  Don't generate not-null assertions for arguments of platform types.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_no_check_actual"></a>x_no_check_actual |  Do not check for the presence of the 'actual' modifier in multiplatform projects.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_no_inline"></a>x_no_inline |  Disable method inlining.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_no_new_java_annotation_targets"></a>x_no_new_java_annotation_targets |  Don't generate Java 1.8+ targets for Kotlin annotation classes.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_no_optimize"></a>x_no_optimize |  Disable optimizations.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_no_param_assertions"></a>x_no_param_assertions |  Don't generate not-null assertions on parameters of methods accessible from Java.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_no_receiver_assertions"></a>x_no_receiver_assertions |  Don't generate not-null assertions for extension receiver arguments of platform types.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_no_unified_null_checks"></a>x_no_unified_null_checks |  Use pre-1.4 exception types instead of 'java.lang.NPE' in null checks. See KT-22275 for more details.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_non_local_break_continue"></a>x_non_local_break_continue |  Enable experimental non-local break and continue.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_nullability_annotations"></a>x_nullability_annotations |  Specify the behavior for specific Java nullability annotations (provided with fully qualified package name). Modes: * ignore * strict * warn (report a warning)   | List of strings | optional |  `[]`  |
| <a id="kt_kotlinc_options-x_phases_to_dump"></a>x_phases_to_dump |  Dump the backend's state both before and after these phases.   | List of strings | optional |  `[]`  |
| <a id="kt_kotlinc_options-x_phases_to_dump_after"></a>x_phases_to_dump_after |  Dump the backend's state after these phases.   | List of strings | optional |  `[]`  |
| <a id="kt_kotlinc_options-x_phases_to_dump_before"></a>x_phases_to_dump_before |  Dump the backend's state before these phases.   | List of strings | optional |  `[]`  |
| <a id="kt_kotlinc_options-x_phases_to_validate"></a>x_phases_to_validate |  Validate the backend's state both before and after these phases.   | List of strings | optional |  `[]`  |
| <a id="kt_kotlinc_options-x_phases_to_validate_after"></a>x_phases_to_validate_after |  Validate the backend's state after these phases.   | List of strings | optional |  `[]`  |
| <a id="kt_kotlinc_options-x_phases_to_validate_before"></a>x_phases_to_validate_before |  Validate the backend's state before these phases.   | List of strings | optional |  `[]`  |
| <a id="kt_kotlinc_options-x_profile_phases"></a>x_profile_phases |  Profile backend phases.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_render_internal_diagnostic_names"></a>x_render_internal_diagnostic_names |  Render the internal names of warnings and errors.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_report_all_warnings"></a>x_report_all_warnings |  Report all warnings even if errors are found.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_report_output_files"></a>x_report_output_files |  Report the source-to-output file mapping.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_report_perf"></a>x_report_perf |  Report detailed performance statistics.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_return_value_checker"></a>x_return_value_checker |  Set improved unused return value checker mode. Use 'check' to run checker only and use 'full' to also enable automatic annotation insertion.   | String | optional |  `""`  |
| <a id="kt_kotlinc_options-x_sam_conversions"></a>x_sam_conversions |  Select the code generation scheme for SAM conversions. -Xsam-conversions=indy          Generate SAM conversions using 'invokedynamic' with 'LambdaMetafactory.metafactory'. -Xsam-conversions=class         Generate SAM conversions as explicit classes. The default value is 'indy'.   | String | optional |  `""`  |
| <a id="kt_kotlinc_options-x_sanitize_parentheses"></a>x_sanitize_parentheses |  Transform '(' and ')' in method names to some other character sequence. This mode can BREAK BINARY COMPATIBILITY and should only be used as a workaround for problems with parentheses in identifiers on certain platforms.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_separate_kmp_compilation"></a>x_separate_kmp_compilation |  Enables the separated compilation scheme, in which common source sets are analyzed against their own dependencies   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_serialize_ir"></a>x_serialize_ir |  Save the IR to metadata (Experimental).   | String | optional |  `""`  |
| <a id="kt_kotlinc_options-x_skip_metadata_version_check"></a>x_skip_metadata_version_check |  Allow loading classes with bad metadata versions and pre-release classes.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_skip_prerelease_check"></a>x_skip_prerelease_check |  Allow loading pre-release classes.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_string_concat"></a>x_string_concat |  Select the code generation scheme for string concatenation: -Xstring-concat=indy-with-constants  Concatenate strings using 'invokedynamic' and 'makeConcatWithConstants'. This requires '-jvm-target 9' or greater. -Xstring-concat=indy                 Concatenate strings using 'invokedynamic' and 'makeConcat'. This requires '-jvm-target 9' or greater. -Xstring-concat=inline               Concatenate strings using 'StringBuilder' default: 'indy-with-constants' for JVM targets 9 or greater, 'inline' otherwise.   | String | optional |  `""`  |
| <a id="kt_kotlinc_options-x_support_compatqual_checker_framework_annotations"></a>x_support_compatqual_checker_framework_annotations |  Specify the behavior for Checker Framework 'compatqual' annotations ('NullableDecl'/'NonNullDecl'). The default value is 'enable'.   | String | optional |  `""`  |
| <a id="kt_kotlinc_options-x_suppress_api_version_greater_than_language_version_error"></a>x_suppress_api_version_greater_than_language_version_error |  Suppress error about API version greater than language version. Warning: This is temporary solution (see KT-63712) intended to be used only for stdlib build.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_suppress_deprecated_jvm_target_warning"></a>x_suppress_deprecated_jvm_target_warning |  Suppress warnings about deprecated JVM target versions. This option has no effect and will be deleted in a future version.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_suppress_missing_builtins_error"></a>x_suppress_missing_builtins_error |  Suppress the "cannot access built-in declaration" error (useful with '-no-stdlib').   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_suppress_version_warnings"></a>x_suppress_version_warnings |  Suppress warnings about outdated, inconsistent, or experimental language or API versions.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_suppress_warning"></a>x_suppress_warning |  Suppress specified warning module-wide. This option is deprecated in favor of "-Xwarning-level" flag   | List of strings | optional |  `[]`  |
| <a id="kt_kotlinc_options-x_type_enhancement_improvements_strict_mode"></a>x_type_enhancement_improvements_strict_mode |  Enable strict mode for improvements to type enhancement for loaded Java types based on nullability annotations, including the ability to read type-use annotations from class files. See KT-45671 for more details.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_unrestricted_builder_inference"></a>x_unrestricted_builder_inference |  Eliminate builder inference restrictions, for example by allowing type variables to be returned from builder inference calls.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_use_14_inline_classes_mangling_scheme"></a>x_use_14_inline_classes_mangling_scheme |  Use the scheme for inline class mangling from version 1.4 instead of the one from 1.4.30.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_use_fast_jar_file_system"></a>x_use_fast_jar_file_system |  Use the fast implementation of Jar FS. This may speed up compilation time, but it is experimental.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_use_fir_experimental_checkers"></a>x_use_fir_experimental_checkers |  Enable experimental frontend IR checkers that are not yet ready for production.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_use_fir_ic"></a>x_use_fir_ic |  Compile using frontend IR internal incremental compilation. Warning: This feature is not yet production-ready.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_use_fir_lt"></a>x_use_fir_lt |  Compile using the LightTree parser with the frontend IR.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_use_inline_scopes_numbers"></a>x_use_inline_scopes_numbers |  Use inline scopes numbers for inline marker variables.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_use_old_class_files_reading"></a>x_use_old_class_files_reading |  Use the old implementation for reading class files. This may slow down the compilation and cause problems with Groovy interop. This can be used in the event of problems with the new implementation.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_use_type_table"></a>x_use_type_table |  Use a type table in metadata serialization.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_validate_bytecode"></a>x_validate_bytecode |  Validate generated JVM bytecode before and after optimizations.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_value_classes"></a>x_value_classes |  Enable experimental value classes.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_verbose_phases"></a>x_verbose_phases |  Be verbose while performing the given backend phases.   | List of strings | optional |  `[]`  |
| <a id="kt_kotlinc_options-x_verify_ir"></a>x_verify_ir |  IR verification mode (no verification by default).   | String | optional |  `""`  |
| <a id="kt_kotlinc_options-x_verify_ir_visibility"></a>x_verify_ir_visibility |  Check for visibility violations in IR when validating it before running any lowerings. Only has effect if '-Xverify-ir' is not 'none'.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_warning_level"></a>x_warning_level |  Suppress specific warnings globally. Ex: 'OPTION': '(error\|warning\|disabled)'   | <a href="https://bazel.build/rules/lib/dict">Dictionary: String -> String</a> | optional |  `{}`  |
| <a id="kt_kotlinc_options-x_when_expressions"></a>x_when_expressions |  Select the code generation scheme for type-checking 'when' expressions: -Xwhen-expressions=indy         Generate type-checking 'when' expressions using 'invokedynamic' with 'SwitchBootstraps.typeSwitch(..)' and                                 following 'tableswitch' or 'lookupswitch'. This requires '-jvm-target 21' or greater. -Xwhen-expressions=inline       Generate type-checking 'when' expressions as a chain of type checks. The default value is 'inline'.   | String | optional |  `""`  |
| <a id="kt_kotlinc_options-x_when_guards"></a>x_when_guards |  Enable experimental language support for when guards.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_xdebug_level_compiler_checks"></a>x_xdebug_level_compiler_checks |  Enable debug level compiler checks. ATTENTION: these checks can slow compiler down or even crash it.   | Boolean | optional |  `False`  |
| <a id="kt_kotlinc_options-x_xdump_model"></a>x_xdump_model |  Dump compilation model to specified directory for use in modularized tests.   | String | optional |  `""`  |
| <a id="kt_kotlinc_options-x_xexplicit_return_types"></a>x_xexplicit_return_types |  Force the compiler to report errors on all public API declarations without an explicit return type. Use the 'warning' level to issue warnings instead of errors. This flag partially enables functionality of `-Xexplicit-api` flag, so please don't use them altogether   | String | optional |  `""`  |
| <a id="kt_kotlinc_options-x_xlanguage"></a>x_xlanguage |  Enables/disables specified language feature. Warning: this flag is not intended for production use. If you want to configure the language behaviour use the -language-version or corresponding experimental feature flags.   | List of strings | optional |  `[]`  |
| <a id="kt_kotlinc_options-x_xlenient_mode"></a>x_xlenient_mode |  Lenient compiler mode. When actuals are missing, placeholder declarations are generated.   | Boolean | optional |  `False`  |


<a id="kt_ksp_plugin"></a>

## kt_ksp_plugin

<pre>
load("@rules_kotlin//kotlin:core.bzl", "kt_ksp_plugin")

kt_ksp_plugin(<a href="#kt_ksp_plugin-name">name</a>, <a href="#kt_ksp_plugin-deps">deps</a>, <a href="#kt_ksp_plugin-generates_java">generates_java</a>, <a href="#kt_ksp_plugin-processor_class">processor_class</a>)
</pre>

Define a KSP plugin for the Kotlin compiler to run. The plugin can then be referenced in the `plugins` attribute
of the `kt_jvm_*` and `kt_android_*` rules.

An example can be found under `//examples/ksp`:

```bzl
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

**ATTRIBUTES**


| Name  | Description | Type | Mandatory | Default |
| :------------- | :------------- | :------------- | :------------- | :------------- |
| <a id="kt_ksp_plugin-name"></a>name |  A unique name for this target.   | <a href="https://bazel.build/concepts/labels#target-names">Name</a> | required |  |
| <a id="kt_ksp_plugin-deps"></a>deps |  The list of libraries to be added to the compiler's plugin classpath   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |
| <a id="kt_ksp_plugin-generates_java"></a>generates_java |  Runs Java compilation action for plugin generating Java output.   | Boolean | optional |  `False`  |
| <a id="kt_ksp_plugin-processor_class"></a>processor_class |  The fully qualified class name that the Java compiler uses as an entry point to the annotation processor.   | String | required |  |


<a id="kt_plugin_cfg"></a>

## kt_plugin_cfg

<pre>
load("@rules_kotlin//kotlin:core.bzl", "kt_plugin_cfg")

kt_plugin_cfg(<a href="#kt_plugin_cfg-name">name</a>, <a href="#kt_plugin_cfg-deps">deps</a>, <a href="#kt_plugin_cfg-data">data</a>, <a href="#kt_plugin_cfg-options">options</a>, <a href="#kt_plugin_cfg-plugin">plugin</a>)
</pre>

Configurations for kt_compiler_plugin, ksp_plugin, and java_plugin.

This allows setting options and dependencies independently from the initial plugin definition.

**ATTRIBUTES**


| Name  | Description | Type | Mandatory | Default |
| :------------- | :------------- | :------------- | :------------- | :------------- |
| <a id="kt_plugin_cfg-name"></a>name |  A unique name for this target.   | <a href="https://bazel.build/concepts/labels#target-names">Name</a> | required |  |
| <a id="kt_plugin_cfg-deps"></a>deps |  Dependencies for this configuration.   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |
| <a id="kt_plugin_cfg-data"></a>data |  The list of data files to be used by compiler's plugin   | <a href="https://bazel.build/concepts/labels">List of labels</a> | optional |  `[]`  |
| <a id="kt_plugin_cfg-options"></a>options |  A dictionary of flag to values to be used as plugin configuration options.   | <a href="https://bazel.build/rules/lib/dict">Dictionary: String -> List of strings</a> | optional |  `{}`  |
| <a id="kt_plugin_cfg-plugin"></a>plugin |  The plugin to associate with this configuration   | <a href="https://bazel.build/concepts/labels">Label</a> | required |  |


<a id="define_kt_toolchain"></a>

## define_kt_toolchain

<pre>
load("@rules_kotlin//kotlin:core.bzl", "define_kt_toolchain")

define_kt_toolchain(<a href="#define_kt_toolchain-name">name</a>, <a href="#define_kt_toolchain-language_version">language_version</a>, <a href="#define_kt_toolchain-api_version">api_version</a>, <a href="#define_kt_toolchain-jvm_target">jvm_target</a>, <a href="#define_kt_toolchain-experimental_use_abi_jars">experimental_use_abi_jars</a>,
                    <a href="#define_kt_toolchain-experimental_treat_internal_as_private_in_abi_jars">experimental_treat_internal_as_private_in_abi_jars</a>,
                    <a href="#define_kt_toolchain-experimental_remove_private_classes_in_abi_jars">experimental_remove_private_classes_in_abi_jars</a>,
                    <a href="#define_kt_toolchain-experimental_remove_debug_info_in_abi_jars">experimental_remove_debug_info_in_abi_jars</a>, <a href="#define_kt_toolchain-experimental_strict_kotlin_deps">experimental_strict_kotlin_deps</a>,
                    <a href="#define_kt_toolchain-experimental_report_unused_deps">experimental_report_unused_deps</a>, <a href="#define_kt_toolchain-experimental_reduce_classpath_mode">experimental_reduce_classpath_mode</a>,
                    <a href="#define_kt_toolchain-experimental_multiplex_workers">experimental_multiplex_workers</a>, <a href="#define_kt_toolchain-experimental_incremental_compilation">experimental_incremental_compilation</a>,
                    <a href="#define_kt_toolchain-experimental_ic_enable_logging">experimental_ic_enable_logging</a>, <a href="#define_kt_toolchain-javac_options">javac_options</a>, <a href="#define_kt_toolchain-kotlinc_options">kotlinc_options</a>, <a href="#define_kt_toolchain-jvm_stdlibs">jvm_stdlibs</a>,
                    <a href="#define_kt_toolchain-jvm_runtime">jvm_runtime</a>, <a href="#define_kt_toolchain-jacocorunner">jacocorunner</a>, <a href="#define_kt_toolchain-btapi_build_tools_impl">btapi_build_tools_impl</a>,
                    <a href="#define_kt_toolchain-btapi_kotlin_compiler_embeddable">btapi_kotlin_compiler_embeddable</a>, <a href="#define_kt_toolchain-btapi_kotlin_daemon_client">btapi_kotlin_daemon_client</a>, <a href="#define_kt_toolchain-btapi_kotlin_stdlib">btapi_kotlin_stdlib</a>,
                    <a href="#define_kt_toolchain-btapi_kotlin_reflect">btapi_kotlin_reflect</a>, <a href="#define_kt_toolchain-btapi_kotlin_coroutines">btapi_kotlin_coroutines</a>, <a href="#define_kt_toolchain-btapi_annotations">btapi_annotations</a>,
                    <a href="#define_kt_toolchain-internal_jvm_abi_gen">internal_jvm_abi_gen</a>, <a href="#define_kt_toolchain-internal_skip_code_gen">internal_skip_code_gen</a>, <a href="#define_kt_toolchain-internal_jdeps_gen">internal_jdeps_gen</a>, <a href="#define_kt_toolchain-internal_kapt">internal_kapt</a>,
                    <a href="#define_kt_toolchain-exec_compatible_with">exec_compatible_with</a>, <a href="#define_kt_toolchain-target_compatible_with">target_compatible_with</a>, <a href="#define_kt_toolchain-target_settings">target_settings</a>)
</pre>

Define the Kotlin toolchain.

**PARAMETERS**


| Name  | Description | Default Value |
| :------------- | :------------- | :------------- |
| <a id="define_kt_toolchain-name"></a>name |  <p align="center"> - </p>   |  none |
| <a id="define_kt_toolchain-language_version"></a>language_version |  <p align="center"> - </p>   |  `None` |
| <a id="define_kt_toolchain-api_version"></a>api_version |  <p align="center"> - </p>   |  `None` |
| <a id="define_kt_toolchain-jvm_target"></a>jvm_target |  <p align="center"> - </p>   |  `None` |
| <a id="define_kt_toolchain-experimental_use_abi_jars"></a>experimental_use_abi_jars |  <p align="center"> - </p>   |  `False` |
| <a id="define_kt_toolchain-experimental_treat_internal_as_private_in_abi_jars"></a>experimental_treat_internal_as_private_in_abi_jars |  <p align="center"> - </p>   |  `False` |
| <a id="define_kt_toolchain-experimental_remove_private_classes_in_abi_jars"></a>experimental_remove_private_classes_in_abi_jars |  <p align="center"> - </p>   |  `False` |
| <a id="define_kt_toolchain-experimental_remove_debug_info_in_abi_jars"></a>experimental_remove_debug_info_in_abi_jars |  <p align="center"> - </p>   |  `False` |
| <a id="define_kt_toolchain-experimental_strict_kotlin_deps"></a>experimental_strict_kotlin_deps |  <p align="center"> - </p>   |  `None` |
| <a id="define_kt_toolchain-experimental_report_unused_deps"></a>experimental_report_unused_deps |  <p align="center"> - </p>   |  `None` |
| <a id="define_kt_toolchain-experimental_reduce_classpath_mode"></a>experimental_reduce_classpath_mode |  <p align="center"> - </p>   |  `None` |
| <a id="define_kt_toolchain-experimental_multiplex_workers"></a>experimental_multiplex_workers |  <p align="center"> - </p>   |  `None` |
| <a id="define_kt_toolchain-experimental_incremental_compilation"></a>experimental_incremental_compilation |  <p align="center"> - </p>   |  `None` |
| <a id="define_kt_toolchain-experimental_ic_enable_logging"></a>experimental_ic_enable_logging |  <p align="center"> - </p>   |  `None` |
| <a id="define_kt_toolchain-javac_options"></a>javac_options |  <p align="center"> - </p>   |  `Label("@rules_kotlin//kotlin/internal:default_javac_options")` |
| <a id="define_kt_toolchain-kotlinc_options"></a>kotlinc_options |  <p align="center"> - </p>   |  `Label("@rules_kotlin//kotlin/internal:default_kotlinc_options")` |
| <a id="define_kt_toolchain-jvm_stdlibs"></a>jvm_stdlibs |  <p align="center"> - </p>   |  `None` |
| <a id="define_kt_toolchain-jvm_runtime"></a>jvm_runtime |  <p align="center"> - </p>   |  `None` |
| <a id="define_kt_toolchain-jacocorunner"></a>jacocorunner |  <p align="center"> - </p>   |  `None` |
| <a id="define_kt_toolchain-btapi_build_tools_impl"></a>btapi_build_tools_impl |  <p align="center"> - </p>   |  `None` |
| <a id="define_kt_toolchain-btapi_kotlin_compiler_embeddable"></a>btapi_kotlin_compiler_embeddable |  <p align="center"> - </p>   |  `None` |
| <a id="define_kt_toolchain-btapi_kotlin_daemon_client"></a>btapi_kotlin_daemon_client |  <p align="center"> - </p>   |  `None` |
| <a id="define_kt_toolchain-btapi_kotlin_stdlib"></a>btapi_kotlin_stdlib |  <p align="center"> - </p>   |  `None` |
| <a id="define_kt_toolchain-btapi_kotlin_reflect"></a>btapi_kotlin_reflect |  <p align="center"> - </p>   |  `None` |
| <a id="define_kt_toolchain-btapi_kotlin_coroutines"></a>btapi_kotlin_coroutines |  <p align="center"> - </p>   |  `None` |
| <a id="define_kt_toolchain-btapi_annotations"></a>btapi_annotations |  <p align="center"> - </p>   |  `None` |
| <a id="define_kt_toolchain-internal_jvm_abi_gen"></a>internal_jvm_abi_gen |  <p align="center"> - </p>   |  `None` |
| <a id="define_kt_toolchain-internal_skip_code_gen"></a>internal_skip_code_gen |  <p align="center"> - </p>   |  `None` |
| <a id="define_kt_toolchain-internal_jdeps_gen"></a>internal_jdeps_gen |  <p align="center"> - </p>   |  `None` |
| <a id="define_kt_toolchain-internal_kapt"></a>internal_kapt |  <p align="center"> - </p>   |  `None` |
| <a id="define_kt_toolchain-exec_compatible_with"></a>exec_compatible_with |  <p align="center"> - </p>   |  `None` |
| <a id="define_kt_toolchain-target_compatible_with"></a>target_compatible_with |  <p align="center"> - </p>   |  `None` |
| <a id="define_kt_toolchain-target_settings"></a>target_settings |  <p align="center"> - </p>   |  `None` |


<a id="kt_register_toolchains"></a>

## kt_register_toolchains

<pre>
load("@rules_kotlin//kotlin:core.bzl", "kt_register_toolchains")

kt_register_toolchains()
</pre>

This macro registers the kotlin toolchain.



<!-- Generated with Stardoc: http://skydoc.bazel.build -->



<a id="kotlin_repositories"></a>

## kotlin_repositories

<pre>
load("@rules_kotlin//kotlin:repositories.doc.bzl", "kotlin_repositories")

kotlin_repositories(<a href="#kotlin_repositories-is_bzlmod">is_bzlmod</a>, <a href="#kotlin_repositories-compiler_repository_name">compiler_repository_name</a>, <a href="#kotlin_repositories-compiler_version">compiler_version</a>)
</pre>

Call this in the WORKSPACE file to setup the Kotlin rules.

**PARAMETERS**


| Name  | Description | Default Value |
| :------------- | :------------- | :------------- |
| <a id="kotlin_repositories-is_bzlmod"></a>is_bzlmod |  <p align="center"> - </p>   |  `False` |
| <a id="kotlin_repositories-compiler_repository_name"></a>compiler_repository_name |  for the kotlinc compiler repository.   |  `"com_github_jetbrains_kotlin"` |
| <a id="kotlin_repositories-compiler_version"></a>compiler_version |  Kotlin compiler version string (e.g. "2.3.20-Beta2").   |  `"2.3.20-Beta2"` |


<a id="versions.use_repository"></a>

## versions.use_repository

<pre>
load("@rules_kotlin//kotlin:repositories.doc.bzl", "versions")

versions.use_repository(<a href="#versions.use_repository-rule">rule</a>, <a href="#versions.use_repository-name">name</a>, <a href="#versions.use_repository-version">version</a>, <a href="#versions.use_repository-kwargs">**kwargs</a>)
</pre>



**PARAMETERS**


| Name  | Description | Default Value |
| :------------- | :------------- | :------------- |
| <a id="versions.use_repository-rule"></a>rule |  <p align="center"> - </p>   |  none |
| <a id="versions.use_repository-name"></a>name |  <p align="center"> - </p>   |  none |
| <a id="versions.use_repository-version"></a>version |  <p align="center"> - </p>   |  none |
| <a id="versions.use_repository-kwargs"></a>kwargs |  <p align="center"> - </p>   |  none |


