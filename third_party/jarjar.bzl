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

def jarjar_action(actions, label, rules, input, output, jarjar):
    actions.run(
        inputs = [rules, input],
        outputs = [output],
        executable = jarjar,
        progress_message = "jarjar %s" % label,
        arguments = ["process", rules.path, input.path, output.path],
    )
    return output

def _jar_jar_impl(ctx):
    jar = jarjar_action(
        actions = ctx.actions,
        label = ctx.label,
        rules = ctx.file.rules,
        input = ctx.file.input_jar,
        output = ctx.outputs.jar,
        jarjar = ctx.executable.jarjar_runner,
    )
    return [
        DefaultInfo(
            files = depset([jar]),
            runfiles = ctx.runfiles(files = [jar]),
        ),
        JavaInfo(
            output_jar = jar,
            compile_jar = jar,
        ),
    ]

jar_jar = rule(
    implementation = _jar_jar_impl,
    attrs = {
        "input_jar": attr.label(allow_single_file = True),
        "rules": attr.label(allow_single_file = True),
        "jarjar_runner": attr.label(
            executable = True,
            cfg = "host",
            default = Label("//third_party:jarjar_runner"),
        ),
    },
    outputs = {
        "jar": "%{name}.jar",
    },
    provides = [JavaInfo],
)
