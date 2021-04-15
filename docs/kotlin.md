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
|<a id="kt_compiler_plugin-name"></a>name |  A unique name for this target.   | <a href="https://bazel.build/docs/build-ref.html#name">Name</a> | required |  |
|<a id="kt_compiler_plugin-compile_phase"></a>compile_phase |  Runs the compiler plugin during kotlin compilation. Known examples: allopen, sam_with_reciever   | Boolean | optional | True |
|<a id="kt_compiler_plugin-deps"></a>deps |  The list of libraries to be added to the compiler's plugin classpath   | <a href="https://bazel.build/docs/build-ref.html#labels">List of labels</a> | optional | [] |
|<a id="kt_compiler_plugin-id"></a>id |  The ID of the plugin   | String | required |  |
|<a id="kt_compiler_plugin-options"></a>options |  Dictionary of options to be passed to the plugin.             Supports the following template values:                <code>{generatedClasses}</code>: directory for generated class output                <code>{temp}</code>: temporary directory, discarded between invocations                <code>{generatedSources}</code>:  directory for generated source output   | <a href="https://bazel.build/docs/skylark/lib/dict.html">Dictionary: String -> String</a> | optional | {} |
|<a id="kt_compiler_plugin-stubs_phase"></a>stubs_phase |  Runs the compiler plugin in kapt stub generation.   | Boolean | optional | True |
|<a id="kt_compiler_plugin-target_embedded_compiler"></a>target_embedded_compiler |  Plugin was compiled against the embeddable kotlin compiler. These plugins expect shaded kotlinc             dependencies, and will fail when running against a non-embeddable compiler.   | Boolean | optional | False |


<a id="#kt_javac_options"></a>

## kt_javac_options

kt_javac_options(<a href="#kt_javac_options-name">name</a>, <a href="#kt_javac_options-warn">warn</a>, <a href="#kt_javac_options-x_ep_disable_all_checks">x_ep_disable_all_checks</a>, <a href="#kt_javac_options-x_lint">x_lint</a>, <a href="#kt_javac_options-xd_suppress_notes">xd_suppress_notes</a>)

                                                                                                
    Define java compiler options for kt_jvm_* rules with java sources.
    

**ATTRIBUTES**


| Name  | Description | Type | Mandatory | Default |
| :------------- | :------------- | :------------- | :------------- | :------------- |
|<a id="kt_javac_options-name"></a>name |  A unique name for this target.   | <a href="https://bazel.build/docs/build-ref.html#name">Name</a> | required |  |
|<a id="kt_javac_options-warn"></a>warn |  Control warning behaviour.   | String | optional | "report" |
|<a id="kt_javac_options-x_ep_disable_all_checks"></a>x_ep_disable_all_checks |  See javac -XepDisableAllChecks documentation   | Boolean | optional | False |
|<a id="kt_javac_options-x_lint"></a>x_lint |  See javac -Xlint: documentation   | List of strings | optional | [] |
|<a id="kt_javac_options-xd_suppress_notes"></a>xd_suppress_notes |  See javac -XDsuppressNotes documentation   | Boolean | optional | False |


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
|<a id="kt_jvm_import-name"></a>name |  A unique name for this target.   | <a href="https://bazel.build/docs/build-ref.html#name">Name</a> | required |  |
|<a id="kt_jvm_import-deps"></a>deps |  Compile and runtime dependencies   | <a href="https://bazel.build/docs/build-ref.html#labels">List of labels</a> | optional | [] |
|<a id="kt_jvm_import-exported_compiler_plugins"></a>exported_compiler_plugins |  Exported compiler plugins.<br><br>            Compiler plugins listed here will be treated as if they were added in the plugins             attribute of any targets that directly depend on this target. Unlike java_plugins'             exported_plugins, this is not transitive   | <a href="https://bazel.build/docs/build-ref.html#labels">List of labels</a> | optional | [] |
|<a id="kt_jvm_import-exports"></a>exports |  Exported libraries.<br><br>            Deps listed here will be made available to other rules, as if the parents explicitly depended on             these deps. This is not true for regular (non-exported) deps.   | <a href="https://bazel.build/docs/build-ref.html#labels">List of labels</a> | optional | [] |
|<a id="kt_jvm_import-jar"></a>jar |  The jar listed here is equivalent to an export attribute.   | <a href="https://bazel.build/docs/build-ref.html#labels">Label</a> | optional | None |
|<a id="kt_jvm_import-jars"></a>jars |  The jars listed here are equavalent to an export attribute. The label should be either to a single             class jar, or one or more filegroup labels.  The filegroups, when resolved, must contain  only one jar             containing classes, and (optionally) one peer file containing sources, named <code>&lt;jarname&gt;-sources.jar</code>.<br><br>            DEPRECATED - please use <code>jar</code> and <code>srcjar</code> attributes.   | <a href="https://bazel.build/docs/build-ref.html#labels">List of labels</a> | optional | [] |
|<a id="kt_jvm_import-neverlink"></a>neverlink |  If true only use this library for compilation and not at runtime.   | Boolean | optional | False |
|<a id="kt_jvm_import-runtime_deps"></a>runtime_deps |  Additional runtime deps.   | <a href="https://bazel.build/docs/build-ref.html#labels">List of labels</a> | optional | [] |
|<a id="kt_jvm_import-srcjar"></a>srcjar |  The sources for the class jar.   | <a href="https://bazel.build/docs/build-ref.html#labels">Label</a> | optional | //third_party:empty.jar |


