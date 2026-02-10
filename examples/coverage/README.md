# Coverage Example

This example demonstrates Kotlin code coverage with Bazel using `bazel coverage`.

## Background

This example verifies the fix for [issue #1447](https://github.com/bazelbuild/rules_kotlin/issues/1447), which addresses a JaCoCo version mismatch between `rules_kotlin` and `rules_java 9.3.0+`.

The issue occurred because the kotlin_worker.jar was compiled against JaCoCo 0.8.11, but `rules_java 9.3.0` uses JaCoCo 0.8.14. This caused a `NoClassDefFoundError` when running `bazel coverage` on Kotlin targets.

## The Fix

The fix reorders `runtime_deps` to put `@bazel_tools//tools/jdk:JacocoCoverage` **before** the worker. This ensures the newer JaCoCo classes from Bazel's tools are loaded first, taking precedence over the older version bundled in the worker.

## Running the Example

```bash
cd examples/coverage
bazel coverage --combined_report=lcov //:coverage_test
```

To view the HTML report:
```bash
genhtml bazel-out/_coverage/_coverage_report.dat -o coverage_html
```

## Expected Results

The coverage test should run successfully without any `NoClassDefFoundError` exceptions, and generate coverage reports in `bazel-out/_coverage/`.
