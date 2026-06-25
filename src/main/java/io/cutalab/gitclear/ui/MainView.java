package io.cutalab.gitclear.ui;

import io.cutalab.gitclear.git.GitCommandResult;
import io.cutalab.gitclear.git.GitService;
import io.cutalab.gitclear.settings.RecentRepositoriesService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

public class MainView {

    private static final String OPEN_REPOSITORY_LABEL = "Open Local Repository";
    private static final String SWITCH_REPOSITORY_LABEL = "Switch Repository";

    private final Stage stage;
    private final GitService gitService;
    private final RecentRepositoriesService recentRepositoriesService;
    private final PathFormatter pathFormatter = new PathFormatter();

    private final BorderPane root = new BorderPane();
    private final Label repositoryNameLabel = new Label("No repository selected");
    private final Label repositoryLocationLabel = new Label("");
    private final Label branchLabel = new Label("—");
    private final Label commandLabel = new Label("No command yet");
    private final Label statusLabel = new Label("Ready");
    private final Label gitVersionLabel = new Label("Git: checking...");
    private final ListView<String> changesList = new ListView<>();
    private final ListView<Path> recentRepositoriesList = new ListView<>();
    private final TextArea commandOutput = new TextArea();

    private final Button cloneButton = new Button("Clone Repository");
    private final Button openButton = new Button(OPEN_REPOSITORY_LABEL);
    private final Button refreshButton = new Button("Refresh");

    private final Button minimizeWindowButton = new Button("—");
    private final Button maximizeWindowButton = new Button("□");
    private final Button closeWindowButton = new Button("×");

    private Path currentRepository;
    private boolean gitAvailable;

    public MainView(Stage stage, GitService gitService, RecentRepositoriesService recentRepositoriesService) {
        this.stage = stage;
        this.gitService = gitService;
        this.recentRepositoriesService = recentRepositoriesService;
        buildLayout();
        refreshRecentRepositories();
        updateActionState();
        checkGitAvailability();
    }

    public Parent getRoot() {
        return root;
    }

    private void buildLayout() {
        root.getStyleClass().add("app-root");
        root.setTop(buildHeader());
        root.setCenter(buildContent());
        root.setBottom(buildFooter());
    }

    private Parent buildHeader() {
        Label title = new Label("GitClear");
        title.getStyleClass().add("app-title");

        Label subtitle = new Label("A calm Git client for real teams.");
        subtitle.getStyleClass().add("app-subtitle");

        cloneButton.getStyleClass().add("primary-button");
        cloneButton.setOnAction(event -> showCloneDialog());

        openButton.setOnAction(event -> openLocalRepository());

        refreshButton.setOnAction(event -> refreshStatus());

        configureWindowControls();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox repositoryActions = new HBox(10, cloneButton, openButton, refreshButton);
        repositoryActions.setAlignment(Pos.CENTER_RIGHT);

        Region windowControlsSeparator = new Region();
        windowControlsSeparator.getStyleClass().add("header-separator");
        windowControlsSeparator.setMinWidth(1);
        windowControlsSeparator.setPrefWidth(1);
        windowControlsSeparator.setMaxWidth(1);
        windowControlsSeparator.setPrefHeight(34);

        HBox windowControls = new HBox(6, minimizeWindowButton, maximizeWindowButton, closeWindowButton);
        windowControls.setAlignment(Pos.CENTER_RIGHT);

        HBox topLine = new HBox(12, title, spacer, repositoryActions, windowControlsSeparator, windowControls);
        topLine.setAlignment(Pos.CENTER_LEFT);

        VBox header = new VBox(4, topLine, subtitle);
        header.getStyleClass().add("header");
        return header;
    }

