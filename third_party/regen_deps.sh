#!/usr/bin/env bash

set -e

WS_ROOT=$(bazel info workspace)
THIRD_PARTY_DIR=${WS_ROOT}/third_party

bazel run //third_party:bazel_deps generate -- \
    -r ${WS_ROOT} \
    -d third_party/dependencies.yaml \
    -s third_party/jvm/workspace.bzl


