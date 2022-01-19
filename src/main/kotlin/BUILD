load("//third_party:jarjar.bzl", "jar_jar")
load("//src/main/starlark/release:packager.bzl", "release_archive")
load("@rules_java//java:defs.bzl", "java_binary", "java_import")

exports_files(
    ["shade.jarjar"],
    visibility = ["//src:__subpackages__"],
)

jar_jar(
    name = "skip-code-gen",
    input_jar = "//src/main/kotlin/io/bazel/kotlin/plugin:skip-code-gen_deploy.jar",
    rules = "shade.jarjar",
    visibility = ["//visibility:public"],
)

jar_jar(
    name = "jdeps-gen",
    input_jar = "//src/main/kotlin/io/bazel/kotlin/plugin/jdeps:jdeps-gen_deploy.jar",
    rules = "shade.jarjar",
    visibility = ["//visibility:public"],
)

java_binary(
    name = "build",
    data = [
        "//src/main/kotlin:jdeps-gen",
        "//src/main/kotlin:skip-code-gen",
        # TODO(restingbull): Remove when toolchain is cleaned up.
        "//src/main/kotlin/io/bazel/kotlin/compiler",
        "@com_github_jetbrains_kotlin//:lib/jvm-abi-gen.jar",
        "@com_github_jetbrains_kotlin//:lib/kotlin-compiler.jar",
    ],
    main_class = "io.bazel.kotlin.builder.cmd.Build",
    visibility = ["//visibility:public"],
    runtime_deps = [
        "//src/main/kotlin/io/bazel/kotlin/compiler:kotlin_runtime",
        "//src/main/kotlin/io/bazel/kotlin/builder/cmd:build_lib",
        "//src/main/kotlin/io/bazel/kotlin/compiler",
        "//src/main/kotlin/io/bazel/worker",
    ],
)

java_binary(
    name = "jdeps_merger",
    main_class = "io.bazel.kotlin.builder.cmd.MergeJdeps",
    visibility = ["//visibility:public"],
    runtime_deps = [
        "//src/main/kotlin/io/bazel/kotlin/builder/cmd:merge_jdeps_lib",
        "//src/main/kotlin/io/bazel/worker",
    ],
)


release_archive(
    name = "pkg",
    srcs = [
        ":jdeps-gen.jar",
        ":skip-code-gen.jar",
    ],
    package_dir = "src/main/kotlin",  # explicitly set the package directory, as there are no parent release_archives.
    src_map = {
        "//src/main/kotlin/io/bazel/kotlin/builder/cmd:build_deploy.jar": "kotlin_worker.jar",
        "//src/main/kotlin/io/bazel/kotlin/builder/cmd:merge_jdeps_deploy.jar": "jdeps_merger_worker.jar",
        "//src/main/kotlin/io/bazel/worker:worker_release_deploy.jar": "worker.jar",
        "BUILD.release.bazel": "BUILD.bazel",
    },
    deps = [
        "//src/main/kotlin/io/bazel/kotlin/compiler:pkg",
    ],
)
