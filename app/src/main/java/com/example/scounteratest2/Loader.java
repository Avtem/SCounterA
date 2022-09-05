package com.example.scounteratest2;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Loader
{
    private static final String TAG = "AVTEM.LoaderClass";

    public List<String> mRootDirs;    // "sdcard0/Music" etc. or "where to search songs?"
    public List<String> allFiles;      // path for every single song
    static public Context appContext; // unused 

    Loader()    // CONSTRUCTOR
    {
        mRootDirs = new ArrayList<>();
        allFiles = new ArrayList<>();

        mRootDirs.add(Environment.getExternalStorageDirectory().toString() + "/Music");
        mRootDirs.add("/storage/external_SD/Music");
    }

    public void extractAllSongs()
    {
        // searches throughout all set paths on the phone and extracts all info about
        // songs - duration, artist, listenings, last played date etc.

        Runnable runnable = new Runnable()
        {
            @Override
            public void run()
            {
                allFiles.clear();

                for(int i=0; i < mRootDirs.size(); i++) {  // extract songs from all root dirs
                    List<String> files = loadAllFileNames(mRootDirs.get(i));
                    if(files != null)
                        allFiles.addAll(files);
                }
            }
        };
        runnable.run();
    }

    public List<String> loadAllFileNames(String dirPath) {
        List <String> fileNamesInThisDir = new ArrayList<>();

        File currDir = new File(dirPath);
        if(!currDir.exists())
            return null;
        
        File [] files = currDir.listFiles();
        
        if(files == null)
            return null;
        
        for(int i=0; i < files.length; i++) {
            if(files[i].isDirectory())  // it's a file, can add it!
                fileNamesInThisDir.addAll(loadAllFileNames(files[i].getPath()));
            else if(hasExtention(files[i].getPath(), ".mp3"))
                fileNamesInThisDir.add(files[i].getPath());
        }

        return fileNamesInThisDir;
    }

    public boolean hasExtention(String str, String extentionWithDot) {
        return str.indexOf(extentionWithDot) == str.length() -extentionWithDot.length();
    }
}
