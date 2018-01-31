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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public final class Utils {
    public static List<String> waitForOutput(String[] command, PrintStream err) {
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            Process process = builder.start();
            try (BufferedReader processError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                 BufferedReader output = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                while (true) {
                    String line = processError.readLine();
                    if (line == null)
                        break;
                    err.println(line);
                }
                if (process.waitFor() != 0) {
                    throw new RuntimeException("non-zero return: " + process.exitValue());
                }
                return output.lines().collect(Collectors.toList());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void waitForSuccess(String[] command, PrintStream err) {
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            Process process = builder.start();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                while (true) {
                    String line = in.readLine();
                    if (line == null)
                        break;
                    err.println(line);
                }
                if (process.waitFor() != 0) {
                    throw new RuntimeException("non-zero return: " + process.exitValue());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static void deleteDirectory(Path directory) throws IOException {
        Files.walk(directory)
                .map(Path::toFile)
                .sorted((o1, o2) -> -o1.compareTo(o2))
                .forEach(File::delete);

    }

    public static Throwable getRootCause(Throwable e) {
        Throwable cause;
        Throwable result = e;

        while (null != (cause = result.getCause()) && (result != cause)) {
            result = cause;
        }
        return result;
    }
}
