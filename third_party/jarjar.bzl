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

JAR_JAR_COMMON_ATTRS = {
    "rules": attr.label(allow_single_file = True),
    "jarjar_runner": attr.label(
            executable = True,
            cfg = "host",
            default = Label("//third_party:jarjar_runner"),
        ),
}

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

def _jar_jar_dir_impl(ctx):
    output_dir =  ctx.attr.output_dir
    shaded_jars = []
    for jars in ctx.attr.input_jars:
        for jar in jars.files.to_list():
            output_jar  = ctx.actions.declare_file(output_dir + "/" + jar.basename)
            shaded_jars.append(
                jarjar_action(
                    actions = ctx.actions,
                    label = ctx.label,
                    rules = ctx.file.rules,
                    input = jar,
                    output = output_jar,
                    jarjar = ctx.executable.jarjar_runner
            ))
    return [
        DefaultInfo(
            files = depset(shaded_jars),
            runfiles = ctx.runfiles(files = shaded_jars),
        ),
    ]


JAR_JAR_ATTRS = {
        "input_jar": attr.label(allow_single_file = True),
}
JAR_JAR_ATTRS.update(JAR_JAR_COMMON_ATTRS)

jar_jar = rule(
    implementation = _jar_jar_impl,
    attrs = JAR_JAR_ATTRS,
    outputs = {
        "jar": "%{name}.jar",
    },
    provides = [JavaInfo],
)


JAR_JAR_DIR_ATTRS = {
        "input_jars": attr.label_list(allow_files = True),
        "output_dir": attr.string(mandatory = True),
}
JAR_JAR_DIR_ATTRS.update(JAR_JAR_COMMON_ATTRS)

jar_jar_dir = rule(
    implementation = _jar_jar_dir_impl,
    attrs = JAR_JAR_DIR_ATTRS,
)

