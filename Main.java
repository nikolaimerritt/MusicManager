package sample;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;
import javafx.stage.WindowEvent;
import javafx.scene.control.Slider;
import java.io.*;
import java.net.*;
import java.net.URL;
import javazoom.jl.decoder.JavaLayerException;
import java.io.IOException;
import java.net.MalformedURLException;

public class Main extends Application
{
    private static MusicPlayer musicPlayer = null;
    private static double percentToSkip = 0;
    private static String fileName = "DarkSouls.mp3";

    private int getFileSize(final URL url)
    {
        try
        {
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("HEAD");
            connection.getInputStream();
            final int fileSize = connection.getContentLength();
            connection.disconnect();
            return fileSize;
        }
        catch (IOException ex) { return -1; }
    }

    private void playFromScratch(final double percentToSkip)
    {
        final URL url;
        final BufferedInputStream inputStream;
        try { url = new URL("http://www.musicmanager.duckdns.org/" + fileName); }
        catch (MalformedURLException ex) { throw new RuntimeException(ex); }

        try {inputStream = new BufferedInputStream(url.openStream()); }
        catch (final IOException ex) { throw new RuntimeException(ex); }

        if (percentToSkip > 0)
        {
            final long framesToSkip = (long) (getFileSize(url) * percentToSkip);
            try { inputStream.skip(framesToSkip); }
            catch (IOException ex) { throw new RuntimeException(ex); }
        }

        try
        {
            musicPlayer = new MusicPlayer(inputStream);
            musicPlayer.play();
        }
        catch (JavaLayerException ex) { throw new RuntimeException(ex); }

    }

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
                playFromScratch(0);
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

        // defining slider <-- will be used later, once Ive got the skip functionality working through a text field first
        final Slider seekSlider = new Slider();
        seekSlider.setMin(0);
        seekSlider.setMax(1);
        seekSlider.setBlockIncrement(0.01);
        seekSlider.valueProperty().addListener((observable, oldValue, newValue) ->
        {
            musicPlayer.stop();
            playFromScratch(newValue.doubleValue());
        });
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
