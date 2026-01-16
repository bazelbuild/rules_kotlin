load(":ic_integration_test.bzl", "ic_integration_test_suite")

ic_integration_test_suite(
    name = "trivial_change",
    tags = ["manual"],  # Run explicitly, not in //...
    workspace_path = "ic_tests/testdata/trivial_change",
)

test_suite(
    name = "all",
    tags = ["ic"],
)
