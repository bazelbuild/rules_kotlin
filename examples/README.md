# Examples

These are series of code samples useful for configuring a variety of use cases. They also serve as integration tests.

## Adding a new integration test
1. Create a new workspace in the `examples` directory
1. Ensure that the `rules_kotlin` repository is named `rules_kotlin`
1. If the example will not work on a bazel version, add an `exclude` directory with a file of that version. See examples/multiplex/exclude.
1. Update the bazel excluded packages via `bazel run //examples:update_bit_ignore`
