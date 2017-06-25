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
