package sample;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.stage.WindowEvent;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.net.URL;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import javazoom.jl.decoder.JavaLayerException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import com.jcraft.jsch.*;

public class Main extends Application
{
    private static QueuePlayer queuePlayer = new QueuePlayer();
    static final String rootURL = "http://musicmanager.duckdns.org/";
    private static final HashMap<String, ArrayList<String>> playlistHashMap = getPlaylistHashMap();
    private static int viewMode = ViewMode.MUSIC_OVERVIEW;
    private static final ArrayList<String> allTracks = getFileNamesAtSite(rootURL + "AllTracks/");
    static ArrayList<String> trackQueue = allTracks;
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
                    System.out.println("Pausing " + trackQueue.get(0) + "...");
                    queuePlayer.pauseQueue();
                    break;

                case PlayerStatus.PAUSED: // should be played
                    System.out.println("Resuming " + trackQueue.get(0) + " from paused...");
                    queuePlayer.resumeQueue();
                    break;

                default: // should be played from scratch
                    System.out.println("Starting " + trackQueue.get(0) + "from scratch. Skipping 0%");
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
            try { queuePlayer.seekCurrentTrack(fractionXPressed); }
            catch (JavaLayerException | IOException ex) { throw new RuntimeException(ex); }
        });
        GridPane.setConstraints(progressBar, 1, 2, 98, 1);
        grid.getChildren().add(progressBar);

        // defining files listview
        ListView<String> mainListView = new ListView<>(FXCollections.observableArrayList(trackQueue));
        mainListView.setOrientation(Orientation.VERTICAL);
        mainListView.setOnMouseClicked(event ->
        {
            String selectedItem = mainListView.getSelectionModel().getSelectedItem();
            if (selectedItem != null)
            {
                switch(viewMode)
                {
                    case ViewMode.MUSIC_OVERVIEW:
                        queuePlayer.stopQueue();
                        while (!trackQueue.get(0).equals(selectedItem))
                        {
                            trackQueue = shiftLeft(trackQueue);
                        }
                        queuePlayer.playNewQueue(0);
                        break;

                    case ViewMode.PLAYLIST_OVERVIEW:
                        showEditSinglePlaylistScene(selectedItem);
                        break;

                    default: break;
                }
            }
        });
        GridPane.setConstraints(mainListView, 0, 1, 100, 1);
        grid.getChildren().add(mainListView);

        // defining thread to auto-update mainListView
        Thread updateListViewThread = new Thread(() ->
        {
            while (true)
            {
                if (updateMainListView)
                {
                    Platform.runLater(() -> mainListView.setItems(FXCollections.observableArrayList(trackQueue)));
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
        viewPlaylistsButton.setOnAction(event ->
        {
            switch (viewMode)
            {
                case ViewMode.MUSIC_OVERVIEW: // should be PLAYLIST_OVERVIEW
                    viewMode = ViewMode.PLAYLIST_OVERVIEW;
                    final ArrayList<String> playlistNames = new ArrayList<>(playlistHashMap.keySet());
                    Collections.sort(playlistNames);
                    mainListView.setItems(FXCollections.observableArrayList(playlistNames));
                    break;

                default: // should be MUSIC_OVERVIEW
                    viewMode = ViewMode.MUSIC_OVERVIEW;
                    mainListView.setItems(FXCollections.observableArrayList(allTracks));
                    break;
            }
        });
        GridPane.setConstraints(viewPlaylistsButton, 0, 0);
        grid.getChildren().add(viewPlaylistsButton);

        // defining add button
        final Button addButton = new Button("✎");
        addButton.setOnAction(event ->
        {
            switch (viewMode)
            {
                case ViewMode.MUSIC_OVERVIEW: // should make new window which allows adding, removing and renaming tracks
                    System.out.println("Edit music mode");
                    showEditMusicScene();
                    break;

                case ViewMode.PLAYLIST_OVERVIEW: // should make new window which allows adding, removing and renaming playlists
                    System.out.println("Edit playlists mode");
                    showEditAllPlaylistsScene();
                    break;

                default: // go back to normal view
                    viewMode = ViewMode.MUSIC_OVERVIEW;
                    System.out.println("Music overview mode");
                    break;
            }
        });
        GridPane.setConstraints(addButton, 99, 0);
        grid.getChildren().add(addButton);

        // defining shuffle button
        final Button shuffleButton = new Button("\uD83D\uDD00"); // shuffle unicode character
        shuffleButton.setOnAction(event ->
        {
            String currentTrack = trackQueue.get(0);
            Collections.shuffle(trackQueue);
            while (!trackQueue.get(0).equals(currentTrack)) // making current track be at front of shuffled queue
            {
                trackQueue = shiftLeft(trackQueue);
            }
            mainListView.setItems(FXCollections.observableArrayList(trackQueue));
        });
        GridPane.setConstraints(shuffleButton, 99, 2);
        grid.getChildren().add(shuffleButton);

        // finally making stage visible
        stage.show();
    }

    private void showEditMusicScene()
    {
        Stage editMusicStage = new Stage();
        GridPane editMusicGrid = new GridPane();

        editMusicStage.setTitle("Edit Music");
        editMusicStage.setResizable(true);
        editMusicStage.setOnCloseRequest((WindowEvent event) -> editMusicStage.hide());
        Scene editMusicScene = new Scene(editMusicGrid);
        editMusicScene.getStylesheets().add(Main.class.getResource("Main.css").toExternalForm());
        editMusicStage.setWidth(550);
        editMusicStage.setHeight(300);
        editMusicStage.setScene(editMusicScene);

        // setting up GridPane controller
        editMusicGrid.setPadding(new Insets(10, 10, 10, 10));
        editMusicGrid.setVgap(5);
        editMusicGrid.setHgap(5);

        // finally showing editMusicStage
        editMusicStage.show();
    }

    private void showEditAllPlaylistsScene()
    {
        Stage editPlaylistStage = new Stage();
        GridPane editPlaylistGrid = new GridPane();

        editPlaylistStage.setTitle("Edit Playlists");
        editPlaylistStage.setResizable(true);
        editPlaylistStage.setOnCloseRequest((WindowEvent event) -> editPlaylistStage.hide());
        Scene editPlaylistsScene = new Scene(editPlaylistGrid);
        editPlaylistsScene.getStylesheets().add(Main.class.getResource("Main.css").toExternalForm());
        editPlaylistStage.setWidth(550);
        editPlaylistStage.setHeight(300);
        editPlaylistStage.setScene(editPlaylistsScene);

        // setting up GridPane controller
        editPlaylistGrid.setPadding(new Insets(10, 10, 10, 10));
        editPlaylistGrid.setVgap(5);
        editPlaylistGrid.setHgap(5);

        // finally showing editMusicStage
        editPlaylistStage.show();
    }

    private void showEditSinglePlaylistScene(String playlistName)
    {
        Stage editPlaylistStage = new Stage();
        GridPane editPlaylistGrid = new GridPane();
        ObservableList<String> playlistTracks = FXCollections.observableArrayList(playlistHashMap.get(playlistName));

        editPlaylistStage.setTitle("Edit Playlist: " + playlistName);
        editPlaylistStage.setResizable(true);
        editPlaylistStage.setOnCloseRequest(event ->
        {
            // saving all playlists to file, with updates. each line in format: playlistName:song1.mp3;song2.mp3;song3.mp3 ...
            String writeInFile = "";
            for (Object objectEntry : playlistHashMap.entrySet())
            {
                final Map.Entry item = (Map.Entry) objectEntry;
                final ObservableList<String> currentPlaylistContents;
                final String currentPlaylistName = (String) item.getKey();
                if (item.getKey() == playlistName) { currentPlaylistContents = playlistTracks; }
                else { currentPlaylistContents = FXCollections.observableArrayList((ArrayList<String>) item.getValue()); } // <3 <3 <3

                writeInFile += currentPlaylistName + ":";
                for (int i = 0; i < currentPlaylistContents.size(); i++)
                {
                    writeInFile += currentPlaylistContents.get(i);
                    writeInFile += ((i + 1 == currentPlaylistContents.size()) ? "\n" : ";");
                }
                final byte[] password = new byte[]{(byte) 0x65, (byte) 0x6e, (byte) 0x75, (byte) 0x6d, (byte) 0x61, (byte) 0x45, (byte) 0x6c, (byte) 0x69, (byte) 0x5f, (byte) 0x73};
                sftpWriter("www.musicmanager.duckdns.org", "pi", password, "/var/www/html/Playlists/playlist.txt", writeInFile);
            }
            editPlaylistStage.hide();
        });
        Scene editPlaylistScene = new Scene(editPlaylistGrid);
        editPlaylistScene.getStylesheets().add(Main.class.getResource("Main.css").toExternalForm());
        editPlaylistStage.setWidth(550);
        editPlaylistStage.setHeight(300);
        editPlaylistStage.setScene(editPlaylistScene);

        // setting up GridPane controller
        editPlaylistGrid.setPadding(new Insets(10, 10, 10, 10));
        editPlaylistGrid.setVgap(5);
        editPlaylistGrid.setHgap(5);

        // setting up playlist header label
        final Label playlistLabel = new Label(playlistName.toUpperCase());
        GridPane.setConstraints(playlistLabel, 0, 0, 60, 1);
        editPlaylistGrid.getChildren().add(playlistLabel);

        // setting up all tracks header label
        final Label allTracksLabel = new Label("ALL TRACKS");
        GridPane.setConstraints(allTracksLabel, 62, 0, 35, 1);
        editPlaylistGrid.getChildren().add(allTracksLabel);

        // setting up search box for all tracks

        // setting up playlist listview. removes track from playlist if clicked on
        final ListView<String> playlistListView = new ListView<>(playlistTracks);
        playlistListView.setOnMouseClicked(event -> playlistTracks.remove(playlistListView.getSelectionModel().getSelectedItem()));
        GridPane.setConstraints(playlistListView, 0, 1, 60, 11);
        editPlaylistGrid.getChildren().add(playlistListView);

        // setting up all tracks listview. adds track to playlist if clicked on
        final ListView<String> allTracksListView = new ListView<>(FXCollections.observableArrayList(allTracks));
        allTracksListView.setOnMouseClicked(event ->
        {
            String trackToAdd = allTracksListView.getSelectionModel().getSelectedItem();
            if (!playlistTracks.contains(trackToAdd))
            {
                playlistTracks.add(trackToAdd);
            }
        });
        GridPane.setConstraints(allTracksListView, 62, 2, 35, 10);
        editPlaylistGrid.getChildren().add(allTracksListView);

        final TextField searchField = new TextField();
        searchField.setPromptText("⚲");
        searchField.textProperty().addListener((observable, oldValue, newValue) ->
        {
            String searchText = newValue.toUpperCase();
            allTracksListView.getItems().clear();
            allTracks.forEach((trackName ->
            {
                if (trackName.toUpperCase().contains(searchText)) { allTracksListView.getItems().add(trackName); }
            }));
        });
        GridPane.setConstraints(searchField, 62, 1,35, 1);
        editPlaylistGrid.getChildren().add(searchField);

        // finally showing editMusicStage
        editPlaylistStage.show();
    }

    private static String fromURL(String deformat) { return deformat.replaceAll("%20", " "); }

    static <T> ArrayList<T> shiftLeft(ArrayList<T> arrayList)
    {
        T firstElement = arrayList.get(0);
        arrayList.remove(firstElement);
        arrayList.add(firstElement);
        return arrayList;
    }

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

    private static HashMap<String, ArrayList<String>> getPlaylistHashMap()
    {
        final URL url;
        final Scanner scanner;
        HashMap<String, ArrayList<String>> playlistHashMap = new HashMap<>();
        try { url = new URL("http://musicmanager.duckdns.org/Playlists/playlist.txt"); }
        catch (MalformedURLException ex) { throw new RuntimeException(ex); }
        try { scanner = new Scanner(url.openStream()); }
        catch (IOException ex) { throw new RuntimeException(ex); }

        while (scanner.hasNextLine())
        {
            // each line is in format playlistName:foo.mp3;bar.mp3;foobar.mp3
            String[] splitOnPlaylist = scanner.nextLine().split(":");
            String playlistName = splitOnPlaylist[0]; // bit before ':'
            ArrayList<String> itemsInPlaylist = new ArrayList<>(Arrays.asList(splitOnPlaylist[1].split(";"))); // getting all items after playlist, as array
            playlistHashMap.put(playlistName, itemsInPlaylist);
        }
        return playlistHashMap;
    }

    private void sftpWriter(final String hostName, final String username, final byte[] password, final String destination, final String toWrite)
    {
        final JSch jSch = new JSch();
        final Session session;
        final Writer writer;
        //final byte[] password = new byte[]{(byte) 0x65, (byte) 0x6e, (byte) 0x75, (byte) 0x6d, (byte) 0x61, (byte) 0x45, (byte) 0x6c, (byte) 0x69, (byte) 0x5f, (byte) 0x73 };
        try
        {
            session = jSch.getSession(username, hostName, 22);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setPassword(password);
            session.connect();

            Channel channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp channelSftp = (ChannelSftp) channel;
            writer = new OutputStreamWriter(channelSftp.put(destination), "utf-8");
            writer.write(toWrite);
            writer.close();
            channelSftp.exit();
            session.disconnect();
        }
        catch (JSchException | IOException | SftpException ex) { throw new RuntimeException(ex); }
    }

    public static void main(String[] args) throws InterruptedException
    {
        launch(args);
    }
}
