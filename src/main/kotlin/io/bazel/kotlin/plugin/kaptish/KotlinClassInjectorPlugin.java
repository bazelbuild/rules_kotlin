/*
 * Copyright 2026 The Bazel Authors. All rights reserved.
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
package io.bazel.kotlin.plugin.kaptish;

import com.sun.tools.javac.main.Arguments;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Annotation processor that injects compiled Kotlin class names into the annotation
 * processing phase, allowing other annotation processors to see Kotlin classes.
 *
 * This enables "kaptish" mode where:
 * 1. Kotlin is compiled first to .class files
 * 2. This processor injects those class names into javac's AP phase
 * 3. Other annotation processors run on the compiled Kotlin classes (no stubs needed)
 *
 * The processor scans the classpath for JARs containing Kotlin-compiled classes
 * (identified by the presence of .kotlin_module files) and injects their
 * class names into the compilation.
 */
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class KotlinClassInjectorPlugin extends AbstractProcessor {

    private boolean injected = false;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        if (injected) {
            return;
        }
        injected = true;

        // Collect all classes from Kotlin JARs on the classpath
        Set<String> allClasses = new HashSet<>();

        // Find Kotlin JARs on the classpath and extract class names
        List<String> classpathJars = findKotlinJarsOnClasspath();
        for (String jarPath : classpathJars) {
            Collection<String> classes = getClassesFromJar(jarPath);
            allClasses.addAll(classes);
        }

        if (!allClasses.isEmpty()) {
            // Try to inject class names into javac's Arguments
            try {
                injectClassNames(processingEnv, allClasses);
            } catch (Exception e) {
                processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.NOTE,
                    "Kaptish: Could not inject class names: " + e.getMessage()
                );
            }
        }
    }

    /**
     * Inject class names into javac's Arguments.
     */
    private void injectClassNames(ProcessingEnvironment processingEnv, Set<String> classNames) {
        JavacProcessingEnvironment javacEnv = unwrap(processingEnv);
        if (javacEnv == null) {
            return;
        }

        Context context = javacEnv.getContext();
        Arguments arguments = Arguments.instance(context);
        arguments.getClassNames().addAll(classNames);
    }

    /**
     * Unwrap the ProcessingEnvironment to get the underlying JavacProcessingEnvironment.
     *
     * Reflection is used here to handle delegating ProcessingEnvironment wrappers
     * (e.g., from Gradle or other build tools). This is unavoidable since we don't
     * know wrapper class types at compile time.
     */
    private JavacProcessingEnvironment unwrap(ProcessingEnvironment processingEnv) {
        if (processingEnv instanceof JavacProcessingEnvironment) {
            return (JavacProcessingEnvironment) processingEnv;
        }

        // Try to unwrap delegating processors (e.g., from Gradle or other build tools)
        try {
            Field delegateField = processingEnv.getClass().getDeclaredField("delegate");
            delegateField.setAccessible(true);
            return unwrap((ProcessingEnvironment) delegateField.get(processingEnv));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // This processor doesn't generate any code itself
        return false;
    }

    /**
     * Find Kotlin-compiled JARs on the classpath.
     * Kotlin JARs are identified by containing .kotlin_module files.
     */
    private List<String> findKotlinJarsOnClasspath() {
        List<String> result = new ArrayList<>();

        // Get classpath from system property
        String classpath = System.getProperty("java.class.path");
        if (classpath == null || classpath.isEmpty()) {
            return result;
        }

        for (String entry : classpath.split(File.pathSeparator)) {
            if (entry.endsWith(".jar") && new File(entry).exists()) {
                try (JarFile jar = new JarFile(entry)) {
                    // Check if this JAR contains Kotlin-compiled classes
                    // by looking for .kotlin_module files
                    boolean hasKotlinModule = false;
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry jarEntry = entries.nextElement();
                        if (jarEntry.getName().endsWith(".kotlin_module")) {
                            hasKotlinModule = true;
                            break;
                        }
                    }

                    if (hasKotlinModule) {
                        result.add(entry);
                    }
                } catch (IOException e) {
                    // Skip JARs we can't read
                }
            }
        }

        return result;
    }

    /**
     * Extract top-level class names from a JAR file.
     *
     * @param path Path to the JAR file
     * @return Collection of fully-qualified class names (excluding inner classes)
     */
    private Collection<String> getClassesFromJar(String path) {
        List<String> classes = new ArrayList<>();
        try (JarFile jar = new JarFile(path)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (name.endsWith(".class")) {
                    String className = name
                            .substring(0, name.length() - 6) // Remove ".class"
                            .replace("/", ".");

                    // Skip inner classes (they're processed with their enclosing class)
                    String simpleName = className.substring(className.lastIndexOf(".") + 1);
                    if (simpleName.contains("$")) {
                        continue;
                    }

                    // Skip module-info and package-info
                    if (className.endsWith("module-info") || className.endsWith("package-info")) {
                        continue;
                    }

                    classes.add(className);
                }
            }
        } catch (IOException e) {
            // If we can't read the JAR, return empty and let javac proceed normally
        }
        return classes;
    }
}
