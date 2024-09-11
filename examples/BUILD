load("@bazel_binaries//:defs.bzl", "bazel_binaries")
load(
    "@rules_bazel_integration_test//bazel_integration_test:defs.bzl",
    "bazel_integration_tests",
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
        timeout = "eternal",
        additional_env_inherit = [
            "ANDROID_HOME",
            "ANDROID_SDK_ROOT",
            "ANDROID_NDK_HOME",
        ],
        bazel_versions = [
            version
            for version in bazel_binaries.versions.all
            if version in metadata["only"] or (not metadata["only"] and version not in metadata["exclude"])
        ],
        tags = [],
        test_runner = "//src/main/kotlin/io/bazel/kotlin/test:BazelIntegrationTestRunner",
        workspace_files = glob(
            ["%s/**/**" % example],
            # exclude any bazel directories if existing
            exclude = ["%s/bazel-*/**" % example],
        ),
        workspace_path = example,
    )
    for (example, metadata) in {
        example: {
            "exclude": [
                # Cut to the file name, and use it as an excluded bazel version. For exclusion to work
                # the file name in the `exclude` directory must match the bazel version in `bazel_binaries.versions.all`.
                # This is done as a secondary loop for readability and avoiding over-globbing.
                version.rpartition("/")[2]
                for version in glob(
                    ["%s/exclude/*" % example],
                    allow_empty = True,
                )
            ],
            "only": [
                # Cut to the file name, and use it as an only bazel version. For exclusion to work
                # the file name in the `only` directory must match the bazel version in `bazel_binaries.versions.all`.
                # This is done as a secondary loop for readability and avoiding over-globbing.
                version.rpartition("/")[2]
                for version in glob(
                    ["%s/only/*" % example],
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
                    "node/**",
                    # Anvil is broken by a verison upgrade.
                    "anvil/**",
                ],
            )
        }
    }.items()
]
