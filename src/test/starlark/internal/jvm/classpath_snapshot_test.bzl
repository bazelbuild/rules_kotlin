# Copyright 2024 The Bazel Authors. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Tests for classpath snapshot tracking in incremental compilation."""

load("@bazel_skylib//lib:unittest.bzl", "analysistest", "asserts")
load("//kotlin:jvm.bzl", "kt_jvm_library")
load("//src/main/starlark/core/compile:common.bzl", "KtJvmInfo")

# Test that verifies KtJvmInfo has classpath_snapshot field when incremental compilation is enabled
def _classpath_snapshot_enabled_test_impl(ctx):
    env = analysistest.begin(ctx)

    target = analysistest.target_under_test(env)

    # Verify KtJvmInfo provider exists
    asserts.true(env, KtJvmInfo in target, "Target should have KtJvmInfo provider")

    kt_info = target[KtJvmInfo]

    # Verify classpath_snapshot field exists (may be None if not enabled)
    asserts.true(
        env,
        hasattr(kt_info, "classpath_snapshot"),
        "KtJvmInfo should have classpath_snapshot field",
    )

    # When incremental compilation is enabled, snapshot should be a File
    if kt_info.classpath_snapshot != None:
        asserts.true(
            env,
            kt_info.classpath_snapshot.basename.endswith(".snapshot"),
            "Classpath snapshot should have .snapshot extension",
        )

    return analysistest.end(env)

classpath_snapshot_enabled_test = analysistest.make(
    _classpath_snapshot_enabled_test_impl,
    config_settings = {
        str(Label("//kotlin/settings:experimental_incremental_compilation")): True,
        str(Label("//kotlin/settings:experimental_build_tools_api")): True,
    },
)

# Test that verifies classpath_snapshot is None when incremental compilation is disabled
def _classpath_snapshot_disabled_test_impl(ctx):
    env = analysistest.begin(ctx)

    target = analysistest.target_under_test(env)

    # Verify KtJvmInfo provider exists
    asserts.true(env, KtJvmInfo in target, "Target should have KtJvmInfo provider")

    kt_info = target[KtJvmInfo]

    # Verify classpath_snapshot field exists
    asserts.true(
        env,
        hasattr(kt_info, "classpath_snapshot"),
        "KtJvmInfo should have classpath_snapshot field",
    )

    # When incremental compilation is disabled, snapshot should be None
    asserts.equals(
        env,
        None,
        kt_info.classpath_snapshot,
        "Classpath snapshot should be None when incremental compilation is disabled",
    )

    return analysistest.end(env)

classpath_snapshot_disabled_test = analysistest.make(
    _classpath_snapshot_disabled_test_impl,
    config_settings = {
        str(Label("//kotlin/settings:experimental_incremental_compilation")): False,
    },
)

# Test that verifies snapshot is passed to dependent targets
def _classpath_snapshot_propagation_test_impl(ctx):
    env = analysistest.begin(ctx)

    target = analysistest.target_under_test(env)

    # Verify KtJvmInfo provider exists
    asserts.true(env, KtJvmInfo in target, "Target should have KtJvmInfo provider")

    kt_info = target[KtJvmInfo]

    # Verify this target also has a snapshot (when incremental is enabled)
    if kt_info.classpath_snapshot != None:
        asserts.true(
            env,
            kt_info.classpath_snapshot.basename.endswith(".snapshot"),
            "Consumer should also have a classpath snapshot",
        )

    return analysistest.end(env)

classpath_snapshot_propagation_test = analysistest.make(
    _classpath_snapshot_propagation_test_impl,
    config_settings = {
        str(Label("//kotlin/settings:experimental_incremental_compilation")): True,
        str(Label("//kotlin/settings:experimental_build_tools_api")): True,
    },
)

def classpath_snapshot_test_suite(name):
    """Creates the test suite for classpath snapshot tracking."""

    # Create a simple library target for testing
    kt_jvm_library(
        name = name + "_lib",
        srcs = ["Lib.kt"],
        tags = ["manual"],
    )

    # Create a consumer that depends on the library
    kt_jvm_library(
        name = name + "_consumer",
        srcs = ["Consumer.kt"],
        deps = [":" + name + "_lib"],
        tags = ["manual"],
    )

    # Test: snapshot exists when incremental compilation is enabled
    classpath_snapshot_enabled_test(
        name = name + "_enabled_test",
        target_under_test = ":" + name + "_lib",
    )

    # Test: snapshot is None when incremental compilation is disabled
    classpath_snapshot_disabled_test(
        name = name + "_disabled_test",
        target_under_test = ":" + name + "_lib",
    )

    # Test: snapshot propagates to dependent targets
    classpath_snapshot_propagation_test(
        name = name + "_propagation_test",
        target_under_test = ":" + name + "_consumer",
    )

    native.test_suite(
        name = name,
        tests = [
            ":" + name + "_enabled_test",
            ":" + name + "_disabled_test",
            ":" + name + "_propagation_test",
        ],
    )
