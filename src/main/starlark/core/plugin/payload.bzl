def _plugin_to_json(plugin):
    return {
        "classpath": [entry.path for entry in plugin.classpath.to_list()],
        "id": plugin.id,
        "options": [
            {
                "key": option.key,
                "value": option.value,
            }
            for option in plugin.options
        ],
    }

def _plugins_payload_json(stubs_plugins, compiler_plugins):
    return json.encode({
        "compiler_plugins": [_plugin_to_json(plugin) for plugin in compiler_plugins],
        "stubs_plugins": [_plugin_to_json(plugin) for plugin in stubs_plugins],
    })

plugin_payload = struct(
    plugins_payload_json = _plugins_payload_json,
)
