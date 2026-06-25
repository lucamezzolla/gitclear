package io.cutalab.gitclear;

import io.cutalab.gitclear.git.GitService;
import io.cutalab.gitclear.settings.RecentRepositoriesService;
import io.cutalab.gitclear.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GitClearApplication extends Application {

    @Override
    public void start(Stage stage) {
        GitService gitService = new GitService();
        RecentRepositoriesService recentRepositoriesService = new RecentRepositoriesService();

        MainView mainView = new MainView(stage, gitService, recentRepositoriesService);
        Scene scene = new Scene(mainView.getRoot(), 980, 680);

        var stylesheet = GitClearApplication.class.getResource("/gitclear.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }

        stage.setTitle("GitClear");
        stage.setMinWidth(820);
        stage.setMinHeight(560);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
