package sample;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
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
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import javafx.scene.control.Slider;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URL;
import javax.swing.*;
import javax.sound.sampled.*;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.Player;
import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;


public class Main extends Application
{
    private static MusicPlayer musicPlayer = null;
    private static String fileName = "CodingDude.mp3";

    @Override
    public void start(Stage primaryStage)
    {
        // setting up stage
        Stage stage = new Stage();
        stage.setTitle("Music Player");
        stage.setResizable(true);
        stage.setOnCloseRequest((WindowEvent event) ->
        {
            try { Main.super.stop(); }
            catch (Exception ex) { ex.printStackTrace(); }
            Platform.exit();
            System.exit(0);
        });

        // setting up GridPane controller
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setVgap(5);
        grid.setHgap(5);

        // defining play/pause button
        final Button playPauseBtn = new Button("Play/Pause!");
        playPauseBtn.setOnAction((ActionEvent ae) ->
        {
            if (musicPlayer == null || musicPlayer.playerStatus == PlayerStatus.NOT_STARTED || musicPlayer.playerStatus == PlayerStatus.FINISHED) // has not yet started. should be started for first time
            {
                System.out.println("Starting playback of " + fileName + " from scratch...");
                final BufferedInputStream inputStream;
                try {inputStream = new BufferedInputStream(new URL("http://www.musicmanager.duckdns.org/" + fileName).openStream()); }// will replace name with variable
                catch (final IOException ex) { throw new RuntimeException(ex); }

                try
                {
                    musicPlayer = new MusicPlayer(inputStream);
                    musicPlayer.play();
                }
                catch (final JavaLayerException ex) { throw new RuntimeException(ex); }
            }

            else if (musicPlayer.playerStatus == PlayerStatus.PAUSED) // is paused. should be played.
            {
                System.out.println("Resuming " + fileName + " from paused...");
                musicPlayer.resume();
            }

            else if (musicPlayer.playerStatus == PlayerStatus.PLAYING) // is playing. should be paused
            {
                System.out.println("Pausing" + fileName + "...");
                musicPlayer.pause();
            }
        });
        GridPane.setConstraints(playPauseBtn, 0, 0);
        grid.getChildren().add(playPauseBtn);

        // defining stop button
        final Button stopBtn = new Button("Stop!");
        stopBtn.setOnAction((ActionEvent ae) ->
        {
            if (musicPlayer.playerStatus == PlayerStatus.PLAYING || musicPlayer.playerStatus == PlayerStatus.PAUSED)
            {
                System.out.println("Stopping playback of " + fileName + "...");
                musicPlayer.stop();
            }
        });
        GridPane.setConstraints(stopBtn, 1, 0);
        grid.getChildren().add(stopBtn);

        // defining slider
        final Slider seekSlider = new Slider();
        GridPane.setConstraints(seekSlider, 0, 1, 2, 1);
        grid.getChildren().add(seekSlider);

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
