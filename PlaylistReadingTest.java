import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class Main
{
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

    public static void main(String[] args)
    {
        HashMap<String, String[]> playlistHashMap = getPlaylistHashMap();
        Iterator iterator = playlistHashMap.entrySet().iterator();
        while (iterator.hasNext())
        {
            Map.Entry entry = (Map.Entry) iterator.next();
            System.out.println("Key|" + entry.getKey() + "|");
            String[] songs = (String[]) entry.getValue();
            for (String song : songs) { System.out.println("song|" + song + "|"); }
            System.out.println();
        }
    }
}
