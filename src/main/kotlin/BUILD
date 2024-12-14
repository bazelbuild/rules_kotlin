load("@released_rules_kotlin//kotlin:core.bzl", "define_kt_toolchain")
load("//src/main/starlark/release:packager.bzl", "release_archive")
load("//third_party:jarjar.bzl", "jar_jar")

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

alias(
    name = "build",
    actual = "//src/main/kotlin/io/bazel/kotlin/builder/cmd:build",
    visibility = ["//visibility:public"],
)

alias(
    name = "jdeps_merger",
    actual = "//src/main/kotlin/io/bazel/kotlin/builder/cmd:merge_jdeps",
    visibility = ["//visibility:public"],
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
        "BUILD.release.bazel": "BUILD.bazel",
    },
    deps = [
        "//src/main/kotlin/io/bazel/kotlin/compiler:pkg",
    ],
)

define_kt_toolchain(
    name = "bootstrap_toolchain",
    api_version = "2.0",
    jvm_target = "11",
    language_version = "2.0",
)
