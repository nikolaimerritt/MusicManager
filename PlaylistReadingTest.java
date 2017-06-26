import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

public class Main
{
    public static void main(String[] args)
    {
	    final URL url;
	    final Scanner scanner;
	    try { url = new URL("http://musicmanager.duckdns.org/Playlists/playlist.txt"); }
	    catch (MalformedURLException ex) { throw new RuntimeException(ex); }
        try { scanner = new Scanner(url.openStream()); }
        catch (IOException ex) { throw new RuntimeException(ex); }

	    while (scanner.hasNextLine())
        {
            System.out.println(scanner.nextLine());
        }
    }
}
