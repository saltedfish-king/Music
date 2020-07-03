package com.example.music;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    ImageButton next,last,play,method;
    ImageView head;
    TextView songName,singerName,start,end;
    RecyclerView mRecyclerView;
    List<Music>mMusic;
    SeekBar bar;
    private musicAdapter mAdapter;
    MediaPlayer mediaPlayer;

    //设置一个全员变量来记录当前播放的音乐
    int currentMusic = -1;

    //记录当前进度位置
    int currentProgress = 0;

    //int一个变量来确认播放方式,0表示循环播放，1表示随机播放，2表示只播放喜欢的歌曲
    int playMethod = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //初始化各个控件
        next = (ImageButton)findViewById(R.id.music_next);
        last = (ImageButton)findViewById(R.id.music_last);
        play = (ImageButton)findViewById(R.id.music_play);
        method = (ImageButton)findViewById(R.id.play_method);

        start = (TextView)findViewById(R.id.start_time);
        end = (TextView)findViewById(R.id.end_time);

        head = (ImageView)findViewById(R.id.music_head);
        songName = (TextView)findViewById(R.id.songName);
        singerName = (TextView)findViewById(R.id.singerName);
        mRecyclerView = (RecyclerView)findViewById(R.id.music_item);
        mMusic = new ArrayList<>();
        mediaPlayer = new MediaPlayer();
        bar = (SeekBar)findViewById(R.id.music_progress) ;

        //按钮点击事件
        next.setOnClickListener(this);
        last.setOnClickListener(this);
        play.setOnClickListener(this);
        method.setOnClickListener(this);

        //设置进度栏
        bar.setMax(1000);
        bar.setProgress(0);
        bar.setOnSeekBarChangeListener(seekListener);

        //创建适配器对象
        mAdapter = new musicAdapter(this,mMusic);
        mRecyclerView.setAdapter(mAdapter);
        //设置布局管理器
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(layoutManager);

        //加载本地数据
        loadMusic();

        //设置每一项的点击事件
        setListener();

        //启动线程控制进度条
        thread.start();
    }

    //线程控制进度条刷新
    private Thread thread = new Thread(){
        @Override
        public void run(){
            while (true) {
                try {
                    sleep(100);
                    handler.sendEmptyMessage(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    //通过Handler来将UI更新操作切换到主线程中执行
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (mediaPlayer.isPlaying()){

                //如果歌曲正在播放，刷新进度条与歌曲进度一致
                long d = mediaPlayer.getDuration();
                long p = mediaPlayer.getCurrentPosition();
                bar.setProgress((int)(1000 * p / d));

                //格式化时间
                SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
                String ctime = sdf.format(new Date(p));
                start.setText(ctime);
                end.setText(mMusic.get(currentMusic).getSongTime());

                //如果进度条满就切换至下一首
                if (bar.getProgress() == 1000) {
                    playByLike(mMusic.size());
                }
            }
        }
    };

    //选择播放方式
    private void playByLike(int size) {
        switch (playMethod) {
            case 0:
                //循环播放
                if (currentMusic == mMusic.size() - 1) {
                    currentMusic = 0;
                    playMusicByPosition(currentMusic);
                    return;
                }
                currentMusic = currentMusic + 1;
                playMusicByPosition(currentMusic);
                break;
            case 1:
                //随机播放
                currentMusic = (int) (Math.random() * mMusic.size());
                playMusicByPosition(currentMusic);
                break;
            case 2:
                //记录喜欢的歌曲数量
                int count = 0;
                //仅播放喜欢的歌曲,判断是否喜欢
                for (int i = 0; i < size; i++) {
                    currentMusic = currentMusic + 1;
                    if (currentMusic == mMusic.size() - 1)
                        currentMusic = 0;
                    if (mMusic.get(currentMusic).isLike()) {
                        count++;
                        if (currentMusic == mMusic.size() - 1)
                            currentMusic = 0;
                        playMusicByPosition(currentMusic);
                        break;
                    }
                }
                //如果没有喜欢的歌曲，就提示用户
                if (count == 0) {
                    Toast.makeText(MainActivity.this, "请选择喜欢的歌曲", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    //进度栏监听器，移动进度条时的触发事件
    private SeekBar.OnSeekBarChangeListener seekListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        //进度条改变时
        public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
            if (b) {
                //进度条发生改变之后，音乐也随之改变
                try {
                    if (mediaPlayer != null){
                        currentProgress = 0;
                        mediaPlayer.pause();
                        mediaPlayer.stop();
                    }
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                    mediaPlayer.seekTo(mediaPlayer.getDuration() * progress/1000);
                    play.setBackgroundResource(R.mipmap.play);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        //按下时
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        //放开时
        public void onStopTrackingTouch(SeekBar seekBar) {
            //如果进度条拉满就切换至下一首
            if (bar.getProgress() == 1000) {
                playByLike(mMusic.size());
            }
        }
    };

    private void setListener() {
        //每一项的点击事件
        mAdapter.setClickListener(new musicAdapter.itemClickListener() {
            @Override
            public void itemClick(View view, int position) {
                currentMusic = position;
                playMusicByPosition(position);
            }
        });
    }

    private void playMusicByPosition(int position) {
        Music cm = mMusic.get(position);
        //底部显示当前播放音乐信息
        songName.setText(cm.getSongName());
        singerName.setText(cm.getSingerName());

        int albumId = Integer.valueOf(cm.getAlbumId()).intValue();
        String albumArt = getAlbumArt(albumId);
        //根据专辑ID获取到专辑封面图
        if (!(albumArt == null)) {
            Bitmap bitmap = BitmapFactory.decodeFile(albumArt);
            head.setImageBitmap(bitmap);
        }else {
            head.setBackgroundResource(R.mipmap.music);
        }

        stopMusic();
        //重置播放器并设置新的播放路径
        mediaPlayer.reset();
        try {
            mediaPlayer.setDataSource(cm.getPath());
            initPlayMusic();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initPlayMusic(){
        //从头播放音乐
        try {
            mediaPlayer.prepare();
            mediaPlayer.start();
            mediaPlayer.seekTo(0);
            bar.setProgress(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        play.setBackgroundResource(R.mipmap.play);
    }

    private void playMusic() {
        //播放音乐
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            if (currentProgress == 0) {
                try {
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else {
                mediaPlayer.seekTo(currentProgress);
                mediaPlayer.start();
            }

            play.setBackgroundResource(R.mipmap.play);
        }
    }

    private void stopMusic() {
        //停止音乐
        if (mediaPlayer != null){
            currentProgress = 0;
            mediaPlayer.pause();
            mediaPlayer.seekTo(0);
            mediaPlayer.stop();
            play.setBackgroundResource(R.mipmap.pause);
        }
    }

    private void pauseMusic() {
        //暂停音乐
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            currentProgress = mediaPlayer.getCurrentPosition();
            mediaPlayer.pause();
            play.setBackgroundResource(R.mipmap.pause);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopMusic();
    }

    private void loadMusic() {
        //加载本地音乐
        ContentResolver resolver = getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = resolver.query(uri, null, null, null, null);

        while (cursor.moveToNext()) {
            String song = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
            String singer = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
            String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
            String albumId = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
            long cursorLong = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
            //格式化时间
            SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
            String time = sdf.format(new Date(cursorLong));

            Music m = new Music(song,singer,time,path,albumId);
            mMusic.add(m);
        }
        //数据变化，需要提示适配器更新
        mAdapter.notifyDataSetChanged();
    }

    private String getAlbumArt(int album_id){
        //判断是否有专辑图片
        String mUriAlbums = "content://media/external/audio/albums";
        String[] projection = new String[] { "album_art" };
        Cursor cur = this.getContentResolver().query( Uri.parse(mUriAlbums + "/" + Integer.toString(album_id)), projection, null, null, null);
        String album_art = null;
        if (cur.getCount() > 0 && cur.getColumnCount() > 0)
        { cur.moveToNext();
            album_art = cur.getString(0);
        }
        cur.close();
        cur = null;
        return album_art;
    }

    @Override
    public void onClick(View view) {
        //暂停，播放，上下曲切换
        switch (view.getId()) {
            case R.id.music_next:
                playByLike(mMusic.size());
                break;
            case R.id.music_last:
                switch (playMethod) {
                    //选择播放方式
                    case 0:
                        //循环播放
                        if (currentMusic == 0 || currentMusic == -1) {
                            currentMusic = mMusic.size()-1;
                            playMusicByPosition(currentMusic);
                            return;
                        }
                        currentMusic = currentMusic - 1;
                        playMusicByPosition(currentMusic);
                        break;
                    case 1:
                        //随机播放
                        currentMusic = (int) (Math.random() * mMusic.size());
                        playMusicByPosition(currentMusic);
                        break;
                    case 2:
                        //记录喜欢的歌曲数量
                        int count = 0;
                        //仅播放喜欢的歌曲,判断是否喜欢
                        for (int i = 0; i < mMusic.size(); i++) {
                            if (currentMusic == 0 || currentMusic == -1)
                                currentMusic = mMusic.size()-1;
                            currentMusic = currentMusic - 1;
                            if (mMusic.get(currentMusic).isLike()) {
                                count++;
                                if (currentMusic == 0 || currentMusic == -1)
                                    currentMusic = mMusic.size()-1;
                                playMusicByPosition(currentMusic);
                                break;
                            }
                        }
                        //如果没有喜欢的歌曲，就提示用户
                        if (count == 0) {
                            Toast.makeText(MainActivity.this, "请选择喜欢的歌曲", Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
                break;
            case R.id.music_play:
                if (currentMusic == -1) {
                    currentMusic = 0;
                    playMusicByPosition(currentMusic);
                    return;
                }

                if (mediaPlayer.isPlaying()) {
                    pauseMusic();
                }else {
                    playMusic();
                }
                break;
            case R.id.play_method:
                switch (playMethod) {
                    case 0:
                        playMethod = 1;
                        method.setBackgroundResource(R.mipmap.random_play);
                        Toast.makeText(MainActivity.this, "随机播放", Toast.LENGTH_SHORT).show();
                        break;
                    case 1:
                        playMethod = 2;
                        method.setBackgroundResource(R.mipmap.like_play);
                        Toast.makeText(MainActivity.this, "选择播放", Toast.LENGTH_SHORT).show();
                        break;
                    case 2:
                        playMethod = 0;
                        method.setBackgroundResource(R.mipmap.cycle_play);
                        Toast.makeText(MainActivity.this, "循环播放", Toast.LENGTH_SHORT).show();
                        break;
                }
                break;
        }
    }
}