<a id="#kt_kotlinc_options"></a>

## kt_kotlinc_options

kt_kotlinc_options(<a href="#kt_kotlinc_options-name">name</a>, <a href="#kt_kotlinc_options-include_stdlibs">include_stdlibs</a>, <a href="#kt_kotlinc_options-java_parameters">java_parameters</a>, <a href="#kt_kotlinc_options-warn">warn</a>, <a href="#kt_kotlinc_options-x_allow_jvm_ir_dependencies">x_allow_jvm_ir_dependencies</a>,
                   <a href="#kt_kotlinc_options-x_allow_result_return_type">x_allow_result_return_type</a>, <a href="#kt_kotlinc_options-x_inline_classes">x_inline_classes</a>, <a href="#kt_kotlinc_options-x_jvm_default">x_jvm_default</a>, <a href="#kt_kotlinc_options-x_multi_platform">x_multi_platform</a>,
                   <a href="#kt_kotlinc_options-x_no_optimized_callable_references">x_no_optimized_callable_references</a>, <a href="#kt_kotlinc_options-x_skip_prerelease_check">x_skip_prerelease_check</a>, <a href="#kt_kotlinc_options-x_use_experimental">x_use_experimental</a>,
                   <a href="#kt_kotlinc_options-x_use_ir">x_use_ir</a>)

                                                                                                
    Define kotlin compiler options.
    

**ATTRIBUTES**


| Name  | Description | Type | Mandatory | Default |
| :------------- | :------------- | :------------- | :------------- | :------------- |
|<a id="kt_kotlinc_options-name"></a>name |  A unique name for this target.   | <a href="https://bazel.build/docs/build-ref.html#name">Name</a> | required |  |
|<a id="kt_kotlinc_options-include_stdlibs"></a>include_stdlibs |  Don't automatically include the Kotlin standard libraries into the classpath (stdlib and reflect).   | String | optional | "all" |
|<a id="kt_kotlinc_options-java_parameters"></a>java_parameters |  Generate metadata for Java 1.8+ reflection on method parameters.   | Boolean | optional | False |
|<a id="kt_kotlinc_options-warn"></a>warn |  Control warning behaviour.   | String | optional | "report" |
|<a id="kt_kotlinc_options-x_allow_jvm_ir_dependencies"></a>x_allow_jvm_ir_dependencies |  Suppress errors thrown when using dependencies not compiled by the IR backend.   | Boolean | optional | False |
|<a id="kt_kotlinc_options-x_allow_result_return_type"></a>x_allow_result_return_type |  Enable kotlin.Result as a return type   | Boolean | optional | False |
|<a id="kt_kotlinc_options-x_inline_classes"></a>x_inline_classes |  Enable experimental inline classes   | Boolean | optional | False |
|<a id="kt_kotlinc_options-x_jvm_default"></a>x_jvm_default |  Specifies that a JVM default method should be generated for non-abstract Kotlin interface member.   | String | optional | "off" |
|<a id="kt_kotlinc_options-x_multi_platform"></a>x_multi_platform |  Enable experimental language support for multi-platform projects   | Boolean | optional | False |
|<a id="kt_kotlinc_options-x_no_optimized_callable_references"></a>x_no_optimized_callable_references |  Do not use optimized callable reference superclasses. Available from 1.4.   | Boolean | optional | False |
|<a id="kt_kotlinc_options-x_skip_prerelease_check"></a>x_skip_prerelease_check |  Suppress errors thrown when using pre-release classes.   | Boolean | optional | False |
|<a id="kt_kotlinc_options-x_use_experimental"></a>x_use_experimental |  Allow the experimental language features.   | Boolean | optional | True |
|<a id="kt_kotlinc_options-x_use_ir"></a>x_use_ir |  Enable or disable the experimental IR backend.   | Boolean | optional | False |


