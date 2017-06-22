package sample;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import javafx.scene.control.Slider;
import sun.audio.AudioPlayer;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URL;
import javax.swing.*;
import javax.sound.sampled.*;

public class Main extends Application
{

    private static MediaPlayer mediaPlayer = null;
    private static long unixTimeWhenFirstPlayed, unixTimeWhenPaused;
    private static final String absPrefix = System.getProperty("user.dir").replace('\\', '/') + "/MusicFiles";
    private static ListView<String> trackNames = new ListView<>();

    public static MediaPlayer getMediaPlayer(String fileName)
    {
        Media media = new Media("file://" + absPrefix + "/" + fileName);
        return new MediaPlayer(media);
    }

    public static void playPause() throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        if (mediaPlayer == null) // playing for first time. should be played
        {
            unixTimeWhenFirstPlayed = System.currentTimeMillis();
            URL url = new URL("http://musicmanager.duckdns.org/CodingDude.wav");
            Clip clip = AudioSystem.getClip();
            AudioInputStream ais = AudioSystem.getAudioInputStream(url);
            clip.open(ais);
            clip.start();
            /* mediaPlayer = getMediaPlayer("CodingDude.wav");
            mediaPlayer.play(); */
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
    public void start(Stage primaryStage) throws Exception
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
        playPauseBtn.setOnAction((ActionEvent ae) -> {
            try {
                playPause();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (UnsupportedAudioFileException e) {
                e.printStackTrace();
            } catch (LineUnavailableException e) {
                e.printStackTrace();
            }
        });
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
        slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            double percentSlid = newValue.doubleValue() / 100; // percent slid on slider
            Duration seekTo = mediaPlayer.getTotalDuration().multiply(percentSlid); // amount that should be seeked to -- namely, corresponding percentage of track
            mediaPlayer.seek(seekTo);
        });
        GridPane.setConstraints(slider, 0, 1, 2, 1);
        grid.getChildren().add(slider);

        // defining listview of wav files that can be played
        File[] files = new File(absPrefix).listFiles();
        for (File file : files)
        {
            if (file.isFile() && ( file.getName().contains(".mp3") || file.getName().contains(".wav") ))
            {
                trackNames.getItems().add(file.getName());
            }
        }
        trackNames.setOnMouseClicked((MouseEvent event) -> {
            if (mediaPlayer != null) { stopPlayer(); }
            mediaPlayer = getMediaPlayer(trackNames.getSelectionModel().getSelectedItem());
            mediaPlayer.play();
            slider.setValue(0);
        });
        GridPane.setConstraints(trackNames, 3, 0, 5, 10);
        grid.getChildren().add(trackNames);


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
