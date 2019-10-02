# Copyright 2018 The Bazel Authors. All rights reserved.
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

exports_files([
    "scripts/noop.sh",
])

# The entire test suite excluding local tests.
test_suite(
    name = "all_tests",
    tests = [
        "//src/test/kotlin/io/bazel/kotlin:assertion_tests",
        "//src/test/kotlin/io/bazel/kotlin/builder:builder_tests",
    ],
)

#  Local tests. Tests that shouldn't be run on the CI server.
test_suite(
    name = "all_local_tests",
    tests = [
        ":all_tests",
        "//src/test/kotlin/io/bazel/kotlin:local_assertion_tests",
    ],
)
