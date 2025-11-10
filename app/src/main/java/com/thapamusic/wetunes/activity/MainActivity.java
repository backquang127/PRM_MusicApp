package com.thapamusic.wetunes.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;
import com.thapamusic.wetunes.model.MusicFiles;
import com.thapamusic.wetunes.R;
import com.thapamusic.wetunes.fragment.AlbumFragment;
import com.thapamusic.wetunes.fragment.FavoritesFragment;
import com.thapamusic.wetunes.fragment.PlaylistsFragment;
import com.thapamusic.wetunes.fragment.SongsFragment;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    public static final int REQUEST_CODE = 1;
    private static final String MY_SORT_PREF = "SortOrder";

    // --- SỬA LỖI: CHUYỂN CÁC DANH SÁCH THÀNH STATIC ---
    // Để các Fragment có thể truy cập và hiển thị dữ liệu
    public static ArrayList<MusicFiles> musicFiles = new ArrayList<>();
    public static ArrayList<MusicFiles> albums = new ArrayList<>();

    // Các biến trạng thái
    static boolean shuffleBoolean = false;
    static boolean repeatBoolean = false;

    private ViewPager viewPager;
    private ViewPagerAdapter viewPagerAdapter;
    private TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissions();
    }

    private void requestPermissions() {
        String[] permissionsToRequest;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest = new String[]{Manifest.permission.READ_MEDIA_AUDIO};
        } else {
            permissionsToRequest = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        }

        if (ContextCompat.checkSelfPermission(this, permissionsToRequest[0]) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissionsToRequest, REQUEST_CODE);
        } else {
            // Đã có quyền, tiến hành tải nhạc và khởi tạo giao diện
            loadAllAudio();
            initViewPager();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadAllAudio();
                initViewPager();
            } else {
                Toast.makeText(this, "Cần cấp quyền để ứng dụng hoạt động!", Toast.LENGTH_LONG).show();
                // Có thể đóng ứng dụng hoặc yêu cầu lại quyền
            }
        }
    }

    private void initViewPager() {
        viewPager = findViewById(R.id.viewpager);
        tabLayout = findViewById(R.id.tab_layout);
        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());

        // Thêm các Fragment vào Adapter
        viewPagerAdapter.addFragment(new FavoritesFragment(), "Yêu thích");
        viewPagerAdapter.addFragment(new SongsFragment(), "Bài hát");
        viewPagerAdapter.addFragment(new AlbumFragment(), "Album");
        viewPagerAdapter.addFragment(new PlaylistsFragment(), "Playlist");

        viewPager.setAdapter(viewPagerAdapter);
        tabLayout.setupWithViewPager(viewPager);
    }

    // --- SỬA LỖI: Phương thức loadAllAudio được cập nhật để dùng biến static ---
    private void loadAllAudio() {
        SharedPreferences preferences = getSharedPreferences(MY_SORT_PREF, MODE_PRIVATE);
        String sortOrder = preferences.getString("sorting", "sortByTitle");

        // Luôn xóa dữ liệu cũ trước khi quét lại
        musicFiles.clear();
        albums.clear();

        ArrayList<String> duplicateAlbums = new ArrayList<>();

        String order = null;
        switch (sortOrder) {
            case "sortByDate":
                order = MediaStore.Audio.Media.DATE_ADDED + " DESC";
                break;
            case "sortBySize":
                order = MediaStore.Audio.Media.SIZE + " DESC";
                break;
            default: // Mặc định là "sortByTitle"
                order = MediaStore.Audio.Media.TITLE + " ASC";
                break;
        }

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media._ID
        };

        try (Cursor cursor = getContentResolver().query(uri, projection, null, null, order)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String album = cursor.getString(0);
                    String title = cursor.getString(1);
                    String duration = cursor.getString(2);
                    String path = cursor.getString(3);
                    String artist = cursor.getString(4);
                    String id = cursor.getString(5);

                    MusicFiles musicFile = new MusicFiles(path, title, artist, album, duration, id);

                    // Thêm vào danh sách tất cả bài hát
                    musicFiles.add(musicFile);

                    // Thêm vào danh sách album (không trùng lặp)
                    if (album != null && !duplicateAlbums.contains(album)) {
                        albums.add(musicFile);
                        duplicateAlbums.add(album);
                    }
                }
            }
        }
        Log.d("LoadAudio", "Đã quét xong: " + musicFiles.size() + " bài hát, " + albums.size() + " albums.");
    }

    // --- CÁC PHƯƠNG THỨC MENU ĐÃ ĐƯỢC CẬP NHẬT ---

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem menuItem = menu.findItem(R.id.search_option);

        if (menuItem != null) {
            SearchView searchView = (SearchView) menuItem.getActionView();
            if (searchView != null) {
                searchView.setOnQueryTextListener(this);
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_refresh) {
            refreshMusicList();
            return true;
        }

        SharedPreferences.Editor editor = getSharedPreferences(MY_SORT_PREF, MODE_PRIVATE).edit();
        boolean shouldRecreate = false;

        if (itemId == R.id.by_title) {
            editor.putString("sorting", "sortByTitle");
            shouldRecreate = true;
        } else if (itemId == R.id.by_date) {
            editor.putString("sorting", "sortByDate");
            shouldRecreate = true;
        } else if (itemId == R.id.by_size) {
            editor.putString("sorting", "sortBySize");
            shouldRecreate = true;
        }

        if (shouldRecreate) {
            editor.apply();
            this.recreate();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void refreshMusicList() {
        Log.d("Refresh", "Bắt đầu làm mới danh sách nhạc.");
        loadAllAudio();
        // Khởi tạo lại ViewPager để cập nhật tất cả các Fragment
        initViewPager();
        Toast.makeText(this, "Đã làm mới danh sách nhạc!", Toast.LENGTH_SHORT).show();
    }

    // --- CÁC PHƯƠNG THỨC KHÁC GIỮ NGUYÊN ---

    @Override
    public boolean onQueryTextSubmit(String query) { return false; }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (viewPager != null && viewPagerAdapter != null) {
            Fragment currentFragment = viewPagerAdapter.getItem(viewPager.getCurrentItem());
            if (currentFragment instanceof SearchableFragment) {
                ((SearchableFragment) currentFragment).onSearchQuery(newText);
            }
        }
        return true;
    }

    // Interface để giao tiếp với các Fragment
    public interface SearchableFragment {
        void onSearchQuery(String query);
    }

    // ViewPagerAdapter không cần newInstance nữa vì các Fragment sẽ tự lấy dữ liệu từ biến static
    public static class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> fragments = new ArrayList<>();
        private final List<String> titles = new ArrayList<>();

        public ViewPagerAdapter(@NonNull FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        void addFragment(Fragment fragment, String title) {
            fragments.add(fragment);
            titles.add(title);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return titles.get(position);
        }
    }
}
