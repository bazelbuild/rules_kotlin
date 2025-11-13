load("@bazel_skylib//lib:unittest.bzl", "analysistest", "asserts")
load("@bazel_skylib//rules:write_file.bzl", "write_file")
load("//kotlin:jvm.bzl", "kt_jvm_library")

def _strip_resource_prefix_test_impl(ctx):
    env = analysistest.begin(ctx)

    actions = analysistest.target_actions(env)

    # Find the only FileWrite action (it's the one responsible for writing the arguments to the resource zipper)
    file_write_actions = [
        action
        for action in actions
        if action.mnemonic == "FileWrite"
    ]
    asserts.equals(env, expected = 1, actual = len(file_write_actions))

    arguments = file_write_actions[0].content

    print("chuj " + arguments)

    pkg = ctx.attr.pkg

    # The only line should be of the form:
    # data.txt=<some prefix>/<pkg>/resourcez/data.txt
    lines = arguments.splitlines()
    asserts.equals(env, expected = 1, actual = len(lines))
    line_parts = lines[0].split("=", 1)
    asserts.equals(env, expected = 2, actual = len(line_parts))
    source_path = line_parts[1]
    expected_suffix = pkg + "/" + ctx.attr.resource_strip_prefix + "/" + ctx.attr.resource_path
    asserts.true(
        env,
        source_path.endswith(expected_suffix),
        msg = "source path " + source_path + " does not have expected suffix " + expected_suffix,
    )

    destination_path = line_parts[0]

    # The destination path should have the resource_strip_prefix removed
    asserts.equals(env, expected = ctx.attr.resource_path, actual = destination_path, msg = "resource_strip_prefix was not applied correctly")

    return analysistest.end(env)

strip_resource_prefix_test = analysistest.make(
    _strip_resource_prefix_test_impl,
    attrs = {
        "pkg": attr.string(),
        "resource_path": attr.string(),
        "resource_strip_prefix": attr.string(),
    },
)

# Macro to setup the test.
def _strip_resource_prefix_contents():
    write_file(
        name = "file",
        out = "resourcez/resource.txt",
        tags = ["manual"],
    )

    write_file(
        name = "source",
        out = "Source.kt",
        tags = ["manual"],
    )

    kt_jvm_library(
        name = "dynamically_created_file",
        srcs = ["source"],
        resource_strip_prefix = "resourcez",
        resources = ["file"],  # note: file created dynamically above
        tags = ["manual"],
    )

    kt_jvm_library(
        name = "static_file",
        srcs = ["source"],
        resource_strip_prefix = "test_resources",
        resources = ["test_resources/resource.txt"],  # static file in the package
        tags = ["manual"],
    )

    kt_jvm_library(
        name = "standard_package",
        srcs = ["source"],
        resources = ["src/main/resources/resource.txt"],  # no explicit strip prefix
        tags = ["manual"],
    )

    package_name = native.package_name().split("/")[-1]
    native.filegroup(
        name = package_name,
        srcs = ["test_resources/actual_file.txt"],
    )

    kt_jvm_library(
        name = "same_as_package_name",
        srcs = ["source"],
        resources = [package_name],  # filegroup defined above
        resource_strip_prefix = "test_resources",
        tags = ["manual"],
    )

    strip_resource_prefix_test(
        name = "dynamically_created_file_test",
        target_under_test = ":dynamically_created_file",
        tags = ["manual"],
        pkg = native.package_name(),
        resource_strip_prefix = "resourcez",
        resource_path = "resource.txt",
    )

    strip_resource_prefix_test(
        name = "static_file_test",
        target_under_test = ":static_file",
        tags = ["manual"],
        pkg = native.package_name(),
        resource_strip_prefix = "test_resources",
        resource_path = "resource.txt",
    )

    strip_resource_prefix_test(
        name = "standard_package_test",
        target_under_test = ":standard_package",
        tags = ["manual"],
        pkg = native.package_name(),
        resource_strip_prefix = "src/main/resources",
        resource_path = "resource.txt",
    )

    strip_resource_prefix_test(
        name = "same_as_package_name_test",
        target_under_test = ":same_as_package_name",
        tags = ["manual"],
        pkg = native.package_name(),
        resource_strip_prefix = "test_resources",
        resource_path = "actual_file.txt",
    )

# Entry point from the BUILD file; macro for running each test case's macro and
# declaring a test suite that wraps them together.
def strip_resource_prefix_test_suite(name):
    # Call all test functions and wrap their targets in a suite.
    _strip_resource_prefix_contents()

    native.test_suite(
        name = name,
        tests = [
            ":dynamically_created_file_test",
            ":static_file_test",
            ":standard_package_test",
            ":same_as_package_name_test",
        ],
        tags = ["manual"],
    )
