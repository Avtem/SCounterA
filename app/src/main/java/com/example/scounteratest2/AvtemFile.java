// This is class for creating, writing and reading AvtemFiles.
// 09.05.2020 - birth day of this class
// v.1.0.0
// 30.06.2020 - birth day of this file (just rewriting the class for Java) 
// The main concept of the file is header has "tags" from 1 to endless amount like   <key>value
// and the rest of the file is data

package com.example.scounteratest2;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AvtemFile
{
    // class members
    static Context appContext = null;
    private static final String TAG = "AvtemFile";
    private static final String mLastTagName = "INFO END";
    private String mFileName = "";  // path of the file
    private HashMap<String, String> mTags = new HashMap<>(); // tags from the header of the file
    private Vector<Byte> mBytes = new Vector<>();  // rest of the data

    AvtemFile(final String filename) {
        mFileName = filename;
    }
    // methods
    private void extractTagsFromBytes() {
        String regexPattern = '<' + mLastTagName + '>' + '\n';
        int firstDataPos; //mBytes.toString().indexOf(">\n");
        for(int i=0; true; i++) {
            if(mBytes.elementAt(i) == '>' && mBytes.elementAt(i +1) == '\n') {
                firstDataPos = i + 2;
                break;
            }
        }
                                   
        mTags.clear();
        // todo: improve the code. Create a byte vector with file tags only 
        Vector<Byte> tagBytes = new Vector<>();//mBytes, firstDataPos);
        for(int i=0; i < firstDataPos; i++)
            tagBytes.add(mBytes.elementAt(i));
        // and delete tag bytes from the mBytes vector
        for(int i=firstDataPos -1; i >= 0; i--)
            mBytes.remove(i);
        
        StringBuilder str = new StringBuilder();
        for(int i=0; i < tagBytes.size(); i++)
            str.append((char)(byte)tagBytes.elementAt(i));
        List <String> keys = new ArrayList<>();
        List <String> values = new ArrayList<>();
        
        // get all keys
        Matcher matcher = Pattern.compile("(?<=<)(.*?)(?=>)").matcher(str.toString());
        while(matcher.find())
            keys.add(matcher.group(0));

        // get all values
        matcher = Pattern.compile("(?<=>)(.*?)(?=<)").matcher(str.toString());
        while(matcher.find())
            values.add(matcher.group(0));
        values.add("\n");

        // finally fill tags to mTags
        for(int f=0; f < keys.size(); f++)
            mTags.put(keys.get(f), values.get(f));
    }
    void loadFile() {
        File file = new File(appContext.getApplicationInfo().dataDir + "/files/" + mFileName);
        if(!file.exists())
        {
            Log.e(TAG, "AvtemFile::loadFile(): " + "failed because file doesn't exist: " + mFileName);
            return;
        }

        byte [] bytes = new byte[(int)file.length()];
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            fileInputStream.read(bytes);
            fileInputStream.close();
            for(int i=0; i < bytes.length; i++)
                mBytes.add(bytes[i]);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        extractTagsFromBytes();

        try {
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    void saveFile(final String filename, final HashMap<String, String> map, byte [] bytes) {
        // PART 1 make file tags in single string
        StringBuilder tags = new StringBuilder();
        for(HashMap.Entry<String, String> entry : map.entrySet())
            tags.append('<' + entry.getKey() + '>' + entry.getValue());
        tags.append('<' + mLastTagName + '>' + '\n');
        
        // make file & file output stream
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = appContext.openFileOutput(filename, Context.MODE_PRIVATE);
            fileOutputStream.write(tags.toString().getBytes());
            fileOutputStream.write(bytes);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    // getters / setters
    final String path() { 
        return mFileName; 
    }
    void setPath(final String path) { 
        mFileName = path; 
    }
    HashMap<String, String> tags() {
        return mTags;
    }
    Vector<Byte> bytes() { 
        return mBytes; 
    }

    //////////////////////////////////////////
    // DEPRECATED
    static boolean exists(String fileName) {
        File file = new File(fileName);
        return file.exists();
    }
    // DEPRECATED
    static List<String> loadLinesFromFile(String dirNames, String fileName) {
        // CHECK FILE EXISTING
        File dir = new File(appContext.getFilesDir() + dirNames + '/' + fileName);
        if(!dir.exists()) {
            Log.e(TAG, "loadFromFile: file doesn't exist: \n" + dir.getAbsolutePath());
            return null;
        }
        
        List<String> stringList = null;

        // try to load all lines from the file
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(dir);
            InputStreamReader isr = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(isr);
            String line;
            stringList = new ArrayList<>();

            while( (line = bufferedReader.readLine()) != null) {  // read everyline
                stringList.add(line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return stringList;
    }
    // DEPRECATED
    static void saveLinesToFile(String dirNames, String fileName, List<String> stringList) {
        FileOutputStream fileOutputStream = null;
        
        try {
            File dir = new File(appContext.getFilesDir() + dirNames);
                dir.mkdirs();
                
            dir = new File(dir.toString() + '/' + fileName);
            
            fileOutputStream = new FileOutputStream(dir);
            
            StringBuilder stringBuilder = new StringBuilder();
            for(int i=0; i < stringList.size(); i++)
            {
                stringBuilder.setLength(0);
                stringBuilder.append(stringList.get(i)).append('\n');
                fileOutputStream.write(stringBuilder.toString().getBytes());
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(fileOutputStream!= null)
                    fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
