package com.thapamusic.wetunes;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.view.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.*;

public class PlaylistsFragment extends Fragment {
    private RecyclerView recyclerView;
    private MusicAdapter adapter;
    private PlaylistManager playlistManager;
    private ArrayList<MusicFiles> playlistSongs = new ArrayList<>();

    public PlaylistsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);
        recyclerView = view.findViewById(R.id.recyclerView_favorites); // Dùng lại layout có sẵn
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        playlistManager = new PlaylistManager(getContext());

        // Khi mới vào, cho phép chọn playlist muốn xem
        showPlaylistSelectionDialog();

        return view;
    }

    private void showPlaylistSelectionDialog() {
        Map<String, Set<String>> allPlaylists = playlistManager.getAllPlaylists();
        String[] names = allPlaylists.keySet().toArray(new String[0]);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Chọn Playlist");
        builder.setItems(names.length > 0 ? names : new String[]{"(Chưa có playlist)"}, (dialog, which) -> {
            if (names.length > 0)
                loadPlaylistSongs(names[which]);
        });
        builder.setPositiveButton("Tạo Playlist Mới", (d, w) -> createNewPlaylist());
        builder.show();
    }

    private void createNewPlaylist() {
        EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        new AlertDialog.Builder(getContext())
                .setTitle("Nhập tên Playlist mới")
                .setView(input)
                .setPositiveButton("Tạo", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        playlistManager.createPlaylist(name);
                        loadPlaylistSongs(name);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void loadPlaylistSongs(String playlistName) {
        playlistSongs.clear();
        Set<String> songIds = playlistManager.getPlaylist(playlistName);

        for (MusicFiles song : MainActivity.musicFiles) {
            if (songIds.contains(song.getId())) {
                playlistSongs.add(song);
            }
        }

        adapter = new MusicAdapter(getContext(), playlistSongs);
        recyclerView.setAdapter(adapter);
    }
}
