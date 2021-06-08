# Contributing

Want to contribute? Great! First, read this page (including the small print at the end).

# Before you contribute

Before we can use your code, you must sign the Google Individual Contributor License Agreement (CLA), which you can do online.

The CLA is necessary mainly because you own the copyright to your changes, even after your contribution becomes part of our codebase, so we need your permission to use and distribute your code. We also need to be sure of various other things — for instance that you'll tell us if you know that your code infringes on other people's patents. You don't have to sign the CLA until after you've submitted your code for review and a member has approved it, but you must do it before we can put your code into our codebase.

# The small print

Contributions made by corporations are covered by a different agreement than the one above, the Software Grant and Corporate Contributor License Agreement.

# Contribution process

Explain your idea and discuss your plan with members of the team. The best way to do this is to create an issue or comment on an existing issue.

Prepare a git commit with your change. Don't forget to add tests. Run the existing tests with bazel test //.... Update README.md if appropriate.

Create a pull request. This will start the code review process. All submissions, including submissions by project members, require review.

You may be asked to make some changes. You'll also need to sign the CLA at this point, if you haven't done so already. Our continuous integration bots will test your change automatically on supported platforms. Once everything looks good, your change will be merged.

## Formatting

Starlark files should be formatted by buildifier.
We suggest using a pre-commit hook to automate this.
First [install pre-commit](https://pre-commit.com/#installation),
then run

```shell
pre-commit install
```

Otherwise the Buildkite CI will yell at you about formatting/linting violations.
