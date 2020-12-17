package io.bazel.kotlin.builder;

import java.util.Arrays;

/**
 * Entry point for Jdeps merger builder. A builder for merging multiple Jdeps files into a single
 * file.
 */
public class JdepsMergerMain {
  public static void main(String[] args) {
    JdepsMergerComponent component =
        DaggerJdepsMergerComponent.builder()
            .build();
    System.exit(component.worker().apply(Arrays.asList(args)));
  }
}