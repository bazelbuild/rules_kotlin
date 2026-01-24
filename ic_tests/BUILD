load(":ic_integration_test.bzl", "ic_integration_test_suite")

ic_integration_test_suite(
    name = "trivial_change",
    tags = ["manual"],  # Run explicitly, not in //...
    workspace_path = "ic_tests/testdata/trivial_change",
)

ic_integration_test_suite(
    name = "inline_function_change",
    tags = ["manual"],  # Run explicitly, not in //...
    workspace_path = "ic_tests/testdata/inline_function_change",
)

ic_integration_test_suite(
    name = "plugin_options_change",
    tags = ["manual"],
    workspace_path = "ic_tests/testdata/plugin_options_change",
)

ic_integration_test_suite(
    name = "kotlinc_flags_change",
    tags = ["manual"],
    workspace_path = "ic_tests/testdata/kotlinc_flags_change",
)

ic_integration_test_suite(
    name = "jvm_target_change",
    tags = ["manual"],
    workspace_path = "ic_tests/testdata/jvm_target_change",
)

test_suite(
    name = "all",
    tags = ["ic"],
)
