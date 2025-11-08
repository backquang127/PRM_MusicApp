package com.thapamusic.wetunes;

import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.util.Log; // Import Log
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale; // Import Locale

public class AlbumDetailsAdapter extends RecyclerView.Adapter<AlbumDetailsAdapter.MyHolder> {

    private final Context mContext;
    // Xóa 'static': Danh sách này chỉ thuộc về adapter này
    private final ArrayList<MusicFiles> albumFiles;

    // Bỏ các biến static không an toàn:
    // static byte[] passAlbumImage;
    // static boolean checkAlbumPass;
    // Dữ liệu nên được quản lý bởi từng item hoặc truyền qua Intent.

    public AlbumDetailsAdapter(Context mContext, ArrayList<MusicFiles> albumFiles) {
        this.mContext = mContext;
        this.albumFiles = albumFiles;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.album_music_items, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        MusicFiles currentSong = albumFiles.get(position);

        holder.album_name.setText(currentSong.getTitle());
        holder.artist_name.setText(currentSong.getArtist());

        // Xử lý Duration an toàn
        try {
            long durationMs = Long.parseLong(currentSong.getDuration());
            holder.alb_duration.setText(formattedTime(durationMs));
        } catch (NumberFormatException e) {
            holder.alb_duration.setText("--:--"); // Hiển thị giá trị mặc định nếu có lỗi
            Log.e("AlbumDetailsAdapter", "Invalid duration format: " + currentSong.getDuration());
        }

        byte[] image = getAlbumArt(currentSong.getPath());
        Glide.with(mContext)
                .load(image != null ? image : R.drawable.musicicon)
                .into(holder.album_image);

        holder.itemView.setOnClickListener(v -> {
            // Sửa lỗi vị trí: Dùng getAdapterPosition()
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) {
                Intent intent = new Intent(mContext, PlayerActivity.class);
                intent.putExtra("sender", "albumDetails");
                // Quan trọng: Truyền toàn bộ danh sách bài hát của album vào PlayerActivity
                // để nó có thể tự quản lý, không phụ thuộc vào biến static.
                intent.putExtra("albumSongs", albumFiles);
                intent.putExtra("position", currentPosition); // Truyền vị trí của bài hát được nhấn
                mContext.startActivity(intent);
            }
        });
    }

    // Viết lại hàm formattedTime cho an toàn và chính xác hơn
    private String formattedTime(long durationMs) {
        long totalSeconds = durationMs / 1000;
        long seconds = totalSeconds % 60;
        long minutes = totalSeconds / 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    @Override
    public int getItemCount() {
        return albumFiles != null ? albumFiles.size() : 0;
    }

    public static class MyHolder extends RecyclerView.ViewHolder {
        ImageView album_image;
        TextView album_name, artist_name, alb_duration;

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            album_image = itemView.findViewById(R.id.music_img);
            album_name = itemView.findViewById(R.id.music_file_name);
            artist_name = itemView.findViewById(R.id.artis_name);
            alb_duration = itemView.findViewById(R.id.alb_duration);
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
            Log.e("AlbumDetailsAdapter", "Error getting album art for URI: " + uri, e);
        } finally {
            try {
                retriever.release();
            } catch (IOException e) {
                Log.e("AlbumDetailsAdapter", "Error releasing MediaMetadataRetriever", e);
            }
        }
        return art;
    }
}
