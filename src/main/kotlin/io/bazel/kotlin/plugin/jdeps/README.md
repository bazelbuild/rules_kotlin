# Per-Class Compilation Avoidance

This feature enables fine-grained dependency tracking for Kotlin compilation, recording exactly which classes from each dependency JAR are used during compilation along with their bytecode hashes.

## Overview

Traditional compilation avoidance works at the JAR level: if any file in a dependency JAR changes, all dependents must recompile. Per-class compilation avoidance improves on this by tracking:

1. **Which classes** from each dependency are actually used
2. **Bytecode hashes** (SHA-256) of those specific classes

If a dependency JAR changes but the specific classes your target uses remain unchanged (same hashes), recompilation can be skipped.

## Configuration

Enable via the Kotlin toolchain:

```starlark
load("@rules_kotlin//kotlin:toolchains.bzl", "define_kt_toolchain")

define_kt_toolchain(
    name = "my_toolchain",
    experimental_track_class_usage = "on",    # Track class usage with hashes
    experimental_track_resource_usage = "on", # Track Android R class references
)
```

### Options

| Option | Values | Description |
|--------|--------|-------------|
| `experimental_track_class_usage` | `"off"` (default), `"on"` | Enable per-class tracking with bytecode hashes |
| `experimental_track_resource_usage` | `"off"` (default), `"on"` | Track Android R class field references |

## How It Works

### Class Usage Tracking

During Kotlin compilation, the JDeps compiler plugin hooks into the analysis phase:

1. **K1 (classic compiler)**: Uses `CallChecker` and `DeclarationChecker` interfaces
2. **K2 (FIR compiler)**: Uses FIR checker extensions

For each class reference, the plugin records:
- Direct type references (class declarations, return types)
- Supertypes (parent classes, implemented interfaces)
- Type arguments (generics)
- Annotations

When writing the jdeps proto, each used class entry includes:
- `fullyQualifiedName`: e.g., `com.example.MyClass`
- `internalPath`: e.g., `com/example/MyClass.class`
- `hash`: SHA-256 hash of the `.class` bytecode

### Resource Usage Tracking

For Android projects, the plugin also tracks R class field references:
- Detects access to fields in classes matching the pattern `*.R.*` or `R.*`
- Records the full resource reference (e.g., `com.example.R.string.app_name`)

## Output Format

The jdeps proto (`deps.proto`) includes:

```protobuf
message Dependency {
  required string path = 1;
  required Kind kind = 2;
  repeated UsedClass usedClasses = 4;  // Added by this feature
}

message UsedClass {
  required string fullyQualifiedName = 1;
  required string internalPath = 2;
  required bytes hash = 3;  // SHA-256
}

message Dependencies {
  repeated string usedResources = 6;  // Added by this feature
}
```

## Build System Integration

To use this feature for compilation avoidance, your build system must:

1. Parse the jdeps proto output
2. Store the `UsedClass` entries and their hashes
3. On dependency change, compare hashes of used classes
4. Skip recompilation if all used class hashes match

## Performance Considerations

**Build-time overhead**:
- Hash computation adds latency (reading and SHA-256 hashing each used class)
- Larger jdeps files (each used class adds ~60-80 bytes)

**Build avoidance benefit**:
- Significant time savings when dependencies change frequently
- Most effective for large monorepos with many transitive dependencies

## Limitations

1. **Kotlin only**: Only tracks class usage during Kotlin compilation. Mixed Java/Kotlin projects may miss Java-only class references.

2. **No method-level tracking**: Tracks whole classes, not individual methods/fields. A change to any method in a used class will trigger recompilation.

3. **Constant inlining**: Compile-time constants from dependencies are inlined into bytecode. If a constant's value changes, the using class won't see the dependency.

4. **Reflection**: Classes accessed via reflection at runtime are not tracked.

5. **Annotation processors**: Dependencies introduced by annotation processors may need separate tracking.

## Example

Given a target that uses `com.lib.Parser` from a dependency:

```kotlin
import com.lib.Parser

fun process(input: String) = Parser.parse(input)
```

The jdeps output will include:

```
dependency {
  path: "bazel-out/.../libparser.jar"
  kind: EXPLICIT
  usedClasses {
    fullyQualifiedName: "com.lib.Parser"
    internalPath: "com/lib/Parser.class"
    hash: <32 bytes SHA-256>
  }
}
```

If `libparser.jar` is rebuilt but `Parser.class` has the same hash, recompilation can be skipped.
