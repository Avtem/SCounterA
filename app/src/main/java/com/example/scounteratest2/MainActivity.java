package com.example.scounteratest2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity
{
    private ImageButton btnPlay;
    private SeekBar seekBar;
    private TextView textDur;
    private TextView textPos;
    private TextView textArtist;
    private TextView textTitle;
    private TextView textAlbum;
    private TextView textYear;
    private TextView textPlayCount;
    private TextView textSkipCount;
    private Intent playIntent;  // intent for the music player service
    private SCounterService playerService;  // binding between service and the activity
    private boolean playerServiceBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    
        btnPlay = findViewById(R.id.btnPlay);
        textDur = findViewById(R.id.textDur);
        textPos = findViewById(R.id.textPos);
        textArtist = findViewById(R.id.textArtist);
        textTitle = findViewById(R.id.textTitle);
        textAlbum = findViewById(R.id.textAlbum);
        textYear = findViewById(R.id.textYear);
        textPlayCount = findViewById(R.id.textViewPlayCount);
        textSkipCount = findViewById(R.id.textViewSkipCount);
        seekBar = findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                if(fromUser && playerService != null)
                    playerService.setPos(progress * 1000);
                    
                textPos.setText(toTimeStr(progress *1000));
            }
    
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
    
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        playIntent = new Intent(this, SCounterService.class);  // intent for the service
        
        setSeekBarMax(5 *60 * 1000);

        startService(playIntent);
        if(!playerServiceBound) {  // bind service to activity
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
        }
    }
    
    public void setSeekBarMax(final int msecsCount) {  // change song duration for the seekBar
        seekBar.setMax(msecsCount /1000);              // it means that new song has arrived.
        seekBar.setProgress(0);                        // thus, we are resetting other stuff
        textPos.setText("00:00");
        textDur.setText(toTimeStr(msecsCount));
    }
    
    public void playPrev(View v) {
        PlaylistActivity.tryPlaySongWithIndex(RecyclerViewAdapter.getmPlayingIndex() -1);
    }
    
    public void playNext(View v) {
//        PlayDatesInfo.loadData("PlayDatesInfo.big");
//        Toast.makeText(this, "loaded", Toast.LENGTH_SHORT).show();
//        PlayDatesInfo.saveData("PlayDatesInfoAndroid.big");
//        Toast.makeText(this, "saved", Toast.LENGTH_SHORT).show();
//
          PlaylistActivity.tryPlaySongWithIndex(RecyclerViewAdapter.getmPlayingIndex() +1);
    }
    
    public void exit(View v) {   // onBtnStopPressed = stops the music player service
        if(playerService != null) 
            playerService.onDestroy();
        System.exit(0);        
    }
    
    public void setSongInfoTextViews() {
        if(playerService == null) 
            return;
        
        Song playingSong = SCounterService.getPlayingSong();
        if(playingSong == null)
        {
            setDefaultFields();
            return;
        }

        if(playingSong.artist() == null) textArtist.setText("No artist");
        else    textArtist.setText(playingSong.artist());
        if(playingSong.title() == null) textTitle.setText("No title");
        else textTitle.setText(playingSong.title());
        if(playingSong.album() == null) textAlbum.setText("No album");
        else   textAlbum.setText(playingSong.album());
        if(playingSong.year() == null)  textYear.setText("No year");
        else   textYear.setText(playingSong.year() + "");

        AllSong as = AllSong.exists(playingSong.artist(), playingSong.title(), AllSong.data).allSong;
        if(as == null) {
            textPlayCount.setText("0");
            textSkipCount.setText("0");
        }
        else {
            textPlayCount.setText(as.playCount() + "");
            textSkipCount.setText(as.skipCount() + "");
        }
    }
    
    public void setDefaultFields() {
        textArtist.setText("Artist");
        textTitle.setText("Title");
        textAlbum.setText("Album");
        textYear.setText("Year");

        textPlayCount.setText("0");
        textSkipCount.setText("0");
        
        textDur.setText("--:--");
        textPos.setText("--:--");
        seekBar.setProgress(0);
        seekBar.setMax(0);
    }
    
    public void playPauseResume(View v) // for "btnPlay" Button.
    {
        if(!playerServiceBound)   // bind service to activity
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);

        if(playerService != null && SCounterService.getPlayingSong() != null)
            playerService.playPauseResume(SCounterService.getPlayingSong().path());
    }

    private static final String TAG = "MainActivity";

    float xDown = 0f, xUp;
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                xDown = event.getX();
                break;

            case MotionEvent.ACTION_UP: {
                xUp = event.getX();
                if (xUp - xDown > 40) {
                    Intent intent = new Intent(this, PlaylistActivity.class);
                    int function = Enums.LOAD_LAST_PLAYLIST;
                    if(SCounterService.currentPlaylist != null
                       && SCounterService.currentPlaylist.getSongs().size() != 0)
                        function = Enums.RESTORE_LOADED;
                        
                    intent.putExtra("functionName", function);
                    startActivity(intent);
                }
            }
            break;
        }

        return false;
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        registerReceiver(mReceiver, new IntentFilter(SCounterService.BROADCAST_FILTER));
        if(!playerServiceBound)   // bind service to activity
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
    }
    
    @Override
    protected void onDestroy()
    {
        Toast.makeText(this, "MAIN ACTIVITY HAS BEEN DESTROYED!", Toast.LENGTH_SHORT).show();
        unregisterReceiver(mReceiver);
        playerService = null;
        super.onDestroy();
    }
    
    
    private ServiceConnection musicConnection = new ServiceConnection()     // connect server & activity
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            // get binder
            SCounterService.MusicBinder binder = (SCounterService.MusicBinder) service;
            // cast to service
            playerService = binder.getService();
            // update boolean
            playerServiceBound = true;
        }
    
        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            // update boolean
            playerServiceBound = false;
        }
    };
    
    @Override
    public void onBackPressed(){  // don't destroy this activity on BAck press
        moveTaskToBack(true);
    }
    
    
    // working functions
    public static final String EXTRA_MESSAGE = "com.Avtem.MESSAGE";
    
    // receive stuff from the server!!
    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String extraStr;
            if(intent.hasExtra("newPlayerState")) {
                extraStr = intent.getStringExtra("newPlayerState");
        
                if(extraStr.equals("Playing"))
                    btnPlay.setImageResource(R.drawable.pause);
                else
                    btnPlay.setImageResource(R.drawable.play);
    
                setSongInfoTextViews();
                if(playerService == null || playerService.getPlayingSong() == null)
                    return;

//                textArtist.setText(playerService.getPlayingSong().artist());
//                textTitle.setText(playerService.getPlayingSong().title());
                int newDur = (int) playerService.getPlayingSong().durInSec();
                setSeekBarMax(newDur * 1000);
            }
            if(intent.hasExtra("currentSongPosInSecs")) {      // set currentPos(song pos)
                extraStr = intent.getStringExtra("currentSongPosInSecs");
            
                int newProgress = (int) Float.parseFloat(extraStr);
                if(seekBar.getProgress() != newProgress)
                    seekBar.setProgress(newProgress);
                
                textPos.setText(toTimeStr(newProgress *1000));
            }

        }
    };
    
    public static String toTimeStr(int millisecondsCount) {
        String result = "0";
        int minutes = millisecondsCount /1000 /60 ;
        int seconds = millisecondsCount /1000 %60 ;
        
        if(minutes < 10)
            result += minutes +"";
        else
            result = minutes +"";
        result += ':';
        
        if(seconds < 10)
            result += '0' + (seconds +"");
        else
            result += seconds +"";
        
        return result;
    }

    static String functionName = "functionName";

    public void extractAllFiles(View v) {
        Intent intent = new Intent(this, PlaylistActivity.class);
        intent.putExtra(functionName, Enums.EXTRACT_ALL);
        startActivity(intent);
    }
}
