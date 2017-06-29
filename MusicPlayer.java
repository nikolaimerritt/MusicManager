package sample;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

class QueuePlayer
{
    private final Object playerLock = new Object();
    volatile int playerStatus = PlayerStatus.NOT_STARTED;
    private final HashMap<String, Integer> frameCounts = getFrameCounts();

    QueuePlayer() {}

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

    private synchronized HashMap<String, Integer> getFrameCounts()
    {
        HashMap<String, Integer> frameCounts = new HashMap<>();
        try
        {
            final URL url = new URL(Main.rootURL + "/Metadata/metadata.txt");
            final Scanner scanner = new Scanner(url.openStream());

            while (scanner.hasNextLine())
            {
                // each line is in format SongName.mp3, FrameCount
                String[] splitAtComma = scanner.nextLine().split(", ");
                frameCounts.put(splitAtComma[0], Integer.parseInt(splitAtComma[1]));
            }
        }
        catch (IOException ex) { throw new RuntimeException(ex); }
        return frameCounts;
    }

    private synchronized <T> ArrayList<T> shiftLeft(ArrayList<T> arrayList)
    {
        T firstElement = arrayList.get(0);
        arrayList.remove(firstElement);
        arrayList.add(firstElement);
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

        return new Player(inputStream);
    }

    private synchronized void playQueueInternal(final double skipMultiplier) throws JavaLayerException, IOException
    {
        final Player player = setUpPlayer(skipMultiplier);
        playerStatus = PlayerStatus.PLAYING;
        boolean isFinished;
        double framesDone = skipMultiplier * frameCounts.get(Main.tracksQueue.get(0) + ".mp3");
        double progress;

        while (playerStatus != PlayerStatus.FINISHED)
        {
            if (playerStatus == PlayerStatus.PLAYING)
            {
                isFinished = !player.play(1);
                if (isFinished)
                {
                    System.out.println("Reached end of " + Main.tracksQueue.get(0) + "...");
                    Main.tracksQueue = shiftLeft(Main.tracksQueue);
                    Main.updateMainListView = true;
                    System.out.println("Automatically moving to " + Main.tracksQueue.get(0) + "...");
                    playQueueInternal(0);
                }
                else
                {
                    framesDone++;
                    progress = framesDone / frameCounts.get(Main.tracksQueue.get(0) + ".mp3");
                    Main.progressBar.setProgress(progress);
                }
            }
        }
        System.out.println("Interrupting self...");
        player.close();
        Thread.currentThread().interrupt();
    }

    void playNewQueue(final double skipMultiplier)
    {
        synchronized (playerLock)
        {
            Runnable queuePlayerRunnable = () ->
            {
                try { playQueueInternal(skipMultiplier); }
                catch (JavaLayerException | IOException ex) { throw new RuntimeException(ex); }
            };
            Thread queuePlayerThread = new Thread(queuePlayerRunnable);
            if (playerStatus != PlayerStatus.NOT_STARTED) { stopQueue(); } // stops playing an already existing queue
            queuePlayerThread.start();
        }
    }

    void pauseQueue()
    {
        synchronized (playerLock)
        {
            if (playerStatus == PlayerStatus.PLAYING)
            {
                System.out.println("Pausing queue...");
                playerStatus = PlayerStatus.PAUSED;
            }
        }
    }

    void resumeQueue()
    {
        synchronized (playerLock)
        {
            if (playerStatus == PlayerStatus.PAUSED)
            {
                System.out.println("Resuming queue...");
                playerStatus = PlayerStatus.PLAYING;
                playerLock.notifyAll();
            }
        }
    }

    void stopQueue()
    {
        synchronized (playerLock)
        {
            playerStatus = PlayerStatus.FINISHED;
            playerLock.notifyAll();
        }
    }

    void skipCurrentTrack(final double skipMultiplier) throws JavaLayerException, IOException
    {
        playerStatus = PlayerStatus.FINISHED;
        playNewQueue(skipMultiplier); // will automatically be on current track
    }
}
