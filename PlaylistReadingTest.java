import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Main
{
    public static void main(String[] args)
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
            String line = scanner.nextLine();
            String playlistName = line.split(":")[0]; // bit before ':'
            String[] itemsInPlaylist = line.split(":")[1].split(";");
            System.out.println("name|" + playlistName);
            for (String item : itemsInPlaylist) { System.out.println("item|" + item); }
        }
    }
}
