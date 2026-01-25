#!/bin/bash
# Additional test script for coverage example
# Runs after standard build/test cycle to verify coverage-specific functionality
#
# This script is executed by BazelIntegrationTestRunner using the bash binary
# from Bazel's shell toolchain.
#
# Environment variables provided by BazelIntegrationTestRunner:
#   BIT_BAZEL_BINARY: path to the bazel binary
#   BIT_STARTUP_FLAGS: flags that go before the command (e.g., --bazelrc)
#   BIT_COMMAND_FLAGS: flags that go after the command (e.g., --override_module, --enable_bzlmod)

set -e

BAZEL="${BIT_BAZEL_BINARY:-bazel}"

echo "Running bazel coverage test..."
# shellcheck disable=SC2086
OUTPUT=$("$BAZEL" $BIT_STARTUP_FLAGS coverage $BIT_COMMAND_FLAGS --combined_report=lcov //:coverage_test 2>&1) || {
    echo "Coverage test failed"
    echo "$OUTPUT"
    exit 1
}

# Check if the output contains NoClassDefFoundError for JaCoCo Offline class
if echo "$OUTPUT" | grep -q "NoClassDefFoundError.*Offline"; then
    echo "JaCoCo version mismatch error detected"
    exit 1
fi

echo "Coverage test passed!"
