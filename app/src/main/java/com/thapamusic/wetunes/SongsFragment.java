package com.thapamusic.wetunes;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;

// Triển khai interface để nhận sự kiện tìm kiếm từ MainActivity
public class SongsFragment extends Fragment implements MainActivity.SearchableFragment {

    private static final String ARG_SONGS = "songs_list_data";

    RecyclerView recyclerView;
    MusicAdapter musicAdapter;
    private ArrayList<MusicFiles> songsList; // Dữ liệu của riêng Fragment này

    public SongsFragment() {
        // Required empty public constructor
    }

    /**
     * Factory method để tạo SongsFragment và truyền dữ liệu một cách an toàn.
     */
    public static SongsFragment newInstance(ArrayList<MusicFiles> songs) {
        SongsFragment fragment = new SongsFragment();
        Bundle args = new Bundle();
        // MusicFiles phải implement Parcelable để dùng được hàm này
        args.putParcelableArrayList(ARG_SONGS, songs);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            // Lấy danh sách bài hát từ arguments
            songsList = getArguments().getParcelableArrayList(ARG_SONGS);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_songs, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);

        // Sử dụng danh sách bài hát (songsList) của chính Fragment này
        if (songsList != null && !songsList.isEmpty()) {
            musicAdapter = new MusicAdapter(getContext(), songsList);
            recyclerView.setAdapter(musicAdapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        }
        return view;
    }

    // Triển khai phương thức từ interface SearchableFragment
    @Override
    public void onSearchQuery(String query) {
        if (musicAdapter != null && songsList != null) {
            String userInput = query.toLowerCase();
            ArrayList<MusicFiles> myFiles = new ArrayList<>();
            for (MusicFiles song : songsList) {
                if (song.getTitle().toLowerCase().contains(userInput)) {
                    myFiles.add(song);
                }
            }
            musicAdapter.updateList(myFiles);
        }
    }
}
