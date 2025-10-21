#!/usr/bin/env bash
# Test that kt_jvm_binary can be executed as a tool in another action.
# This verifies the fix for issue #1332.

set -euo pipefail

# The test succeeds if the hello_as_tool target was built successfully.
# The actual execution happened during the build of that target.
# We just need to verify the output file exists.

if [[ -f "$1" ]]; then
    echo "Test PASSED: kt_jvm_binary successfully executed as a tool"
    exit 0
else
    echo "Test FAILED: Output file not found at $1"
    exit 1
fi
