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
import javafx.util.Duration;

public class Main extends Application
{

    private static MediaPlayer mediaPlayer = null;
    private static long unixTimeWhenFirstPlayed, unixTimeWhenPaused;

    public static MediaPlayer getMediaPlayer(String fileName)
    {
        System.out.println(System.getProperty("user.dir"));
        Media media = new Media("file:///" + System.getProperty("user.dir").replace('\\', '/') + "/" + fileName);
        return new MediaPlayer(media);
    }

    public static void playPause()
    {
        if (mediaPlayer == null) // playing for first time. should be played
        {
            unixTimeWhenFirstPlayed = System.currentTimeMillis();
            mediaPlayer = getMediaPlayer("foobar.wav");
            mediaPlayer.play();
        }
        else if (mediaPlayer.getStatus() == MediaPlayer.Status.PAUSED) // has been played before, but is paued. should be played at time
        {
            Duration skipTo = new Duration(unixTimeWhenPaused - unixTimeWhenFirstPlayed);
            mediaPlayer.seek(skipTo);
            mediaPlayer.play();
        }
        else if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) // is playing. should be paused.
        {
            unixTimeWhenPaused = System.currentTimeMillis(); // saving where it was up to
            mediaPlayer.pause();
        }
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

        // defining play button -- will play a given wav file
        Button playBtn = new Button("Play!");
        playBtn.setOnAction((ActionEvent ae) -> playPause());
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
