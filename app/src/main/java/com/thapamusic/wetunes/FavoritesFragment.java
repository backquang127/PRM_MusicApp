package com.thapamusic.wetunes;


import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.Set;

public class FavoritesFragment extends Fragment {

    private RecyclerView recyclerView;
    private MusicAdapter musicAdapter;
    private ArrayList<MusicFiles> favoriteSongs = new ArrayList<>();
    private FavoritesManager favoritesManager;

    public FavoritesFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);
        recyclerView = view.findViewById(R.id.recyclerView_favorites);
        recyclerView.setHasFixedSize(true);

        favoritesManager = new FavoritesManager(getContext());

        // Luôn gọi phương thức để tải dữ liệu khi Fragment được tạo
        loadFavoriteSongs();

        if (!favoriteSongs.isEmpty()) {
            musicAdapter = new MusicAdapter(getContext(), favoriteSongs);
            recyclerView.setAdapter(musicAdapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        }

        return view;
    }

    private void loadFavoriteSongs() {
        // Xóa danh sách cũ để tránh trùng lặp khi tải lại
        favoriteSongs.clear();

        // Lấy danh sách ID các bài hát yêu thích
        Set<String> favoriteIds = favoritesManager.getFavoriteSongs();

        // Lấy danh sách tất cả các bài hát từ MainActivity
        ArrayList<MusicFiles> allSongs = MainActivity.musicFiles; // Giả sử musicFiles là public static trong MainActivity

        if (allSongs != null && !allSongs.isEmpty()) {
            // Lọc ra các bài hát có ID nằm trong danh sách yêu thích
            for (MusicFiles song : allSongs) {
                if (favoriteIds.contains(song.getId())) {
                    favoriteSongs.add(song);
                }
            }
        }
    }

    /**
     * Cần một phương thức để làm mới danh sách khi người dùng
     * thêm/bỏ yêu thích từ nơi khác và quay lại tab này.
     */
    @Override
    public void onResume() {
        super.onResume();
        // Tải lại dữ liệu và cập nhật adapter
        loadFavoriteSongs();
        if (musicAdapter != null) {
            musicAdapter.updateList(favoriteSongs);
        }
    }
}