    private void configureWindowControls() {
        minimizeWindowButton.getStyleClass().add("window-control-button");
        maximizeWindowButton.getStyleClass().add("window-control-button");
        closeWindowButton.getStyleClass().addAll("window-control-button", "close-window-button");

        minimizeWindowButton.setTooltip(new Tooltip("Minimize window"));
        closeWindowButton.setTooltip(new Tooltip("Close GitClear"));

        minimizeWindowButton.setOnAction(event -> stage.setIconified(true));
        maximizeWindowButton.setOnAction(event -> toggleMaximizedWindow());
        closeWindowButton.setOnAction(event -> stage.close());

        stage.maximizedProperty().addListener((observable, oldValue, newValue) -> updateMaximizeWindowButton());
        updateMaximizeWindowButton();
    }

    private void toggleMaximizedWindow() {
        stage.setMaximized(!stage.isMaximized());
        updateMaximizeWindowButton();
    }

    private void updateMaximizeWindowButton() {
        boolean maximized = stage.isMaximized();

        maximizeWindowButton.setText(maximized ? "❐" : "□");

        if (maximizeWindowButton.getTooltip() == null) {
            maximizeWindowButton.setTooltip(new Tooltip());
        }

        maximizeWindowButton.getTooltip().setText(maximized ? "Restore window" : "Maximize window");
    }

