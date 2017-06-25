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

class PlayerStatus
{

}

class MusicPlayer
{
    private static final int NOT_STARTED = 0, PLAYING = 1, PAUSED = 2, FINISHED = 3; // statuses
    private final Player player;
    private final Object playerLock = new Object();
    private int playerStatus = NOT_STARTED;

    public MusicPlayer(final InputStream inputStream) throws JavaLayerException
    {
        this.player = new Player(inputStream);
    }
    public MusicPlayer(final InputStream inputStream, final AudioDevice audioDevice) throws JavaLayerException
    {
        this.player = new Player(inputStream, audioDevice);
    }

    // starts playback. resumes if paused
    public void play() throws JavaLayerException
    {
        synchronized (playerLock)
        {
            switch (playerStatus)
            {
                case NOT_STARTED:
                    final Runnable runnable = () -> playInternal();
                    final Thread playerThread = new Thread(runnable);
                    playerThread.setPriority(Thread.MAX_PRIORITY);
                    playerStatus = PLAYING;
                    playerThread.start();
                    break;
                case PAUSED:
                    resume();
                    break;
                default:
                    break;
            }
        }
    }

    // pauses playback. returns true if successfully paused
    public boolean pause()
    {
        synchronized (playerLock)
        {
            if (playerStatus == PLAYING)
            {
                playerStatus = PAUSED;
            }
            return playerStatus == PAUSED;
        }
    }

    // resumes playback.returns true if new state is PLAYING
    public boolean resume()
    {
        synchronized (playerLock)
        {
            if (playerStatus == PAUSED)
            {
                playerStatus = PLAYING;
                playerLock.notifyAll();
            }
            return playerStatus == PLAYING;
        }
    }

    // stops playback. if not playing, does nothing
    public void stop()
    {
        synchronized (playerLock)
        {
            playerStatus = FINISHED;
            playerLock.notifyAll();
        }
    }

    private void playInternal()
    {
        while (playerStatus != FINISHED)
        {
            try
            {
                if (!player.play(1))
                {
                    break;
                }
            }
            catch (final JavaLayerException ex)
            {
                break;
            }

            // check if paused or terminated
            synchronized (playerLock)
            {
                while (playerStatus == PAUSED)
                {
                    try { playerLock.wait(); }
                    catch (final InterruptedException ex) { break; } // terminates player
                }
            }
        }
        closePlayer();
    }

    public void closePlayer() // closes player, regardless of current state
    {
        synchronized (playerLock) { playerStatus = FINISHED; }
        try { player.close(); }
        catch (final Exception ex) {  } // we're ending anyway. ignore
    }
}

public class Main extends Application
{
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
            final BufferedInputStream inputStream;
            try {inputStream = new BufferedInputStream(new URL("http://www.musicmanager.duckdns.org/" + "CodingDude.mp3").openStream()); }// will replace name with variable
            catch (final IOException ex) { throw new RuntimeException(ex); }

            final MusicPlayer musicPlayer;
            try
            {
                musicPlayer = new MusicPlayer(inputStream);
                musicPlayer.play();
                Thread.sleep(5000);
                musicPlayer.pause();
                Thread.sleep(5000);
                musicPlayer.resume();
            }
            catch (final JavaLayerException | InterruptedException ex) { throw new RuntimeException(ex); }

        });
        GridPane.setConstraints(playPauseBtn, 0, 0);
        grid.getChildren().add(playPauseBtn);

        // defining stop button
        final Button stopBtn = new Button("Stop!");
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
