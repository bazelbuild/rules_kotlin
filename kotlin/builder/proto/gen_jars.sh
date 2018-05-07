#!/usr/bin/env bash

WS_ROOT=$(bazel info workspace)
PATH_PROTO=kotlin/builder/proto
function build_lib() {
bazel build //${PATH_PROTO}:$1_java_proto
local FILE=lib$1_proto-speed.jar
cp ${WS_ROOT}/bazel-bin/${PATH_PROTO}/${FILE} ${WS_ROOT}/${PATH_PROTO}/jars/${FILE}
}

build_lib "deps"
build_lib "worker_protocol"
build_lib "kotlin_model"

chmod -R 0754 jars

