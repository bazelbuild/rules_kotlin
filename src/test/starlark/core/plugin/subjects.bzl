load("@rules_testing//lib:truth.bzl", "subjects")

def plugin_option_subject_factory(value, meta):
    return subjects.struct(
        value,
        meta = meta.derive("option"),
        attrs = {
            "id": subjects.str,
            "value": subjects.str,
        },
    )

def plugin_subject_factory(value, meta):
    return subjects.struct(
        value,
        meta = meta,
        attrs = {
            "id": subjects.str,
            "plugin_jars": subjects.collection,
            "classpath": subjects.collection,
            "stubs": subjects.bool,
            "compile": subjects.bool,
            "options": subjects.collection,
        },
    )

def plugin_configuration_subject_factory(value, meta):
    return subjects.struct(
        value,
        meta = meta,
        attrs = {
            "id": subjects.str,
            "options": subjects.collection,
            "classpath": subjects.collection,
            "data": subjects.collection,
        },
    )
