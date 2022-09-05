package com.example.scounteratest2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;


class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
    static private List<Song> mData = null;
    static private int mPlayingIndex = 0;

    public RecyclerViewAdapter(ArrayList<Song> mData)
    {
        this.mData = mData;
    }

    
    static public void setData(List<Song> list) {
        mData = list;
    }
    static public List<Song> getmData() {
        return mData;
    }
    static public int getmPlayingIndex() {
        return mPlayingIndex;
    }
    static public void setmPlayingIndex(int index) {
        if(index > -1 && index < mData.size())
            mPlayingIndex = index;
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        switch (viewType) {
            case Enums.PLAYING_SONG_DELEGATE:
                return new PlayingSongViewHolder(LayoutInflater.from(parent.getContext())
                       .inflate(R.layout.recycler_view_playing_song, parent, false));
            case Enums.HIDDEN_SONG_DELEGATE:
                return new HiddenSongViewHolder(LayoutInflater.from(parent.getContext())
                       .inflate(R.layout.recycler_view_hidden_song, parent, false));
            default:
                return new NormalSongViewHolder(LayoutInflater.from(parent.getContext())
                       .inflate(R.layout.recycler_view_normal_song, parent, false));
        }
    }

    private static final String TAG = "RecyclerViewAdapter";
    
    
     
    @Override 
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position)
    {   // create all necessary fields for the views
        String artist = mData.get(position).artist();
        String title = mData.get(position).title();
        String dur = mData.get(position).durStr();
        String playCount = 0 +"";
        AllSong allSong = AllSong.exists(artist, title, AllSong.data).allSong;
        if(allSong != null) 
            playCount = allSong.playCount() +"";
        View.OnClickListener onClickListener = new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String path = mData.get(position).path();
                mPlayingIndex = position;
                PlaylistActivity.playSong(position);
            }
        };
        
        // determine which View to choose.
        if(getItemViewType(position) == Enums.PLAYING_SONG_DELEGATE) 
            ((PlayingSongViewHolder) holder).setViewData(artist, title, dur, playCount, onClickListener);
        else if(getItemViewType(position) == Enums.HIDDEN_SONG_DELEGATE)
            ((HiddenSongViewHolder) holder).setViewData(onClickListener);
        else   // it's a normal song!
            ((NormalSongViewHolder) holder).setViewData(artist, title, dur, playCount, onClickListener);
    }

    @Override
    public int getItemCount()
    {
        return mData.size();
    }

    @Override
    public int getItemViewType(int position)
    {
        Song plSong = SCounterService.getPlayingSong();

        if(plSong != null && mData.get(position).path() != null   // it's a playing song!
           && mData.get(position).path().equals(plSong.path())) 
            return Enums.PLAYING_SONG_DELEGATE;
//        else if(position > 10)
//            return Enums.HIDDEN_SONG_DELEGATE;

        return Enums.NORMAL_SONG_DELEGATE;
    }

    class NormalSongViewHolder extends RecyclerView.ViewHolder {

        private TextView textArtist;
        private TextView textTitle;
        private TextView textDur;
        private TextView textPlayCount;
        private ImageView bg;

        public NormalSongViewHolder(@NonNull View itemView)
        {
            super(itemView);

            bg = itemView.findViewById(R.id.bg);
            textArtist = itemView.findViewById(R.id.txtTitle);
            textTitle = itemView.findViewById(R.id.txtArtist);
            textDur = itemView.findViewById(R.id.txtDur);
            textPlayCount = itemView.findViewById(R.id.txtPlayCount);
        }
        
        public void setViewData(String artist, String title, String dur, String playCount, 
                                View.OnClickListener onClickListener) {
            textArtist.setText(artist);
            textTitle.setText(title);
            textDur.setText(dur);
            textPlayCount.setText(playCount);
            bg.setOnClickListener(onClickListener);
        }
    }

    class PlayingSongViewHolder extends RecyclerView.ViewHolder {

        private TextView textArtist;
        private TextView textTitle;
        private TextView textDur;
        private TextView textPlayCount;
        private ImageView bg;

        public PlayingSongViewHolder(@NonNull View itemView)
        {
            super(itemView);

            bg = itemView.findViewById(R.id.bg);
            textArtist = itemView.findViewById(R.id.txtTitle);
            textTitle = itemView.findViewById(R.id.txtArtist);
            textDur = itemView.findViewById(R.id.txtDur);
            textPlayCount = itemView.findViewById(R.id.txtPlayCount);
        }

        public void setViewData(String artist, String title, String dur, String playCount,
                                View.OnClickListener onClickListener) {
            textArtist.setText(artist);
            textTitle.setText(title);
            textDur.setText(dur);
            textPlayCount.setText(playCount);
            bg.setOnClickListener(onClickListener);
        }
    }

    class HiddenSongViewHolder extends RecyclerView.ViewHolder {
        private ImageView bg;

        public HiddenSongViewHolder(@NonNull View itemView)
        {
            super(itemView);
            bg = itemView.findViewById(R.id.bg);
        }

        public void setViewData(View.OnClickListener onClickListener) {
            bg.setOnClickListener(onClickListener);
        }
    }
}