<a name="#define_kt_toolchain"></a>

## define_kt_toolchain

<pre>
define_kt_toolchain(<a href="#define_kt_toolchain-name">name</a>, <a href="#define_kt_toolchain-language_version">language_version</a>, <a href="#define_kt_toolchain-api_version">api_version</a>, <a href="#define_kt_toolchain-jvm_target">jvm_target</a>, <a href="#define_kt_toolchain-experimental_use_abi_jars">experimental_use_abi_jars</a>,
                    <a href="#define_kt_toolchain-experimental_strict_kotlin_deps">experimental_strict_kotlin_deps</a>, <a href="#define_kt_toolchain-experimental_report_unused_deps">experimental_report_unused_deps</a>,
                    <a href="#define_kt_toolchain-experimental_reduce_classpath_mode">experimental_reduce_classpath_mode</a>, <a href="#define_kt_toolchain-experimental_multiplex_workers">experimental_multiplex_workers</a>, <a href="#define_kt_toolchain-javac_options">javac_options</a>,
                    <a href="#define_kt_toolchain-kotlinc_options">kotlinc_options</a>, <a href="#define_kt_toolchain-jacocorunner">jacocorunner</a>)
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
| experimental_strict_kotlin_deps |  <p align="center"> - </p>   |  <code>None</code> |
| experimental_report_unused_deps |  <p align="center"> - </p>   |  <code>None</code> |
| experimental_reduce_classpath_mode |  <p align="center"> - </p>   |  <code>None</code> |
| experimental_multiplex_workers |  <p align="center"> - </p>   |  <code>None</code> |
| javac_options |  <p align="center"> - </p>   |  <code>None</code> |
| kotlinc_options |  <p align="center"> - </p>   |  <code>None</code> |
| jacocorunner |  <p align="center"> - </p>   |  <code>None</code> |


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


<a name="#kt_jvm_binary"></a>

## kt_jvm_binary

<pre>
kt_jvm_binary(<a href="#kt_jvm_binary-name">name</a>, <a href="#kt_jvm_binary-srcs">srcs</a>, <a href="#kt_jvm_binary-kwargs">kwargs</a>)
</pre>



**PARAMETERS**


| Name  | Description | Default Value |
| :-------------: | :-------------: | :-------------: |
| name |  <p align="center"> - </p>   |  none |
| srcs |  <p align="center"> - </p>   |  <code>None</code> |
| kwargs |  <p align="center"> - </p>   |  none |


<a name="#kt_jvm_library"></a>

## kt_jvm_library

<pre>
kt_jvm_library(<a href="#kt_jvm_library-name">name</a>, <a href="#kt_jvm_library-srcs">srcs</a>, <a href="#kt_jvm_library-kwargs">kwargs</a>)
</pre>



**PARAMETERS**


| Name  | Description | Default Value |
| :-------------: | :-------------: | :-------------: |
| name |  <p align="center"> - </p>   |  none |
| srcs |  <p align="center"> - </p>   |  <code>None</code> |
| kwargs |  <p align="center"> - </p>   |  none |


<a name="#kt_jvm_test"></a>

## kt_jvm_test

<pre>
kt_jvm_test(<a href="#kt_jvm_test-name">name</a>, <a href="#kt_jvm_test-srcs">srcs</a>, <a href="#kt_jvm_test-kwargs">kwargs</a>)
</pre>



**PARAMETERS**


| Name  | Description | Default Value |
| :-------------: | :-------------: | :-------------: |
| name |  <p align="center"> - </p>   |  none |
| srcs |  <p align="center"> - </p>   |  <code>None</code> |
| kwargs |  <p align="center"> - </p>   |  none |


<a name="#kt_register_toolchains"></a>

## kt_register_toolchains

<pre>
kt_register_toolchains()
</pre>

This macro registers the kotlin toolchain.

**PARAMETERS**



