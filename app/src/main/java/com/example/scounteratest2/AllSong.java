package com.example.scounteratest2;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AllSong
{
    private static Object BadTag;
    private static long mFirstFreeId = 256;   // 0-255 reserved
    // members for objects
    private long mId;       
    private String mArtist;
    private String mTitle;
    private Calendar mBirthDay;
    private Calendar mDateLastPlayed;
    private Calendar mDateLastSkipped;
    private int mPlayCount;
    private int mSkipCount; 
 
    // ***** static stuff, default values 
    static final char SEPARATOR = 0xF00F;
    static boolean dataLoaded = false;
    static private final String TAG = "AllSongClass";
    static private final String SCOUNTER_DATETIME_FORMAT = "dd.MM.yyyy HH:mm";  // date and time
    static private final String SCOUNTER_DATE_FORMAT = "dd.MM.yyyy";            // date
    static private final Locale USER_LOCALE = Locale.getDefault();
    static private final SimpleDateFormat mSimpleDateTimeFormat   
                 = new SimpleDateFormat(SCOUNTER_DATETIME_FORMAT, USER_LOCALE);
    static private final SimpleDateFormat mSimpleDateFormat
                 = new SimpleDateFormat(SCOUNTER_DATE_FORMAT, USER_LOCALE);
    static private Context mAppContext;
    
    // ***** data arrays.
    static List<AllSong> data = new ArrayList<>();
    static List<AllSong> dataSes = new ArrayList<>();
  // ================================================
  // methods <<<
    AllSong ()  // constructor for NullAllSong
    {
        mId = -1;
        mArtist = "";
        mTitle = "";
        mPlayCount = 0;
        mSkipCount = 0;
        mBirthDay = null;
        mDateLastPlayed = null;
        mDateLastSkipped = null;
    }

    AllSong (String artist, String title)   // FOR BRAND NEW SONG
    {
        mId = mFirstFreeId;
        mFirstFreeId ++;
        mArtist = artist;
        mTitle = title;
        
        mBirthDay = Calendar.getInstance();   // today
        mDateLastPlayed = null;
        mDateLastSkipped = null;

        mPlayCount = 0;
        mSkipCount = 0;
    }

    AllSong(String mArtist, String mTitle, String mBirthDay, String mDateLastPlayed, 
                   String mDateLastSkipped, int mPlayCount, int mSkipCount, long id)
    { 
        if(id == -1) {  // allsong file had no id
            this.mId = mFirstFreeId;
            mFirstFreeId ++;
        }
        else
            this.mId = id;
        
        this.mArtist = mArtist;
        this.mTitle = mTitle;

        this.mPlayCount = mPlayCount;
        this.mSkipCount = mSkipCount;
        
        try {
            if("NBD".equals(mBirthDay))          // birth day
                this.mBirthDay = null;
            else {
                this.mBirthDay = Calendar.getInstance();
                this.mBirthDay.setTime(mSimpleDateTimeFormat.parse(mBirthDay));
            }
            
            if("NPD".equals(mDateLastPlayed))                // last played
                this.mDateLastPlayed = null;
            else {
                this.mDateLastPlayed = Calendar.getInstance();
                this.mDateLastPlayed.setTime(mSimpleDateTimeFormat.parse(mDateLastPlayed));
            }
            
            if("NSD".equals(mDateLastSkipped))
                this.mDateLastSkipped = null;
            else {
                this.mDateLastSkipped = Calendar.getInstance();
                this.mDateLastSkipped.setTime(mSimpleDateTimeFormat.parse(mDateLastSkipped));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    static Indexation makeAllSongsIndexation(List<AllSong> allSongList)
    {
        List<String> artists = new ArrayList<>();
        for(int i=0; i < allSongList.size(); i++)
            artists.add(allSongList.get(i).mArtist);
            
        Indexation allSongIndexation = new Indexation();
        allSongIndexation.makeIndexation(artists);
        return allSongIndexation;
    }

    static void makeRevision(List<AllSong> allSongList)
        // deletes all duplicates in allSongs
    {
        Indexation idx = makeAllSongsIndexation(allSongList);

        List <Integer> badSongs = new ArrayList<>();  // indexes to delete from AllSongsList
        
        boolean alreadyChecked = false;
        for (int i = 0; i < idx.artists.size(); i++)  // check every artist
        {
            for (int j = 0; j < idx.artistIndexes.get(i).size(); j++)  // check every song of this artist
            {
                for (int k = j +1; k < idx.artistIndexes.get(i).size(); k++) 
                {
                    for (int z = badSongs.size(); z != 0 
                                     && badSongs.get(z -1) > idx.artistIndexes.get(i).get(0); z--) 
                    {
                        if (idx.artistIndexes.get(i).get(k).equals(badSongs.get(z -1))) {  
                            // we already checked this song.
                            alreadyChecked = true;
                            break;
                        }
                    }

                    if (alreadyChecked) {
                        alreadyChecked = false;
                        continue;
                    }

                    if (allSongList.get(idx.artistIndexes.get(i).get(j)).mTitle
                         .equalsIgnoreCase(allSongList.get(idx.artistIndexes.get(i).get(k)).mTitle)
                         && allSongList.get(idx.artistIndexes.get(i).get(j)).mArtist
                            .equalsIgnoreCase(allSongList.get(idx.artistIndexes.get(i).get(k)).mArtist)
                    )
                        badSongs.add(idx.artistIndexes.get(i).get(k));   // punish double!
                }
            }
        }

        Collections.sort(badSongs);             // delete doubles
        for (int i = badSongs.size() -1; i > -1; i--)
            allSongList.remove(badSongs.get(i));
    }

    static AllSong exists(final long id, List<AllSong> allSongList) {
        for(int i=0; i < allSongList.size(); i++)
            if(id == allSongList.get(i).mId)
                return allSongList.get(i);

        return null;      // nothing found
    }
    
    static BoolIntAS exists(String artist, String title, List<AllSong> allSongList)
    {
        BoolIntAS retValue = new BoolIntAS (false, -1, null);

        for (int i = 0; i < allSongList.size(); i++) {
            if (allSongList.get(i).mArtist.equalsIgnoreCase(artist)
                 && allSongList.get(i).mTitle.equalsIgnoreCase(title)) 
            {
                retValue = new BoolIntAS (true, i, allSongList.get(i));
                break;
            }
        }

        return retValue;
    }

    static BoolIntAS exists(String artist, String title, Indexation idx, 
             int indexInVector, List<AllSong> allSongList)
    {
        BoolIntAS retValue = new BoolIntAS (false, -1, null);

        for (int i = 0; i < idx.artists.size(); i++)
        // check every artist from the indexation
        {   
            if (artist.equalsIgnoreCase(idx.artists.get(i)))
            // we got the same artist!
            {  
                for (int j = 0; j < idx.artistIndexes.get(i).size(); j++)
                // search for the same title!
                { 
                    if (!idx.artistIndexes.get(i).get(j).equals(indexInVector)  // twin?
                         && title.equalsIgnoreCase(allSongList.get(idx.artistIndexes.get(i).get(j)).mTitle) )
                    {
                        return new BoolIntAS(true, idx.artistIndexes.get(i).get(j),
                                allSongList.get(idx.artistIndexes.get(i).get(j)));
                    }
                }
            }
        }

        return retValue;
    }

    static int bytesRequiredForIds()  // how many bytes are needed for storing ids?
    {
        return (int)(Math.log((float)(mFirstFreeId)) /Math.log(256.0f) +1);
    }
    
    enum BadTagEnum {  // used for "BadTags" array
        NoTagsV2, SpaceOnTheBeg, FileNotFound, ConvertToV2_4, NoArtist, NoTitle, NoYear, NoTrack, 
        NoAllSong, EmptyString, StringIsNull, StringIsOK
    }
    
    final static List<String> BadTags = Arrays.asList(new String[]{
            "=== NO TAGS V2",
            "=== SPACE ON THE BEG.",
            "=== FILE NOT FOUND",
            "=== CONVERT TO TAG V2.4",
            "=== NO ARTIST",
            "=== NO TITLE",
            "=== NO ALBUM",
            "----",
            "-",
            "= NO ALLSONG",
            "",
            "STRING IS NULL",
            "string is ok"                                                                     
    });
    
    static boolean artistTitleAreValid (String artist, String title) {
        boolean res = true;

        if(artist == null || title == null || BadTags.contains(artist) || BadTags.contains(title)) {
            res = false;
         
            Toast.makeText(mAppContext, "Played/skipped not counted because\nartist and title fields must be correct",
                Toast.LENGTH_LONG).show();
        }
        return res;
    }
    
    static void incrementPlayCount(String artistStr, String titleStr)
    {
        if(!artistTitleAreValid(artistStr, titleStr))     // it has bad artist/title like "=== NO TITLE"
            return;

        AllSong allsong = exists(artistStr, titleStr, data).allSong;

        if (allsong == null)  // we want to iterate, but there is no such song
        {                     //.. in the database. Create a new AllSong!
            AllSong newAllSong = new AllSong(artistStr, titleStr);
            data.add(newAllSong);
            allsong = newAllSong;
        }

        allsong.mIncrementPlayCount();

        PlayDatesInfo.addPlayDate(allsong.id(), new Date());
        addToSession(allsong, "played");
    }

    static void addToSession(AllSong as, String played_or_skipped)
    {
        // does AllSongs session has this song? 
        AllSong allSongSession = exists(as.mArtist, as.mTitle, dataSes).allSong;

        if (allSongSession != null) {   // this song exists in the session
            if (played_or_skipped == "played")
                allSongSession.mIncrementPlayCount();
            else if (played_or_skipped == "skipped")
                allSongSession.mIncrementSkipCount();
        } 
        else                            // this song DOES NOT exist in the session
        {
            int playCount = 0;
            int skipCount = 0;
            if (played_or_skipped.equals("played"))
                playCount++;
            else if (played_or_skipped.equals("skipped"))
                skipCount++;

            dataSes.add(new AllSong(as.mArtist, as.mTitle, as.birthDayStr(),
                    as.lastPlayedStr(), as.lastSkippedStr(), playCount, skipCount, -1));
        }
    }

    static boolean strIsEmpty(String str)
    {
        return (str == null || str.equals(""));
    }
    
    static void incrementSkipCount(String artistStr, String titleStr)
    {
        if(!artistTitleAreValid(artistStr, titleStr))     // it has bad artist/title like "=== NO TITLE"
            return;

        AllSong allsong = exists(artistStr, titleStr, AllSong.data).allSong;

        if (allsong == null)  // we want to iterate, but there is no such song
        {                     //.. in the database. Create a new AllSong!
            AllSong newAllSong = new AllSong(artistStr, titleStr);
            data.add(newAllSong);
            allsong = newAllSong;
        }

        allsong.mIncrementSkipCount();

        PlayDatesInfo.addSkipDate(allsong.id(), new Date());
        addToSession(allsong, "skipped");
    }
    
    static void loadFromFile(String allSongFileName, List<AllSong> allSongList) 
    {        
        allSongList.clear();
        Log.i(TAG, "loadFromFile: " + allSongFileName);
        
        List<String> stringList = readFile(allSongFileName);
        if(stringList == null)
            return;

        // push allsongs info
        int firstAllSongIndex = 0;
        for (int i=0; i < stringList.size(); i++) {
            if(stringList.get(i).contains("### END OF THE INFO")) {
                firstAllSongIndex = i +1;
                break;
            }
        }
        
        // add allsongs one by one
        for(int i=firstAllSongIndex; i < stringList.size(); i++) 
        {
            String[] fields = stringList.get(i).split(String.valueOf(SEPARATOR));
            // let's assume allsong file didn't have id. If we pass '-1' constructor will create new id automatically
            long allSongId = -1;  
            if(fields.length >= 8) // oh, the allsong file had id
                allSongId = Long.parseLong(fields[7]);
            else {
//                Log.e(TAG, "loadFromFile: for some reason ''fields'' is empty"
//                  + fields.toString());
                continue;
            }
                
            // update mFirstFreeId
            if(allSongId >= mFirstFreeId)
                mFirstFreeId = allSongId +1;
            
            allSongList.add( new AllSong(fields[0],  // artist
                    fields[1],                       // title
                    fields[2],                       // birth day
                    fields[3],                       // date last played
                    fields[4],                       // date last skipped
                    toInt(fields[5]),                // play count
                    toInt(fields[6]),                // skip count
                    allSongId));                     // allsong id
        }
        dataLoaded = true;
    }

    static void appendDataSesListenings(List<AllSong> listMain, final List<AllSong> listSession)
    {    // combine allSong info from two lists into "listMain"
        Indexation idx = makeAllSongsIndexation(listMain);

        for(int i=0; i < listSession.size(); i++)   // add every song from dataSes
        {
            // every song from "dataSes" is IMPORTANT and every song is added/edited
            // this allSong from "data"!!!
            AllSong allSong = exists(listSession.get(i).mArtist, listSession.get(i).mTitle,
                    idx, -1, listMain).allSong;

            if(allSong == null)  // this song didn't exist in "data".
            {
                AllSong as = listSession.get(i);  // deep copy.
                listMain.add(new AllSong(as.mArtist, as.mTitle, as.birthDayStr(), as.lastPlayedStr(),
                    as.lastSkippedStr(), as.mPlayCount, as.mSkipCount, as.mId));  
            }
            else {               // this song exists in "data".
                allSong.mPlayCount += listSession.get(i).mPlayCount;
                allSong.mSkipCount += listSession.get(i).mSkipCount;
                allSong.mDateLastPlayed = listSession.get(i).mDateLastPlayed;
                allSong.mDateLastSkipped = listSession.get(i).mDateLastSkipped;
            }
        }
    }
    long id() { 
        return mId; 
    }
    static void saveToFile(String allSongFileName, List<AllSong> allSongList)
    {
        FileOutputStream fileOutputStream = null;

        try {
            fileOutputStream = mAppContext.openFileOutput(allSongFileName, Context.MODE_PRIVATE);
            
            // writing AllSong info
            String lines = "AllSongsInfo: " + allSongList.size() + '\n';
            lines += "SCounterBirth: \n" +
                           "Reserved.\n" +
                           "Reserved.\n" +
                           "Reserved.\n";
            lines += "### END OF THE INFO\n";
            
            fileOutputStream.write(lines.getBytes());

            StringBuilder stringBuilder = new StringBuilder();
            for(int i=0; i < allSongList.size(); i++) 
            {
                stringBuilder.setLength(0);
                stringBuilder.append(allSongList.get(i).mArtist).append(SEPARATOR);
                stringBuilder.append(allSongList.get(i).mTitle).append(SEPARATOR);
                stringBuilder.append(allSongList.get(i).birthDayStr()).append(SEPARATOR);
                stringBuilder.append(allSongList.get(i).lastPlayedStr()).append(SEPARATOR);
                stringBuilder.append(allSongList.get(i).lastSkippedStr()).append(SEPARATOR);
                stringBuilder.append(allSongList.get(i).mPlayCount).append(SEPARATOR);
                stringBuilder.append(allSongList.get(i).mSkipCount).append(SEPARATOR);
                stringBuilder.append(allSongList.get(i).mId).append(SEPARATOR);
                
                stringBuilder.append('\n');
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
    
    static int toInt(String str) {
        int integer = 0;
        try {
            integer = Integer.parseInt(str);
        } catch (NumberFormatException e) {
            Log.i(TAG, "toInt: str is not an int. str = " + str);
        }
        
        return integer;
    }
    
    private static List<String> readFile(String fileName) 
    {
        List<String> stringList = null;

        // CHECK FILE EXISTING
        File file = new File(mAppContext.getFilesDir() + "/" + fileName);   
        if(!file.exists()) {        // doesn't exist!
            Log.e(TAG, "loadFromFile: file doesn't exist: \n" + fileName);
            return null;
        }

        // try to load all lines from the file
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = mAppContext.openFileInput(fileName);
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
    
    static void setmAppContext(Context context) {
        if(mAppContext == null)
            mAppContext = context;
    }
    
    // GETTERS
    final String artist() {
        return mArtist;
    }
    final String title() {
        return mTitle;
    }
    final int playCount() {
        return mPlayCount;
    }
    final int skipCount() {
        return mSkipCount;
    }
    final Calendar birthDay() {
        return mBirthDay;
    }
     final Calendar dateLastPlayed() {
        return mDateLastPlayed;
    }
    final Calendar dateLastSkipped() {
        return mDateLastSkipped;
    }

    // GETTERS AS STRINGS
    private String birthDayStr() {     
        if(mBirthDay == null)
            return "NBD";
        return mSimpleDateTimeFormat.format(mBirthDay.getTime());
    }
    final String lastPlayedStr() {
        if(mDateLastPlayed == null)
            return "NPD";
        return mSimpleDateTimeFormat.format(mDateLastPlayed.getTime());
    }
    final String lastSkippedStr() {
        if(mDateLastSkipped == null)
            return "NSD";
        return mSimpleDateTimeFormat.format(mDateLastSkipped.getTime());
    }
    void updatePlayDate () {
        mDateLastPlayed = Calendar.getInstance();
    }
    void updateSkipDate () {
        mDateLastSkipped = Calendar.getInstance();
    }
    
    static Calendar scounterBirthday() {
        if(data.isEmpty())
            return Calendar.getInstance();
        
        Calendar earlierDate = data.get(0).mBirthDay;
        for(int i=1; i < data.size() -1; i++)
        {
            if(data.get(i).mBirthDay.compareTo(earlierDate) < 0)
                earlierDate = data.get(i).mBirthDay;
        }

        return earlierDate;
    }

    static final String scounterBirthDayStr() {
        return mSimpleDateFormat.format(scounterBirthday().getTime());
    }
    
    static long scounterTotalPlayCount(List<AllSong> dataList) {
        long totalPlayCount = 0;
        
        for(AllSong allSong: dataList)
            totalPlayCount += allSong.mPlayCount;
        
        return totalPlayCount;
    }

    static long scounterTotalSkipCount(List<AllSong> dataList) {
        long totalSkipCount = 0;

        for(AllSong allSong: dataList)
            totalSkipCount += allSong.mSkipCount;

        return totalSkipCount;
    }
    
    private void mIncrementPlayCount() {
        mPlayCount ++;
        updatePlayDate();
    }
    private void mIncrementSkipCount() {
        mSkipCount ++;
        updateSkipDate();
    }
    
    @Override
    public boolean equals(@Nullable Object obj)
    {
        if(obj == null || this.getClass() != obj.getClass())
            return false;
        if(this == obj)
            return true;
        
        AllSong secondObj = (AllSong) obj;
        
        return (this.mArtist.equalsIgnoreCase(secondObj.mArtist)
               && this.mTitle.equalsIgnoreCase(secondObj.mTitle));
    }
}
