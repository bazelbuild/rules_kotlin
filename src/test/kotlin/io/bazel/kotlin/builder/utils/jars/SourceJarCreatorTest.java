/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.bazel.kotlin.builder.utils.jars;

import com.google.common.truth.StandardSubjectBuilder;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Truth.assertWithMessage;

public class SourceJarCreatorTest {
  private final String expectedPackage = "iO.some1.package";

  private final List<String> cases =
      Arrays.asList(
          "package iO.some1.package",
          "package iO.some1.package ",
          "package iO.some1.package;",
          " package iO.some1.package; ",
          " /* a comment*/ package iO.some1.package; ",
          " /** a comment*/ package iO.some1.package; ",
          " /* a comment*//*blah*/package iO.some1.package; ",
          " /* a comment*//*blah*/ package iO.some1.package; ",
          " /* a comment*/package /* blah */ iO.some1.package /*lala*/; ",
          "/* a comment*/package/**/iO.some1.package/*b*/;");

  @Test
  public void testPackageNameRegex() {
    cases.forEach(
        (testCase) -> {
          String pkg = SourceJarCreator.Companion.extractPackage(testCase);
          StandardSubjectBuilder subj = assertWithMessage("positive test case: " + testCase);
          subj.that(pkg).isNotNull();
          subj.that(pkg).isEqualTo(expectedPackage);
        });
  }
}
