package com.example.music;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class musicAdapter extends RecyclerView.Adapter<musicAdapter.musicHolder> {

    Context context;
    List<Music>mMusic;
    itemClickListener clickListener;

    public void setClickListener(itemClickListener clickListener) {
        this.clickListener = clickListener;
    }

    //设置一个回调接口，方便每一项的点击事件
    public interface itemClickListener{
        void itemClick(View view,int position);
    }

    //适配器构造方法
    public musicAdapter(Context context, List<Music> mMusic) {
        this.context = context;
        this.mMusic = mMusic;
    }

    @NonNull
    @Override
    public musicHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context).inflate(R.layout.item_music,parent,false);
        musicHolder holder = new musicHolder(view);
        return holder;
    }

    @Override
    public int getItemCount() {
        return mMusic.size();
    }

    @Override
    public void onBindViewHolder(@NonNull final musicHolder holder, final int position) {

        final Music music = mMusic.get(position);
        holder.songTime.setText(music.getSongTime());
        holder.songName.setText(music.getSongName());
        holder.singerName.setText(music.getSingerName());

        if (!music.isLike()) {//判断是否点击喜欢
            holder.songLike.setBackgroundResource(R.mipmap.unlike);
        }else {
            holder.songLike.setBackgroundResource(R.mipmap.like);
        }

        holder.songLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //添加喜欢按钮
                if (!music.isLike()) {
                    holder.songLike.setBackgroundResource(R.mipmap.like);
                    music.setLike(true);
                }else {
                    holder.songLike.setBackgroundResource(R.mipmap.unlike);
                    music.setLike(false);
                }

            }
        });

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //每一个子项的点击事件
                clickListener.itemClick(view,position);
            }
        });
    }

    class musicHolder extends RecyclerView.ViewHolder {

        TextView songName,singerName,songTime;
        ImageButton songLike;

        public musicHolder(@NonNull View itemView) {
            super(itemView);

            songName = itemView.findViewById(R.id.music_song_name);
            singerName = itemView.findViewById(R.id.music_singer_name);
            songTime = itemView.findViewById(R.id.music_song_time);
            songLike = itemView.findViewById(R.id.music_Like);
        }
    }
}
