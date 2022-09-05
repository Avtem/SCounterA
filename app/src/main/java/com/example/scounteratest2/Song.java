package com.example.scounteratest2;

import android.media.MediaMetadataRetriever;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Song
{
    private String mArtist;  // song for recycler view
    private String mTitle;
    private String mAlbum;
    private String mYear;
    private long mDurInSec;
    
    private String mPath;
    private int mIndexNorm = -1;                      // normal playlist
    private int mIndexFiltered;                       // normal playlist with filter
    public List<Integer> order = new ArrayList<>();
    
    private static final String TAG = "Song";

    public String path() {
        return mPath;
    }
    public String artist() {
        return mArtist;
    }
    public String title() {
        return mTitle;
    }
    public String album() {
        return mAlbum;
    }
    public String year() {
        return mYear;
    }
    public String durStr() { return MainActivity.toTimeStr((int) (mDurInSec *1000));}
    public long durInSec() { return mDurInSec; }
    public int indexNorm() { return mIndexNorm; }
    
    public void setIndexNorm(int indexNorm) {
        mIndexNorm = indexNorm;
    }
    
    static public MediaMetadataRetriever mmr = new MediaMetadataRetriever();
    
    Song(String songPath, int indexNorm) {  // constructor
        for(int i=0; i < 5; i++)
            order.add(0);
        mIndexNorm = indexNorm;
        
        try {
//            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(songPath);
            mPath = songPath;
            mTitle = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            mArtist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            mYear = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR);
            mAlbum = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            mDurInSec = Long.parseLong(mmr.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION)) /1000;
            
        } catch (Exception e) {
            Log.e("AvtemSong", "mmr.SetDataSource failed. Wrong path: "+songPath
                    + "  or something other happened");
            e.printStackTrace();
        }
    }

    // constructor for loading
    public Song(String mArtist, String mTitle, long mDurInSec, String mPath, int mIndexNorm)
    {
        for(int i=0; i < 5; i++)
            order.add(0);
        this.mArtist = mArtist;
        this.mTitle = mTitle;
        this.mDurInSec = mDurInSec;
        this.mPath = mPath;
        this.mIndexNorm = mIndexNorm;
    }

    @Override
    public boolean equals(@Nullable Object obj)
    {
        if(this == null || obj == null || obj.getClass() != this.getClass())
            return false;

        Song secondSong = (Song) obj;

        return ( this.mArtist.equalsIgnoreCase(secondSong.mArtist)
                && this.mTitle.equalsIgnoreCase(secondSong.mTitle) );
    }
}
