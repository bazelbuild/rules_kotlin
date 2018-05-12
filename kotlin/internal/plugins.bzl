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
load("//kotlin/internal:kt.bzl", "kt")

def _mk_processor_entry(l,p):
    merged_info=java_common.merge([j[JavaInfo] for j in p.deps])
    classpath_jars = depset([cp for cp in merged_info.full_compile_jars])
    classpath_jars = classpath_jars + merged_info.transitive_runtime_jars
    return struct(
          label=l,
          processor_class=p.processor_class,
          classpath=[cpj.path for cpj in classpath_jars.to_list()],
          generates_api=p.generates_api,
    )

def _merge_plugin_infos(attrs):
    tally={}
    annotation_processors=[]
    for info in [a[kt.info.KtPluginInfo] for a in attrs]:
        for p in info.annotation_processors:
            if p.label not in tally:
                tally[p.label] = True
                annotation_processors.append(p)
    return kt.info.KtPluginInfo(annotation_processors=annotation_processors)

def _restore_label(l):
    lbl = l.workspace_root
    if lbl.startswith("external/"):
        lbl = lbl.replace("external/", "@")
    return lbl + "//" + l.package + ":" + l.name

_EMPTY_PLUGIN_INFO = [kt.info.KtPluginInfo(annotation_processors = [])]

def _kt_jvm_plugin_aspect_impl(target, ctx):
    if ctx.rule.kind == "java_plugin":
        return [kt.info.KtPluginInfo(
            annotation_processors = [_mk_processor_entry(_restore_label(ctx.label),ctx.rule.attr)]
        )]
    else:
      if ctx.rule.kind == "java_library":
          return [_merge_plugin_infos(ctx.rule.attr.exported_plugins)]
      else:
          return _EMPTY_PLUGIN_INFO

kt_jvm_plugin_aspect = aspect(
    attr_aspects = [
        "plugins",
        "exported_plugins",
    ],
    implementation = _kt_jvm_plugin_aspect_impl,
)

"""renders a java info into a kt.info.KtPluginInfo."""

plugins = struct(
    kt_jvm_plugin_aspect = kt_jvm_plugin_aspect,
    merge_plugin_infos = _merge_plugin_infos,
)
