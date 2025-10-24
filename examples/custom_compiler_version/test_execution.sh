#!/usr/bin/env bash
# Test script to verify the custom compiler version example works

set -e

cd "$(dirname "$0")"

echo "Building with custom Kotlin 2.1.21 compiler (using bzlmod)..."
bazel build //app:main

echo "Running the binary..."
bazel run //app:main

echo "Verifying trove4j is available in the Kotlin repository..."
if bazel query "@com_github_jetbrains_kotlin//:all" 2>/dev/null | grep -q trove4j; then
    echo "✅ trove4j target found in @com_github_jetbrains_kotlin (expected for Kotlin 2.1.21)"
else
    echo "ERROR: trove4j target should exist in Kotlin 2.1.21 distribution"
    exit 1
fi

echo "Verifying android-extensions are available (removed in 2.2+)..."
if bazel query "@com_github_jetbrains_kotlin//:all" 2>/dev/null | grep -q android-extensions; then
    echo "✅ android-extensions targets found (expected for Kotlin 2.1.21)"
else
    echo "⚠️  WARNING: android-extensions not found (should exist in 2.1.21)"
fi

echo "✅ Custom compiler version test passed!"
