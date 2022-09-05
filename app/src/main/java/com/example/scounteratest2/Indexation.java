package com.example.scounteratest2;

import java.util.ArrayList;
import java.util.List;

public class Indexation
{   // class for indexation
    List<String> artists = new ArrayList<>();
    List<List <Integer> > artistIndexes = new ArrayList<>();

    public void makeIndexation (List <String> inputList)
    {
        artists.clear();
        artistIndexes.clear();

        boolean inArtistsIndexes = false;
        for(int i=0; i < inputList.size(); i++)   // check every record of input
        {
            for(int j = artists.size(); j != 0; j--) // artist
            {
                if(inputList.get(i).equalsIgnoreCase(artists.get(j-1)))
                {
                    artistIndexes.get(j-1).add(i); // add index of same artist
                    break;
                }

                if(j == 1) // We haven't seen such artist so far!
                    inArtistsIndexes = false;
            }

            if(inArtistsIndexes == false)            // CREATE NEW ARTIST VECTOR
            {
                artists.add(inputList.get(i));                     // Metallica
                List <Integer> newIndexesList = new ArrayList<>(); // create vector for indexes of (metallica)
                newIndexesList.add(i);                             // first index of new art.
                artistIndexes.add(newIndexesList);
            }
            inArtistsIndexes = true; //everyone is found in the end of the day
        }
    }
}    

