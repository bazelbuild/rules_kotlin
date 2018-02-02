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
package io.bazel.ruleskotlin.workers.utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class IOUtils {
    // sort this one out
    public static List<String> executeAndWaitOutput(int timeoutSeconds, String... command) {
        try {
            ProcessBuilder builder = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.INHERIT);
            Process process = builder.start();
            ArrayList<String> al = new ArrayList<>();
            CompletableFuture<Void> streamReader = null;
            try (BufferedReader output = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                streamReader = CompletableFuture.runAsync(() -> {
                    while (true) {
                        try {
                            String line = output.readLine();
                            if (line == null)
                                break;
                            al.add(line);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                });
                executeAwait(timeoutSeconds, process);
                return al;
            } finally {
                if (streamReader != null && !streamReader.isDone()) {
                    streamReader.cancel(true);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static int executeAwait(int timeoutSeconds, Process process) throws TimeoutException {
        try {
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                throw new TimeoutException();
            }
            return process.exitValue();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (process.isAlive()) {
                process.destroy();
            }
        }
    }

    public static int executeAndAwait(int timeoutSeconds, List<String> args) {
        BufferedReader is = null;
        BufferedReader es = null;
        try {
            ProcessBuilder builder = new ProcessBuilder(args.toArray(new String[args.size()]));
            builder.redirectInput(ProcessBuilder.Redirect.PIPE);
            builder.redirectError(ProcessBuilder.Redirect.PIPE);
            Process process = builder.start();
            is = new BufferedReader(new InputStreamReader(process.getInputStream()));
            es = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            return executeAwait(timeoutSeconds, process);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            drainStreamTo(System.out, is);
            drainStreamTo(System.err, es);
        }
    }

    private static void drainStreamTo(PrintStream writer, BufferedReader reader) {
        if (reader != null) {
            reader.lines().forEach(writer::println);
            try {
                reader.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    public static void executeAndAwaitSuccess(int timeoutSeconds, List<String> command) {
        int status = executeAndAwait(timeoutSeconds, command);
        if (status != 0) {
            throw new RuntimeException("process failed with status: " + status);
        }
    }


    public static void purgeDirectory(Path directory) throws IOException {
        File directoryAsFile = directory.toFile();
        Files.walk(directory)
                .map(Path::toFile)
                .sorted((o1, o2) -> -o1.compareTo(o2))
                .filter(file -> !directoryAsFile.equals(file)) // nasty
                .forEach(file -> {
                    assert !file.delete();
                });
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
