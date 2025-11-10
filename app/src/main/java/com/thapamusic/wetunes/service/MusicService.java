package com.thapamusic.wetunes.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.PlaybackParams; // Cần import cho PlaybackParams
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import android.os.Handler;

import java.io.IOException;
import java.util.ArrayList;

import static com.thapamusic.wetunes.ui.ApplicationClass.ACTION_NEXT;
import static com.thapamusic.wetunes.ui.ApplicationClass.ACTION_PLAY;
import static com.thapamusic.wetunes.ui.ApplicationClass.ACTION_PREVIOUS;
import static com.thapamusic.wetunes.ui.ApplicationClass.CHANNEL_ID_2;

import com.thapamusic.wetunes.ActionPlaying;
import com.thapamusic.wetunes.R;
import com.thapamusic.wetunes.activity.MainActivity;
import com.thapamusic.wetunes.model.MusicFiles;
import com.thapamusic.wetunes.ui.NotificationReceiver;

public class MusicService extends Service implements MediaPlayer.OnCompletionListener {

    private final IBinder mBinder = new MyBinder();
    MediaPlayer mediaPlayer;
    // Dữ liệu của riêng Service, không phụ thuộc vào bất kỳ Activity nào
    public ArrayList<MusicFiles> musicFiles = new ArrayList<>();
    Uri uri;
    public int position = -1;
    ActionPlaying actionPlaying;
    MediaSessionCompat mediaSessionCompat;
    private final Handler timerHandler = new Handler();
    private Runnable sleepTimerRunnable;

    // Biến lưu trữ tốc độ hiện tại để áp dụng lại nếu cần
    private float currentSpeed = 1.0f;

    public float getCurrentPlaybackSpeed() {
        return currentSpeed;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaSessionCompat = new MediaSessionCompat(getBaseContext(), "My Audio");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d("MusicService", "onBind called");
        return mBinder;
    }

    public class MyBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            // SỬA LỖI: Thêm ép kiểu (cast) ở đây
            ArrayList<MusicFiles> receivedList = intent.getParcelableArrayListExtra("songList");
            if (receivedList != null) {
                this.musicFiles = receivedList;
            }


            int myPosition = intent.getIntExtra("servicePosition", -1);
            if (myPosition != -1 && this.musicFiles != null && !this.musicFiles.isEmpty()) {
                playMedia(myPosition);
            }

