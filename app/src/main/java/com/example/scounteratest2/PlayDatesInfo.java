package com.example.scounteratest2;

import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.Vector;

import static android.content.ContentValues.TAG;

// ALL play/skip entries
class PlayDatesInfo
{
    // MEMBER VARIABLES
    static private Calendar mCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));  // for internal purposes
    static private Vector<SingleDay> mData = new Vector<>();
    static private AvtemFile mAvtemFile = new AvtemFile("");
    
    // CLASSES
    // an PLAY/SKIP entry = allSongId + single play(skip) date 
    static public class SingleEntry {
        long allSongId;  // id in AllSongs data
        Date date; // date of playing / skipping
        
        long minuteCountSincMidnight() {
            // returns amount of Minutes since the day of listening/skippp
            int hours, mins;
            mCalendar.setTime(date);
            hours = mCalendar.get(Calendar.HOUR_OF_DAY);
            mins = mCalendar.get(Calendar.MINUTE);
                    
            return hours*60 + mins;
        }
    }
    // a DAY entry (since 1970...)
    static public class SingleDay {
        Date day;
        Vector<SingleEntry> playDates = new Vector<>();
        Vector<SingleEntry> skipDates = new Vector<>();
    
        SingleDay(final Date theDay)
        {
            this.day = theDay;
        }
    
        void addPlayTime(final long allSongId, final Date date) {
            playDates.add(new SingleEntry());
            playDates.lastElement().allSongId = allSongId;
            playDates.lastElement().date = date;
        }
        void addSkipTime(final long allSongId, final Date date) {
            skipDates.add(new SingleEntry());
            skipDates.lastElement().allSongId = allSongId;
            skipDates.lastElement().date = date;
        }

        long daysSince1970() {
            return day.getTime() /1000 /60 /60 /24;
        }
    }

    PlayDatesInfo() {
//        mCalendar.clear(Calendar.ZONE_OFFSET);
    }
    // METHODS
    // count of play entries in the database
    private static long totalPlayEntryCount() {
        long count = 0;
        for(int i=0; i < mData.size(); i++)
            count += mData.elementAt(i).playDates.size();

        return count;
    }
    // count of skip entries in the database
    private static long totalSkipEntryCount() {
        long count = 0;
        for(int i=0; i < mData.size(); i++)
            count += mData.elementAt(i).skipDates.size();

        return count;
    }
    // get reference to the day if exists
    private static SingleDay findDay(final Date Day) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyy");
        
        for(int i=0; i < mData.size(); i ++) {
            if(dateFormat.format(mData.elementAt(i).day).equals(dateFormat.format(Day)))
                return mData.elementAt(i);
        }

        return null;
    }
    private static void addDayIfNotExist(final Date Day) {
        if(findDay(Day) == null)
            mData.add(new SingleDay(Day));
    }
    // user listened to song! Add it to mData!
    static void addPlayDate(final long allSongId, final Date time) {
        SingleDay dateInmData = findDay(time);
        if(dateInmData == null) {
            mData.add(new SingleDay(time));
            dateInmData = mData.lastElement();
        }

        dateInmData.addPlayTime(allSongId, time);
    }
    // user skipped a song! Add it to mData!
    static void addSkipDate(final long allSongId, final Date time) {
        SingleDay dateInmData = findDay(time);
        if(dateInmData == null) {
            mData.add(new SingleDay(time));
            dateInmData = mData.lastElement();
        }

        dateInmData.addSkipTime(allSongId, time);
    }
    // load all data from file!
    static void loadData(String fileName) {
        mAvtemFile.setPath(fileName);
        mAvtemFile.loadFile();
        
        mData.clear();
        String apiVersionOfFile = mAvtemFile.tags().get("API version of this file");
        if(apiVersionOfFile.equals("1.0.0")) 
            load100();
        else
            Toast.makeText(AvtemFile.appContext, "Unknown version of the PlayDatesInfo.big file!",
                    Toast.LENGTH_SHORT).show();
    }
    // save all data to file!
    static void saveData(String filename) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(); // we will pass this to saveData() method
        StringBuilder strTags = new StringBuilder();
        
        // create info tags
        HashMap <String, String> tags = new HashMap<>();
        tags.put("File name", "PlayDatesInfo.Android");
        tags.put("API version of this file", "1.0.0");
        tags.put("Play entries", String.valueOf(totalPlayEntryCount()));
        tags.put("Skip entries", String.valueOf(totalSkipEntryCount()));
        tags.put("AllSongId ByteCount", String.valueOf(AllSong.bytesRequiredForIds()));
        
        for(int LOOP=0; LOOP < 2; LOOP++)  // write play, then skip entries
        {
            Vector <SingleEntry> vec;

            for(int i=0; i < mData.size(); i++) {
                vec = (LOOP == 0) ? mData.get(i).playDates : mData.get(i).skipDates;

                // this day doesn't have play dates OR skip dates
                if(vec.size() == 0)
                    continue;

			final int idByteCount = AllSong.bytesRequiredForIds();
                byte [] arr2bytes = new byte[2]; 
                byte [] arrASidbytes = new byte[idByteCount]; // for ALLSONG_ID only

                // #1 DAYS since 1970
                intToByteArray(mData.get(i).daysSince1970(), (short) 2, arr2bytes);
                try {
                    bytes.write(arr2bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // #2 ENTRY_COUNT for the day
                intToByteArray(vec.size(), (short) 2, arr2bytes);
                try {
                    bytes.write(arr2bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                for(int j=0; j < vec.size(); j++) {
                    // #A.1 ALLSONG_ID
                    intToByteArray(vec.get(j).allSongId, (short) idByteCount, arrASidbytes);
                    try {
                        bytes.write(arrASidbytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // #A.2 MINUTE_COUNT since 0:00 of the day
                    intToByteArray(vec.get(j).minuteCountSincMidnight(), (short) 2, arr2bytes);
                    try {
                        bytes.write(arr2bytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        // write bytes to the file!
        AvtemFile avtemFile = new AvtemFile(filename);
        avtemFile.saveFile(filename, tags, bytes.toByteArray());
    }
    // load data if file version is  1 0 0
    // convert int to byte Array. All uints are Big endian (MSB)
    private static void intToByteArray(final long number, final short byteCount, byte[] array) {
        for(int i=0; i < byteCount; i++)
            array[i] = (byte)(0xFF & (number >> 8 *(byteCount-1 -i)));
    }
    private static void load100() {
        Vector<Byte> bytes = mAvtemFile.bytes();  // for shorter name
        HashMap<String, String> tags = mAvtemFile.tags();

        // receiving info from tags
        final int allSongSize = Integer.parseInt(tags.get("AllSongId ByteCount"));
        final int playEntryCount = Integer.parseInt(tags.get("Play entries"));
        final int skipEntryCount = Integer.parseInt(tags.get("Skip entries"));

        // variables for loop
        int dayEntryCount = 0; // entry count for a day
        long allSongId;
        Date day;  // for DAY since 1970
        Date time; // for every entry (time of playing/skipping)
        int pos = 0; // position in fileBytes

        // read all play entries
        int entryCount = playEntryCount;

        // 1st loop for play entries, 2nd for skip entries
        for (int LOOP = 0; LOOP < 2; LOOP++) {
            for (int entry = 0; entry < entryCount; ) {
                // get days since 1970
                int byte1 = bytes.elementAt(pos);
                byte1 &= 0xff;
                long daysSinceEpoch = ((bytes.elementAt(pos) & 0xFF) << 8)  
                                       | (bytes.elementAt(pos +1) & 0xFF);
                day = new Date((daysSinceEpoch *24 *60 *60 *1000));
                addDayIfNotExist(day);
                pos += 2;

                // entry count for this day
                dayEntryCount = (bytes.elementAt(pos) & 0xFF) << 8 
                                        | (bytes.elementAt(pos +1) & 0xFF);
                pos += 2;

                // read all entries for this day
                for (int j = 0; j < dayEntryCount; j++) {
                    allSongId = 0;
                    for (char f = 0; f < allSongSize; f++, pos++)
                        allSongId += (bytes.elementAt(pos) & 0xFF) << 8 
                                             * (allSongSize -1 -f);

                    // the file stores amount minutes since 00:00 (midnight). 
                    time = new Date(day.getTime() 
                        + ( (bytes.elementAt(pos) & 0xFF) << 8 
                            | (bytes.elementAt(pos +1) & 0xFF) ) *60 *1000);
                    pos += 2;

                    entry++;
                    
                    SingleDay dayInmData = findDay(time);
                    if(dayInmData == null) {
                        Log.d(TAG, "load100: oh, no! why the day not found among other days???"
                         + " time: " + time.toString());
                        continue;
                    }
                    
                    if (LOOP == 0) // play entry
                        dayInmData.addPlayTime(allSongId, time);
                    else  // skip entry
                        dayInmData.addSkipTime(allSongId, time);
                }
            }
            
            // do the same for skip entries
            entryCount = skipEntryCount;
        }
    }
}
