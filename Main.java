package sample;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;
import javafx.stage.WindowEvent;
import javafx.scene.control.Slider;
import java.net.URL;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class Main /* extends Application */
{
    /* private static QueuePlayer musicPlayer = null;
    private static String songName;
    private static final String rootURL = "http://musicmanager.duckdns.org/";
    private static final HashMap<String, String[]> playlistHashMap = getPlaylistHashMap();
    private static int viewMode = ViewMode.MUSIC_OVERVIEW;*/
    protected static final String rootURL = "http://musicmanager.duckdns.org/";
    private static final ArrayList<String> tracksArray = getFileNamesAtSite(rootURL + "AllTracks/");
    protected static ArrayList<String> tracksQueue = tracksArray;

    /*@Override
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
        stage.setWidth(580);
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
            switch (musicPlayer.playerStatus)
            {
                case PlayerStatus.PLAYING: // should be paused
                    System.out.println("Pausing " + songName + "...");
                    musicPlayer.pause();
                    break;

                case PlayerStatus.PAUSED: // should be played
                    System.out.println("Resuming " + songName + " from paused...");
                    musicPlayer.resume();
                    break;

                default: // should be played from scratch
                    System.out.println("Starting " + songName + "from scratch. Skipping 0%");
                    playFromScratch(0);
                    break;
            }
        });
        GridPane.setConstraints(playPauseBtn, 0, 2, 1, 1);
        grid.getChildren().add(playPauseBtn);

        // defining slider <-- will be used later, once Ive got the skip functionality working through a text field first
        final Slider seekSlider = new Slider();
        seekSlider.setMin(0);
        seekSlider.setMax(1);
        seekSlider.setBlockIncrement(0.01);
        seekSlider.valueProperty().addListener((observable, oldValue, newValue) ->
        {
            double skipMultiplier = newValue.doubleValue();
            System.out.println("Playing " + songName + " from scratch. Skipping " + 100 * skipMultiplier + "%");
            if (musicPlayer != null) { musicPlayer.stop(); }
            playFromScratch(skipMultiplier);
        });
        GridPane.setConstraints(seekSlider, 1, 2, 98, 1);
        grid.getChildren().add(seekSlider);

        // defining files listview
        ListView<String> mainListView = new ListView<>(FXCollections.observableArrayList(tracksArray));
        mainListView.setOrientation(Orientation.VERTICAL);
        mainListView.setOnMouseClicked(event ->
        {
            if (musicPlayer != null) { System.out.println("Stopping " +songName + "..."); musicPlayer.stop(); }
            songName = mainListView.getSelectionModel().getSelectedItem();
            if (songName != null)
            {
                System.out.println("Starting " + songName + " from scratch. Skipping 0%");
                if (seekSlider.getValue() != 0) { seekSlider.setValue(0); }// <-- will automatically play it
                else { playFromScratch(0); }
            }
        });
        GridPane.setConstraints(mainListView, 0, 1, 100, 1);
        grid.getChildren().add(mainListView);

        // defining search box
        final TextField searchField = new TextField();
        searchField.setPromptText("⚲");
        searchField.textProperty().addListener(((observable, oldValue, newValue) ->
        {
            String searchText = newValue.toLowerCase();
            System.out.println(searchText);
            mainListView.getItems().clear();
            System.out.println("size: " + tracksArray.size());
            for (String trackName : tracksArray)
            {
                if (trackName.toLowerCase().contains(searchText)) { mainListView.getItems().add(trackName); }
            }
        }));
        GridPane.setConstraints(searchField, 1, 0,98, 1);
        grid.getChildren().add(searchField);

        // defining viewPlaylists button
        final Button viewPlaylistsButton = new Button("≡");
        viewPlaylistsButton.setOnAction((ActionEvent ae) ->
        {
            switch (viewMode)
            {
                case ViewMode.MUSIC_OVERVIEW: // going to playlist overview
                    viewMode = ViewMode.PLAYLIST_OVERVIEW;
                    ObservableList<String> playlistNames = FXCollections.observableArrayList();
                    for (Object object : playlistHashMap.entrySet())
                    {
                        Map.Entry entry = (Map.Entry) object;
                        playlistNames.add((String) entry.getKey()); // key is playlist name
                    }
                    Collections.sort(playlistNames);
                    mainListView.setItems(playlistNames);
                    break;

                default: // reverting to music overview
                    viewMode = ViewMode.MUSIC_OVERVIEW;
                    mainListView.getItems().clear();
                    tracksArray.forEach(track -> mainListView.getItems().add(track));
                    break;

            }
        });
        GridPane.setConstraints(viewPlaylistsButton, 0, 0);
        grid.getChildren().add(viewPlaylistsButton);

        // defining add button
        final Button addButton = new Button("✎");
        addButton.setOnAction((ActionEvent ae) ->
        {
            switch (viewMode)
            {
                case ViewMode.MUSIC_OVERVIEW: // will allow user to upload new music file
                    System.out.println("Selected: add/remove tracks from library");
                    break;

                case ViewMode.PLAYLIST_OVERVIEW: // will allow user to add new playlist
                    System.out.println("Selected: add/remove playlist");
                    break;

                case ViewMode.SINGLE_PLAYLIST: // will allow user to edit playlist
                    System.out.println("Selected: add/remove tracks from playlist");
                    break;

                default: break; // this should never be reached, but just in case
            }
        });
        GridPane.setConstraints(addButton, 99, 0);
        grid.getChildren().add(addButton);

        // defining shuffle button
        final Button shuffleButton = new Button("\uD83D\uDD00"); // shuffle unicode character
        shuffleButton.setOnAction((ActionEvent ae) -> Collections.shuffle(tracksQueue));
        GridPane.setConstraints(shuffleButton, 99, 2);
        grid.getChildren().add(shuffleButton);

        // finally making stage visible
        stage.show();
    }

    private String toURL(String toFormat) { return toFormat.replaceAll(" ", "%20"); }*/
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

    /*private static HashMap<String, String[]> getPlaylistHashMap()
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
    }*/

    public static void main(String[] args) throws InterruptedException
    {
        //launch(args);
        QueuePlayer queuePlayer = new QueuePlayer();
        queuePlayer.playQueue();
    }
}
