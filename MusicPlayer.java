package sample;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

class QueuePlayer
{
    private Player player = null;
    private volatile int playerStatus = PlayerStatus.NOT_STARTED;
    protected QueuePlayer() {}

    private synchronized int getFileSize(final URL url)
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
        catch (IOException ex) { throw new RuntimeException(ex); }
    }

    private synchronized <T> ArrayList<T> shiftLeftBy(ArrayList<T> arrayList, int shiftBy)
    {
        for (int i = 0; i < shiftBy; i++)
        {
            T firstElement = arrayList.get(0);
            arrayList.remove(firstElement);
            arrayList.add(firstElement); // taking it out of the front, adding it back to the end
        }
        return arrayList;
    }

    private synchronized Player setUpPlayer(final double skipMultiplier) throws JavaLayerException, IOException
    {
        final String songName = Main.tracksQueue.get(0).replaceAll(" ", "%20");
        final URL url = new URL(Main.rootURL + "AllTracks/" + songName+ ".mp3");
        final BufferedInputStream inputStream = new BufferedInputStream(url.openStream());

        if (skipMultiplier > 0)
        {
            final long framesToSkip = (long) (getFileSize(url) * skipMultiplier);
            inputStream.skip(framesToSkip);
        }

        final Player player = new Player(inputStream);
        return player;
    }

    private synchronized void playQueueInternal(final double skipMultiplier) throws JavaLayerException, IOException
    {
        player = setUpPlayer(skipMultiplier);
        playerStatus = PlayerStatus.PLAYING;
        while (playerStatus == PlayerStatus.PLAYING)
        {
            if (!player.play(1)) // got to end of song
            {
                System.out.println("Reached end of " + Main.tracksQueue.get(0) + "...");
                Main.tracksQueue = shiftLeftBy(Main.tracksQueue, 1);
                playQueueInternal(0);
            }
        }
    }

    protected synchronized void playQueue()
    {
        Runnable queuePlayerRunnable = () ->
        {
            try { playQueueInternal(0); }
            catch (JavaLayerException | IOException ex) { throw new RuntimeException(ex); }
        };
        Thread queuePlayerThread = new Thread(queuePlayerRunnable);
        queuePlayerThread.start();
    }

    protected synchronized void pauseQueue()
    {
        if (playerStatus == PlayerStatus.PLAYING) { playerStatus = PlayerStatus.PAUSED; }
    }

    protected synchronized void resumeQueue()
    {
        if (playerStatus == PlayerStatus.PAUSED) { playerStatus = PlayerStatus.PLAYING; }
    }

    protected synchronized void skipCurrentTrack(final double skipMultiplier) throws JavaLayerException, IOException
    {
        playerStatus = PlayerStatus.PAUSED;
        playQueueInternal(skipMultiplier);
    }
}
