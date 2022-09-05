package com.example.scounteratest2;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Playlist
{
    // static variables
    static private String activePlaylistName = "";
    static private final String TAG = "Playlist";
    static private Context mAppContext = null;
    static public List<Playlist> data = new ArrayList<>();  // all playlists
    static boolean shuffleIsOn = false;
    static int currentFilter = 0;

    private String mPlaylistName = "";
    private boolean mUserPlaylist = true;
    private int mFolderIndex = -1; // -1 for playlist that is not saved yet
    private List<Song> mSongs = new ArrayList<>();  // here we store all data
    private List<List<Integer>> mSongIndexes = new ArrayList<>();  // for shuffle & filter indexes
    private boolean [] mShuffleStarted = new boolean[5];

    public void toggleShuffle()
    {
        shuffleIsOn = !shuffleIsOn;
        
        if(shuffleIsOn)
            shuffle();
        else
            unshuffle();
    }
    
    public void shuffle() {
        if(!mShuffleStarted[currentFilter]) {
            List <Integer> randomIntegers = generateRandomIntegers(0, mSongs.size() -1);  // generate new order

            for(int i=0; i < mSongs.size(); i++) 
                mSongs.get(i).order.set(currentFilter, randomIntegers.get(i));
            
            mShuffleStarted[currentFilter] = true;
        }
        
        sortByShuffleAll();
    }
    
    public void unshuffle() {
        sortByIndexNorm();
    }
    
    public void fixIndexes(int filter) {
        switch (filter) {
            case 0:
                sortByIndexNorm();
                for(int i=0; i < mSongs.size(); i++)
                    mSongs.get(i).setIndexNorm(i);
                break;
        }
    }

    public int folderIndex()
    {
        return mFolderIndex;
    }

    public void setFolderIndex(int mFolderIndex)
    {
        this.mFolderIndex = mFolderIndex;
    }
 
    
    public Playlist(String playlistName, boolean userPlaylist)
    {
        this.mPlaylistName = playlistName;
        this.mUserPlaylist = userPlaylist;
    }

    public static void setmAppContext(Context mAppContext)
    {
        Playlist.mAppContext = mAppContext;
    }

    static private int getNewUserPlaylistIndex() {
        File file = new File(mAppContext.getFilesDir() + "/playlists/user");
        if(!file.exists()) 
            return 0;
        
        File [] dirNames = file.listFiles();
        if (dirNames != null && dirNames.length == 0) 
            return 0;

        return AllSong.toInt(dirNames[dirNames.length -1].getName())  +1;
    }
    
    public void addSong(Song newSong) {
        newSong.setIndexNorm(mSongs.size());
        mSongs.add(newSong);
    }
    
    
    static public void loadAllPlaylists() {
        data.clear();
//        List <String> lines = AvtemFile.loadLinesFromFile("/playlists/app/All Songs", 
//                "data.txt");
        
        Playlist allSongsPl = new Playlist("All Songs", false);
        allSongsPl.setFolderIndex(0);
    }
    
    public void load() {
        String dirPath = "/playlists/app/" + mPlaylistName;
        if(mUserPlaylist) {
            if(mFolderIndex == -1)
                mFolderIndex = getNewUserPlaylistIndex();
            
            dirPath = "/playlists/user/" + mFolderIndex + '/';
        }
        
        List<String>  listInfo = AvtemFile.loadLinesFromFile(dirPath, "info.txt");
        
        mSongs.clear();
        List<String> listData = AvtemFile.loadLinesFromFile(dirPath, "data.txt");
        if(listData == null)
            return;
        
        for(int i=0; i < listData.size(); i++) {
            String[] songData = listData.get(i).split(AllSong.SEPARATOR+"");
            
            if(songData.length < 5)
                return;
            mSongs.add(new Song(songData[0],        // artist 
                        songData[1],                // title
                        Long.parseLong(songData[2]), // duration in secs
                        songData[3],                // path
                        Integer.parseInt(songData[4]))); // indexNorm
        }
    }
    
    public void save() {
        String dirPath = "/playlists/app/" + mPlaylistName;
        if(mUserPlaylist) {
            if(mFolderIndex == -1)
                mFolderIndex = getNewUserPlaylistIndex();
            
            dirPath = "/playlists/user/" + mFolderIndex + '/';
        }
        
        List<String> listInfo = new ArrayList<>();   // write all info
        listInfo.add("<name>" + mPlaylistName);
        listInfo.add("<songcount>" + mSongs.size());
        StringBuilder shuffleStarted = new StringBuilder("<shuffleStarted>");
        for(int i=0; i < 5; i++) {
            shuffleStarted.append(mShuffleStarted[i] ? "1" : "0");
        }
        listInfo.add("### END OF THE INFO");
        AvtemFile.saveLinesToFile(dirPath, "info.txt", listInfo);

        List<String> listSongs = new ArrayList<>();   // write all songs
        for(int i=0; i < mSongs.size(); i++) {
            String line = mSongs.get(i).artist() + AllSong.SEPARATOR;
            line += mSongs.get(i).title() + AllSong.SEPARATOR;
            line += String.valueOf(mSongs.get(i).durInSec()) + AllSong.SEPARATOR;
            line += mSongs.get(i).path() + AllSong.SEPARATOR;
            line += (mSongs.get(i).indexNorm() +"") + AllSong.SEPARATOR;
            
            listSongs.add(line);
        }
        
        AvtemFile.saveLinesToFile(dirPath, "data.txt", listSongs);
    }

    public List<Song> getSongs()
    {
        return mSongs;
    }
    
    public void sortByIndexNorm() {
        Collections.sort(mSongs, new Comparator<Song>()
        {
            @Override
            public int compare(Song o1, Song o2)
            {
                int result = -1; // o1.indexNorm < o2.indexNorm
                if(o1.indexNorm() > o2.indexNorm())
                    result = 1;
                
                return result;
            }
        });
    }

    public void sortByShuffleAll() {
        Collections.sort(mSongs, new Comparator<Song>()
        {
            @Override
            public int compare(Song o1, Song o2)
            {
                int result = -1; // o1.shuffle[0] < o2.shuffle[0]
                if(o1.order.get(0) > o2.order.get(0))
                    result = 1;

                return result;
            }
        });
    }
    
    static public List<Integer> generateRandomIntegers(int firstIndex, int lastIndex) {
        List <Integer> integerList = new ArrayList<>();
        for(int i=firstIndex; i <= lastIndex; i++)
            integerList.add(i);
        
        Collections.shuffle(integerList);
        
        return integerList;
    }

    public void putSongs(List<Song> songs)
    {
        mSongs = songs;
    }
}
