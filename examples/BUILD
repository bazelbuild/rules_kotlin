load(":integration.bzl", "derive_metadata", "example_integration_test_suite")

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

SHARD_COUNT = 3

[
    example_integration_test_suite(
        name = example,
        metadata = metadata,
        tags = ["shard_%s" % (idx % SHARD_COUNT)],
    )
    for (
        idx,
        (example, metadata),
    ) in enumerate({
        example: derive_metadata(
            directory = example,
        )
        for example in {
            # Cut to the directory, de-duplicate via dict.
            file.partition("/")[0]: True
            for file in glob(
                ["**/*"],
                # Exclude files in `examples`.
                exclude = [
                    "*",
                    # Node is currently broken.
                    "node/**",
                    # Anvil is broken by a version upgrade.
                    "anvil/**",
                ],
            )
        }
    }.items())
]
