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
package io.bazel.ruleskotlin.workers.compilers.jvm.utils;

import java.io.*;
import java.nio.file.Paths;
import java.util.stream.Collectors;


/**
 * Utility class to perform common pre-processing on the compiler output before it is passed onto a delegate
 * PrintStream.
 */
// The kotlin compiler produces absolute file paths but the intellij plugin expects workspace root relative paths to
// render errors.
public abstract class KotlinCompilerOutputProcessor {
    private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    // Get the absolute path to ensure the sandbox root is resolved.
    private final String executionRoot = Paths.get("").toAbsolutePath().toString() + File.separator;
    final PrintStream delegate;

    private KotlinCompilerOutputProcessor(PrintStream delegate) {
        this.delegate = delegate;
    }

    public PrintStream getCollector() {
        return new PrintStream(byteArrayOutputStream);
    }

    public static class ForKotlinC extends KotlinCompilerOutputProcessor {
        public ForKotlinC(PrintStream delegate) {
            super(delegate);
        }

        @Override
        protected boolean processLine(String line) {
            delegate.println(trimExecutionRootPrefix(line));
            return true;
        }
    }


    final String trimExecutionRootPrefix(String toPrint) {
        // trim off the workspace component
        if (toPrint.startsWith(executionRoot)) {
            return toPrint.replaceFirst(executionRoot, "");
        }
        return toPrint;
    }

    protected abstract boolean processLine(String line);

    public void process() {
        for (String s : new BufferedReader(new InputStreamReader(new ByteArrayInputStream(byteArrayOutputStream.toByteArray())))
                .lines().collect(Collectors.toList())) {
            boolean shouldContinue = processLine(s);
            if(!shouldContinue) {
                break;
            }
        }
        delegate.flush();
    }
}
