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

Otherwise, the Buildkite CI will yell at you about formatting/linting violations.

## Packaging

The release process for rules_kotlin is a bit more involved than packaging and shipping an archive. This is for a few reasons:
  1. Reduce download size
  1. Support versioned rules, necessary due to api churn of the koltin compiler.
 
Steps performed in generating a release:
  1. Assemble a tgz that contains the source defined by [release_archive](src/main/starlark/release/packager.bzl)
    1. `release_archive` allows renaming files in the archive. By convention, any source with `<name>.release.<ext>` should be renamed in the release archive as `<name>.<ext>`
  1. Test against example projects
  1. Generate documentation
  
All of these steps are tested by [scripts/release.sh](scripts/release.sh). Contributors are highly encouraged to run the script before opening prs. Cleaning up warnings is especially welcomed.

To ease development, pains should be taken to keep the packages in the archive the same as the location in the source.

### Multi-repo runtime

The `rules_kotlin` runtime is comprised of multiple repositories. The end user will interact with a single repository, that repository delegates to 
versioned feature sub-repositories. Currently, the delegation is managed by using well known names (e.g. core lives in `@io_bazel_rules_kotlin_configured`),
a necessity while the initial repository can be named arbitrarily. Future development intends to remove this restriction.

### Versioning

To cope with API churn, the release archive can configure different rule attributes for depending on the chosen kotlin version.
 Each major release of kotlin (1.4, 1.5) has a specific sub-repository under [src/main/starlark](src/main/starlark). The naming convention for these
  is `rkt_<major>_<minor>` ([r]elease [k]o[t]lin).

The version is selected by the [kotlin_repositories](src/main/starlark/repositories/initialize.release.bzl) rule during initialization. 
New versions of kotlin that change the API should be added to [versions.bzl](src/main/starlark/repositories/versions.bzl), under `CORE` following the 
existing naming convention.

Multiple versions of kotlin are not currently handled.(_help wanted_)

## Idioms and Styles
TBD

### Kotlin
TBD

### Starlark
  1. New starlark should be placed under `src/main/starlark`:
      1. `core` of the `rules_kotlin` module, limited to generic structures  
      1. `legacy` is deprecated, handling options for pre 1.4 kotlin version
      1. `rkt_<version>` all version specific kotlin related pieces should live here. This of it as versioned core.
      1. `<feature>` new features like `ktlint`, `android`, etc. etc. should live here.
  1. Tests. As much as possible all new starlark features should have tests. PRs that extend coverage a very welcome.
  1. Prefer toolchain to implicit dependencies on rules. Toolchains are handled lazily and offer more versatility.
  1. Avoid wrapping rule in macros. `rules_kotlin` should be considered a building block for an organization specific DSL, as such macros should be used sparingly.
  1. Restrict, then Open new rule apis. It's much better to add features based on feedback than to try and remove them. 
  
