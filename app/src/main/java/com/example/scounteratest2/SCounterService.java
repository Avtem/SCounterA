package com.example.scounteratest2;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import static android.content.ContentValues.TAG;

public class SCounterService extends Service implements
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener
{
    public static final String BROADCAST_FILTER = "MESSAGE_FROM_SERVICE";
    private static Song playingSong = null;
    private boolean mIterated = false;
    static public Playlist currentPlaylist = null;
    static public List <Playlist> playlists = null;
    
    public void setPos(int newPosition)
    {
        if(mPlayer != null && newPosition != mPlayer.getCurrentPosition())
            mPlayer.seekTo(newPosition);
    }

    enum PlayerState { Playing, Paused, Stopped }
    
    private MediaPlayer mPlayer;
    public static Song getPlayingSong() {
        return playingSong;
    }
    private PlayerState mState = PlayerState.Stopped;
    private void setNewPlayerState(PlayerState newState) {
        mState = newState;
        
        switch (newState) {  // "emit the signal" (from Qt framework)
            case Playing:
                sendBroadcastToMainActivity("newPlayerState", "Playing");
                break;
            case Paused:
                sendBroadcastToMainActivity("newPlayerState", "Paused");
                break;
            case Stopped:
                sendBroadcastToMainActivity("newPlayerState", "Stopped");
                break;
        }
    }
    private final IBinder musicBinder = new MusicBinder();
    public class MusicBinder extends Binder {
        SCounterService getService() {
            return SCounterService.this;
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if(intent.hasExtra("songPath")) {
            String passedPath;
            passedPath = intent.getStringExtra("songPath");
            playPauseResume(passedPath);
            playingSong = new Song(passedPath, -1);
        }
        
        String topField = "SCounter";
        String bottomField = "";
        if(playingSong != null) {
            topField = playingSong.artist();
            bottomField = playingSong.title();
        }
        Notification notification = createNotification(topField, bottomField);
                
        startForeground(1, notification);
        return START_NOT_STICKY;
    }
    
    @Override
    public void onCreate()
    {
        Toast.makeText(this, "SERVICE CREATED!", Toast.LENGTH_SHORT).show();
        
        Loader.appContext = getApplicationContext();
        AvtemFile.appContext = getApplicationContext();
        initPlayer();  // create the player!
        Log.i(TAG, "onCreate: player initialized");
        initAllSongsClass(); // make allSongs stuff
        Log.i(TAG, "onCreate: allsongs initialized");
        initPlaylist();
        Log.i(TAG, "onCreate: playlist initialized");
        PlayDatesInfo.loadData("PlayDatesInfo.big");
        Log.i(TAG, "onCreate: PlayDatesInfo initialized");
//        PlayDatesInfo.loadData("PlayDatesInfoSes.big");
        
        super.onCreate();
    }
    
    private void initPlaylist() {
        Playlist.setmAppContext(getApplicationContext());

        currentPlaylist = new Playlist("All Songs", false);
        Playlist.loadAllPlaylists();
//        currentPlaylist = new Playlist("playlistoo 1");
//        currentPlaylist.setFolderIndex(0);

//        Loader loader = new Loader();  // load last playlist
//        loader.extractAllSongs();
//        currentPlaylist.getSongs().clear();
//        for(String filePath : loader.allFiles) {
//            currentPlaylist.getSongs().add(new Song(filePath));
//        }
//        
//        currentPlaylist.save();
    }
    
    private void initAllSongsClass() {
        AllSong.setmAppContext(getApplicationContext());
        if(!AllSong.dataLoaded)
            AllSong.loadFromFile("AllSongsPC.txt", AllSong.data);
        // load android session
        AllSong.loadFromFile("AllSongsAndrSes.txt", AllSong.dataSes);
        AllSong.appendDataSesListenings(AllSong.data, AllSong.dataSes);
    }
    
    public void initPlayer() {
        if(mPlayer == null)
            mPlayer = new MediaPlayer();
        mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnCompletionListener(this);
        mPlayer.setOnErrorListener(this);
        timer.start();
    }
    
    @Override
    public void onDestroy()
    {
        Toast.makeText(this, "Service: i have been destroyed!", Toast.LENGTH_SHORT).show();
        stopPlayer();
        
        // save all android listenings 
        AllSong.saveToFile("AllSongsAndrSes.txt", AllSong.dataSes);
        PlayDatesInfo.saveData("PlayDatesInfo.big");
        stopForeground(true);  // remove notification
        
        super.onDestroy();
    }

    public void tryIncrementSkipCount()
    {
        if(mIterated == true || playingSong == null || mPlayer == null 
            || mPlayer.getCurrentPosition() < 500)
            return;
        
        AllSong.incrementSkipCount(playingSong.artist(), playingSong.title());
    }

    public void playPauseResume(String plSongFilePath) {
        if(playingSong == null || !playingSong.path().equals(plSongFilePath)) {
            tryIncrementSkipCount();    // playingSong && mPlayer must not be null
            playFromBeginning(plSongFilePath); 
            return;
        }
            
        switch (mState) {
            case Paused:
                resume();
                break;
            case Playing:
                pause();
                break;
        }
    }
    
    CountDownTimer timer = new CountDownTimer(60 *1000, 300)
    {
        @Override
        public void onTick(long millisUntilFinished)
        {
            if(mPlayer == null)   // send position in milliseconds to the seekbar
                return;
            
            float currPos = mPlayer.getCurrentPosition();
            if(currPos < 0)
                currPos = 0;

            sendBroadcastToMainActivity("currentSongPosInSecs",
                    (currPos /1000) + "");
            
            if(!mIterated
                && playingSong != null && playingSong.durInSec() != 0
                && currPos / (float)(playingSong.durInSec() *1000) > 0.7)
            {
                mIterated = true;
                AllSong.incrementPlayCount(playingSong.artist(), playingSong.title());
            }
        }
    
        @Override
        public void onFinish()
        {
            timer.start(); // endless timer!
        }
    };
    
    // SEND INFO TO THE ACTIVITY!!!
    Intent broadcastIntent = new Intent(BROADCAST_FILTER);
    public void sendBroadcastToMainActivity(String messageName, String message) {
        broadcastIntent.putExtra(messageName, message);
        sendBroadcast(broadcastIntent);
    }
    
    public void pause() {
        mPlayer.pause();
        setNewPlayerState(PlayerState.Paused);
    }
    public void resume() {
        mPlayer.start();
        setNewPlayerState(PlayerState.Playing);
    }
    
    public void playFromBeginning(String filePath)
    {
        if(filePath.length() == 0) {
            Log.e(TAG, "playFromBeginning: empty song file path!");
            return;
        }

        if(mPlayer != null)
            mPlayer.reset();
        else
            initPlayer();
        
        try {
            mPlayer.setDataSource(getApplicationContext(), Uri.parse(filePath));
        } catch (IOException e) {
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }
        
        setNewPlayerState(PlayerState.Playing);
        mPlayer.prepareAsync();
    }
    
    public void stopPlayer() {
        if(mPlayer != null) {
            tryIncrementSkipCount();  // playingSong && mPlayer must not be null 
            mPlayer.release();
            mPlayer = null;
            playingSong = null;  
            setNewPlayerState(PlayerState.Stopped);
            timer.cancel();
            sendBroadcastToMainActivity("currentSongDurInSecs", "0");
            sendBroadcastToMainActivity("currentSongPosInSecs", "0");
        }
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return musicBinder;
    }
    
    public Notification createNotification(String title, String text) {
        Intent intent = new Intent(this, MainActivity.class);   // go back to the main activity on tap
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                intent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,
                getString(R.string.notificationMainID))
                                 .setSmallIcon(R.drawable.ic_notification)
                                 .setContentTitle(title)
                                 .setContentText(text)
                                 .setContentIntent(pendingIntent)
                                 .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        
        Notification notification = builder.build();
        
        return notification;
    }
    
    @Override
    public boolean onUnbind(Intent intent)
    {
//        stopPlayer();  // isitsafe? i don't want to stop player! Don't i?
        return false;
    }
    
    @Override
    public void onCompletion(MediaPlayer mp)
    {
        stopPlayer(); // release player ?? or just stop?
        PlaylistActivity.tryPlaySongWithIndex(RecyclerViewAdapter.getmPlayingIndex() +1);
//        stopSelf();   // terminate the server!
    }
    
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra)
    {
        return false;
    }
    
    @Override
    public void onPrepared(MediaPlayer mp)
    {
        mp.start();
        mIterated = false; // reset the value
        if(PlaylistActivity.recyclerView != null)
            PlaylistActivity.recyclerView.getAdapter().notifyDataSetChanged();
    }
}
