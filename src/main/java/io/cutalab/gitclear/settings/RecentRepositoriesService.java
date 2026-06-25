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

    public void addRepository(Path repositoryPath) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        values.add(repositoryPath.toAbsolutePath().normalize().toString());

        for (Path path : loadRecentRepositories()) {
            values.add(path.toAbsolutePath().normalize().toString());
        }

        List<String> limitedValues = new ArrayList<>(values).stream()
                .limit(MAX_RECENT_REPOSITORIES)
                .toList();

        try {
            Files.createDirectories(storageFile.getParent());
            Files.write(storageFile, limitedValues, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // Recent repositories are a convenience feature. The app can work without them.
        }
    }
}
