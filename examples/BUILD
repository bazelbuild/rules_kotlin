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
        bazel_versions = bazel_binaries.versions.all,
        tags = ["exclusive"],
        test_runner = "//src/main/kotlin/io/bazel/kotlin/test:BazelIntegrationTestRunner",
        workspace_files = glob(["%s/**/*" % example]),
        workspace_path = example,
    )
    for example in {
        file.partition("/")[0]: True
        for file in glob(
            ["**/*"],
            exclude = ["*"],
        )
    }
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
