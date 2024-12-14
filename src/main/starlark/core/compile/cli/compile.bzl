load("//src/main/starlark/core/compile:common.bzl", "TYPE")

def compile_kotlin_for_jvm(
        actions,
        srcs,
        dep_jars,
        class_jar,
        module_name,
        path_separator,
        toolchain_info,
        kotlinc_opts,
        output_srcjar = None):
    if not srcs:
        # still gotta create the jars for keep bazel haps.
        actions.symlink(
            output = output_srcjar,
            target_file = toolchain_info.empty_jar,
        )
        actions.symlink(
            output = class_jar,
            target_file = toolchain_info.empty_jar,
        )
        return

    classpath = depset(
        transitive = [
            dep_jars,
            toolchain_info.kotlin_stdlib.transitive_compile_time_jars,
        ],
    )

    args = actions.args().add("-d", class_jar)
    args.add("-jdk-home", toolchain_info.java_runtime.java_home)
    args.add("-jvm-target", toolchain_info.jvm_target)
    args.add("-no-stdlib")
    args.add("-verbose")
    args.add("-api-version", toolchain_info.api_version)
    args.add("-language-version", toolchain_info.language_version)
    args.add("-module-name", module_name)
    args.add_joined("-cp", classpath, join_with = path_separator)
    for (k, v) in kotlinc_opts.items():
        args.add(k, v)
    args.add_all(srcs)

    actions.run(
        outputs = [class_jar],
        executable = toolchain_info.kotlinc,
        inputs = depset(direct = srcs, transitive = [classpath, toolchain_info.java_runtime.files]),
        arguments = [args],
        mnemonic = toolchain_info.compile_mnemonic,
        toolchain = TYPE,
    )

    if output_srcjar:
        actions.run(
            outputs = [output_srcjar],
            executable = toolchain_info.executable_zip,
            inputs = srcs,
            arguments = [actions.args().add("c").add(output_srcjar).add_all(srcs)],
            mnemonic = "SourceJar",
            toolchain = TYPE,
        )

def write_jvm_launcher(toolchain_info, actions, path_separator, workspace_prefix, jvm_flags, runtime_jars, main_class, executable_output):
    template = toolchain_info.java_stub_template
    java_runtime = toolchain_info.java_runtime
    java_bin_path = java_runtime.java_executable_runfiles_path

    # Following https://github.com/bazelbuild/bazel/blob/6d5b084025a26f2f6d5041f7a9e8d302c590bc80/src/main/starlark/builtins_bzl/bazel/java/bazel_java_binary.bzl#L66-L67
    # Enable the security manager past deprecation.
    # On bazel 6, this check isn't possible...
    if getattr(java_runtime, "version", 0) >= 17:
        jvm_flags = jvm_flags + " -Djava.security.manager=allow"

    classpath = path_separator.join(
        ["${RUNPATH}%s" % (j.short_path) for j in runtime_jars.to_list() + toolchain_info.kotlin_stdlib.transitive_compile_time_jars.to_list()],
    )
    needs_runfiles = "0" if java_bin_path.startswith("/") or (len(java_bin_path) > 2 and java_bin_path[1] == ":") else "1"

    actions.expand_template(
        template = template,
        output = executable_output,
        substitutions = {
            "%classpath%": classpath,
            "%runfiles_manifest_only%": "",
            "%java_start_class%": main_class,
            "%javabin%": "JAVABIN=" + java_bin_path,
            "%jvm_flags%": jvm_flags,
            "%set_jacoco_metadata%": "",
            "%set_jacoco_main_class%": "",
            "%set_jacoco_java_runfiles_root%": "",
            "%set_java_coverage_new_implementation%": """export JAVA_COVERAGE_NEW_IMPLEMENTATION=NO""",
            "%workspace_prefix%": workspace_prefix,
            "%test_runtime_classpath_file%": "export TEST_RUNTIME_CLASSPATH_FILE=${JAVA_RUNFILES}",
            "%needs_runfiles%": needs_runfiles,
        },
        is_executable = True,
    )

    return depset(
        transitive = [
            runtime_jars,
            java_runtime.files,
            toolchain_info.kotlin_stdlib.transitive_compile_time_jars,
        ],
    )

def build_deploy_jar(toolchain_info, actions, jars, output_jar):
    args = actions.args()
    args.add("--exclude_build_data")
    args.add("--dont_change_compression")
    args.add_all("--sources", jars)
    args.add("--normalize")
    args.add("--output", output_jar)
    actions.run(
        inputs = jars,
        outputs = [output_jar],
        executable = toolchain_info.single_jar,
        mnemonic = "SingleJar",
        arguments = [args],
        toolchain = TYPE,
    )
