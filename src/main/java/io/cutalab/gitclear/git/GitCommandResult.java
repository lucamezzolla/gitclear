package io.cutalab.gitclear.git;

import java.util.List;
import java.util.stream.Collectors;

public record GitCommandResult(List<String> command, int exitCode, String output) {

    public boolean success() {
        return exitCode == 0;
    }

    public String commandLine() {
        return command.stream()
                .map(GitCommandResult::quoteIfNeeded)
                .collect(Collectors.joining(" "));
    }

    private static String quoteIfNeeded(String value) {
        if (value == null || value.isBlank()) {
            return "''";
        }

        boolean needsQuote = value.chars().anyMatch(Character::isWhitespace);
        if (!needsQuote) {
            return value;
        }

        return "'" + value.replace("'", "'\"'\"'") + "'";
    }
}
