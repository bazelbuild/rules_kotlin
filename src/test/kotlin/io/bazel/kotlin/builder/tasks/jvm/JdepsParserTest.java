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
package io.bazel.kotlin.builder.tasks.jvm;

import com.google.common.truth.Truth;
import com.google.devtools.build.lib.view.proto.Deps;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"KotlinInternalInJava", "SpellCheckingInspection"})
@RunWith(JUnit4.class)
public class JdepsParserTest {
    private static final List<String> JDK8_FIXTURE =
            toPlatformPaths(
                    "alt.jar -> bazel-server-cloud/external/com_github_jetbrains_kotlin/lib/kotlin-stdlib-jdk7.jar",
                    "alt.jar -> bazel-server-cloud/external/com_github_jetbrains_kotlin/lib/kotlin-stdlib.jar",
                    "alt.jar -> bazel-bin/cloud/qa/integrationtests/pkg/extensions/postgres/postgres.jar",
                    "alt.jar -> bazel-server-cloud/bazel-out/darwin-fastbuild/bin/cloud/qa/integrationtests/pkg/alt/alt.runfiles/__main__/external/org_postgresql_postgresql/jar/postgresql-42.1.1.jar",
                    "alt.jar -> /Library/Java/JavaVirtualMachines/jdk1.8.0_144.jdk/Contents/Home/jre/lib/rt.jar",
                    "alt.jar -> bazel-bin/cloud/qa/integrationtests/pkg/utils/utils.jar",
                    "   com.axsy.testing.alt (alt.jar)",
                    "      -> com.axsy.testing.extensions.postgres               postgres.jar",
                    "      -> com.axsy.testing.pkg.utils                         utils.jar",
                    "      -> java.io                                            ",
                    "      -> java.lang                                          ",
                    "      -> java.sql                                           ",
                    "      -> javax.sql                                          ",
                    "      -> kotlin                                             kotlin-stdlib.jar",
                    "      -> kotlin.jdk7                                        kotlin-stdlib-jdk7.jar",
                    "      -> kotlin.jvm.internal                                kotlin-stdlib.jar",
                    "      -> org.postgresql.ds                                  postgresql-42.1.1.jar",
                    "   com.axsy.testing.alt.sub (alt.jar)",
                    "      -> java.lang                                          ",
                    "      -> kotlin                                             kotlin-stdlib.jar");

    private static final List<String> JDK9_FIXTURE =
            toPlatformPaths(
                    "alt.jar -> java.base",
                    "alt.jar -> java.sql",
                    "alt.jar -> bazel-server-cloud/external/com_github_jetbrains_kotlin/lib/kotlin-stdlib-jdk7.jar",
                    "alt.jar -> bazel-server-cloud/external/com_github_jetbrains_kotlin/lib/kotlin-stdlib.jar",
                    "alt.jar -> bazel-bin/cloud/qa/integrationtests/pkg/extensions/postgres/postgres.jar",
                    "alt.jar -> bazel-server-cloud/bazel-out/darwin-fastbuild/bin/cloud/qa/integrationtests/pkg/alt/alt.runfiles/__main__/external/org_postgresql_postgresql/jar/postgresql-42.1.1.jar",
                    "alt.jar -> bazel-bin/cloud/qa/integrationtests/pkg/utils/utils.jar",
                    "   com.axsy.testing.alt                               -> com.axsy.testing.extensions.postgres               postgres.jar",
                    "   com.axsy.testing.alt                               -> com.axsy.testing.pkg.utils                         utils.jar",
                    "   com.axsy.testing.alt                               -> java.io                                            java.base",
                    "   com.axsy.testing.alt                               -> java.lang                                          java.base",
                    "   com.axsy.testing.alt                               -> java.sql                                           java.sql",
                    "   com.axsy.testing.alt                               -> javax.sql                                          java.sql",
                    "   com.axsy.testing.alt                               -> kotlin                                             kotlin-stdlib.jar",
                    "   com.axsy.testing.alt                               -> kotlin.jdk7                                        kotlin-stdlib-jdk7.jar",
                    "   com.axsy.testing.alt                               -> kotlin.jvm.internal                                kotlin-stdlib.jar",
                    "   com.axsy.testing.alt                               -> org.postgresql.ds                                  postgresql-42.1.1.jar",
                    "   com.axsy.testing.alt.sub                           -> java.lang                                          java.base",
                    "   com.axsy.testing.alt.sub                           -> kotlin                                             kotlin-stdlib.jar");

