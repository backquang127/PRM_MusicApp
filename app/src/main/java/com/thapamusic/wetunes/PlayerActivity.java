package com.thapamusic.wetunes;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.palette.graphics.Palette;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Locale;

public class PlayerActivity extends AppCompatActivity implements ActionPlaying, ServiceConnection {

    // Views
    TextView song_name, artist_name, duration_played, duration_total, album_name, textNowplaying;
    // Trong PlayerActivity.java
    ImageView cover_art, nextBtn, prevBtn, backBtn, shuffleBtn, repeatBtn, sleepTimerBtn;
    FloatingActionButton playPauseBtn;
    SeekBar seekBar;

    // Dữ liệu của riêng Activity này
    private int position = -1;
    private ArrayList<MusicFiles> listSongs = new ArrayList<>();
    private Uri uri;

    // Service
    private MusicService musicService;
    private boolean isBound = false;

    // Handler để cập nhật seekbar
    private final Handler handler = new Handler();

    // Các biến trạng thái (không còn static)
    private boolean shuffleBoolean = false;
    private boolean repeatBoolean = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFullScreen();
        setContentView(R.layout.activity_player);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initViews();
        getIntentData();
        setupClickListeners();

        startMusicService();

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (musicService != null && fromUser) {
                    musicService.seekTo(progress * 1000);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void getIntentData() {
        position = getIntent().getIntExtra("position", -1);
        String sender = getIntent().getStringExtra("sender");

        if (sender != null) {
            if (sender.equals("albumDetails")) {
                listSongs = getIntent().getParcelableArrayListExtra("albumSongs");
            } else {
                listSongs = getIntent().getParcelableArrayListExtra("songList");
            }
        }

        if (listSongs == null || listSongs.isEmpty() || position == -1) {
            Log.e("PlayerActivity", "No valid song data received. Finishing activity.");
            finish();
        }
    }

    private void startMusicService() {
        Intent intent = new Intent(this, MusicService.class);
        intent.putExtra("songList", listSongs);
        intent.putExtra("servicePosition", position);
        startService(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, this, BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isBound) {
            unbindService(this);
            isBound = false;
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        MusicService.MyBinder myBinder = (MusicService.MyBinder) service;
        musicService = myBinder.getService();
        isBound = true;
        musicService.setCallBack(this);

        // Luôn cập nhật UI từ trạng thái của service khi kết nối
        updateUIFromService();

        // Bắt đầu cập nhật seekbar
        handler.post(updateSeekBar);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        musicService = null;
        isBound = false;
    }

    // --- Các phương thức callback từ ActionPlaying ---
    @Override
    public void playPauseBtnClicked() {
        if (musicService == null) return;

        if (musicService.isPlaying()) {
            musicService.pause();
        } else {
            musicService.start();
        }
        updatePlayPauseButton();
    }

    @Override
    public void nextBtnClicked() {
        if (musicService == null) return;

        position = calculateNextPosition();
        musicService.playMedia(position); // Service sẽ tự xử lý stop, release và create
        updateUIFromService();
    }

    @Override
    public void prevBtnClicked() {
        if (musicService == null) return;

        position = calculatePrevPosition();
        musicService.playMedia(position);
        updateUIFromService();
    }

    private void updateUIFromService() {
        if (musicService == null || listSongs.isEmpty() || musicService.position == -1) return;

        this.position = musicService.position; // Đồng bộ vị trí
        MusicFiles currentSong = listSongs.get(this.position);
        uri = Uri.parse(currentSong.getPath());

        song_name.setText(currentSong.getTitle());
        artist_name.setText(currentSong.getArtist());
        album_name.setText(currentSong.getAlbum()); // Cập nhật tên album

        int totalDurationSecs = musicService.getDuration() / 1000;
        seekBar.setMax(totalDurationSecs);
        duration_total.setText(formattedTime(totalDurationSecs));

        metaData(uri);
        updatePlayPauseButton();
    }

    private void updatePlayPauseButton() {
        if (musicService != null && musicService.isPlaying()) {
            playPauseBtn.setImageResource(R.drawable.ic_pause);
        } else {
            playPauseBtn.setImageResource(R.drawable.ic_play);
        }
    }

    private final Runnable updateSeekBar = new Runnable() {
        @Override
        public void run() {
            if (musicService != null && isBound) {
                int mCurrentPosition = musicService.getCurrentPosition() / 1000;
                seekBar.setProgress(mCurrentPosition);
                duration_played.setText(formattedTime(mCurrentPosition));
                // Lặp lại sau mỗi giây
                handler.postDelayed(this, 1000);
            }
        }
    };

    private int calculateNextPosition() {
        if (shuffleBoolean && !repeatBoolean) {
            return getRandom(listSongs.size() - 1);
        } else if (!shuffleBoolean && !repeatBoolean) {
            return (position + 1) % listSongs.size();
        }
        // Nếu repeat hoặc cả hai đều bật, giữ nguyên vị trí
        return position;
    }

    private int calculatePrevPosition() {
        if (shuffleBoolean && !repeatBoolean) {
            return getRandom(listSongs.size() - 1);
        } else if (!shuffleBoolean && !repeatBoolean) {
            return (position - 1 < 0) ? (listSongs.size() - 1) : (position - 1);
        }
        return position;
    }

    private void setupClickListeners() {
        playPauseBtn.setOnClickListener(v -> playPauseBtnClicked());
        nextBtn.setOnClickListener(v -> nextBtnClicked());
        prevBtn.setOnClickListener(v -> prevBtnClicked());
        backBtn.setOnClickListener(v -> finish());

        shuffleBtn.setOnClickListener(v -> {
            shuffleBoolean = !shuffleBoolean;
            shuffleBtn.setImageResource(shuffleBoolean ? R.drawable.ic_shuffle_on : R.drawable.ic_shuffle_off);
        });

        repeatBtn.setOnClickListener(v -> {
            repeatBoolean = !repeatBoolean;
            repeatBtn.setImageResource(repeatBoolean ? R.drawable.ic_repeat_on : R.drawable.ic_repeat_off);
        });
        sleepTimerBtn.setOnClickListener(v -> showTimerDialog());
    }

    private void initViews() {
        song_name = findViewById(R.id.song_name);
        artist_name = findViewById(R.id.song_artist);
        album_name = findViewById(R.id.song_album); // Sửa lỗi: Thêm khai báo
        duration_played = findViewById(R.id.durationPlayed);
        duration_total = findViewById(R.id.durationTotal);
        cover_art = findViewById(R.id.cover_art);
        nextBtn = findViewById(R.id.id_next);
        prevBtn = findViewById(R.id.id_prev);
        backBtn = findViewById(R.id.back_btn);
        shuffleBtn = findViewById(R.id.id_shuffle);
        repeatBtn = findViewById(R.id.id_repeat);
        playPauseBtn = findViewById(R.id.play_pause);
        seekBar = findViewById(R.id.seekBar);
        textNowplaying = findViewById(R.id.nowplaing); // Sửa lỗi: Thêm khai báo
        sleepTimerBtn = findViewById(R.id.sleep_timer_btn);
    }

    private void metaData(Uri uri) {
        if (uri == null) return;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(this, uri);
            byte[] art = retriever.getEmbeddedPicture();
            Bitmap bitmap = null;
            if (art != null) {
                bitmap = BitmapFactory.decodeByteArray(art, 0, art.length);
                ImageAnimation(this, cover_art, bitmap);
            } else {
                Glide.with(this).asBitmap().load(R.drawable.musicicon).into(cover_art);
            }

            // Palette logic
            if (bitmap != null) {
                Palette.from(bitmap).generate(palette -> {
                    Palette.Swatch swatch = palette.getDominantSwatch();
                    if (swatch != null) {
                        // ... (toàn bộ logic palette của bạn)
                        // Xóa các tham chiếu đến NowPlayingFragmentBottom
                    } else {
                        // ...
                    }
                });
            }
        } catch (Exception e) {
            Log.e("PlayerActivity", "Error setting metadata source", e);
        } finally {
            try {
                retriever.release();
            } catch (IOException e) {
                Log.e("PlayerActivity", "Error releasing retriever", e);
            }
        }
    }

    private String formattedTime(int mCurrentPosition) {
        long totalSeconds = mCurrentPosition;
        long seconds = totalSeconds % 60;
        long minutes = totalSeconds / 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private void setFullScreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private int getRandom(int i) {
        if (i < 0) return 0;
        return new Random().nextInt(i + 1);
    }

    public void ImageAnimation(Context context, ImageView imageView, Bitmap bitmap)
    {
        Animation animOut = AnimationUtils.loadAnimation(context, android.R.anim.fade_out);
        Animation animIn = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
        animOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Glide.with(context).load(bitmap).into(imageView);
                animIn.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                imageView.startAnimation(animIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        imageView.startAnimation(animOut);
    }
    private void showTimerDialog() {
        // Chỉ hiển thị dialog nếu service đã được kết nối
        if (musicService == null) {
            Toast.makeText(this, "Dịch vụ chưa sẵn sàng", Toast.LENGTH_SHORT).show();
            return;
        }

        final String[] timerOptions = {"15 phút", "30 phút", "60 phút", "Tắt hẹn giờ"};
        // Chuyển đổi phút sang mili-giây
        final long[] timerValues = {15 * 60 * 1000, 30 * 60 * 1000, 60 * 60 * 1000, 0};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Hẹn giờ tắt nhạc");
        builder.setItems(timerOptions, (dialog, which) -> {
            long durationInMillis = timerValues[which];
            // Gọi phương thức trong service để đặt hẹn giờ
            musicService.setSleepTimer(durationInMillis);

            // Thông báo cho người dùng
            if (durationInMillis > 0) {
                Toast.makeText(this, "Nhạc sẽ tự tắt sau " + timerOptions[which], Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Đã tắt hẹn giờ", Toast.LENGTH_SHORT).show();
            }
        });
        builder.create().show();
    }
}
