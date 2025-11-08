package com.thapamusic.wetunes;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.util.ArrayList;

public class AlbumDetails extends AppCompatActivity {

    RecyclerView recyclerView;
    ImageView albumPhoto;
    TextView passAlbumName;
    String albumName;
    ArrayList<MusicFiles> albumSongs = new ArrayList<>();
    AlbumDetailsAdapter albumDetailsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_details);

        recyclerView = findViewById(R.id.recyclerView);
        albumPhoto = findViewById(R.id.albumPhoto);
        passAlbumName = findViewById(R.id.alb_name);

        // Lấy tên album từ Intent mà AlbumAdapter đã gửi
        albumName = getIntent().getStringExtra("albumName");

        // Tải danh sách bài hát cho album này một cách độc lập
        if (albumName != null) {
            passAlbumName.setText(albumName);
            albumSongs = getSongsForAlbum(albumName);
        }

        if (!albumSongs.isEmpty()) {
            // Thiết lập RecyclerView với danh sách bài hát đã tải
            albumDetailsAdapter = new AlbumDetailsAdapter(this, albumSongs);
            recyclerView.setAdapter(albumDetailsAdapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));

            // Lấy ảnh bìa từ bài hát đầu tiên
            byte[] image = getAlbumArt(albumSongs.get(0).getPath());
            if (image != null) {
                Glide.with(this).load(image).into(albumPhoto);
            } else {
                Glide.with(this).load(R.drawable.musicicon).into(albumPhoto);
            }
        }
    }

    // Phương thức này tự query MediaStore để lấy các bài hát cho một album cụ thể
    private ArrayList<MusicFiles> getSongsForAlbum(String targetAlbumName) {
        ArrayList<MusicFiles> tempAudioList = new ArrayList<>();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media._ID
        };

        // Điều kiện WHERE để chỉ lấy các bài hát từ album này
        String selection = MediaStore.Audio.Media.ALBUM + "=?";
        String[] selectionArgs = new String[]{targetAlbumName};

        try (Cursor cursor = getContentResolver().query(uri, projection, selection, selectionArgs, null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String album = cursor.getString(0);
                    String title = cursor.getString(1);
                    String duration = cursor.getString(2);
                    String path = cursor.getString(3);
                    String artist = cursor.getString(4);
                    String id = cursor.getString(5);

                    MusicFiles musicFile = new MusicFiles(path, title, artist, album, duration, id);
                    tempAudioList.add(musicFile);
                }
            }
        }
        return tempAudioList;
    }

    private byte[] getAlbumArt(String uri) {
        if (uri == null) return null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        byte[] art = null;
        try {
            retriever.setDataSource(uri);
            art = retriever.getEmbeddedPicture();
        } catch (Exception e) {
            Log.e("AlbumDetails", "Error getting album art for URI: " + uri, e);
        } finally {
            try {
                retriever.release();
            } catch (IOException e) {
                Log.e("AlbumDetails", "Error releasing MediaMetadataRetriever", e);
            }
        }
        return art;
    }
}
