package io.cutalab.gitclear.settings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class RecentRepositoriesService {

    private static final int MAX_RECENT_REPOSITORIES = 10;

    private final Path storageFile;

    public RecentRepositoriesService() {
        this.storageFile = Path.of(System.getProperty("user.home"), ".gitclear", "recent-repositories.txt");
    }

    public List<Path> loadRecentRepositories() {
        List<Path> storedRepositories = readStoredRepositories();

        List<Path> existingRepositories = storedRepositories.stream()
                .filter(this::isExistingDirectory)
                .toList();

        if (existingRepositories.size() != storedRepositories.size()) {
            writeRepositories(existingRepositories);
        }

        return existingRepositories;
    }

    public void addRepository(Path repositoryPath) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        values.add(normalize(repositoryPath));

        for (Path path : loadRecentRepositories()) {
            values.add(normalize(path));
        }

        List<Path> limitedValues = values.stream()
                .limit(MAX_RECENT_REPOSITORIES)
                .map(Path::of)
                .toList();

        writeRepositories(limitedValues);
    }

    public void removeRepository(Path repositoryPath) {
        String repositoryToRemove = normalize(repositoryPath);

        List<Path> remainingRepositories = readStoredRepositories().stream()
                .filter(path -> !normalize(path).equals(repositoryToRemove))
                .toList();

        writeRepositories(remainingRepositories);
    }

    private List<Path> readStoredRepositories() {
        if (!Files.exists(storageFile)) {
            return List.of();
        }

        try {
            return Files.readAllLines(storageFile, StandardCharsets.UTF_8).stream()
                    .filter(line -> !line.isBlank())
                    .map(Path::of)
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private void writeRepositories(List<Path> repositories) {
        List<String> values = new ArrayList<>();
        LinkedHashSet<String> uniqueValues = new LinkedHashSet<>();

        for (Path repository : repositories) {
            uniqueValues.add(normalize(repository));
        }

        values.addAll(uniqueValues.stream()
                .limit(MAX_RECENT_REPOSITORIES)
                .toList());

        try {
            Files.createDirectories(storageFile.getParent());
            Files.write(storageFile, values, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // Recent repositories are a convenience feature. The app can work without them.
        }
    }

    private boolean isExistingDirectory(Path path) {
        try {
            return Files.isDirectory(path.toAbsolutePath().normalize());
        } catch (RuntimeException e) {
            return false;
        }
    }

    private String normalize(Path path) {
        return path.toAbsolutePath().normalize().toString();
    }
}
