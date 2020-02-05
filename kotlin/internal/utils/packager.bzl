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

def release_archive(name, dist_files = {}, srcs = None, renames = {}, **kwargs):
    """
    Creates an tar of the srcs, dist_files, and renamed label artifacts.

    Usage:

    //:BUILD
    load("//kotlin/internal/utils:packager.bzl", "release_archive")
    release_archive(
        name = "release_archive",
        dist_files = {
            "BUILD": "",
            "WORKSPACE": "workspace(name = "io_bazel_rules_kotlin")",
        },
        extension = "tgz",
        deps = [
            "//dep:pkg"
        ],
    )

    //dep:BUILD
    load("//kotlin/internal/utils:packager.bzl", "release_archive")
    release_archive(
        name = "pkg",
        srcs = [
            ":label_of_artifact",
        ],
    )


    Args:
        name: target identifier, points to a pkg_tar target.
        package_dir: directory to place the srcs, renames, and dist_files under. Defaults to the current directory.
        dist_files: dict of <filename string>:<contents string> for files to be generated in the distribution artifact.
        renames: dict of <label>:<name string> for labels to be renamed and included in the distribution.
        srcs: files to include in the distribution.
        ext: Extension of the archive. Controls the type of tar file generated.
    """

    if "package_dir" not in kwargs:
        pkg_name = native.package_name()
        local_pkg_index = pkg_name.rfind("/")
        if local_pkg_index > -1:
            pkg_name = pkg_name[local_pkg_index:]
        kwargs["package_dir"] = pkg_name

    # release_archives are always public.
    kwargs["visibility"] = ["//visibility:public"]
    if srcs == None:
        srcs = []
    for path, content in dist_files.items():
        gen_name = name + "_" + path
        _gen_file(
            name = gen_name,
            contents = content,
            path = path,
        )
        srcs += [gen_name]

    for source, target in renames.items():
        rename_name = name + "_" + target
        _rename(
            name = rename_name,
            source = source,
            target = target,
        )
        srcs += [rename_name]

    pkg_tar(
        name = name,
        srcs = srcs,
        **kwargs
    )

_file_header = """
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
"""

def _gen_file_impl(ctx):
    """
    Creates a file in a directory based on the rule name.
    Useful for creating source files that may overlap existing sources in a given directory.

    ** This is not windows safe. **

    Args:
        path: a valid, raltive path string. Unvalidated.
        contents: string to write to the path.
    """
    out = ctx.actions.declare_file(ctx.label.name + "/" + ctx.attr.path)
    ctx.actions.write(out, _file_header + ctx.attr.contents)
    return [DefaultInfo(files = depset([out]))]

_gen_file = rule(
    implementation = _gen_file_impl,
    attrs = {
        "contents": attr.string(mandatory = True),
        "path": attr.string(mandatory = True),
    },
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
