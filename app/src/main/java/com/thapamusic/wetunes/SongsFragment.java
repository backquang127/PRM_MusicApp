package com.thapamusic.wetunes;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;

// Triển khai interface để nhận sự kiện tìm kiếm từ MainActivity
public class SongsFragment extends Fragment implements MainActivity.SearchableFragment {

    private RecyclerView recyclerView;
    private MusicAdapter musicAdapter;
    // Không cần newInstance, getArguments, hoặc biến songsList cục bộ nữa

    public SongsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_songs, container, false);
        // SỬA LẠI ID CHO ĐÚNG VỚI FILE LAYOUT CỦA BẠN
        recyclerView = view.findViewById(R.id.recyclerView_songs);
        recyclerView.setHasFixedSize(true);

        // Lấy dữ liệu trực tiếp từ biến static của MainActivity
        // và chỉ thiết lập Adapter khi có dữ liệu
        if (MainActivity.musicFiles != null && !MainActivity.musicFiles.isEmpty()) {
            musicAdapter = new MusicAdapter(getContext(), MainActivity.musicFiles);
            recyclerView.setAdapter(musicAdapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Cập nhật RecyclerView mỗi khi người dùng quay lại tab này
        if (musicAdapter != null) {
            musicAdapter.updateList(MainActivity.musicFiles);
        }
    }

    // Triển khai phương thức từ interface SearchableFragment
    @Override
    public void onSearchQuery(String query) {
        if (musicAdapter != null) {
            String userInput = query.toLowerCase();
            ArrayList<MusicFiles> myFiles = new ArrayList<>();
            // Luôn tìm kiếm trên danh sách đầy đủ từ MainActivity
            for (MusicFiles song : MainActivity.musicFiles) {
                if (song.getTitle().toLowerCase().contains(userInput)) {
                    myFiles.add(song);
                }
            }
            musicAdapter.updateList(myFiles);
        }
    }
}
