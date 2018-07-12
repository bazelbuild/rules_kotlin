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
package io.bazel.kotlin.builder.mode.jvm.utils;

import com.google.devtools.build.lib.view.proto.Deps;
import io.bazel.kotlin.builder.tasks.jvm.JdepsParser;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@RunWith(JUnit4.class)
public class JdepsParserTest {
  private static final String JDK8_FIXTURE =
      "alt.jar -> bazel-server-cloud/external/com_github_jetbrains_kotlin/lib/kotlin-stdlib-jdk7.jar\n"
          + "alt.jar -> bazel-server-cloud/external/com_github_jetbrains_kotlin/lib/kotlin-stdlib.jar\n"
          + "alt.jar -> bazel-bin/cloud/qa/integrationtests/pkg/extensions/postgres/postgres.jar\n"
          + "alt.jar -> bazel-server-cloud/bazel-out/darwin-fastbuild/bin/cloud/qa/integrationtests/pkg/alt/alt.runfiles/__main__/external/org_postgresql_postgresql/jar/postgresql-42.1.1.jar\n"
          + "alt.jar -> /Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/jre/lib/rt.jar\n"
          + "alt.jar -> bazel-bin/cloud/qa/integrationtests/pkg/utils/utils.jar\n"
          + "   com.axsy.testing.alt (alt.jar)\n"
          + "      -> com.axsy.testing.extensions.postgres               postgres.jar\n"
          + "      -> com.axsy.testing.pkg.utils                         utils.jar\n"
          + "      -> java.io                                            \n"
          + "      -> java.lang                                          \n"
          + "      -> java.sql                                           \n"
          + "      -> javax.sql                                          \n"
          + "      -> kotlin                                             kotlin-stdlib.jar\n"
          + "      -> kotlin.jdk7                                        kotlin-stdlib-jdk7.jar\n"
          + "      -> kotlin.jvm.internal                                kotlin-stdlib.jar\n"
          + "      -> org.postgresql.ds                                  postgresql-42.1.1.jar\n"
          + "   com.axsy.testing.alt.sub (alt.jar)\n"
          + "      -> java.lang                                          \n"
          + "      -> kotlin                                             kotlin-stdlib.jar\n";

  private static final String JDK9_FIXTURE =
      "alt.jar -> java.base\n"
          + "alt.jar -> java.sql\n"
          + "alt.jar -> bazel-server-cloud/external/com_github_jetbrains_kotlin/lib/kotlin-stdlib-jdk7.jar\n"
          + "alt.jar -> bazel-server-cloud/external/com_github_jetbrains_kotlin/lib/kotlin-stdlib.jar\n"
          + "alt.jar -> bazel-bin/cloud/qa/integrationtests/pkg/extensions/postgres/postgres.jar\n"
          + "alt.jar -> bazel-server-cloud/bazel-out/darwin-fastbuild/bin/cloud/qa/integrationtests/pkg/alt/alt.runfiles/__main__/external/org_postgresql_postgresql/jar/postgresql-42.1.1.jar\n"
          + "alt.jar -> bazel-bin/cloud/qa/integrationtests/pkg/utils/utils.jar\n"
          + "   com.axsy.testing.alt                               -> com.axsy.testing.extensions.postgres               postgres.jar\n"
          + "   com.axsy.testing.alt                               -> com.axsy.testing.pkg.utils                         utils.jar\n"
          + "   com.axsy.testing.alt                               -> java.io                                            java.base\n"
          + "   com.axsy.testing.alt                               -> java.lang                                          java.base\n"
          + "   com.axsy.testing.alt                               -> java.sql                                           java.sql\n"
          + "   com.axsy.testing.alt                               -> javax.sql                                          java.sql\n"
          + "   com.axsy.testing.alt                               -> kotlin                                             kotlin-stdlib.jar\n"
          + "   com.axsy.testing.alt                               -> kotlin.jdk7                                        kotlin-stdlib-jdk7.jar\n"
          + "   com.axsy.testing.alt                               -> kotlin.jvm.internal                                kotlin-stdlib.jar\n"
          + "   com.axsy.testing.alt                               -> org.postgresql.ds                                  postgresql-42.1.1.jar\n"
          + "   com.axsy.testing.alt.sub                           -> java.lang                                          java.base\n"
          + "   com.axsy.testing.alt.sub                           -> kotlin                                             kotlin-stdlib.jar\n";

  private static final List<String> CLASSPATH =
      Arrays.asList(
          "bazel-bin/cloud/qa/integrationtests/pkg/extensions/postgres/unused.jar",
          "bazel-server-cloud/external/com_github_jetbrains_kotlin/lib/kotlin-stdlib-jdk8.jar",
          "bazel-server-cloud/external/com_github_jetbrains_kotlin/lib/kotlin-stdlib-jdk7.jar",
          "bazel-server-cloud/external/com_github_jetbrains_kotlin/lib/kotlin-stdlib.jar",
          "bazel-bin/cloud/qa/integrationtests/pkg/extensions/postgres/postgres.jar",
          "bazel-server-cloud/bazel-out/darwin-fastbuild/bin/cloud/qa/integrationtests/pkg/alt/alt.runfiles/__main__/external/org_postgresql_postgresql/jar/postgresql-42.1.1.jar",
          "bazel-bin/cloud/qa/integrationtests/pkg/utils/utils.jar");

  private static final String LABEL = "//cloud/qa/integrationtests/pkg/alt";
  private static final String CLASS_JAR = "bazel-bin/something/alt.jar";

  private static final Predicate<String> IS_KOTLIN_IMPLICIT =
      JdepsParser.Companion.pathSuffixMatchingPredicate(
          Paths.get("external", "com_github_jetbrains_kotlin", "lib"),
          "kotlin-stdlib.jar",
          "kotlin-stdlib-jdk7.jar",
          "kotlin-stdlib-jdk8.jar");

  @Test
  public void parseJDK8Format() throws IOException {
    testWithFixture(JDK8_FIXTURE);
  }

  @Test
  public void parseJDK9Format() throws IOException {
    testWithFixture(JDK9_FIXTURE);
  }

  private void testWithFixture(String fixture) throws IOException {
    Deps.Dependencies result =
        JdepsParser.Companion.parse(
            LABEL,
            CLASS_JAR,
            CLASSPATH.stream().collect(Collectors.joining(":")),
            Arrays.asList(fixture.split("\n")),
            IS_KOTLIN_IMPLICIT);
    Assert.assertEquals(LABEL, result.getRuleLabel());

    Assert.assertEquals(7, result.getDependencyCount());
    Assert.assertEquals(1, depKinds(result, Deps.Dependency.Kind.UNUSED).size());
    Assert.assertEquals(3, depKinds(result, Deps.Dependency.Kind.IMPLICIT).size());
    Assert.assertEquals(3, depKinds(result, Deps.Dependency.Kind.EXPLICIT).size());

    Assert.assertEquals(2, result.getContainedPackageCount());

    result.writeTo(System.out);
    System.out.flush();
  }

  private List<Deps.Dependency> depKinds(Deps.Dependencies result, Deps.Dependency.Kind kind) {
    return result
        .getDependencyList()
        .stream()
        .filter(x -> x.getKind() == kind)
        .collect(Collectors.toList());
  }
}
