package com.thapamusic.wetunes;

import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.util.Log;
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

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.MyHolder> {

    private final Context mContext;
    // Chỉ cần danh sách các album duy nhất
    private ArrayList<MusicFiles> albumFiles;

    // **SỬA LỖI**: Khôi phục lại constructor 2 tham số ban đầu
    public AlbumAdapter(Context mContext, ArrayList<MusicFiles> albumFiles) {
        this.mContext = mContext;
        this.albumFiles = albumFiles;
    }
    View view;
    void updateList(ArrayList<MusicFiles> newList) {
        albumFiles = new ArrayList<>();
        albumFiles.addAll(newList);
        notifyDataSetChanged(); // Báo cho RecyclerView biết dữ liệu đã thay đổi và cần cập nhật UI
    }
    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.album_item, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        MusicFiles currentAlbum = albumFiles.get(position);
        holder.album_name.setText(currentAlbum.getAlbum());

        // Lấy ảnh bìa cho album
        byte[] image = getAlbumArt(currentAlbum.getPath());
        if (image != null) {
            Glide.with(mContext).asBitmap().load(image).into(holder.album_image);
        } else {
            Glide.with(mContext).load(R.drawable.musicicon).into(holder.album_image);
        }

        holder.itemView.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) {
                Intent intent = new Intent(mContext, AlbumDetails.class);
                // **QUAN TRỌNG**: Chỉ cần gửi tên album đi là đủ
                intent.putExtra("albumName", albumFiles.get(currentPosition).getAlbum());
                mContext.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return albumFiles != null ? albumFiles.size() : 0;
    }

    public static class MyHolder extends RecyclerView.ViewHolder {
        ImageView album_image;
        TextView album_name;

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            album_image = itemView.findViewById(R.id.album_image);
            album_name = itemView.findViewById(R.id.album_name);
        }
    }

    // Phương thức getAlbumArt đã được sửa lỗi
    private byte[] getAlbumArt(String uri) {
        if (uri == null) return null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        byte[] art = null;
        try {
            retriever.setDataSource(uri);
            art = retriever.getEmbeddedPicture();
        } catch (Exception e) {
            Log.e("AlbumAdapter", "Error getting album art for URI: " + uri, e);
        } finally {
            try {
                retriever.release();
            } catch (IOException e) {
                Log.e("AlbumAdapter", "Error releasing MediaMetadataRetriever", e);
            }
        }
        return art;
    }
}
