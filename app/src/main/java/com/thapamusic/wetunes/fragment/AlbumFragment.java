package com.thapamusic.wetunes.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.thapamusic.wetunes.activity.MainActivity;
import com.thapamusic.wetunes.R;
import com.thapamusic.wetunes.adapter.AlbumAdapter;

public class AlbumFragment extends Fragment {

    private RecyclerView recyclerView;
    private AlbumAdapter albumAdapter;
    // Không cần dùng newInstance và getArguments nữa

    public AlbumFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_album, container, false);

        // SỬA LẠI DÒNG NÀY CHO KHỚP VỚI ID TRONG XML
        recyclerView = view.findViewById(R.id.recyclerView_albums);

        // Bây giờ, dòng dưới đây sẽ không còn bị crash
        recyclerView.setHasFixedSize(true);

        // Lấy dữ liệu trực tiếp từ biến static của MainActivity
        if (MainActivity.albums != null && !MainActivity.albums.isEmpty()) {
            albumAdapter = new AlbumAdapter(getContext(), MainActivity.albums);
            recyclerView.setAdapter(albumAdapter);
            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Cập nhật lại danh sách album khi quay lại tab
        if (albumAdapter != null) {
            albumAdapter.updateList(MainActivity.albums);
        }
    }
}
