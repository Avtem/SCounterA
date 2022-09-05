package com.example.scounteratest2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import static com.example.scounteratest2.MainActivity.functionName;

public class PlaylistActivity extends AppCompatActivity
{
    private static final String TAG = "PlaylistActivity";
    static public RecyclerView recyclerView = null;
    public static TextView txtArtist;
    public static TextView txtTitle;
    static private ImageView btnShuffle;
    static private Parcelable recylerViewState;
    float xDown = 0f, xUp;
    float xRecViewDown = 0f, xRecViewUp;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        setContentView(R.layout.activity_playlist);
        
//        recyclerView.scrollToPosition();

        txtArtist = findViewById(R.id.txtTitle);
        txtTitle = findViewById(R.id.txtArtist);
        btnShuffle = findViewById(R.id.btnShuffle);
        
        final int function = getIntent().getIntExtra(functionName, Enums.EXTRACT_ALL);
        Loader loader = new Loader();

        switch (function) {
            case Enums.EXTRACT_ALL:
                loader.extractAllSongs();
                ArrayList<Song> songs = new ArrayList<>();
                
                List<String> paths = loader.allFiles;
                for(int i=0; i < paths.size(); i++) 
                    songs.add(new Song(paths.get(i), songs.size()));
                
                SCounterService.currentPlaylist = new Playlist("All Songs", false);
                SCounterService.currentPlaylist.putSongs(songs);
                SCounterService.currentPlaylist.save();
                
                initRecyclerView(songs);
                break;
            case Enums.LOAD_LAST_PLAYLIST:
                SCounterService.currentPlaylist.load();
                initRecyclerView((ArrayList<Song>) SCounterService.currentPlaylist.getSongs());
                break;
            case Enums.RESTORE_LOADED:
                initRecyclerView((ArrayList<Song>) SCounterService.currentPlaylist.getSongs());
                break;
        }
        
        syncBtnShuffle(Playlist.shuffleIsOn);
        if(recylerViewState != null)
             recyclerView.getLayoutManager().onRestoreInstanceState(recylerViewState);
    }

    private void syncBtnShuffle(boolean shuffleIsOn)
    {
        btnShuffle.setBackgroundColor(0xFF000000);     // black
        if(shuffleIsOn) 
            btnShuffle.setBackgroundColor(0xFF11FF00);  // set green color
    }

    public void shuffle(View v) {
        SCounterService.currentPlaylist.toggleShuffle();
        
        // change appearance of the button
        syncBtnShuffle(Playlist.shuffleIsOn);
        recyclerView.getAdapter().notifyDataSetChanged();
    }

    @Override
    protected void onDestroy()
    {
        recylerViewState = recyclerView.getLayoutManager().onSaveInstanceState();
        super.onDestroy();
    }

    public static Context context;

    public static void playSong(int indexInPlaylist) {
        Intent intent1 = new Intent(context, SCounterService.class);
        String songPath = RecyclerViewAdapter.getmData().get(indexInPlaylist).path();
        intent1.putExtra("songPath", songPath);
        context.startService(intent1);
        Song tempSong = new Song(songPath, -1);

        txtArtist.setText(tempSong.artist());
        txtTitle.setText(tempSong.title());
    }
    
    static public void tryPlaySongWithIndex(final int index) {
        if(RecyclerViewAdapter.getmData() == null ||
                index < 0 || index >= RecyclerViewAdapter.getmData().size())
            return;
        
        RecyclerViewAdapter.setmPlayingIndex(index);
        playSong(index);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                xDown = event.getX();
                break;

            case MotionEvent.ACTION_UP: {
                xUp = event.getX();
                if (xDown - xUp > 40) {
                    Intent intent = new Intent(this, MainActivity.class);
                    startActivity(intent);
                }
            }
            break;
        }

        return false;
    }

    private void initRecyclerView(ArrayList<Song> songs) {
        recyclerView = findViewById(R.id.recyclerView);
        RecyclerViewAdapter adapter = new RecyclerViewAdapter(songs);

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        recyclerView.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener()
        {
            public boolean onTouch(View v, MotionEvent event)
            {
                Toast.makeText(PlaylistActivity.this, "getting here!", Toast.LENGTH_SHORT).show();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        xRecViewDown = event.getX();
                        break;

                    case MotionEvent.ACTION_UP: {
                        xRecViewUp = event.getX();
                        if (xRecViewDown - xUp > 40) {
                            Intent intent = new Intent(null, MainActivity.class);
                            startActivity(intent);
                        }
                    }
                    break;
                }
                return false;
            }
        });
    }

}

