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

"""Macro to translate Bazel modules into legacy WORKSPACE compatible repos.

Used only for Bazel modules that don't offer a legacy WORKSPACE compatible API
already. This became necessary for bazel-worker-api which only provides bzlmod
support.
"""

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
load("@bazel_tools//tools/build_defs/repo:utils.bzl", "maybe")

def _bazel_worker_api_repo(name, strip_prefix):
    """Download bazel-worker-api from GitHub releases.

    Args:
        name: The name of the repository to create.
        strip_prefix: The path within the archive to use as the root (e.g., "proto" or "java").
    """
    maybe(
        http_archive,
        name = name,
        sha256 = "5aac6ae6a23015cc7984492a114dc539effc244ec5ac7f8f6b1539c15fb376eb",
        urls = [
            "https://github.com/bazelbuild/bazel-worker-api/releases/download/v0.0.6/bazel-worker-api-v0.0.6.tar.gz",
        ],
        strip_prefix = "bazel-worker-api-0.0.6/" + strip_prefix,
    )

def workspace_compat():
    """Setup bazel-worker-api repositories for WORKSPACE.

    This function creates the necessary repository rules for bazel-worker-api
    proto and java implementations, making them available in WORKSPACE mode.
    In bzlmod mode, these are loaded via bazel_dep in MODULE.bazel.
    """

    # Proto definitions (worker_protocol.proto)
    _bazel_worker_api_repo(
        name = "bazel_worker_api",
        strip_prefix = "proto",
    )

    # Java implementation (WorkRequestHandler, etc.)
    _bazel_worker_api_repo(
        name = "bazel_worker_java",
        strip_prefix = "java",
    )
