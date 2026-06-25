package io.cutalab.gitclear.ui;

import java.io.File;
import java.nio.file.Path;

public class PathFormatter {

    private static final int MAX_COMPACT_PATH_LENGTH = 64;

    private final Path homePath;

    public PathFormatter() {
        this.homePath = Path.of(System.getProperty("user.home")).toAbsolutePath().normalize();
    }

    public String repositoryName(Path repositoryPath) {
        if (repositoryPath == null) {
            return "No repository selected";
        }

        Path fileName = repositoryPath.toAbsolutePath().normalize().getFileName();
        return fileName == null ? repositoryPath.toString() : fileName.toString();
    }

    public String compactParent(Path repositoryPath) {
        if (repositoryPath == null) {
            return "";
        }

        Path parent = repositoryPath.toAbsolutePath().normalize().getParent();
        return parent == null ? compactPath(repositoryPath) : compactPath(parent);
    }

    public String compactPath(Path path) {
        if (path == null) {
            return "";
        }

        Path normalizedPath = path.toAbsolutePath().normalize();
        String displayPath = abbreviateHome(normalizedPath);
        return shortenIfNeeded(displayPath);
    }

    private String abbreviateHome(Path normalizedPath) {
        try {
            if (normalizedPath.startsWith(homePath)) {
                Path relativePath = homePath.relativize(normalizedPath);
                if (relativePath.toString().isBlank()) {
                    return "~";
                }

                return "~" + File.separator + relativePath;
            }
        } catch (IllegalArgumentException ignored) {
            // Different roots can happen on Windows, for example C:\\ and D:\\.
        }

        return normalizedPath.toString();
    }

    private String shortenIfNeeded(String value) {
        if (value.length() <= MAX_COMPACT_PATH_LENGTH) {
            return value;
        }

        String separator = File.separator;
        String normalizedSeparators = value
                .replace("\\", separator)
                .replace("/", separator);

        String[] parts = normalizedSeparators.split(java.util.regex.Pattern.quote(separator));
        if (parts.length >= 4) {
            String firstPart = parts[0].isBlank() ? separator : parts[0];
            String previousPart = parts[parts.length - 2];
            String lastPart = parts[parts.length - 1];

            return firstPart + separator + "…" + separator + previousPart + separator + lastPart;
        }

        int startLength = Math.min(24, value.length() / 2);
        int endLength = Math.min(32, value.length() - startLength);
        return value.substring(0, startLength) + "…" + value.substring(value.length() - endLength);
    }
}
