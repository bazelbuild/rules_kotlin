# BTAPI Compatibility Example

This example shows how to configure a custom Kotlin toolchain that uses Kotlin `2.3.10` while building with `rules_kotlin`.

The key idea is to override BTAPI runtime wiring with coordinates resolved by `rules_jvm_external`:

- `org.jetbrains.kotlin:kotlin-build-tools-impl:2.3.10`

`rules_kotlin` derives the full BTAPI runtime classpath from the `kotlin-build-tools-impl` JavaInfo transitives.

The custom toolchain also overrides:

- `org.jetbrains.kotlin:kotlin-stdlib:2.3.10`

The custom toolchain is declared in `BUILD.bazel` with `define_kt_toolchain` and registered in `MODULE.bazel` via:

`register_toolchains("//:kotlin_2310_toolchain")`

`Compat.kt` verifies two different things:

- `KotlinVersion.CURRENT.toString() == "2.3.10"`
- a compiler-plugin-generated value is also `"2.3.10"`

The first assertion proves that the stdlib on the runtime/test classpath is `2.3.10`.

The second assertion proves that the compiler that compiled the target was also `2.3.10`: a custom compiler plugin injects the compiler version during compilation, and the test reads that generated value at runtime.

Together, those checks validate that the example is running with Kotlin `2.3.10` stdlib and actually being compiled with Kotlin `2.3.10`.
