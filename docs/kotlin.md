<!-- Generated with Stardoc: http://skydoc.bazel.build -->

<a id="#kt_compiler_plugin"></a>

## kt_compiler_plugin

kt_compiler_plugin(<a href="#kt_compiler_plugin-name">name</a>, <a href="#kt_compiler_plugin-compile_phase">compile_phase</a>, <a href="#kt_compiler_plugin-deps">deps</a>, <a href="#kt_compiler_plugin-id">id</a>, <a href="#kt_compiler_plugin-options">options</a>, <a href="#kt_compiler_plugin-stubs_phase">stubs_phase</a>, <a href="#kt_compiler_plugin-target_embedded_compiler">target_embedded_compiler</a>)

                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    
            Define a plugin for the Kotlin compiler to run. The plugin can then be referenced in the `plugins` attribute
        of the `kt_jvm_*` rules.
                
        An example can be found under `//examples/plugin`:
                
        ```bzl
        kt_compiler_plugin(
            name = "open_for_testing_plugin",
            id = "org.jetbrains.kotlin.allopen",
            options = {
                "annotation": "plugin.OpenForTesting",
            },
            deps = [
                "@com_github_jetbrains_kotlin//:allopen-compiler-plugin",
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
        |
    <a id="kt_compiler_plugin-name"></a>name |  A unique name for this target.   | <a href="https://bazel.build/docs/build-ref.html#name">Name</a> | required |  |
        |
    <a id="kt_compiler_plugin-compile_phase"></a>compile_phase |  Runs the compiler plugin during kotlin compilation. Known examples: allopen, sam_with_reciever   | Boolean | optional | False |
        |
    <a id="kt_compiler_plugin-deps"></a>deps |  The list of libraries to be added to the compiler's plugin classpath   | <a href="https://bazel.build/docs/build-ref.html#labels">List of labels</a> | optional | [] |
        |
    <a id="kt_compiler_plugin-id"></a>id |  The ID of the plugin   | String | required |  |
        |
    <a id="kt_compiler_plugin-options"></a>options |  Dictionary of options to be passed to the plugin.             Supports the following template values:                <code>{generatedClasses}</code>: directory for generated class output                <code>{temp}</code>: temporary directory, discarded between invocations                <code>{generatedSources}</code>:  directory for generated source output   | <a href="https://bazel.build/docs/skylark/lib/dict.html">Dictionary: String -> String</a> | optional | {} |
        |
    <a id="kt_compiler_plugin-stubs_phase"></a>stubs_phase |  Runs the compiler plugin before compile.   | Boolean | optional | False |
        |
    <a id="kt_compiler_plugin-target_embedded_compiler"></a>target_embedded_compiler |  Plugin was compiled agains the embeddable kotlin compiler. Requires different classpath   | Boolean | optional | False |
    

<a id="#kt_jvm_binary"></a>

## kt_jvm_binary

kt_jvm_binary(<a href="#kt_jvm_binary-name">name</a>, <a href="#kt_jvm_binary-data">data</a>, <a href="#kt_jvm_binary-deps">deps</a>, <a href="#kt_jvm_binary-jvm_flags">jvm_flags</a>, <a href="#kt_jvm_binary-main_class">main_class</a>, <a href="#kt_jvm_binary-module_name">module_name</a>, <a href="#kt_jvm_binary-plugins">plugins</a>, <a href="#kt_jvm_binary-resource_jars">resource_jars</a>,
              <a href="#kt_jvm_binary-resource_strip_prefix">resource_strip_prefix</a>, <a href="#kt_jvm_binary-resources">resources</a>, <a href="#kt_jvm_binary-runtime_deps">runtime_deps</a>, <a href="#kt_jvm_binary-srcs">srcs</a>)

                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        
            Builds a Java archive ("jar file"), plus a wrapper shell script with the same name as the rule. The wrapper
        shell script uses a classpath that includes, among other things, a jar file for each library on which the binary
        depends.
                
        **Note:** This rule does not have all of the features found in [`java_binary`](https://docs.bazel.build/versions/master/be/java.html#java_binary).
        It is appropriate for building workspace utilities. `java_binary` should be preferred for release artefacts.
        
    

**ATTRIBUTES**


| Name  | Description | Type | Mandatory | Default |
| :------------- | :------------- | :------------- | :------------- | :------------- |
        |
    <a id="kt_jvm_binary-name"></a>name |  A unique name for this target.   | <a href="https://bazel.build/docs/build-ref.html#name">Name</a> | required |  |
        |
    <a id="kt_jvm_binary-data"></a>data |  The list of files needed by this rule at runtime. See general comments about <code>data</code> at         [Attributes common to all build rules](https://docs.bazel.build/versions/master/be/common-definitions.html#common-attributes).   | <a href="https://bazel.build/docs/build-ref.html#labels">List of labels</a> | optional | [] |
        |
    <a id="kt_jvm_binary-deps"></a>deps |  A list of dependencies of this rule.See general comments about <code>deps</code> at         [Attributes common to all build rules](https://docs.bazel.build/versions/master/be/common-definitions.html#common-attributes).   | <a href="https://bazel.build/docs/build-ref.html#labels">List of labels</a> | optional | [] |
        |
    <a id="kt_jvm_binary-jvm_flags"></a>jvm_flags |  A list of flags to embed in the wrapper script generated for running this binary. Note: does not yet         support make variable substitution.   | List of strings | optional | [] |
        |
    <a id="kt_jvm_binary-main_class"></a>main_class |  Name of class with main() method to use as entry point.   | String | required |  |
        |
    <a id="kt_jvm_binary-module_name"></a>module_name |  The name of the module, if not provided the module name is derived from the label. --e.g.,         <code>//some/package/path:label_name</code> is translated to         <code>some_package_path-label_name</code>.   | String | optional | "" |
        |
    <a id="kt_jvm_binary-plugins"></a>plugins |  -   | <a href="https://bazel.build/docs/build-ref.html#labels">List of labels</a> | optional | [] |
        |
    <a id="kt_jvm_binary-resource_jars"></a>resource_jars |  Set of archives containing Java resources. If specified, the contents of these jars are merged into         the output jar.   | <a href="https://bazel.build/docs/build-ref.html#labels">List of labels</a> | optional | [] |
        |
    <a id="kt_jvm_binary-resource_strip_prefix"></a>resource_strip_prefix |  The path prefix to strip from Java resources, files residing under common prefix such as         <code>src/main/resources</code> or <code>src/test/resources</code> or <code>kotlin</code> will have stripping applied by convention.   | String | optional | "" |
        |
    <a id="kt_jvm_binary-resources"></a>resources |  A list of files that should be include in a Java jar.   | <a href="https://bazel.build/docs/build-ref.html#labels">List of labels</a> | optional | [] |
        |
    <a id="kt_jvm_binary-runtime_deps"></a>runtime_deps |  Libraries to make available to the final binary or test at runtime only. Like ordinary deps, these will         appear on the runtime classpath, but unlike them, not on the compile-time classpath.   | <a href="https://bazel.build/docs/build-ref.html#labels">List of labels</a> | optional | [] |
        |
    <a id="kt_jvm_binary-srcs"></a>srcs |  The list of source files that are processed to create the target, this can contain both Java and Kotlin         files. Java analysis occurs first so Kotlin classes may depend on Java classes in the same compilation unit.   | <a href="https://bazel.build/docs/build-ref.html#labels">List of labels</a> | optional | [] |
    

<a id="#kt_jvm_import"></a>

## kt_jvm_import

kt_jvm_import(<a href="#kt_jvm_import-name">name</a>, <a href="#kt_jvm_import-deps">deps</a>, <a href="#kt_jvm_import-exported_compiler_plugins">exported_compiler_plugins</a>, <a href="#kt_jvm_import-exports">exports</a>, <a href="#kt_jvm_import-jar">jar</a>, <a href="#kt_jvm_import-jars">jars</a>, <a href="#kt_jvm_import-neverlink">neverlink</a>, <a href="#kt_jvm_import-runtime_deps">runtime_deps</a>,
              <a href="#kt_jvm_import-srcjar">srcjar</a>)

                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        
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
        |
    <a id="kt_jvm_import-name"></a>name |  A unique name for this target.   | <a href="https://bazel.build/docs/build-ref.html#name">Name</a> | required |  |
        |
    <a id="kt_jvm_import-deps"></a>deps |  Compile and runtime dependencies   | <a href="https://bazel.build/docs/build-ref.html#labels">List of labels</a> | optional | [] |
        |
    <a id="kt_jvm_import-exported_compiler_plugins"></a>exported_compiler_plugins |  Exported compiler plugins.<br><br>            Compiler plugins listed here will be treated as if they were added in the plugins             attribute of any targets that directly depend on this target. Unlike java_plugins'             exported_plugins, this is not transitive   | <a href="https://bazel.build/docs/build-ref.html#labels">List of labels</a> | optional | [] |
        |
    <a id="kt_jvm_import-exports"></a>exports |  Exported libraries.<br><br>            Deps listed here will be made available to other rules, as if the parents explicitly depended on             these deps. This is not true for regular (non-exported) deps.   | <a href="https://bazel.build/docs/build-ref.html#labels">List of labels</a> | optional | [] |
        |
    <a id="kt_jvm_import-jar"></a>jar |  The jar listed here is equivalent to an export attribute.   | <a href="https://bazel.build/docs/build-ref.html#labels">Label</a> | optional | None |
        |
    <a id="kt_jvm_import-jars"></a>jars |  The jars listed here are equavalent to an export attribute. The label should be either to a single             class jar, or one or more filegroup labels.  The filegroups, when resolved, must contain  only one jar             containing classes, and (optionally) one peer file containing sources, named <code>&lt;jarname&gt;-sources.jar</code>.<br><br>            DEPRECATED - please use <code>jar</code> and <code>srcjar</code> attributes.   | <a href="https://bazel.build/docs/build-ref.html#labels">List of labels</a> | optional | [] |
        |
    <a id="kt_jvm_import-neverlink"></a>neverlink |  If true only use this library for compilation and not at runtime.   | Boolean | optional | False |
        |
    <a id="kt_jvm_import-runtime_deps"></a>runtime_deps |  Additional runtime deps.   | <a href="https://bazel.build/docs/build-ref.html#labels">List of labels</a> | optional | [] |
        |
    <a id="kt_jvm_import-srcjar"></a>srcjar |  The sources for the class jar.   | <a href="https://bazel.build/docs/build-ref.html#labels">Label</a> | optional | @io_bazel_rules_kotlin//third_party:empty.jar |
    

<a id="#kt_jvm_library"></a>

## kt_jvm_library

kt_jvm_library(<a href="#kt_jvm_library-name">name</a>, <a href="#kt_jvm_library-data">data</a>, <a href="#kt_jvm_library-deps">deps</a>, <a href="#kt_jvm_library-exported_compiler_plugins">exported_compiler_plugins</a>, <a href="#kt_jvm_library-exports">exports</a>, <a href="#kt_jvm_library-module_name">module_name</a>, <a href="#kt_jvm_library-neverlink">neverlink</a>,
               <a href="#kt_jvm_library-plugins">plugins</a>, <a href="#kt_jvm_library-resource_jars">resource_jars</a>, <a href="#kt_jvm_library-resource_strip_prefix">resource_strip_prefix</a>, <a href="#kt_jvm_library-resources">resources</a>, <a href="#kt_jvm_library-runtime_deps">runtime_deps</a>, <a href="#kt_jvm_library-srcs">srcs</a>)

                                                                                                
    This rule compiles and links Kotlin and Java sources into a .jar file.
    

**ATTRIBUTES**


| Name  | Description | Type | Mandatory | Default |
| :------------- | :------------- | :------------- | :------------- | :------------- |
        |
    <a id="kt_jvm_library-name"></a>name |  A unique name for this target.   | <a href="https://bazel.build/docs/build-ref.html#name">Name</a> | required |  |
        |
    <a id="kt_jvm_library-data"></a>data |  The list of files needed by this rule at runtime. See general comments about <code>data</code> at         [Attributes common to all build rules](https://docs.bazel.build/versions/master/be/common-definitions.html#common-attributes).   | <a href="https://bazel.build/docs/build-ref.html#labels">List of labels</a> | optional | [] |
        |
    <a id="kt_jvm_library-deps"></a>deps |  A list of dependencies of this rule.See general comments about <code>deps</code> at         [Attributes common to all build rules](https://docs.bazel.build/versions/master/be/common-definitions.html#common-attributes).   | <a href="https://bazel.build/docs/build-ref.html#labels">List of labels</a> | optional | [] |
        |
    <a id="kt_jvm_library-exported_compiler_plugins"></a>exported_compiler_plugins |  Exported compiler plugins.<br><br>        Compiler plugins listed here will be treated as if they were added in the plugins attribute         of any targets that directly depend on this target. Unlike java_plugins' exported_plugins,         this is not transitive   | <a href="https://bazel.build/docs/build-ref.html#labels">List of labels</a> | optional | [] |
        |
    <a id="kt_jvm_library-exports"></a>exports |  Exported libraries.<br><br>        Deps listed here will be made available to other rules, as if the parents explicitly depended on         these deps. This is not true for regular (non-exported) deps.   | <a href="https://bazel.build/docs/build-ref.html#labels">List of labels</a> | optional | [] |
        |
    <a id="kt_jvm_library-module_name"></a>module_name |  The name of the module, if not provided the module name is derived from the label. --e.g.,         <code>//some/package/path:label_name</code> is translated to         <code>some_package_path-label_name</code>.   | String | optional | "" |
        |
    <a id="kt_jvm_library-neverlink"></a>neverlink |  If true only use this library for compilation and not at runtime.   | Boolean | optional | False |
        |
    <a id="kt_jvm_library-plugins"></a>plugins |  -   | <a href="https://bazel.build/docs/build-ref.html#labels">List of labels</a> | optional | [] |
        |
    <a id="kt_jvm_library-resource_jars"></a>resource_jars |  Set of archives containing Java resources. If specified, the contents of these jars are merged into         the output jar.   | <a href="https://bazel.build/docs/build-ref.html#labels">List of labels</a> | optional | [] |
        |
    <a id="kt_jvm_library-resource_strip_prefix"></a>resource_strip_prefix |  The path prefix to strip from Java resources, files residing under common prefix such as         <code>src/main/resources</code> or <code>src/test/resources</code> or <code>kotlin</code> will have stripping applied by convention.   | String | optional | "" |
        |
    <a id="kt_jvm_library-resources"></a>resources |  A list of files that should be include in a Java jar.   | <a href="https://bazel.build/docs/build-ref.html#labels">List of labels</a> | optional | [] |
        |
    <a id="kt_jvm_library-runtime_deps"></a>runtime_deps |  Libraries to make available to the final binary or test at runtime only. Like ordinary deps, these will         appear on the runtime classpath, but unlike them, not on the compile-time classpath.   | <a href="https://bazel.build/docs/build-ref.html#labels">List of labels</a> | optional | [] |
        |
    <a id="kt_jvm_library-srcs"></a>srcs |  The list of source files that are processed to create the target, this can contain both Java and Kotlin         files. Java analysis occurs first so Kotlin classes may depend on Java classes in the same compilation unit.   | <a href="https://bazel.build/docs/build-ref.html#labels">List of labels</a> | optional | [] |
    

<a id="#kt_jvm_test"></a>

## kt_jvm_test

kt_jvm_test(<a href="#kt_jvm_test-name">name</a>, <a href="#kt_jvm_test-data">data</a>, <a href="#kt_jvm_test-deps">deps</a>, <a href="#kt_jvm_test-friends">friends</a>, <a href="#kt_jvm_test-jvm_flags">jvm_flags</a>, <a href="#kt_jvm_test-main_class">main_class</a>, <a href="#kt_jvm_test-module_name">module_name</a>, <a href="#kt_jvm_test-plugins">plugins</a>, <a href="#kt_jvm_test-resource_jars">resource_jars</a>,
            <a href="#kt_jvm_test-resource_strip_prefix">resource_strip_prefix</a>, <a href="#kt_jvm_test-resources">resources</a>, <a href="#kt_jvm_test-runtime_deps">runtime_deps</a>, <a href="#kt_jvm_test-srcs">srcs</a>, <a href="#kt_jvm_test-test_class">test_class</a>)

                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        
            Setup a simple kotlin_test.
                
        **Notes:**
        * The kotlin test library is not added implicitly, it is available with the label
        `@com_github_jetbrains_kotlin//:kotlin-test`.
        
    