            String actionName = intent.getStringExtra("ActionName");
            if (actionName != null) {
                switch (actionName) {
                    case "playPause":
                        playPauseBtnClicked();
                        break;
                    case "next":
                        nextBtnClicked();
                        break;
                    case "previous":
                        prevBtnClicked();
                        break;
                }
            }
        }
        return START_STICKY;
    }

    public void playMedia(int startPosition) {
        position = startPosition;
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        createMediaPlayer(position);
        if (mediaPlayer != null) {
            mediaPlayer.start();
            // Áp dụng tốc độ đã lưu (nếu có)
            setPlaybackSpeed(currentSpeed);
        }
    }

    public void start() {
        if (mediaPlayer != null) mediaPlayer.start();
    }
    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }
    public void pause() {
        if (mediaPlayer != null) mediaPlayer.pause();
    }
    void stop() {
        if (mediaPlayer != null) mediaPlayer.stop();
    }
    void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
    public int getDuration() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
    }
    public void seekTo(int position) {
        if (mediaPlayer != null) mediaPlayer.seekTo(position);
    }
    public int getCurrentPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }

    void createMediaPlayer(int positionInner) {
        if (musicFiles == null || musicFiles.isEmpty() || positionInner < 0 || positionInner >= musicFiles.size()) {
            return; // Không làm gì nếu danh sách không hợp lệ
        }
        position = positionInner;
        uri = Uri.parse(musicFiles.get(position).getPath());
        mediaPlayer = MediaPlayer.create(getBaseContext(), uri);
        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(this);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (actionPlaying != null) {
            actionPlaying.nextBtnClicked();
            // Không cần tạo lại media player ở đây, nextBtnClicked sẽ xử lý
        }
    }

    public void setCallBack(ActionPlaying actionPlaying) {
        this.actionPlaying = actionPlaying;
    }

    void showNotification(int playPauseBtn) {
        // Sửa lỗi PendingIntent mutability
        int flag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE : 0;

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, flag);

        Intent prevIntent = new Intent(this, NotificationReceiver.class).setAction(ACTION_PREVIOUS);
        PendingIntent prevPending = PendingIntent.getBroadcast(this, 0, prevIntent, flag | PendingIntent.FLAG_UPDATE_CURRENT);

        Intent pauseIntent = new Intent(this, NotificationReceiver.class).setAction(ACTION_PLAY);
        PendingIntent pausePending = PendingIntent.getBroadcast(this, 0, pauseIntent, flag | PendingIntent.FLAG_UPDATE_CURRENT);

        Intent nextIntent = new Intent(this, NotificationReceiver.class).setAction(ACTION_NEXT);
        PendingIntent nextPending = PendingIntent.getBroadcast(this, 0, nextIntent, flag | PendingIntent.FLAG_UPDATE_CURRENT);

        byte[] picture = getAlbumArt(musicFiles.get(position).getPath());
        Bitmap thumb = (picture != null) ? BitmapFactory.decodeByteArray(picture, 0, picture.length)
                : BitmapFactory.decodeResource(getResources(), R.drawable.musicicon);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID_2)
                .setSmallIcon(playPauseBtn)
                .setLargeIcon(thumb)
                .setContentTitle(musicFiles.get(position).getTitle())
                .setContentText(musicFiles.get(position).getArtist())
                .addAction(R.drawable.ic_skip_previous, "Previous", prevPending)
                .addAction(playPauseBtn, "Pause", pausePending)
                .addAction(R.drawable.ic_skip_next, "Next", nextPending)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSessionCompat.getSessionToken()))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(contentIntent)
                .build();

        startForeground(2, notification);
    }

    // Sửa lỗi Unhandled Exception và rò rỉ tài nguyên
    private byte[] getAlbumArt(String uri) {
        if (uri == null) return null;
        android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
        byte[] art = null;
        try {
            retriever.setDataSource(uri);
            art = retriever.getEmbeddedPicture();
        } catch (Exception e) {
            Log.e("MusicService", "Error getting album art for URI: " + uri, e);
        } finally {
            try {
                retriever.release();
            } catch (IOException e) {
                Log.e("MusicService", "Error releasing MediaMetadataRetriever", e);
            }
        }
        return art;
    }

    // Các phương thức delegate
    public void playPauseBtnClicked() { if (actionPlaying != null) actionPlaying.playPauseBtnClicked(); }
    public void nextBtnClicked() { if (actionPlaying != null) actionPlaying.nextBtnClicked(); }
    public void prevBtnClicked() { if (actionPlaying != null) actionPlaying.prevBtnClicked(); }

    public void setSleepTimer(long milliseconds) {
        // Hủy bỏ bất kỳ bộ hẹn giờ nào đang chạy trước đó
        if (sleepTimerRunnable != null) {
            timerHandler.removeCallbacks(sleepTimerRunnable);
        }

        // Nếu người dùng chọn một khoảng thời gian (không phải "Tắt hẹn giờ")
        if (milliseconds > 0) {
            // Tạo một hành động mới sẽ được thực thi khi hết giờ
            sleepTimerRunnable = () -> {
                // Hành động khi hết giờ: Dừng nhạc và tự hủy service
                if (isPlaying()) {
                    pause();
                    // Bạn có thể gửi broadcast để báo cho UI cập nhật nút play/pause nếu muốn
                }
                stopSelf(); // Lệnh để service tự dừng lại
            };

            // Bắt đầu đếm ngược
            timerHandler.postDelayed(sleepTimerRunnable, milliseconds);
        }
    }

    /**
     * Phương thức điều chỉnh tốc độ phát nhạc (chỉ hoạt động trên API 23/Marshmallow trở lên).
     * @param speed Tốc độ phát (ví dụ: 0.5f, 1.0f, 1.5f, 2.0f)
     */
    public void setPlaybackSpeed(float speed) {
        currentSpeed = speed; // Lưu lại tốc độ hiện tại

        if (mediaPlayer != null) {
            // Yêu cầu API 23 trở lên để sử dụng PlaybackParams
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    PlaybackParams params = mediaPlayer.getPlaybackParams();
                    params.setSpeed(speed);
                    mediaPlayer.setPlaybackParams(params);
                    Log.d("MusicService", "Playback speed set to: " + speed);
                } catch (Exception e) {
                    Log.e("MusicService", "Error setting playback speed on API >= 23: " + e.getMessage());
                }
            } else {
                Log.w("MusicService", "Playback speed adjustment requires API 23 or higher.");
                // Có thể gửi Toast hoặc log lỗi nếu thiết bị quá cũ
            }
        }
    }
}