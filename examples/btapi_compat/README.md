# BTAPI Compatibility Example

This example shows how to configure a custom Kotlin toolchain that uses an older Kotlin compiler/BTAPI runtime (`2.3.10`) while building with `rules_kotlin`.

The key idea is to override toolchain runtime artifacts with coordinates resolved by `rules_jvm_external`:

- `org.jetbrains.kotlin:kotlin-build-tools-impl:2.3.10`
- `org.jetbrains.kotlin:kotlin-compiler-embeddable:2.3.10`
- `org.jetbrains.kotlin:kotlin-daemon-client:2.3.10`
- `org.jetbrains.kotlin:kotlin-stdlib:2.3.10`
- `org.jetbrains.kotlin:kotlin-reflect:2.3.10`

The custom toolchain is declared in `BUILD.bazel` with `define_kt_toolchain` and registered in `MODULE.bazel` via:

`register_toolchains("//:kotlin_2310_toolchain")`

`Compat.kt` contains a runtime test asserting:

`KotlinVersion.CURRENT.toString() == "2.3.10"`

This validates that compilation/testing runs against the older Kotlin runtime configured through BTAPI-compatible toolchain wiring.