**ATTRIBUTES**


| Name  | Description | Type | Mandatory | Default |
| :------------- | :------------- | :------------- | :------------- | :------------- |
        |
    <a id="kt_jvm_test-name"></a>name |  A unique name for this target.   | <a href="https://bazel.build/docs/build-ref.html#name">Name</a> | required |  |
        |
    <a id="kt_jvm_test-data"></a>data |  The list of files needed by this rule at runtime. See general comments about <code>data</code> at         [Attributes common to all build rules](https://docs.bazel.build/versions/master/be/common-definitions.html#common-attributes).   | <a href="https://bazel.build/docs/build-ref.html#labels">List of labels</a> | optional | [] |
        |
    <a id="kt_jvm_test-deps"></a>deps |  A list of dependencies of this rule.See general comments about <code>deps</code> at         [Attributes common to all build rules](https://docs.bazel.build/versions/master/be/common-definitions.html#common-attributes).   | <a href="https://bazel.build/docs/build-ref.html#labels">List of labels</a> | optional | [] |
        |
    <a id="kt_jvm_test-friends"></a>friends |  A single Kotlin dep which allows the test code access to internal members. Currently uses the output             jar of the module -- i.e., exported deps won't be included.   | <a href="https://bazel.build/docs/build-ref.html#labels">List of labels</a> | optional | [] |
        |
    <a id="kt_jvm_test-jvm_flags"></a>jvm_flags |  A list of flags to embed in the wrapper script generated for running this binary. Note: does not yet         support make variable substitution.   | List of strings | optional | [] |
        |
    <a id="kt_jvm_test-main_class"></a>main_class |  -   | String | optional | "com.google.testing.junit.runner.BazelTestRunner" |
        |
    <a id="kt_jvm_test-module_name"></a>module_name |  The name of the module, if not provided the module name is derived from the label. --e.g.,         <code>//some/package/path:label_name</code> is translated to         <code>some_package_path-label_name</code>.   | String | optional | "" |
        |
    <a id="kt_jvm_test-plugins"></a>plugins |  -   | <a href="https://bazel.build/docs/build-ref.html#labels">List of labels</a> | optional | [] |
        |
    <a id="kt_jvm_test-resource_jars"></a>resource_jars |  Set of archives containing Java resources. If specified, the contents of these jars are merged into         the output jar.   | <a href="https://bazel.build/docs/build-ref.html#labels">List of labels</a> | optional | [] |
        |
    <a id="kt_jvm_test-resource_strip_prefix"></a>resource_strip_prefix |  The path prefix to strip from Java resources, files residing under common prefix such as         <code>src/main/resources</code> or <code>src/test/resources</code> or <code>kotlin</code> will have stripping applied by convention.   | String | optional | "" |
        |
    <a id="kt_jvm_test-resources"></a>resources |  A list of files that should be include in a Java jar.   | <a href="https://bazel.build/docs/build-ref.html#labels">List of labels</a> | optional | [] |
        |
    <a id="kt_jvm_test-runtime_deps"></a>runtime_deps |  Libraries to make available to the final binary or test at runtime only. Like ordinary deps, these will         appear on the runtime classpath, but unlike them, not on the compile-time classpath.   | <a href="https://bazel.build/docs/build-ref.html#labels">List of labels</a> | optional | [] |
        |
    <a id="kt_jvm_test-srcs"></a>srcs |  The list of source files that are processed to create the target, this can contain both Java and Kotlin         files. Java analysis occurs first so Kotlin classes may depend on Java classes in the same compilation unit.   | <a href="https://bazel.build/docs/build-ref.html#labels">List of labels</a> | optional | [] |
        |
    <a id="kt_jvm_test-test_class"></a>test_class |  The Java class to be loaded by the test runner.   | String | optional | "" |
    

<a name="#define_kt_toolchain"></a>

## define_kt_toolchain

<pre>
define_kt_toolchain(<a href="#define_kt_toolchain-name">name</a>, <a href="#define_kt_toolchain-language_version">language_version</a>, <a href="#define_kt_toolchain-api_version">api_version</a>, <a href="#define_kt_toolchain-jvm_target">jvm_target</a>, <a href="#define_kt_toolchain-experimental_use_abi_jars">experimental_use_abi_jars</a>
                    <a href="#define_kt_toolchain-javac_options">javac_options</a>, <a href="#define_kt_toolchain-kotlinc_options">kotlinc_options</a>)
</pre>

Define the Kotlin toolchain.

**PARAMETERS**


| Name  | Description | Default Value |
| :-------------: | :-------------: | :-------------: |
| name |  <p align="center"> - </p>   |  none |
| language_version |  <p align="center"> - </p>   |  <code>None</code> |
| api_version |  <p align="center"> - </p>   |  <code>None</code> |
| jvm_target |  <p align="center"> - </p>   |  <code>None</code> |
| experimental_use_abi_jars |  <p align="center"> - </p>   |  <code>False</code> |
| javac_options |  <p align="center"> - </p>   |  <code>None</code> |
| kotlinc_options |  <p align="center"> - </p>   |  <code>None</code> |


<a name="#kt_android_library"></a>

## kt_android_library

<pre>
kt_android_library(<a href="#kt_android_library-name">name</a>, <a href="#kt_android_library-exports">exports</a>, <a href="#kt_android_library-visibility">visibility</a>, <a href="#kt_android_library-kwargs">kwargs</a>)
</pre>

Creates an Android sandwich library.

`srcs`, `deps`, `plugins` are routed to `kt_jvm_library` the other android
related attributes are handled by the native `android_library` rule.

**PARAMETERS**


| Name  | Description | Default Value |
| :-------------: | :-------------: | :-------------: |
| name |  <p align="center"> - </p>   |  none |
| exports |  <p align="center"> - </p>   |  <code>[]</code> |
| visibility |  <p align="center"> - </p>   |  <code>None</code> |
| kwargs |  <p align="center"> - </p>   |  none |


<a name="#kt_js_import"></a>

## kt_js_import

<pre>
kt_js_import(<a href="#kt_js_import-name">name</a>, <a href="#kt_js_import-kwargs">kwargs</a>)
</pre>



**PARAMETERS**


| Name  | Description | Default Value |
| :-------------: | :-------------: | :-------------: |
| name |  <p align="center"> - </p>   |  none |
| kwargs |  <p align="center"> - </p>   |  none |


<a name="#kt_js_library"></a>

## kt_js_library

<pre>
kt_js_library(<a href="#kt_js_library-name">name</a>, <a href="#kt_js_library-kwargs">kwargs</a>)
</pre>



**PARAMETERS**


| Name  | Description | Default Value |
| :-------------: | :-------------: | :-------------: |
| name |  <p align="center"> - </p>   |  none |
| kwargs |  <p align="center"> - </p>   |  none |


<a name="#kt_register_toolchains"></a>

## kt_register_toolchains

<pre>
kt_register_toolchains()
</pre>

This macro registers the kotlin toolchain.

**PARAMETERS**



