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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.control.ButtonType;
import java.io.*;
import java.net.URL;
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
    private final String rootDir = "/var/www/html";
    private static String username;
    private static HashMap<String, ArrayList<String>> playlistHashMap;
    private static int viewMode = ViewMode.MUSIC_OVERVIEW;
    private static ArrayList<String> allTracks;
    static ArrayList<String> trackQueue;
    static final ProgressBar progressBar = new ProgressBar(0);
    static volatile boolean updateMainListView = false;

    @Override
    public void start(Stage primaryStage) // launches login stage
    {
        GridPane loginGrid = new GridPane();

        // setting up stage
        Stage loginStage = new Stage();
        loginStage.setTitle("LOG IN");
        loginStage.setResizable(true);
        loginStage.setOnCloseRequest(event -> closeAll());
        Scene loginScene = new Scene(loginGrid, 300, 300);
        loginScene.getStylesheets().add(Main.class.getResource("Main.css").toExternalForm());
        loginStage.setWidth(400);
        loginStage.setHeight(300);
        loginStage.setScene(loginScene);

        // setting up GridPane controller
        loginGrid.setPadding(new Insets(10, 10, 10, 10));
        loginGrid.setVgap(5);
        loginGrid.setHgap(5);

        // setting up username textfield
        final TextField usernameTextField = new TextField();
        usernameTextField.setPromptText("User Name: ");
        GridPane.setConstraints(usernameTextField, 0, 0, 40, 1);
        loginGrid.getChildren().add(usernameTextField);

        // setting up cancel button
        final Button cancelButton = new Button("CANCEL");
        cancelButton.setOnAction(event -> closeAll());
        GridPane.setConstraints(cancelButton, 0, 1);
        loginGrid.getChildren().add(cancelButton);

        // setting up OK button
        final Button loginButton = new Button("OK");
        loginButton.setOnAction(event ->
        {
            final Session session = sessionToServer();
            try { session.connect(); }
            catch (JSchException ex) { throw new RuntimeException(ex); }

            final Channel channel;
            try
            {
                channel = session.openChannel("sftp");
                channel.connect();
            }
            catch (JSchException ex) { throw new RuntimeException(ex); }
            username = usernameTextField.getText();

            if (userExistsWithName(channel, username))
            {
                channel.disconnect();
                session.disconnect();
                allTracks = getFileNamesAtSite(rootURL + username + "/AllTracks");
                trackQueue = allTracks;
                playlistHashMap = getPlaylistHashMap();
                launchMainScene();
                loginStage.hide();
                return;
            }
            else
            {
                final Alert badUsernameAlert = new Alert(Alert.AlertType.ERROR, "get that fake ass username outta here fam", ButtonType.OK);
                badUsernameAlert.showAndWait();
            }
        });
        GridPane.setConstraints(loginButton, 1, 1);
        loginGrid.getChildren().add(loginButton);

        // setting up make new user button
        final Button newUserButton = new Button("SIGN UP");
        newUserButton.setOnAction(event ->
        {
            String newUsername = usernameTextField.getText();
            if (newUsername != null)
            {
                final Session session = sessionToServer();
                try { session.connect(); }
                catch (JSchException ex) { throw new RuntimeException(ex); }

                final Channel channel;
                try
                {
                    channel = session.openChannel("sftp");
                    channel.connect();
                }
                catch (JSchException ex) { throw new RuntimeException(ex); }
                if (!userExistsWithName(channel, newUsername))
                {
                    username = newUsername;
                    ChannelSftp sftp = (ChannelSftp) channel;
                    try
                    {
                        sftp.mkdir(rootDir + newUsername);
                        sftp.cd(rootDir + newUsername);
                        String[] dirsToMake = new String[]{"AllTracks", "Metadata", "Playlists"};
                        for (String dir : dirsToMake) { sftp.mkdir(dir); }
                        sftpWriter(rootDir + newUsername + "/Metadata/metadata.txt", "");      // making empty metadata file
                        sftpWriter(rootDir + newUsername + "/Playlists/playlists.txt", "");    // making empty playlists file
                    }
                    catch (SftpException ex) { throw new RuntimeException(ex); }

                    allTracks = getFileNamesAtSite(rootURL + newUsername + "/AllTracks");
                    trackQueue = allTracks;
                    playlistHashMap = getPlaylistHashMap();
                    channel.disconnect();
                    session.disconnect();
                    launchMainScene();
                    loginStage.hide();
                }
            }
            else
            {
                final Alert usernameExistsAlert = new Alert(Alert.AlertType.ERROR, "dat username already here fam", ButtonType.OK);
                usernameExistsAlert.showAndWait();
            }
        });
        GridPane.setConstraints(newUserButton, 7, 1);
        loginGrid.getChildren().add(newUserButton);

        // finally showing stage
        loginStage.show();
    }

    private void launchMainScene()
    {
        GridPane grid = new GridPane();

        // setting up stage
        Stage stage = new Stage();
        stage.setTitle("Music Player");
        stage.setResizable(true);
        stage.setOnCloseRequest(event -> closeAll());
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
            if (trackQueue != null && trackQueue.size() > 0)
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
            }
        });
        GridPane.setConstraints(playPauseBtn, 0, 2, 1, 1);
        grid.getChildren().add(playPauseBtn);

        // defining progress bar. shows % of progress
        progressBar.setMaxWidth(Double.MAX_VALUE); // making it stretch all the way. this does not conflict with the shuffle button
        progressBar.setOnMouseClicked(event ->
        {
            double fractionXPressed = event.getX() / progressBar.getLayoutBounds().getWidth();
            try { queuePlayer.seekCurrentTrack(fractionXPressed); }
            catch (JavaLayerException | IOException ex) { throw new RuntimeException(ex); }
        });
        GridPane.setConstraints(progressBar, 1, 2, 98, 1);
        grid.getChildren().add(progressBar);

        // defining main listview
        final ListView<String> mainListView = new ListView<>(FXCollections.observableArrayList(trackQueue));
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
                        queuePlayer.stopQueue();
                        trackQueue = playlistHashMap.get(selectedItem);
                        mainListView.setItems(FXCollections.observableArrayList(trackQueue));
                        queuePlayer.playNewQueue(0);
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
        final Button editButton = new Button("✎");
        editButton.setOnAction(event ->
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
        GridPane.setConstraints(editButton, 99, 0);
        grid.getChildren().add(editButton);

        // defining shuffle button
        final Button shuffleButton = new Button("\uD83D\uDD00"); // shuffle unicode character
        shuffleButton.setOnAction(event ->
        {
            if (trackQueue != null && trackQueue.size() > 0)
            {
                String currentTrack = trackQueue.get(0);
                Collections.shuffle(trackQueue);
                while (!trackQueue.get(0).equals(currentTrack)) // making current track be at front of shuffled queue
                {
                    trackQueue = shiftLeft(trackQueue);
                }
                mainListView.setItems(FXCollections.observableArrayList(trackQueue));
            }
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
        final List<String> songPathsToUpload = new ArrayList<>();
        final List<String> songNamesToRemove = new ArrayList<>();

        editMusicStage.setTitle("Edit Music");
        editMusicStage.setResizable(true);
        editMusicStage.setOnCloseRequest(event ->
        {
            final Alert saveAlert = new Alert(Alert.AlertType.CONFIRMATION, "save dis shit bruh????", ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
            saveAlert.showAndWait();
            if (saveAlert.getResult() == ButtonType.YES)
            {
                if (songPathsToUpload.size() != 0) { System.out.println(rootDir + "/" + username + "/AllTracks/"); uploadFilesToServer(songPathsToUpload, rootDir + "/" + username + "/AllTracks/"); }
                if (songNamesToRemove.size() != 0) { removeFilesFromServer(songNamesToRemove, rootDir + "/" + username + "/AllTracks/"); }
            }
            editMusicStage.hide();
        });
        Scene editMusicScene = new Scene(editMusicGrid);
        editMusicScene.getStylesheets().add(Main.class.getResource("Main.css").toExternalForm());
        editMusicStage.setWidth(550);
        editMusicStage.setHeight(300);
        editMusicStage.setScene(editMusicScene);

        // setting up GridPane controller
        editMusicGrid.setPadding(new Insets(10, 10, 10, 10));
        editMusicGrid.setVgap(5);
        editMusicGrid.setHgap(5);

        // setting up music listview
        final ObservableList<String> allTracksOL = FXCollections.observableArrayList(allTracks);
        final ListView<String> tracksListView = new ListView<>(allTracksOL);
        GridPane.setConstraints(tracksListView, 0, 1, 96, 4);
        editMusicGrid.getChildren().add(tracksListView);

        // setting up search box for all tracks
        final TextField searchField = new TextField();
        searchField.setPromptText("⚲");
        searchField.textProperty().addListener((observable, oldValue, newValue) ->
        {
            String searchText = newValue.toLowerCase();
            tracksListView.getItems().clear();
            allTracks.forEach((trackName ->
            {
                if (trackName.toLowerCase().contains(searchText) && true)
                {
                    tracksListView.getItems().add(trackName);
                    allTracksOL.add(trackName);
                    Collections.sort(allTracksOL);
                } // added that so stupid IntelliJ doesnt flag up duplicate code, when really, adding this as a function would make little difference given how many parameters it'd need to be passed
            }));
        });
        GridPane.setConstraints(searchField, 1, 0,90, 1);
        editMusicGrid.getChildren().add(searchField);

        // setting up add music button
        final Button addButton = new Button("+");
        addButton.setOnAction(event ->
        {
            final FileChooser fileChooser = new FileChooser();
            List<File> fileList = fileChooser.showOpenMultipleDialog(editMusicStage);
            if (fileList != null)
            {
                fileList.forEach(file ->
                {
                    songPathsToUpload.add(file.getAbsolutePath());
                    allTracksOL.add(file.getName().replace(".mp3", ""));
                    Collections.sort(allTracksOL);
                });
            }
        });
        GridPane.setConstraints(addButton, 95, 0);
        editMusicGrid.getChildren().add(addButton);

        // setting up remove music button
        final Button removeButton = new Button("--");
        removeButton.setOnAction(event ->
        {
            String selectedItem = tracksListView.getSelectionModel().getSelectedItem();
            if (selectedItem != null)
            {
                songNamesToRemove.add(selectedItem);
                allTracksOL.remove(selectedItem);
                Collections.sort(allTracksOL);
            }
        });
        GridPane.setConstraints(removeButton, 0, 0);
        editMusicGrid.getChildren().add(removeButton);

        // finally showing editMusicStage
        editMusicStage.show();
    }

    private void showEditAllPlaylistsScene()
    {
        Stage editPlaylistStage = new Stage();
        GridPane editPlaylistGrid = new GridPane();
        ObservableList<String> allPlaylistNames = FXCollections.observableArrayList(new ArrayList<>(playlistHashMap.keySet()));

        editPlaylistStage.setTitle("Edit Playlists");
        editPlaylistStage.setResizable(true);
        editPlaylistStage.setOnCloseRequest(event ->
        {
            final Alert saveAlert = new Alert(Alert.AlertType.CONFIRMATION, "save dis shit bruh????", ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
            saveAlert.showAndWait();
            if (saveAlert.getResult() == ButtonType.YES) { saveAllPlaylists(); }
            editPlaylistStage.hide();
        });
        Scene editPlaylistsScene = new Scene(editPlaylistGrid);
        editPlaylistsScene.getStylesheets().add(Main.class.getResource("Main.css").toExternalForm());
        editPlaylistStage.setWidth(550);
        editPlaylistStage.setHeight(300);
        editPlaylistStage.setScene(editPlaylistsScene);

        // setting up GridPane controller
        editPlaylistGrid.setPadding(new Insets(10, 10, 10, 10));
        editPlaylistGrid.setVgap(5);
        editPlaylistGrid.setHgap(5);

        // setting up new playlist name textfield
        final TextField nameTextField = new TextField();
        nameTextField.setPromptText("New playlist name: ");
        GridPane.setConstraints(nameTextField, 1, 0, 93,1);
        editPlaylistGrid.getChildren().add(nameTextField);

        // setting up add new playlist button
        final Button addButton = new Button("+");
        addButton.setOnAction(event ->
        {
            final String newPlaylistName = nameTextField.getText();
            if (newPlaylistName != null && !newPlaylistName.equals("") && !playlistHashMap.keySet().contains(newPlaylistName))
            {
                playlistHashMap.put(newPlaylistName, new ArrayList<>());
                allPlaylistNames.add(newPlaylistName);
                Collections.sort(allPlaylistNames);
                showEditSinglePlaylistScene(newPlaylistName);
            }
        });
        GridPane.setConstraints(addButton, 94, 0);
        editPlaylistGrid.getChildren().add(addButton);

        // setting up all playlists listview
        Collections.sort(allPlaylistNames);
        ListView<String> allPlaylistsListView = new ListView<>(allPlaylistNames);
        GridPane.setConstraints(allPlaylistsListView, 0, 1, 95, 4);
        editPlaylistGrid.getChildren().add(allPlaylistsListView);

        // setting up remove playlist button
        final Button removeButton = new Button("--");
        removeButton.setOnAction(event ->
        {
            final String playlistToRemove = allPlaylistsListView.getSelectionModel().getSelectedItem();
            if (playlistToRemove != null)
            {
                final Alert deleteAlert = new Alert(Alert.AlertType.CONFIRMATION, "bruh u sure bout deletin " + playlistToRemove + "???", ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
                deleteAlert.showAndWait();
                if (deleteAlert.getResult() == ButtonType.YES)
                {
                    playlistHashMap.remove(playlistToRemove);
                    allPlaylistNames.remove(playlistToRemove);
                }
            }
        });
        GridPane.setConstraints(removeButton, 0, 0);
        editPlaylistGrid.getChildren().add(removeButton);

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
            final Alert saveAlert = new Alert(Alert.AlertType.CONFIRMATION, "save dis shit bruh????", ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
            saveAlert.showAndWait();
            if (saveAlert.getResult() == ButtonType.YES)
            {
                playlistHashMap.remove(playlistName);
                playlistHashMap.put(playlistName, new ArrayList<>(playlistTracks));
                saveAllPlaylists();
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

        // setting up search box for all tracks
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

    private boolean userExistsWithName(Channel channelToServer, String name)
    {
        ChannelSftp sftp = (ChannelSftp) channelToServer;
        final Vector<ChannelSftp.LsEntry> usernameEntries;
        try { usernameEntries = sftp.ls(rootDir); }
        catch (SftpException ex) { throw new RuntimeException(ex); }

        for (ChannelSftp.LsEntry lsEntry : usernameEntries)
        {
            if (lsEntry.getFilename().equals(name)) { return true; }
        }
        return false;
    }

    private void saveAllPlaylists()
    {
        // saving all playlists to file, with updates. each line in format: playlistName:song1.mp3;song2.mp3;song3.mp3 ...
        String writeInFile = "";
        for (Object objectEntry : playlistHashMap.entrySet())
        {
            final Map.Entry item = (Map.Entry) objectEntry;
            final ArrayList<String> currentPlaylistContents = (ArrayList<String>) item.getValue();
            writeInFile += item.getKey() + ":";
            for (int i = 0; i < currentPlaylistContents.size(); i++)
            {
                writeInFile += currentPlaylistContents.get(i);
                writeInFile += ((i + 1 == currentPlaylistContents.size()) ? "\n" : ";");
            }
            final byte[] password = new byte[]{(byte) 0x65, (byte) 0x6e, (byte) 0x75, (byte) 0x6d, (byte) 0x61, (byte) 0x45, (byte) 0x6c, (byte) 0x69, (byte) 0x5f, (byte) 0x73};
            sftpWriter(rootDir + "/" + username + "/Playlists/playlists.txt", writeInFile);
        }
    }

    private Session sessionToServer()
    {
        final JSch jSch = new JSch();
        final Session session;
        final byte[] password = new byte[]{(byte) 0x65, (byte) 0x6e, (byte) 0x75, (byte) 0x6d, (byte) 0x61, (byte) 0x45, (byte) 0x6c, (byte) 0x69, (byte) 0x5f, (byte) 0x73 };
        final Channel channel;

        try { session = jSch.getSession("pi", "www.musicmanager.duckdns.org", 22); }
        catch (JSchException ex) { throw new RuntimeException(ex); }

        session.setPassword(password);
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);

        return session;
    }

    private void uploadFilesToServer(List<String> sourcePaths, String destDirectory)
    {
        final Session session = sessionToServer();
        try { session.connect(); }
        catch (JSchException ex) { throw new RuntimeException(ex); }

        final Channel channel;
        try
        {
            channel = session.openChannel("sftp");
            channel.connect();
        }
        catch (JSchException ex) { throw new RuntimeException(ex); }

        ChannelSftp sftp = (ChannelSftp) channel;

        sourcePaths.forEach(sourcePath ->
        {
            File file = new File(sourcePath);
            try
            {
                System.out.println(sourcePath);
                String destName = destDirectory + file.getName().replaceAll(" ", "_");
                System.out.println(destName);
                sftp.put(sourcePath, destName, new UploadProgressMonitor());
            }
            catch (SftpException ex) { throw new RuntimeException(ex); }
        });
        sftp.exit();
        channel.disconnect();
        session.disconnect();
    }

    private void removeFilesFromServer(List<String> fileNames, String directory)
    {
        final Session session = sessionToServer();
        try { session.connect(); }
        catch (JSchException ex) { throw new RuntimeException(ex); }

        final Channel channel;
        try
        {
            channel = session.openChannel("sftp");
            channel.connect();
        }
        catch (JSchException ex) { throw new RuntimeException(ex); }

        ChannelSftp sftp = (ChannelSftp) channel;
        try
        {
            sftp.cd(directory);
        }
        catch (SftpException ex) { throw new RuntimeException(ex); }

        fileNames.forEach(fileName ->
        {
            try { sftp.rm(fileName + ".mp3"); }
            catch (SftpException ex) { throw new RuntimeException(ex); }
        });

        sftp.exit();
        channel.disconnect();
        session.disconnect();
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
        try { url = new URL(rootURL + username + "/Playlists/playlists.txt"); }
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

    private void sftpWriter(final String destination, final String toWrite)
    {
        final JSch jSch = new JSch();
        final Session session;
        final Writer writer;
        final byte[] password = new byte[]{(byte) 0x65, (byte) 0x6e, (byte) 0x75, (byte) 0x6d, (byte) 0x61, (byte) 0x45, (byte) 0x6c, (byte) 0x69, (byte) 0x5f, (byte) 0x73 };
        try
        {
            session = jSch.getSession("pi", "www.musicmanager.duckdns.org", 22);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setPassword(password);
            session.connect();

            Channel channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp channelSftp = (ChannelSftp) channel;
            writer = new OutputStreamWriter(channelSftp.put(destination, ChannelSftp.OVERWRITE), "utf-8");
            writer.write(toWrite);
            writer.close();
            channelSftp.exit();
            session.disconnect();
        }
        catch (JSchException | IOException | SftpException ex) { throw new RuntimeException(ex); }
    }

    private void closeAll()
    {
        try { Main.super.stop(); }
        catch (Exception ex) { throw new RuntimeException(ex); }
        Platform.exit();
        System.exit(0);
    }

    public static void main(String[] args) throws InterruptedException
    {
        launch(args);
    }
}
