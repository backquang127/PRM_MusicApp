package com.thapamusic.wetunes;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.io.IOException;

public class NowPlayingFragmentBottom extends Fragment implements ServiceConnection {

    // Bỏ tất cả các biến static. Các view này là của riêng Fragment.
    private ImageView nextBtn, prevBtn, albumArt;
    private TextView artist, songName;
    private FloatingActionButton playPauseBtn;
    private RelativeLayout bottomBar;

    private MusicService musicService;
    private boolean isBound = false;

    public NowPlayingFragmentBottom() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_now_playing_bottom, container, false);

        // Khởi tạo views (không còn static)
        artist = view.findViewById(R.id.song_artist_miniPlayer);
        songName = view.findViewById(R.id.song_name_miniPlayer);
        albumArt = view.findViewById(R.id.bottom_album_art);
        nextBtn = view.findViewById(R.id.skip_next_bottom);
        prevBtn = view.findViewById(R.id.skip_prev_bottom);
        playPauseBtn = view.findViewById(R.id.play_pause_miniPlayer);
        bottomBar = view.findViewById(R.id.card_bottom_player);

        setupClickListeners();

        return view;
    }

    private void setupClickListeners() {
        nextBtn.setOnClickListener(v -> {
            if (musicService != null) {
                musicService.nextBtnClicked();
                // Giao diện sẽ được cập nhật bởi onServiceConnected hoặc một callback
            }
        });

        prevBtn.setOnClickListener(v -> {
            if (musicService != null) {
                musicService.prevBtnClicked();
            }
        });

        playPauseBtn.setOnClickListener(v -> {
            if (musicService != null) {
                musicService.playPauseBtnClicked();
            }
        });

        // Thêm OnClickListener cho toàn bộ layout để mở PlayerActivity
        bottomBar.setOnClickListener(v -> {
            if (musicService != null && musicService.isPlaying()) {
                Intent intent = new Intent(getContext(), PlayerActivity.class);
                // Gửi dữ liệu cần thiết để PlayerActivity có thể tự khôi phục trạng thái
                intent.putExtra("songList", musicService.musicFiles);
                intent.putExtra("position", musicService.position);
                intent.putExtra("sender", "NowPlayingFragment");
                startActivity(intent);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Khi fragment quay trở lại, kết nối với service
        Intent intent = new Intent(getContext(), MusicService.class);
        if (getActivity() != null) {
            getActivity().bindService(intent, this, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Hủy kết nối khi fragment không còn hiển thị
        if (getActivity() != null && isBound) {
            getActivity().unbindService(this);
            isBound = false;
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        MusicService.MyBinder binder = (MusicService.MyBinder) service;
        musicService = binder.getService();
        isBound = true;
        Log.d("NowPlayingFragment", "Service Connected");

        // Khi kết nối thành công, cập nhật giao diện ngay lập tức
        updateUI();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        musicService = null;
        isBound = false;
        Log.d("NowPlayingFragment", "Service Disconnected");
    }

    // Một phương thức tập trung để cập nhật toàn bộ giao diện
    public void updateUI() {
        if (isAdded() && musicService != null && musicService.musicFiles != null && !musicService.musicFiles.isEmpty() && musicService.position != -1) {
            bottomBar.setVisibility(View.VISIBLE);

            MusicFiles currentSong = musicService.musicFiles.get(musicService.position);
            songName.setText(currentSong.getTitle());
            artist.setText(currentSong.getArtist());

            byte[] art = getAlbumArt(currentSong.getPath());
            Glide.with(requireContext())
                    .load(art != null ? art : R.drawable.musicicon)
                    .into(albumArt);

            if (musicService.isPlaying()) {
                playPauseBtn.setImageResource(R.drawable.ic_pause);
            } else {
                playPauseBtn.setImageResource(R.drawable.ic_play);
            }
        } else if (isAdded()) {
            bottomBar.setVisibility(View.GONE);
        }
    }

    // Sửa lỗi Unhandled Exception và rò rỉ tài nguyên
    private byte[] getAlbumArt(String uri) {
        if (uri == null) return null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        byte[] art = null;
        try {
            retriever.setDataSource(uri);
            art = retriever.getEmbeddedPicture();
        } catch (Exception e) {
            Log.e("NowPlayingFragment", "Error getting album art for URI: " + uri, e);
        } finally {
            try {
                retriever.release();
            } catch (IOException e) {
                Log.e("NowPlayingFragment", "Error releasing MediaMetadataRetriever", e);
            }
        }
        return art;
    }
}
