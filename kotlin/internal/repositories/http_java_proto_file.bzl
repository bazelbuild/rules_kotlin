# Copyright 2020 The Bazel Authors. All rights reserved.
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

def _http_java_proto_file_impl(repo_ctx):
    """ See documentation below for usage. """
    repo_ctx.download(
        sha256 = repo_ctx.attr.sha256,
        url = repo_ctx.attr.urls,
        output = repo_ctx.attr.name + ".proto",
    )
    repo_ctx.file("WORKSPACE", content = """
workspace(name = "{name}")
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive", "http_file")

http_archive(
    name = "rules_proto",
    sha256 = "602e7161d9195e50246177e7c55b2f39950a9cf7366f74ed5f22fd45750cd208",
    strip_prefix = "rules_proto-97d8af4dc474595af3900dd85cb3a29ad28cc313",
    urls = [
        "https://mirror.bazel.build/github.com/bazelbuild/rules_proto/archive/97d8af4dc474595af3900dd85cb3a29ad28cc313.tar.gz",
        "https://github.com/bazelbuild/rules_proto/archive/97d8af4dc474595af3900dd85cb3a29ad28cc313.tar.gz",
    ],
)
load("@rules_proto//proto:repositories.bzl", "rules_proto_dependencies", "rules_proto_toolchains")
rules_proto_dependencies()
rules_proto_toolchains()
    """.format(
        name = repo_ctx.name,
    ))
    repo_ctx.file("BUILD", content = """
package(default_visibility = ["//visibility:public"])

load("@rules_proto//proto:defs.bzl", "proto_library")
load("@rules_java//java:defs.bzl", "java_proto_library")

proto_library(
    name = "proto",
    srcs = glob(["*.proto"]),
)

java_proto_library(
    name = "java_proto",
    deps = [":proto"],
)
""")

http_java_proto_file = repository_rule(
    doc = """Downloads a proto file from an outside source and provides a java_proto_library in a repository.

    The java_proto_library will be made available as @<name>//:proto.

    Example:

    WORKSPACE:
    load("//kotlin/internal/repositories/http_java_proto_file.bzl", "http_java_proto_file")

    http_java_proto_file(
      name = "oysters",
      urls = [
        "https://west.ocean/mollusc.proto",
        "https://futher_west.ocean/mollusc.proto",
      ]
    )

    BUILD.bazel:

    java_library(
        name = "walrus",
        srcs = [...],
        deps = [
            "@oysters//:proto"
        ]
    )

    """,
    implementation = _http_java_proto_file_impl,
    attrs = {
        "sha256": attr.string(
            doc = """The expected SHA-256 of the file downloaded.

         This must match the SHA-256 of the file downloaded. _It is a security risk
         to omit the SHA-256 as remote files can change._ At best omitting this
         field will make your build non-hermetic. It is optional to make development
         easier but should be set before shipping.""",
        ),
        "urls": attr.string_list(
            mandatory = True,
            doc = """A list of URLs to a file that will be made available to Bazel.

         Each entry must be a file, http or https URL. Redirections are followed.
         Authentication is not supported.""",
        ),
    },
)
