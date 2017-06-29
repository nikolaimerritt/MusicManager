package sample;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
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

    private synchronized Player setUpPlayer(final double seekMultiplier) throws JavaLayerException, IOException
    {
        final String songName = Main.tracksQueue.get(0).replaceAll(" ", "%20");
        final URL url = new URL(Main.rootURL + "AllTracks/" + songName+ ".mp3");
        final BufferedInputStream inputStream = new BufferedInputStream(url.openStream());

        if (seekMultiplier > 0)
        {
            final long framesToSkip = (long) (getFileSize(url) * seekMultiplier);
            inputStream.skip(framesToSkip);
        }

        return new Player(inputStream);
    }

    private synchronized void playQueueInternal(final double seekMultiplier) throws JavaLayerException, IOException
    {
        final Player player = setUpPlayer(seekMultiplier);
        playerStatus = PlayerStatus.PLAYING;
        boolean isFinished;
        double framesDone = seekMultiplier * frameCounts.get(Main.tracksQueue.get(0) + ".mp3");
        double progress;

        while (playerStatus != PlayerStatus.FINISHED)
        {
            if (playerStatus == PlayerStatus.PLAYING)
            {
                isFinished = !player.play(1);
                if (isFinished)
                {
                    System.out.println("Reached end of " + Main.tracksQueue.get(0) + "...");
                    Main.tracksQueue = Main.shiftLeft(Main.tracksQueue);
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

    void playNewQueue(final double seekMultiplier)
    {
        synchronized (playerLock)
        {
            Runnable queuePlayerRunnable = () ->
            {
                try { playQueueInternal(seekMultiplier); }
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

    void seekCurrentTrack(final double seekMultiplier) throws JavaLayerException, IOException
    {
        playerStatus = PlayerStatus.FINISHED;
        playNewQueue(seekMultiplier); // will automatically be on current track
    }
}
