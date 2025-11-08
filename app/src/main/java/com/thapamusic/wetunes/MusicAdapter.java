package com.thapamusic.wetunes;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MyViewHolder> {

    private final Context mContext;
    // Bỏ 'static', đây là dữ liệu của riêng adapter này
    private ArrayList<MusicFiles> mFiles;

    MusicAdapter(Context mContext, ArrayList<MusicFiles> mFiles) {
        this.mContext = mContext;
        this.mFiles = mFiles;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.music_items, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, @SuppressLint("RecyclerView") int position) {
        MusicFiles currentFile = mFiles.get(position);
        holder.file_name.setText(currentFile.getTitle());
        holder.artist_name.setText(currentFile.getArtist());

        // Xử lý Duration an toàn
        try {
            long durationMs = Long.parseLong(currentFile.getDuration());
            holder.duration.setText(formatTime(durationMs));
        } catch (NumberFormatException e) {
            holder.duration.setText("--:--");
            Log.e("MusicAdapter", "Invalid duration format: " + currentFile.getDuration());
        }

        byte[] image = getAlbumArt(currentFile.getPath());
        Glide.with(mContext)
                .load(image != null ? image : R.drawable.musicicon)
                .into(holder.album_art);

        holder.itemView.setOnClickListener(v -> {
            // Sửa lỗi vị trí: Dùng getAdapterPosition()
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) {
                Intent intent = new Intent(mContext, PlayerActivity.class);
                // Truyền toàn bộ danh sách và vị trí, không dùng static
                intent.putExtra("songList", mFiles);
                intent.putExtra("position", currentPosition);
                intent.putExtra("sender", "MusicAdapter"); // Thêm sender để PlayerActivity biết ai gọi nó
                mContext.startActivity(intent);
                // Cập nhật giao diện mini-player nên được thực hiện qua một cơ chế khác (Service/Broadcast)
                // thay vì gọi trực tiếp như thế này
                // NowPlayingFragmentBottom.playPauseBtn.setImageResource(R.drawable.ic_pause);
            }
        });

        holder.menuMore.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(mContext, v);
            popupMenu.getMenuInflater().inflate(R.menu.popup, popupMenu.getMenu());
            popupMenu.show();
            popupMenu.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                // Sửa lỗi vị trí và dùng if-else
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION) {
                    if (itemId == R.id.delete) {
                        deleteFile(currentPosition, v);
                    }
                    // Thêm các case khác nếu có
                }
                return true;
            });
        });
    }

    // Viết lại hàm formatTime cho chính xác
    private String formatTime(long durationMs) {
        long totalSeconds = durationMs / 1000;
        long seconds = totalSeconds % 60;
        long minutes = totalSeconds / 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private void deleteFile(int position, View v) {
        // Lấy thông tin file trước khi xóa khỏi danh sách
        MusicFiles fileToDelete = mFiles.get(position);
        Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.parseLong(fileToDelete.getId()));
        File file = new File(fileToDelete.getPath());

        try {
            if (file.delete()) {
                // Chỉ xóa khỏi MediaStore nếu xóa file vật lý thành công
                mContext.getContentResolver().delete(contentUri, null, null);
                mFiles.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, mFiles.size());
                Snackbar.make(v, "File Deleted: " + fileToDelete.getTitle(), Snackbar.LENGTH_LONG).show();
            } else {
                Snackbar.make(v, "Cannot delete file. It might be on an SD card or protected.", Snackbar.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e("MusicAdapter", "Error deleting file: " + fileToDelete.getPath(), e);
            Snackbar.make(v, "An error occurred while deleting the file.", Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public int getItemCount() {
        return mFiles != null ? mFiles.size() : 0;
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView file_name, artist_name, duration;
        ImageView album_art, menuMore;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            file_name = itemView.findViewById(R.id.music_file_name);
            artist_name = itemView.findViewById(R.id.music_artist_name);
            duration = itemView.findViewById(R.id.duration);
            album_art = itemView.findViewById(R.id.music_img);
            menuMore = itemView.findViewById(R.id.menuMore);
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
            Log.e("MusicAdapter", "Error getting album art: " + uri, e);
        } finally {
            try {
                retriever.release();
            } catch (IOException e) {
                Log.e("MusicAdapter", "Error releasing retriever", e);
            }
        }
        return art;
    }

    // Cập nhật lại danh sách một cách an toàn
    @SuppressLint("NotifyDataSetChanged")
    void updateList(ArrayList<MusicFiles> musicFilesArrayList) {
        mFiles = new ArrayList<>(musicFilesArrayList); // Tạo một bản sao mới
        notifyDataSetChanged(); // Cần thiết để cập nhật toàn bộ danh sách khi tìm kiếm
    }
}
