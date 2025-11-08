package com.thapamusic.wetunes;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;

public class AlbumFragment extends Fragment {

    private static final String ARG_ALBUMS = "albums_list_data";

    RecyclerView recyclerView;
    AlbumAdapter albumAdapter;
    private ArrayList<MusicFiles> albumsList; // Dữ liệu của riêng Fragment này

    public AlbumFragment() {
        // Required empty public constructor
    }

    /**
     * Factory method để tạo AlbumFragment và truyền dữ liệu một cách an toàn.
     */
    public static AlbumFragment newInstance(ArrayList<MusicFiles> albums) {
        AlbumFragment fragment = new AlbumFragment();
        Bundle args = new Bundle();
        // MusicFiles phải implement Parcelable
        args.putParcelableArrayList(ARG_ALBUMS, albums);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            // Lấy danh sách album từ arguments
            albumsList = getArguments().getParcelableArrayList(ARG_ALBUMS);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_album, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);

        // Sử dụng danh sách album (albumsList) của chính Fragment này
        if (albumsList != null && !albumsList.isEmpty()) {
            albumAdapter = new AlbumAdapter(getContext(), albumsList);
            recyclerView.setAdapter(albumAdapter);
            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        }
        return view;
    }
}
