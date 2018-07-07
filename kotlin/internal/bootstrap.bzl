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
"""This file contains rules used to bootstrap the compiler repository."""

load("//kotlin/internal:kt.bzl", "kt")
load("//kotlin/internal:rules.bzl", _kt_jvm_import_impl="kt_jvm_import_impl")
load("@bazel_tools//tools/build_defs/repo:http.bzl", _http_archive="http_archive")

kotlin_stdlib = rule(
    attrs = {
        "jars": attr.label_list(
            allow_files = True,
            mandatory = True,
        ),
        "srcjar": attr.label(
            allow_single_file = True,
        ),
    },
    implementation = _kt_jvm_import_impl,
)

"""Import Kotlin libraries that are part of the compiler release."""

def _kt_toolchain_ide_info_impl(ctx):
    tc=ctx.toolchains[kt.defs.TOOLCHAIN_TYPE]
    info = struct(
        label = tc.label,
        common = struct(
            language_version = tc.language_version,
            api_version = tc.api_version,
            coroutines = tc.coroutines
        ),
        jvm = struct(
            jvm_target = tc.jvm_target,
        )
    )
    ctx.actions.write(ctx.outputs.ide_info, info.to_json())
    return [DefaultInfo(files=depset([ctx.outputs.ide_info]))]

kt_toolchain_ide_info = rule(
    outputs = {"ide_info": "kt_toolchain_ide_info.json"},
    toolchains = [kt.defs.TOOLCHAIN_TYPE],
    implementation = _kt_toolchain_ide_info_impl,
)

def github_archive(name, repo, commit, build_file_content = None):
    if build_file_content:
        _http_archive(
            name = name,
            strip_prefix = "%s-%s" % (repo.split("/")[1], commit),
            url = "https://github.com/%s/archive/%s.zip" % (repo, commit),
            type = "zip",
            build_file_content = build_file_content,
        )
    else:
        _http_archive(
            name = name,
            strip_prefix = "%s-%s" % (repo.split("/")[1], commit),
            url = "https://github.com/%s/archive/%s.zip" % (repo, commit),
            type = "zip",
        )
