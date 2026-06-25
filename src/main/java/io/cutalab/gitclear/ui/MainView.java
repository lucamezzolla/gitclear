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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class MainView {

    private final Stage stage;
    private final GitService gitService;
    private final RecentRepositoriesService recentRepositoriesService;

    private final BorderPane root = new BorderPane();
    private final Label repositoryLabel = new Label("No repository selected");
    private final Label branchLabel = new Label("—");
    private final Label commandLabel = new Label("No command yet");
    private final Label statusLabel = new Label("Ready");
    private final ListView<String> changesList = new ListView<>();
    private final ListView<Path> recentRepositoriesList = new ListView<>();
    private final TextArea commandOutput = new TextArea();

    private Path currentRepository;

    public MainView(Stage stage, GitService gitService, RecentRepositoriesService recentRepositoriesService) {
        this.stage = stage;
        this.gitService = gitService;
        this.recentRepositoriesService = recentRepositoriesService;
        buildLayout();
        refreshRecentRepositories();
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

        Button cloneButton = new Button("Clone Repository");
        cloneButton.getStyleClass().add("primary-button");
        cloneButton.setOnAction(event -> showCloneDialog());

        Button openButton = new Button("Open Local Repository");
        openButton.setOnAction(event -> openLocalRepository());

        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(event -> refreshStatus());
        refreshButton.disableProperty().bind(repositoryLabel.textProperty().isEqualTo("No repository selected"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actions = new HBox(10, cloneButton, openButton, refreshButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        HBox topLine = new HBox(12, title, spacer, actions);
        topLine.setAlignment(Pos.CENTER_LEFT);

        VBox header = new VBox(4, topLine, subtitle);
        header.getStyleClass().add("header");
        return header;
    }

    private Parent buildContent() {
        Label repoCaption = new Label("Current repository");
        repoCaption.getStyleClass().add("section-caption");

        repositoryLabel.getStyleClass().add("repo-path");

        Label branchCaption = new Label("Branch");
        branchCaption.getStyleClass().add("section-caption");

        branchLabel.getStyleClass().add("branch-label");

        HBox repositoryInfo = new HBox(24, buildInfoBlock(repoCaption, repositoryLabel), buildInfoBlock(branchCaption, branchLabel));
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

        recentRepositoriesList.setPlaceholder(new Label("No recent repositories yet."));
        recentRepositoriesList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Path item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
            }
        });
        recentRepositoriesList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Path selected = recentRepositoriesList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openRepository(selected);
                }
            }
        });

        VBox recentCard = new VBox(12, recentCaption, recentRepositoriesList);
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
        HBox footer = new HBox(statusLabel);
        footer.getStyleClass().add("footer");
        return footer;
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
        if (request.remoteUrl().isBlank() || request.destinationFolder().isBlank()) {
            showWarning("Remote URL and destination folder are required.");
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

    private void openLocalRepository() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Open Git repository");

        Path home = Path.of(System.getProperty("user.home"));
        if (Files.isDirectory(home)) {
            chooser.setInitialDirectory(home.toFile());
        }

        var selected = chooser.showDialog(stage);
        if (selected != null) {
            openRepository(selected.toPath());
        }
    }

    private void openRepository(Path repositoryPath) {
        Path normalizedPath = repositoryPath.toAbsolutePath().normalize();

        runTask("Opening repository...", () -> gitService.isGitRepository(normalizedPath), validation -> {
            updateCommandResult(validation);

            if (!validation.success() || !validation.output().trim().equals("true")) {
                showWarning("This folder does not look like a Git repository.");
                return;
            }

            currentRepository = normalizedPath;
            repositoryLabel.setText(currentRepository.toString());
            recentRepositoriesService.addRepository(currentRepository);
            refreshRecentRepositories();
            refreshStatus();
        });
    }

    private void refreshStatus() {
        if (currentRepository == null) {
            return;
        }

        runTask("Refreshing status...", () -> gitService.status(currentRepository), result -> {
            updateCommandResult(result);

            if (!result.success()) {
                showError("Status failed", result.output());
                return;
            }

            applyStatusOutput(result.output());
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
