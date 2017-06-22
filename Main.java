package sample;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.io.File;


public class Main extends Application
{

    public static MediaPlayer currentlyPlaying;

    public static MediaPlayer getMediaPlayer(String fileName)
    {
        System.out.println(System.getProperty("user.dir"));
        Media media = new Media("file:///" + System.getProperty("user.dir").replace('\\', '/') + "/" + fileName);
        return new MediaPlayer(media);
    }

    @Override
    public void start(Stage primaryStage)
    {
        // setting up stage
        Stage stage = new Stage();
        stage.setTitle("Music Player");
        stage.setResizable(true);

        // setting up GridPane controller
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setVgap(5);
        grid.setHgap(5);

        // defining play button -- will play a preset MP3 file
        Button playBtn = new Button("Play!");
        playBtn.setOnAction((ActionEvent ae) -> {
            currentlyPlaying = getMediaPlayer("foobar.wav"); // using wav file because mp3s are not supported on ubuntu 16
            currentlyPlaying.play();
        });
        GridPane.setConstraints(playBtn, 0, 0);
        grid.getChildren().add(playBtn);

        // finally making stage visible
        stage.setScene(new Scene(grid, 300, 300));
        stage.setWidth(300);
        stage.setHeight(300);
        stage.show();
    }

    public static void main(String[] args) throws InterruptedException
    {
        launch(args);
    }
}