    private Parent buildContent() {
        Label repoCaption = new Label("Current repository");
        repoCaption.getStyleClass().add("section-caption");

        repositoryNameLabel.getStyleClass().add("repo-name");
        repositoryLocationLabel.getStyleClass().add("repo-location");

        VBox repositoryBlock = new VBox(4, repoCaption, repositoryNameLabel, repositoryLocationLabel);
        repositoryBlock.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(repositoryBlock, Priority.ALWAYS);

        Label branchCaption = new Label("Branch");
        branchCaption.getStyleClass().add("section-caption");

        branchLabel.getStyleClass().add("branch-label");

        HBox repositoryInfo = new HBox(24, repositoryBlock, buildInfoBlock(branchCaption, branchLabel));
        repositoryInfo.setAlignment(Pos.CENTER_LEFT);

        Label changesCaption = new Label("Changes");
        changesCaption.getStyleClass().add("section-title");

        changesList.setPlaceholder(new Label("Open or clone a repository to see changes."));
        changesList.setFocusTraversable(false);
        VBox.setVgrow(changesList, Priority.ALWAYS);

        Label commandCaption = new Label("Last command");
        commandCaption.getStyleClass().add("section-title");

        commandLabel.getStyleClass().add("command-label");

        commandOutput.setEditable(false);
        commandOutput.setWrapText(true);
        commandOutput.setPrefRowCount(5);
        commandOutput.setPromptText("Git command output will appear here.");

        VBox statusCard = new VBox(12, repositoryInfo, changesCaption, changesList, commandCaption, commandLabel, commandOutput);
        statusCard.getStyleClass().add("card");
        VBox.setVgrow(statusCard, Priority.ALWAYS);

        Label recentCaption = new Label("Recent");
        recentCaption.getStyleClass().add("section-title");

        Label recentHint = new Label("Double-click a repository to switch.");
        recentHint.getStyleClass().add("hint-label");

        recentRepositoriesList.setPlaceholder(new Label("No recent repositories yet."));
        recentRepositoriesList.setCellFactory(list -> new RecentRepositoryCell());
        recentRepositoriesList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Path selected = recentRepositoriesList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openRecentRepository(selected);
                }
            }
        });

        VBox recentCard = new VBox(8, recentCaption, recentHint, recentRepositoriesList);
        recentCard.getStyleClass().add("card");
        recentCard.setPrefWidth(320);

        HBox content = new HBox(16, statusCard, recentCard);
        content.getStyleClass().add("content");
        HBox.setHgrow(statusCard, Priority.ALWAYS);
        return content;
    }

    private Parent buildInfoBlock(Label caption, Label value) {
        VBox box = new VBox(4, caption, value);
        box.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private Parent buildFooter() {
        statusLabel.getStyleClass().add("footer-label");
        gitVersionLabel.getStyleClass().add("footer-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox footer = new HBox(12, statusLabel, spacer, gitVersionLabel);
        footer.getStyleClass().add("footer");
        footer.setAlignment(Pos.CENTER_LEFT);
        return footer;
    }

    private void checkGitAvailability() {
        runTask("Checking Git...", gitService::version, result -> {
            updateCommandResult(result);

            gitAvailable = result.success();
            if (gitAvailable) {
                gitVersionLabel.setText("Git: " + result.output().trim());
                statusLabel.setText("Ready");
            } else {
                gitVersionLabel.setText("Git: not available");
                statusLabel.setText("Git is not available");
                changesList.getItems().setAll("GitClear cannot run Git commands until Git is installed and available in PATH.");
                showWarning("GitClear could not find Git. Please install Git or make sure it is available in PATH.");
            }

            updateActionState();
        });
    }

    private boolean hasRepositoryOpen() {
        return currentRepository != null;
    }

    private void updateActionState() {
        openButton.setText(hasRepositoryOpen() ? SWITCH_REPOSITORY_LABEL : OPEN_REPOSITORY_LABEL);
        cloneButton.setDisable(!gitAvailable);
        openButton.setDisable(!gitAvailable);
        refreshButton.setDisable(!gitAvailable || !hasRepositoryOpen());
        recentRepositoriesList.setDisable(!gitAvailable);
    }

    private void showCloneDialog() {
        Dialog<CloneRequest> dialog = new Dialog<>();
        dialog.setTitle("Clone Repository");
        dialog.setHeaderText("Clone a remote Git repository");

        ButtonType cloneButtonType = new ButtonType("Clone", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(cloneButtonType, ButtonType.CANCEL);

        TextField remoteUrlField = new TextField();
        remoteUrlField.setPromptText("https://github.com/company/project.git or git@server:team/project.git");

        TextField destinationField = new TextField();
        destinationField.setPromptText(System.getProperty("user.home") + "/projects/project");

        Button chooseDestinationButton = new Button("Choose Folder");
        chooseDestinationButton.setOnAction(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Choose destination parent folder");
            Path initial = Path.of(System.getProperty("user.home"));
            if (Files.isDirectory(initial)) {
                chooser.setInitialDirectory(initial.toFile());
            }

            var selected = chooser.showDialog(stage);
            if (selected != null) {
                String repositoryName = guessRepositoryFolderName(remoteUrlField.getText());
                destinationField.setText(selected.toPath().resolve(repositoryName).toString());
            }
        });

        HBox destinationRow = new HBox(8, destinationField, chooseDestinationButton);
        HBox.setHgrow(destinationField, Priority.ALWAYS);

        VBox content = new VBox(
                10,
                new Label("Remote URL"),
                remoteUrlField,
                new Label("Destination folder"),
                destinationRow
        );
        content.setPadding(new Insets(12));
        content.setPrefWidth(640);

        dialog.getDialogPane().setContent(content);
        dialog.setResultConverter(button -> {
            if (button == cloneButtonType) {
                return new CloneRequest(remoteUrlField.getText().trim(), destinationField.getText().trim());
            }
            return null;
        });

        Optional<CloneRequest> result = dialog.showAndWait();
        result.ifPresent(this::cloneRepository);
    }

    private void cloneRepository(CloneRequest request) {
        Optional<String> validationError = validateCloneRequest(request);
        if (validationError.isPresent()) {
            showWarning(validationError.get());
            return;
        }

        Path destinationPath = Path.of(request.destinationFolder()).toAbsolutePath().normalize();

        runTask("Cloning repository...", () -> gitService.cloneRepository(request.remoteUrl(), destinationPath), result -> {
            updateCommandResult(result);

            if (result.success()) {
                openRepository(destinationPath);
            } else {
                showError("Clone failed", result.output());
            }
        });
    }

    private Optional<String> validateCloneRequest(CloneRequest request) {
        if (request.remoteUrl().isBlank()) {
            return Optional.of("Remote URL is required.");
        }

        if (request.destinationFolder().isBlank()) {
            return Optional.of("Destination folder is required.");
        }

        Path destinationPath;
        try {
            destinationPath = Path.of(request.destinationFolder()).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            return Optional.of("Destination folder path is not valid.");
        }

        Path parent = destinationPath.getParent();
        if (parent == null) {
            return Optional.of("Destination folder must have a parent folder.");
        }

        if (!Files.exists(parent)) {
            return Optional.of("Destination parent folder does not exist.");
        }

        if (!Files.isDirectory(parent)) {
            return Optional.of("Destination parent path is not a folder.");
        }

        if (Files.exists(destinationPath) && !Files.isDirectory(destinationPath)) {
            return Optional.of("Destination path already exists and is not a folder.");
        }

        if (Files.isDirectory(destinationPath) && !isDirectoryEmpty(destinationPath)) {
            return Optional.of("Destination folder already exists and is not empty. Choose an empty folder or a new folder name.");
        }

        return Optional.empty();
    }

    private boolean isDirectoryEmpty(Path directory) {
        try (Stream<Path> entries = Files.list(directory)) {
            return entries.findAny().isEmpty();
        } catch (IOException e) {
            return false;
        }
    }

    private void openLocalRepository() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(hasRepositoryOpen() ? SWITCH_REPOSITORY_LABEL : OPEN_REPOSITORY_LABEL);

        Path initial = hasRepositoryOpen() ? currentRepository.getParent() : Path.of(System.getProperty("user.home"));
        if (initial != null && Files.isDirectory(initial)) {
            chooser.setInitialDirectory(initial.toFile());
        }

        var selected = chooser.showDialog(stage);
        if (selected != null) {
            openRepository(selected.toPath());
        }
    }

    private void openRecentRepository(Path repositoryPath) {
        Path normalizedPath = repositoryPath.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalizedPath)) {
            showMissingRecentRepositoryDialog(normalizedPath);
            return;
        }

        openRepository(normalizedPath);
    }

    private void openRepository(Path repositoryPath) {
        Path normalizedPath = repositoryPath.toAbsolutePath().normalize();

        runTask("Opening repository...", () -> gitService.isGitRepository(normalizedPath), validation -> {
            updateCommandResult(validation);

            if (!validation.success() || !validation.output().trim().equals("true")) {
                showWarning("This folder does not look like a Git repository.");
                return;
            }

            setCurrentRepository(normalizedPath);
            recentRepositoriesService.addRepository(currentRepository);
            refreshRecentRepositories();
            refreshStatus();
        });
    }

    private void setCurrentRepository(Path repositoryPath) {
        currentRepository = repositoryPath;
        repositoryNameLabel.setText(pathFormatter.repositoryName(currentRepository));
        repositoryLocationLabel.setText(pathFormatter.compactPath(currentRepository));
        repositoryLocationLabel.setTooltip(new Tooltip(currentRepository.toString()));
        updateActionState();
    }

    private void refreshStatus() {
        if (!hasRepositoryOpen()) {
            return;
        }

        runTask("Refreshing status...", () -> gitService.status(currentRepository), result -> {
            updateCommandResult(result);

            if (!result.success()) {
                showError("Status failed", result.output());
                return;
            }

            applyStatusOutput(result.output());
            updateActionState();
        });
    }

    private void applyStatusOutput(String output) {
        changesList.getItems().clear();

        String[] lines = output.split("\\R");
        boolean branchFound = false;

        for (String line : lines) {
            if (line.startsWith("## ")) {
                branchLabel.setText(parseBranchLine(line));
                branchFound = true;
            } else if (!line.isBlank()) {
                changesList.getItems().add(line);
            }
        }

        if (!branchFound) {
            branchLabel.setText("unknown");
        }

        if (changesList.getItems().isEmpty()) {
            changesList.getItems().add("Working tree clean");
        }

        statusLabel.setText("Ready");
    }

    private String parseBranchLine(String line) {
        String branchPart = line.substring(3).trim();
        int upstreamSeparator = branchPart.indexOf("...");
        if (upstreamSeparator >= 0) {
            return branchPart.substring(0, upstreamSeparator).trim();
        }
        return branchPart;
    }

    private void updateCommandResult(GitCommandResult result) {
        commandLabel.setText(result.commandLine());
        commandOutput.setText(result.output() == null || result.output().isBlank() ? "(no output)" : result.output());
    }

    private void refreshRecentRepositories() {
        recentRepositoriesList.getItems().setAll(recentRepositoriesService.loadRecentRepositories());
    }

    private String guessRepositoryFolderName(String remoteUrl) {
        if (remoteUrl == null || remoteUrl.isBlank()) {
            return "repository";
        }

        String clean = remoteUrl.trim();

        int slashIndex = clean.lastIndexOf('/');
        int colonIndex = clean.lastIndexOf(':');
        int index = Math.max(slashIndex, colonIndex);

        String name = index >= 0 ? clean.substring(index + 1) : clean;
        if (name.endsWith(".git")) {
            name = name.substring(0, name.length() - 4);
        }

        return name.isBlank() ? "repository" : name;
    }

    private void showMissingRecentRepositoryDialog(Path repositoryPath) {
        Platform.runLater(() -> {
            ButtonType removeButton = new ButtonType("Remove from Recent", ButtonBar.ButtonData.OK_DONE);
            ButtonType keepButton = new ButtonType("Keep", ButtonBar.ButtonData.CANCEL_CLOSE);

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("GitClear");
            alert.setHeaderText("Repository not found");
            alert.setContentText("""
                    This recent repository no longer exists:

                    %s

                    Do you want to remove it from Recent?
                    """.formatted(repositoryPath));
            alert.getButtonTypes().setAll(removeButton, keepButton);

            alert.showAndWait()
                    .filter(button -> button == removeButton)
                    .ifPresent(button -> {
                        recentRepositoriesService.removeRepository(repositoryPath);
                        refreshRecentRepositories();
                        statusLabel.setText("Recent repository removed");
                    });
        });
    }

    private <T> void runTask(String message, TaskSupplier<T> supplier, TaskSuccessHandler<T> successHandler) {
        statusLabel.setText(message);
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(16, 16);
        statusLabel.setGraphic(progressIndicator);

        Task<T> task = new Task<>() {
            @Override
            protected T call() {
                return supplier.get();
            }
        };

        task.setOnSucceeded(event -> {
            statusLabel.setGraphic(null);
            successHandler.handle(task.getValue());
        });

        task.setOnFailed(event -> {
            statusLabel.setGraphic(null);
            statusLabel.setText("Error");
            Throwable exception = task.getException();
            showError("Unexpected error", exception == null ? "Unknown error" : exception.getMessage());
        });

        Thread thread = new Thread(task, "gitclear-background-task");
        thread.setDaemon(true);
        thread.start();
    }

    private void showWarning(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("GitClear");
            alert.setHeaderText("Attention");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("GitClear");
            alert.setHeaderText(title);
            alert.setContentText(message == null || message.isBlank() ? "No details available." : message);
            alert.showAndWait();
        });
    }

    private class RecentRepositoryCell extends ListCell<Path> {

        @Override
        protected void updateItem(Path item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                setTooltip(null);
                return;
            }

            Label nameLabel = new Label(pathFormatter.repositoryName(item));
            nameLabel.getStyleClass().add("recent-repo-name");

            Label pathLabel = new Label(pathFormatter.compactParent(item));
            pathLabel.getStyleClass().add("recent-repo-path");
            pathLabel.setTooltip(new Tooltip(item.toString()));

            VBox content = new VBox(2, nameLabel, pathLabel);
            content.getStyleClass().add("recent-repo-item");

            setText(null);
            setGraphic(content);
            setTooltip(new Tooltip(item.toString()));
        }
    }

    @FunctionalInterface
    private interface TaskSupplier<T> {
        T get();
    }

    @FunctionalInterface
    private interface TaskSuccessHandler<T> {
        void handle(T value);
    }

    private record CloneRequest(String remoteUrl, String destinationFolder) {
    }
}
