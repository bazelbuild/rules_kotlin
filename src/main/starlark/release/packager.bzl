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

load("@rules_pkg//:pkg.bzl", "pkg_tar")

def release_archive(name, srcs = None, src_map = {}, package_dir = None, extension = "tgz", deps = None):
    """
    Creates a tar of the srcs with content filtering for release.

    All source files are filtered through the content_filter tool which extracts
    only content between RELEASE-CONTENT-START and RELEASE-CONTENT-END markers.
    Files without these markers are included unchanged. Binary files are copied as-is.

    Usage:

    //:BUILD
    load("//src/main/starlark/release:packager.bzl", "release_archive")
    release_archive(
        name = "release_archive",
        src_map = {
            "BUILD": "BUILD.bazel",  # Filter BUILD and output as BUILD.bazel
        },
        deps = [
            "//dep:pkg"
        ],
    )

    //dep:BUILD
    load("//src/main/starlark/release:packager.bzl", "release_archive")
    release_archive(
        name = "pkg",
        srcs = [
            ":label_of_artifact",
        ],
    )


    Args:
        name: target identifier, points to a pkg_tar target.
        package_dir: directory to place the srcs, src_map, and dist_files under. Defaults to the current directory.
        src_map: dict of <label>:<name string> for labels to be filtered and renamed in the distribution.
        srcs: files to include in the distribution (will be filtered, keeping original names).
        extension: Extension of the archive. Controls the type of tar file generated.
        deps: release_archives to be included.
    """

    if package_dir == None:
        pkg_name = native.package_name()
        local_pkg_index = pkg_name.rfind("/")
        if local_pkg_index > -1:
            pkg_name = pkg_name[local_pkg_index:]
        package_dir = pkg_name

    if srcs == None:
        srcs = []

    filtered_srcs = []

    # Filter regular srcs (keeping original filename)
    for i, src in enumerate(srcs):
        # Extract just the filename from the label for the target name
        src_str = str(src)
        if ":" in src_str:
            filename = src_str.split(":")[-1]
        else:
            filename = src_str.split("/")[-1]
        filter_name = name + "_filter_" + str(i) + "_" + filename.replace(".", "_").replace("-", "_")
        _content_filter(
            name = filter_name,
            source = src,
            target = filename,
        )
        filtered_srcs.append(filter_name)

    # Filter and rename src_map entries
    for source, target in src_map.items():
        filter_name = name + "_" + target.replace(".", "_").replace("/", "_")
        _content_filter(
            name = filter_name,
            source = source,
            target = target,
        )
        filtered_srcs.append(filter_name)

    pkg_tar(
        name = name,
        srcs = filtered_srcs,
        extension = extension,
        package_dir = package_dir,
        visibility = ["//visibility:public"],
        deps = deps if deps != None else [],
    )

def _rename_impl(ctx):
    out_file = ctx.actions.declare_file(ctx.label.name + "/" + ctx.attr.target)
    in_file = ctx.file.source
    ctx.actions.run_shell(
        inputs = [in_file],
        outputs = [out_file],
        progress_message = "%s -> %s" % (in_file, ctx.attr.target),
        command = "mkdir -p {dir} && cp {in_file} {out_file}".format(
            dir = ctx.label.name,
            in_file = in_file.path,
            out_file = out_file.path,
        ),
    )
    return [DefaultInfo(files = depset([out_file]))]

_rename = rule(
    implementation = _rename_impl,
    attrs = {
        "source": attr.label(allow_single_file = True, mandatory = True),
        "target": attr.string(mandatory = True),
    },
)

def _content_filter_impl(ctx):
    """Filter file content based on RELEASE-CONTENT tags."""
    out_file = ctx.actions.declare_file(ctx.label.name + "/" + ctx.attr.target)
    in_file = ctx.file.source

    ctx.actions.run(
        inputs = [in_file],
        outputs = [out_file],
        executable = ctx.executable._filter_tool,
        arguments = [
            "--input",
            in_file.path,
            "--output",
            out_file.path,
        ],
        progress_message = "Filtering %s -> %s" % (in_file.short_path, ctx.attr.target),
    )
    return [DefaultInfo(files = depset([out_file]))]

_content_filter = rule(
    implementation = _content_filter_impl,
    attrs = {
        "source": attr.label(allow_single_file = True, mandatory = True),
        "target": attr.string(mandatory = True),
        "_filter_tool": attr.label(
            default = "//src/main/starlark/release:content_filter",
            executable = True,
            cfg = "exec",
        ),
    },
)
