def partial(function, **defaults):
    def partial(**call):
        resolved = dict(defaults)
        resolved.update(call)
        return function(**resolved)

    return partial
