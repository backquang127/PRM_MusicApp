package com.thapamusic.wetunes;

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
import android.view.Menu;
import android.view.MenuItem;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    public static final int REQUEST_CODE = 1;
    private static final String MY_SORT_PREF = "SortOrder";

    // Dữ liệu là của riêng MainActivity, không còn static
    private ArrayList<MusicFiles> musicFilesList = new ArrayList<>();
    private ArrayList<MusicFiles> albumsList = new ArrayList<>();

    // Các biến static cho trạng thái có thể tạm chấp nhận, nhưng tốt nhất nên nằm trong Service.
    static boolean shuffleBoolean = false;
    static boolean repeatBoolean = false;

    private ViewPager viewPager;
    private ViewPagerAdapter viewPagerAdapter;

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
                // Có thể hiển thị thông báo cho người dùng biết tại sao cần quyền
                ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE);
            }
        }
    }

    private void initViewPager() {
        viewPager = findViewById(R.id.viewpager);
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());

        // Truyền dữ liệu cho Fragment một cách an toàn qua newInstance
        viewPagerAdapter.addFragment(SongsFragment.newInstance(musicFilesList), "Songs");
        viewPagerAdapter.addFragment(AlbumFragment.newInstance(albumsList), "Albums");

        viewPager.setAdapter(viewPagerAdapter);
        tabLayout.setupWithViewPager(viewPager);
    }

    // Interface để giao tiếp với các Fragment có khả năng tìm kiếm
    public interface SearchableFragment {
        void onSearchQuery(String query);
    }

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

    private void loadAllAudio() {
        SharedPreferences preferences = getSharedPreferences(MY_SORT_PREF, MODE_PRIVATE);
        String sortOrder = preferences.getString("sorting", "sortByName");

        ArrayList<String> duplicateAlbums = new ArrayList<>();
        musicFilesList.clear();
        albumsList.clear();

        String order = null;
        switch (sortOrder) {
            case "sortByTitle":
                order = MediaStore.Audio.Media.TITLE + " ASC";
                break;
            case "sortByDate":
                order = MediaStore.Audio.Media.DATE_ADDED + " DESC";
                break;
            case "sortBySize":
                order = MediaStore.Audio.Media.SIZE + " DESC";
                break;
            default:
                order = MediaStore.Audio.Media.TITLE + " ASC"; // Mặc định
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
                    musicFilesList.add(musicFile);

                    if (album != null && !duplicateAlbums.contains(album)) {
                        albumsList.add(musicFile);
                        duplicateAlbums.add(album);
                    }
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search, menu);
        MenuItem menuItem = menu.findItem(R.id.search_option);
        SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setOnQueryTextListener(this);
        return super.onCreateOptionsMenu(menu);
    }

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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        SharedPreferences.Editor editor = getSharedPreferences(MY_SORT_PREF, MODE_PRIVATE).edit();

        int itemId = item.getItemId();
        if (itemId == R.id.by_title) {
            editor.putString("sorting", "sortByTitle");
        } else if (itemId == R.id.by_date) {
            editor.putString("sorting", "sortByDate");
        } else if (itemId == R.id.by_size) {
            editor.putString("sorting", "sortBySize");
        }
        editor.apply();
        this.recreate(); // Khởi động lại Activity để áp dụng sắp xếp mới

        return super.onOptionsItemSelected(item);
    }
}
