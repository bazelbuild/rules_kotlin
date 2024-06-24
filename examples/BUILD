load("//src/main/starlark/release:packager.bzl", "release_archive")
load("@bazel_binaries//:defs.bzl", "bazel_binaries")
load(
    "@rules_bazel_integration_test//bazel_integration_test:defs.bzl",
    "bazel_integration_test",
    "bazel_integration_tests",
    "default_test_runner",
    "integration_test_utils",
)

genrule(
    name = "update_bit_ignore",
    srcs = [],
    outs = [
        "update_bit_ignore.sh",
    ],
    cmd = "\n".join([
        "cat <<'EOS' > $@",
        "#!/usr/bin/env bash",
        "cd $${BUILD_WORKSPACE_DIRECTORY}",
        "echo 'common --deleted_packages='$$(find examples -type 'd' | grep -v '^examples$$' | tr '\n' ',' | sed 's/,$$//')" +
        "> bit-ignore.bazelrc",
        "EOS",
    ]),
    executable = True,
)

[
    bazel_integration_tests(
        name = "%s_test" % example,
        additional_env_inherit = ["ANDROID_HOME","ANDROID_NDK_HOME"],
        bazel_versions = [version for version in bazel_binaries.versions.all if version not in metadata["exclude"]],
        tags = ["exclusive"] + metadata["exclude"],
        test_runner = "//src/main/kotlin/io/bazel/kotlin/test:BazelIntegrationTestRunner",
        workspace_files = glob(["%s/**/*" % example]),
        workspace_path = example,
    )
    for (example, metadata) in {
        example: {
            "exclude": [
                # Cut to the file name, and use it as an excluded bazel version. For exclusion to work
                # the file name in the `exclude` directory must match the bazel version in `bazel_binaries.versions.all`.
                # This is done as a secondary loop for readability and avoiding over-globbing.
                version.rpartition["/"][2]
                for version in glob(
                    ["examples/%s/exclude/*" % example],
                    allow_empty = True,
                )
            ],
        }
        for example in {
            # Cut to the directory.
            file.partition("/")[0]: True
            for file in glob(
                ["**/*"],
                # Exclude files in `examples`.
                exclude = [
                    "*",
                    # Node is currently broken.
                    "examples/node/**",
                ],
            )
        }
    }.items()
]

# By default, the integration test targets are tagged as `manual`. This
# prevents the targets from being found from most target expansion (e.g.
# `//...`, `:all`). To easily execute a group of integration tests, you may
# want to define a test suite which includes the desired test targets.
#test_suite(
#    name = "all_integration_tests",
#    # If you don't apply the test tags to the test suite, the test suite will
#    # be found when `bazel test //...` is executed.
#    tags = integration_test_utils.DEFAULT_INTEGRATION_TEST_TAGS,
#    tests = integration_test_utils.bazel_integration_test_names(
#        "simple_test",
#        bazel_binaries.versions.other,
#    ),
#    visibility = ["//:__subpackages__"],
#)
