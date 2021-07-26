"""tools for managing repositories."""

def absolute_target(repo_relative_target):
    """Converts a relative path inside a repository to a fully qualified name.

    This uses builtin Label to translate relative paths to repository qualified targets of the defining bzl file.

    For instance, absolute_target is defined in repository "zumgali". Calling absolute_target("//gali/gali:zum") returns
    "@zumgali//gali/gali:zum"

    Args:
      repo_relative_target: relative target expression
    Returns:
      Fully qualified repository string.
    """
    label = Label(repo_relative_target)
    return "@%s//%s:%s" % (label.workspace_name, label.package, label.name)
