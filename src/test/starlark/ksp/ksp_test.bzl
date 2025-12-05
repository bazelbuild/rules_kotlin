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

"""Tests for KSP2 integration."""

load("@bazel_skylib//lib:unittest.bzl", "analysistest", "asserts")
load("@rules_java//java/common:java_info.bzl", "JavaInfo")

def _ksp_outputs_test_impl(ctx):
    """Verify KSP2 action produces expected outputs."""
    env = analysistest.begin(ctx)

    target = analysistest.target_under_test(env)

    # Check that the target has JavaInfo provider
    asserts.true(
        env,
        JavaInfo in target,
        "Target should provide JavaInfo",
    )

    # Check that compilation outputs exist
    java_info = target[JavaInfo]
    asserts.true(
        env,
        len(java_info.runtime_output_jars) > 0,
        "Target should have runtime output jars",
    )

    return analysistest.end(env)

ksp_outputs_test = analysistest.make(_ksp_outputs_test_impl)

def _ksp_action_test_impl(ctx):
    """Verify KSP2 action is created with correct mnemonic."""
    env = analysistest.begin(ctx)

    # Find the KotlinKsp2 action
    actions = analysistest.target_actions(env)
    ksp2_actions = [a for a in actions if a.mnemonic == "KotlinKsp2"]

    asserts.true(
        env,
        len(ksp2_actions) > 0,
        "Should have at least one KotlinKsp2 action",
    )

    # Verify the KSP2 action outputs JAR files (not tree artifacts)
    if ksp2_actions:
        ksp2_action = ksp2_actions[0]
        outputs = ksp2_action.outputs.to_list()

        # All outputs should be files (not directories)
        for output in outputs:
            asserts.true(
                env,
                output.path.endswith(".jar"),
                "KSP2 output should be a JAR file: %s" % output.path,
            )

    return analysistest.end(env)

ksp_action_test = analysistest.make(_ksp_action_test_impl)

def _ksp_single_action_test_impl(ctx):
    """Verify KSP2 uses a single action (no tree artifacts or staging actions)."""
    env = analysistest.begin(ctx)

    actions = analysistest.target_actions(env)

    # Count KSP-related actions
    ksp_actions = [a for a in actions if "Ksp" in a.mnemonic or "ksp" in a.mnemonic.lower()]

    # Should only have one KSP2 action
    asserts.equals(
        env,
        1,
        len(ksp_actions),
        "Should have exactly one KSP action, got: %s" % [a.mnemonic for a in ksp_actions],
    )

    # Verify no staging actions
    staging_actions = [a for a in actions if "Stage" in a.mnemonic or "Unpack" in a.mnemonic]
    asserts.equals(
        env,
        0,
        len(staging_actions),
        "Should have no staging actions, got: %s" % [a.mnemonic for a in staging_actions],
    )

    return analysistest.end(env)

ksp_single_action_test = analysistest.make(_ksp_single_action_test_impl)

def ksp_test_suite(name):
    """Create test suite for KSP2 integration tests.

    Args:
        name: Name of the test suite
    """

    # We can't create actual kt_jvm_library targets in .bzl files,
    # so the tests are defined in BUILD.bazel and this just creates the suite
    native.test_suite(
        name = name,
        tests = [
            ":ksp_outputs_test",
            ":ksp_action_test",
            ":ksp_single_action_test",
        ],
    )
