package dev.cs3431.yelpapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class YelpApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(YelpApp.class.getResource("yelp-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1300, 900);
            scene.getStylesheets()
                    .add(getClass().getResource("/styles/styles.css").toExternalForm());
            stage.setTitle("YelpApp!");
            stage.setScene(scene);
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
