package sample;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class Main extends Application
{
    private static MusicPlayer musicPlayer = null;
    private static String fileName = "DarkSouls.mp3";

    private ListView<String> getFileNamesAtSite(String urlString)
    {
        ListView<String> fileNames = new ListView<>();
        Document document;
        try { document = Jsoup.connect(urlString).get(); }
        catch (IOException ex) { throw new RuntimeException(ex); }
        for (Element file : document.select("*")) // getting all files
        {
            String fileName = file.attr("href").replaceAll("%20", " ");
            if (fileName.contains(".mp3")) { fileNames.getItems().add(fileName.replace(".mp3", "")); }
        }
        return fileNames;
    }

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
        stage.setWidth(460);
        stage.setHeight(220);

        // defining files listview
        final ListView<String> tracksListVew = getFileNamesAtSite("http://www.musicmanager.duckdns.org/");
        tracksListVew.setOrientation(Orientation.VERTICAL);
        GridPane.setConstraints(tracksListVew, 0, 0, 100, 1);
        grid.getChildren().add(tracksListVew);

        // defining play/pause button
        final Button playPauseBtn = new Button("|>");
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
        GridPane.setConstraints(playPauseBtn, 0, 1, 1, 1);
        grid.getChildren().add(playPauseBtn);

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
        GridPane.setConstraints(seekSlider, 1, 1, 98, 1);
        grid.getChildren().add(seekSlider);

        // defining stop button
        final Button stopBtn = new Button("#");
        stopBtn.setOnAction((ActionEvent ae) ->
        {
            if (musicPlayer.playerStatus == PlayerStatus.PLAYING || musicPlayer.playerStatus == PlayerStatus.PAUSED)
            {
                System.out.println("Stopping playback of " + fileName + "...");
                musicPlayer.stop();
            }
        });
        GridPane.setConstraints(stopBtn, 99, 1);
        grid.getChildren().add(stopBtn);

        // finally making stage visible
        stage.setScene(new Scene(grid, 300, 300));
        stage.setWidth(600);
        stage.setHeight(250);
        stage.show();
    }

    public static void main(String[] args) throws InterruptedException
    {
        launch(args);
    }
}
