package sample;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.stage.WindowEvent;
import java.net.URL;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import javazoom.jl.decoder.JavaLayerException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class Main extends Application
{
    private static QueuePlayer queuePlayer = new QueuePlayer();
    static final String rootURL = "http://musicmanager.duckdns.org/";
    private static final HashMap<String, String[]> playlistHashMap = getPlaylistHashMap();
    private static int viewMode = ViewMode.MUSIC_OVERVIEW;
    private static final ArrayList<String> allTracks = getFileNamesAtSite(rootURL + "AllTracks/");
    static ArrayList<String> tracksQueue = allTracks;
    static final ProgressBar progressBar = new ProgressBar(0);
    static volatile boolean updateMainListView = false;

    @Override
    public void start(Stage primaryStage)
    {
        GridPane grid = new GridPane();

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
        Scene scene = new Scene(grid, 300, 300);
        scene.getStylesheets().add(Main.class.getResource("Main.css").toExternalForm());
        stage.setWidth(585);
        stage.setHeight(300);
        stage.setScene(scene);

        // setting up GridPane controller
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setVgap(5);
        grid.setHgap(5);

        // defining play/pause button
        final Button playPauseBtn = new Button("▮▶");
        playPauseBtn.setOnAction((ActionEvent ae) ->
        {
            switch (queuePlayer.playerStatus)
            {
                case PlayerStatus.PLAYING: // should be paused
                    System.out.println("Pausing " + tracksQueue.get(0) + "...");
                    queuePlayer.pauseQueue();
                    break;

                case PlayerStatus.PAUSED: // should be played
                    System.out.println("Resuming " + tracksQueue.get(0) + " from paused...");
                    queuePlayer.resumeQueue();
                    break;

                default: // should be played from scratch
                    System.out.println("Starting " + tracksQueue.get(0) + "from scratch. Skipping 0%");
                    queuePlayer.playNewQueue(0);
                    break;
            }
        });
        GridPane.setConstraints(playPauseBtn, 0, 2, 1, 1);
        grid.getChildren().add(playPauseBtn);

        // defining progress bar. shows % of progress. UPDATED AUTOMATICALLY IN MUSICPLAYER.JAVA
        progressBar.setMaxWidth(Double.MAX_VALUE); // making it stretch all the way. this does not conflict with the shuffle button
        progressBar.setOnMouseClicked(event ->
        {
            double fractionXPressed = event.getX() / progressBar.getLayoutBounds().getWidth();
            try { queuePlayer.skipCurrentTrack(fractionXPressed); }
            catch (JavaLayerException | IOException ex) { throw new RuntimeException(ex); }
        });
        GridPane.setConstraints(progressBar, 1, 2, 98, 1);
        grid.getChildren().add(progressBar);

        // defining files listview
        ListView<String> mainListView = new ListView<>(FXCollections.observableArrayList(tracksQueue));
        mainListView.setOrientation(Orientation.VERTICAL);
        GridPane.setConstraints(mainListView, 0, 1, 100, 1);
        grid.getChildren().add(mainListView);

        // defining thread to auto-update mainListView
        Thread updateListViewThread = new Thread(() ->
        {
            while (true)
            {
                if (updateMainListView)
                {
                    Platform.runLater(() ->
                    {
                        mainListView.setItems(FXCollections.observableArrayList(tracksQueue));
                    });
                    updateMainListView = false;
                }
            }
        });
        updateListViewThread.start();

        // defining search box
        final TextField searchField = new TextField();
        searchField.setPromptText("⚲");
        searchField.textProperty().addListener((observable, oldValue, newValue) -> 
        {
           String searchText = newValue.toLowerCase();
           mainListView.getItems().clear();
           allTracks.forEach((trackName ->
           {
               if (trackName.toLowerCase().contains(searchText)) { mainListView.getItems().add(trackName); }
           }));
        });
        GridPane.setConstraints(searchField, 1, 0,98, 1);
        grid.getChildren().add(searchField);

        // defining viewPlaylists button
        final Button viewPlaylistsButton = new Button("≡");
        GridPane.setConstraints(viewPlaylistsButton, 0, 0);
        grid.getChildren().add(viewPlaylistsButton);

        // defining add button
        final Button addButton = new Button("✎");
        GridPane.setConstraints(addButton, 99, 0);
        grid.getChildren().add(addButton);

        // defining shuffle button
        final Button shuffleButton = new Button("\uD83D\uDD00"); // shuffle unicode character
        GridPane.setConstraints(shuffleButton, 99, 2);
        grid.getChildren().add(shuffleButton);

        // finally making stage visible
        stage.show();
    }

    private static String fromURL(String deformat) { return deformat.replaceAll("%20", " "); }

    private static ArrayList<String> getFileNamesAtSite(String urlString)
    {
        ArrayList<String> songNames = new ArrayList<>();
        Document document;
        try { document = Jsoup.connect(urlString).get(); }
        catch (IOException ex) { throw new RuntimeException(ex); }
        for (Element file : document.select("*")) // getting all files
        {
            String songName = fromURL(file.attr("href"));
            if (songName.contains(".mp3")) { songNames.add(songName.replace(".mp3", "")); }
        }
        return songNames;
    }

    private static HashMap<String, String[]> getPlaylistHashMap()
    {
        final URL url;
        final Scanner scanner;
        HashMap<String, String[]> playlistHashMap = new HashMap<>();
        try { url = new URL("http://musicmanager.duckdns.org/Playlists/playlist.txt"); }
        catch (MalformedURLException ex) { throw new RuntimeException(ex); }
        try { scanner = new Scanner(url.openStream()); }
        catch (IOException ex) { throw new RuntimeException(ex); }

        while (scanner.hasNextLine())
        {
            // each line is in format playlistName:foo.mp3;bar.mp3;foobar.mp3
            String[] splitOnPlaylist = scanner.nextLine().split(":");
            String playlistName = splitOnPlaylist[0]; // bit before ':'
            String[] itemsInPlaylist = splitOnPlaylist[1].split(";"); // getting all items after playlist, as array
            playlistHashMap.put(playlistName, itemsInPlaylist);
        }
        return playlistHashMap;
    }

    public static void main(String[] args) throws InterruptedException
    {
        launch(args);
    }
}
