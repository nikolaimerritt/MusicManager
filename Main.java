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
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URL;
import javax.swing.*;
import javax.sound.sampled.*;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;
import java.io.BufferedInputStream;
import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class Main extends Application
{

    private static AdvancedPlayer mp3player = null;
    private static int pausedOnFrame = 0;
    private static ListView<String> trackNames = new ListView<>();

    private static void setupMp3player(String fileName)
    {
        BufferedInputStream inputStream;
        try
        {
            inputStream = new BufferedInputStream(new URL("http://www.musicmanager.duckdns.org/" + fileName).openStream());
            mp3player = new AdvancedPlayer(inputStream);
            mp3player.setPlayBackListener(new PlaybackListener()
            {
                @Override
                public void playbackFinished(PlaybackEvent playbackEvent) {
                    pausedOnFrame = playbackEvent.getFrame();
                }
            });
        }
        catch (IOException | JavaLayerException | NullPointerException ex) { ex.printStackTrace(); }
    }

    private static void playPause()
    {
        if (mp3player == null) // playing for first time. should be played
        {
            System.out.println("Now playing for first time...");
            setupMp3player("CodingDude.mp3");
            try { mp3player.play(pausedOnFrame, Integer.MAX_VALUE); } // using MAX_VALUE so it stops whenever the mp3 finishes, and is not limited
            catch (JavaLayerException ex) { ex.printStackTrace(); }
        }
        else if (mp3player != null) // is playing. should be paused
        {
            System.out.println("Is playing. Now pausing...");
            mp3player.stop();
            mp3player = null;
        }
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
        playPauseBtn.setOnAction((ActionEvent ae) -> playPause());
        GridPane.setConstraints(playPauseBtn, 0, 0);
        grid.getChildren().add(playPauseBtn);

        // defining stop button
        final Button stopBtn = new Button("Stop!");
        stopBtn.setOnAction((ActionEvent ae) -> {
            System.out.println("Is playing. Now stopping...");
            mp3player.stop();
            mp3player = null;
            pausedOnFrame = 0; // resetting
        });
        GridPane.setConstraints(stopBtn, 1, 0);
        grid.getChildren().add(stopBtn);

        /*// defining slider
        final Slider slider = new Slider(0, 100, 0);
        slider.setBlockIncrement(1);
        slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (mp3player != null)
            {
                double percentSlid = newValue.longValue() / 100; // percent slid on slider
                int goToFrame = (int) (mp3player. * percentSlid);
                currentClip.setFramePosition(goToFrame);
            }
        });
        GridPane.setConstraints(slider, 0, 1, 2, 1);
        grid.getChildren().add(slider);
        /*
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
        grid.getChildren().add(trackNames); */


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
