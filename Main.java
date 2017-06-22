package sample;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.io.File;


public class Main
{

    public static MediaPlayer getMediaPlayer(String location)
    {
        Media media = new Media(new File(location).toURI().toString());
        return new MediaPlayer(media);
    }

    public static void initialiseGUI()
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
        playBtn.setOnAction((ActionEvent ae) -> getMediaPlayer("/Users/seraph/Downloads/TestMP3.mp3").play());
        GridPane.setConstraints(playBtn, 0, 0);
        grid.getChildren().add(playBtn);

        // finally making stage visible
        stage.setScene(new Scene(grid, 300, 300));
        stage.setWidth(300);
        stage.setHeight(300);
        stage.show();
    }

    public static void launchFX()
    {
        new JFXPanel();
        Platform.runLater(() -> initialiseGUI());
    }

    public static void main(String[] args)
    {
        launchFX();
    }
}
