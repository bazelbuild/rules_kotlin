"""
Collection of utility functions for the action subject
"""

def fail_messages_in(target_subject):
    return target_subject.failures().transform(
        desc = "failure.message",
        map_each = lambda f: f.partition("Error in fail:")[2].strip() if "Error in fail:" in f else f,
    )

def flags_and_values_of(action_subject):
    return action_subject.argv().transform(desc = "parsed()", loop = _action_subject_parse_flags)

def _action_subject_parse_flags(argv):
    parsed_flags = {}

    # argv might be none for e.g. builtin actions
    if argv == None:
        return parsed_flags
    last_flag = None
    for arg in argv:
        value = None
        if arg == "--":
            # skip the rest of the arguments, this is standard end of the flags.
            break
        if arg.startswith("-"):
            if "=" in arg:
                last_flag, value = arg.split("=", 1)
            else:
                last_flag = arg
        elif last_flag:
            # have a flag, therefore this is probably an associated argument
            value = arg
        else:
            # skip non-flag arguments
            continue

        # only set the value if it exists
        if value:
            parsed_flags.setdefault(last_flag, []).append(value)
    return parsed_flags.items()
