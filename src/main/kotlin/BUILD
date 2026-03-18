load("//src/main/starlark/release:packager.bzl", "release_archive")

alias(
    name = "build",
    actual = "//src/main/kotlin/io/bazel/kotlin/builder/cmd:build",
    visibility = ["//visibility:public"],
)

alias(
    name = "skip-code-gen",
    actual = "//src/main/kotlin/io/bazel/kotlin/plugin:skip-code-gen_deploy.jar",
    visibility = ["//visibility:public"],
)

alias(
    name = "jdeps-gen",
    actual = "//src/main/kotlin/io/bazel/kotlin/plugin/jdeps:jdeps-gen_deploy.jar",
    visibility = ["//visibility:public"],
)

alias(
    name = "jdeps_merger",
    actual = "//src/main/kotlin/io/bazel/kotlin/builder/cmd:merge_jdeps",
    visibility = ["//visibility:public"],
)

alias(
    name = "ksp2",
    actual = "//src/main/kotlin/io/bazel/kotlin/builder/cmd:ksp2",
    visibility = ["//visibility:public"],
)

alias(
    name = "ksp2_invoker",
    actual = "//src/main/kotlin/io/bazel/kotlin/ksp2",
    visibility = ["//visibility:public"],
)

release_archive(
    name = "pkg",
    package_dir = "src/main/kotlin",  # explicitly set the package directory, as there are no parent release_archives.
    src_map = {
        "//src/main/kotlin/io/bazel/kotlin/builder/cmd:build_deploy.jar": "kotlin_worker.jar",
        "//src/main/kotlin/io/bazel/kotlin/builder/cmd:ksp2_deploy.jar": "ksp2_worker.jar",
        "//src/main/kotlin/io/bazel/kotlin/builder/cmd:merge_jdeps_deploy.jar": "jdeps_merger_worker.jar",
        "//src/main/kotlin/io/bazel/kotlin/ksp2:ksp2.jar": "ksp2_invoker.jar",
        "//src/main/kotlin/io/bazel/kotlin/plugin/jdeps:jdeps-gen_deploy.jar": "jdeps-gen.jar",
        "//src/main/kotlin/io/bazel/kotlin/plugin:skip-code-gen_deploy.jar": "skip-code-gen.jar",
        "BUILD.release.bazel": "BUILD.bazel",
    },
)