    private static final List<String> CLASSPATH =
            toPlatformPaths(
                    "bazel-bin/cloud/qa/integrationtests/pkg/extensions/postgres/unused.jar",
                    "bazel-server-cloud/external/com_github_jetbrains_kotlin/lib/kotlin-stdlib-jdk8.jar",
                    "bazel-server-cloud/external/com_github_jetbrains_kotlin/lib/kotlin-stdlib-jdk7.jar",
                    "bazel-server-cloud/external/com_github_jetbrains_kotlin/lib/kotlin-stdlib.jar",
                    "bazel-bin/cloud/qa/integrationtests/pkg/extensions/postgres/postgres.jar",
                    "bazel-server-cloud/bazel-out/darwin-fastbuild/bin/cloud/qa/integrationtests/pkg/alt/alt.runfiles/__main__/external/org_postgresql_postgresql/jar/postgresql-42.1.1.jar",
                    "bazel-bin/cloud/qa/integrationtests/pkg/utils/utils.jar");

    private static final String LABEL = "//cloud/qa/integrationtests/pkg/alt";
    private static final String CLASS_JAR = toPlatformPath("bazel-bin/something/alt.jar");

    private static final Predicate<String> IS_KOTLIN_IMPLICIT =
            JdepsParser.Companion.pathSuffixMatchingPredicate(
                    Paths.get("external", "com_github_jetbrains_kotlin", "lib"),
                    "kotlin-stdlib.jar",
                    "kotlin-stdlib-jdk7.jar",
                    "kotlin-stdlib-jdk8.jar");

    private static void testWithFixture(List<String> fixture) throws IOException {
        Deps.Dependencies result =
                JdepsParser.Companion.parse(LABEL, CLASS_JAR, CLASSPATH, fixture, IS_KOTLIN_IMPLICIT);

        Assert.assertEquals(LABEL, result.getRuleLabel());

        Truth.assertThat(
                result
                        .getDependencyList()
                        .stream()
                        .map(Deps.Dependency::getPath)
                        .collect(Collectors.toSet()))
                .containsExactlyElementsIn(CLASSPATH);

        Assert.assertEquals(7, result.getDependencyCount());
        Assert.assertEquals(1, depKinds(result, Deps.Dependency.Kind.UNUSED).size());
        Assert.assertEquals(3, depKinds(result, Deps.Dependency.Kind.IMPLICIT).size());
        Assert.assertEquals(3, depKinds(result, Deps.Dependency.Kind.EXPLICIT).size());

        Assert.assertEquals(2, result.getContainedPackageCount());

        result.writeTo(System.out);
        System.out.flush();
    }

    private static List<String> toPlatformPaths(String... lines) {
        return Stream.of(lines).map(JdepsParserTest::toPlatformPath).collect(Collectors.toList());
    }

    // on windows translate absolute paths to c:\ . Also swap the seperators from "/" to "\".
    private static String toPlatformPath(String it) {
        return File.separatorChar != '/' ? it.replace(" /", "c:\\").replace("/", File.separator) : it;
    }

    private static List<Deps.Dependency> depKinds(Deps.Dependencies result, Deps.Dependency.Kind kind) {
        return result
                .getDependencyList()
                .stream()
                .filter(x -> x.getKind() == kind)
                .collect(Collectors.toList());
    }

    @Test
    public void parseJDK8Format() throws IOException {
        testWithFixture(JDK8_FIXTURE);
    }

    @Test
    public void parseJDK9Format() throws IOException {
        testWithFixture(JDK9_FIXTURE);
    }
}
