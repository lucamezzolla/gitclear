package io.cutalab.gitclear.git;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GitService {

    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(120);

    public GitCommandResult status(Path repositoryPath) {
        return run(repositoryPath, List.of("status", "--short", "--branch"));
    }

    public GitCommandResult cloneRepository(String remoteUrl, Path destinationPath) {
        Path parent = destinationPath.toAbsolutePath().getParent();
        String folderName = destinationPath.toAbsolutePath().getFileName().toString();

        if (parent == null) {
            return new GitCommandResult(
                    List.of("git", "clone", remoteUrl, destinationPath.toString()),
                    1,
                    "Invalid destination folder."
            );
        }

        return run(parent, List.of("clone", remoteUrl, folderName));
    }

    public GitCommandResult isGitRepository(Path repositoryPath) {
        return run(repositoryPath, List.of("rev-parse", "--is-inside-work-tree"));
    }

    private GitCommandResult run(Path workingDirectory, List<String> gitArguments) {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(gitArguments);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        if (workingDirectory != null) {
            processBuilder.directory(workingDirectory.toFile());
        }

        try {
            Process process = processBuilder.start();

            ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
            Thread readerThread = new Thread(() -> copy(process.getInputStream(), outputBuffer), "gitclear-git-output-reader");
            readerThread.setDaemon(true);
            readerThread.start();

            boolean finished = process.waitFor(COMMAND_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new GitCommandResult(command, 124, "Command timed out after " + COMMAND_TIMEOUT.toSeconds() + " seconds.");
            }

            readerThread.join(1000);
            String output = outputBuffer.toString(StandardCharsets.UTF_8);
            return new GitCommandResult(command, process.exitValue(), output);

        } catch (IOException e) {
            return new GitCommandResult(command, 1, "Unable to run Git command: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new GitCommandResult(command, 130, "Git command interrupted.");
        }
    }

    private static void copy(InputStream inputStream, ByteArrayOutputStream outputBuffer) {
        try (inputStream; outputBuffer) {
            inputStream.transferTo(outputBuffer);
        } catch (IOException ignored) {
            // The process may be killed on timeout. In that case the stream can close abruptly.
        }
    }
}
