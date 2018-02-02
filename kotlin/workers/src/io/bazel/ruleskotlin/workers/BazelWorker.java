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
package io.bazel.ruleskotlin.workers;


import com.google.devtools.build.lib.worker.WorkerProtocol;
import io.bazel.ruleskotlin.workers.utils.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Bazel worker runner.
 * <p>
 * <p>This class adapts a traditional command line program so it can be spawned by Bazel as a
 * persistent worker process that handles multiple invocations per JVM. It will also be backwards
 * compatible with being run as a normal single-invocation command.
 *
 * @param <T> delegate program type
 */
public final class BazelWorker<T extends CommandLineProgram> implements CommandLineProgram {
    private final CommandLineProgram delegate;
    private final String mnemonic;
    private final PrintStream output;

    public BazelWorker(T delegate, PrintStream output, String mnemonic) {
        this.delegate = delegate;
        this.output = output;
        this.mnemonic = mnemonic;
    }

    @Override
    public Integer apply(List<String> args) {
        for (String arg : args) {
            if (arg.equals("--persistent_worker")) {
                return runAsPersistentWorker(args);
            }
        }
        return delegate.apply(loadArguments(args, false));
    }

    @SuppressWarnings("unused")
    private int runAsPersistentWorker(List<String> ignored) {
        InputStream realStdIn = System.in;
        PrintStream realStdOut = System.out;
        PrintStream realStdErr = System.err;
        try (InputStream emptyIn = new ByteArrayInputStream(new byte[0]);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream();
             PrintStream ps = new PrintStream(buffer)) {
            System.setIn(emptyIn);
            System.setOut(ps);
            System.setErr(ps);
            while (true) {
                WorkerProtocol.WorkRequest request = WorkerProtocol.WorkRequest.parseDelimitedFrom(realStdIn);
                if (request == null) {
                    return 0;
                }
                int exitCode;

                try {
                    exitCode = delegate.apply(loadArguments(request.getArgumentsList(), true));
                } catch (RuntimeException e) {
                    if (wasInterrupted(e)) {
                        return 0;
                    }
                    System.err.println(
                            "ERROR: Worker threw uncaught exception with args: " +
                                    request.getArgumentsList().stream().collect(Collectors.joining(" ")));
                    e.printStackTrace(System.err);
                    exitCode = 1;
                }
                WorkerProtocol.WorkResponse.newBuilder()
                        .setOutput(buffer.toString())
                        .setExitCode(exitCode)
                        .build()
                        .writeDelimitedTo(realStdOut);
                realStdOut.flush();
                buffer.reset();
                System.gc();  // be a good little worker process and consume less memory when idle
            }
        } catch (IOException | RuntimeException e) {
            if (wasInterrupted(e)) {
                return 0;
            }
            throw new RuntimeException(e);
        } finally {
            System.setIn(realStdIn);
            System.setOut(realStdOut);
            System.setErr(realStdErr);
        }
    }

    private List<String> loadArguments(List<String> args, boolean isWorker) {
        if (args.size() > 0) {
            String lastArg = args.get(args.size() - 1);

            if (lastArg.startsWith("@")) {
                String pathElement = lastArg.substring(1);
                Path flagFile = Paths.get(pathElement);
                if ((isWorker && lastArg.startsWith("@@")) || Files.exists(flagFile)) {
                    if (!isWorker && !mnemonic.isEmpty()) {
                        output.printf(
                                "HINT: %s will compile faster if you run: "
                                        + "echo \"build --strategy=%s=worker\" >>~/.bazelrc\n",
                                mnemonic, mnemonic);
                    }
                    try {
                        return Files.readAllLines(flagFile, UTF_8);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return args;
    }

    private boolean wasInterrupted(Throwable e) {
        Throwable cause = IOUtils.getRootCause(e);
        if (cause instanceof InterruptedException
                || cause instanceof InterruptedIOException) {
            output.println("Terminating worker due to interrupt signal");
            return true;
        }
        return false;
    }
}
