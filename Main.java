package sample;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import javafx.scene.control.Slider;


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
        else if (mediaPlayer.getStatus() == MediaPlayer.Status.PAUSED) // has been played before, but is paused. should be played at time
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

    public static void stopPlayer()
    {
        mediaPlayer.pause();
        mediaPlayer = null;
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

        // defining play/pause button -- will play a given wav file
        final Button playPauseBtn = new Button("Play/Pause!");
        playPauseBtn.setOnAction((ActionEvent ae) -> playPause());
        GridPane.setConstraints(playPauseBtn, 0, 0);
        grid.getChildren().add(playPauseBtn);

        // defining stop button
        final Button stopBtn = new Button("Stop!");
        stopBtn.setOnAction((ActionEvent ae) -> stopPlayer());
        GridPane.setConstraints(stopBtn, 1, 0);
        grid.getChildren().add(stopBtn);

        // defining slider
        final Slider slider = new Slider(0, 100, 0);
        slider.setBlockIncrement(1);
        slider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
            {
                double percentSlid = newValue.doubleValue() / 100; // percent slid on slider
                Duration seekTo = mediaPlayer.getTotalDuration().multiply(percentSlid); // amount that should be seeked to -- namely, corresponding percentage of track
                mediaPlayer.seek(seekTo);
            }
        });
        GridPane.setConstraints(slider, 0, 1, 2, 1);
        grid.getChildren().add(slider);

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
