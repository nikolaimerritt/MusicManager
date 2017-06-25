package sample;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;
import java.io.InputStream;

class MusicPlayer
{
    private final Player player;
    private final Object playerLock = new Object();
    int playerStatus;

    MusicPlayer(final InputStream inputStream) throws JavaLayerException
    {
        this.player = new Player(inputStream);
        playerStatus = PlayerStatus.NOT_STARTED;
    }

    // starts playback. resumes if paused
    public void play() throws JavaLayerException
    {
        synchronized (playerLock)
        {
            switch (playerStatus)
            {
                case PlayerStatus.NOT_STARTED:
                    final Runnable runnable = this::playInternal;
                    final Thread playerThread = new Thread(runnable);
                    playerThread.setPriority(Thread.MAX_PRIORITY);
                    playerStatus = PlayerStatus.PLAYING;
                    playerThread.start();
                    break;
                case PlayerStatus.PAUSED:
                    resume();
                    break;
                default:
                    break;
            }
        }
    }

    // pauses playback. returns true if successfully paused
    void pause()
    {
        synchronized (playerLock)
        {
            if (playerStatus == PlayerStatus.PLAYING)
            {
                playerStatus = PlayerStatus.PAUSED;
            }
        }
    }

    // resumes playback.returns true if new state is PLAYING
    void resume()
    {
        synchronized (playerLock)
        {
            if (playerStatus == PlayerStatus.PAUSED)
            {
                playerStatus = PlayerStatus.PLAYING;
                playerLock.notifyAll();
            }
        }
    }

    // stops playback. if not playing, does nothing
    void stop()
    {
        synchronized (playerLock)
        {
            playerStatus = PlayerStatus.FINISHED;
            playerLock.notifyAll();
        }
    }

    private void playInternal()
    {
        while (playerStatus != PlayerStatus.FINISHED)
        {
            try
            {
                if (!player.play(1))
                {
                    break;
                }
            }
            catch (final JavaLayerException ex)
            {
                break;
            }

            // check if paused or terminated
            synchronized (playerLock)
            {
                while (playerStatus == PlayerStatus.PAUSED)
                {
                    try { playerLock.wait(); }
                    catch (final InterruptedException ex) { break; } // terminates player
                }
            }
        }
        closePlayer();
    }

    void closePlayer() // closes player, regardless of current state
    {
        synchronized (playerLock) { playerStatus = PlayerStatus.FINISHED; }
        try { player.close(); }
        catch (final Exception ignored) {  } // we're ending anyway. ignore
    }
}