package com.thapamusic.wetunes.ui;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.*;

public class PlaylistManager {
    private static final String PREFS_NAME = "MusicAppPrefs";
    private static final String PLAYLISTS_KEY = "UserPlaylists";

    private SharedPreferences sharedPreferences;
    private Gson gson = new Gson();

    public PlaylistManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // Lấy toàn bộ danh sách playlist
    public Map<String, Set<String>> getAllPlaylists() {
        String json = sharedPreferences.getString(PLAYLISTS_KEY, null);
        if (json == null) return new HashMap<>();
        Type type = new TypeToken<HashMap<String, Set<String>>>() {}.getType();
        return gson.fromJson(json, type);
    }

    // Lưu toàn bộ danh sách playlist
    private void saveAllPlaylists(Map<String, Set<String>> playlists) {
        sharedPreferences.edit().putString(PLAYLISTS_KEY, gson.toJson(playlists)).apply();
    }

    // Lấy danh sách bài hát của 1 playlist
    public Set<String> getPlaylist(String playlistName) {
        Map<String, Set<String>> playlists = getAllPlaylists();
        Set<String> songs = playlists.get(playlistName);
        if (songs == null) {
            songs = new HashSet<>();
        }
        return songs;
    }

    // Thêm bài hát vào playlist
    public void addToPlaylist(String playlistName, String songId) {
        Map<String, Set<String>> playlists = getAllPlaylists();
        Set<String> songs = playlists.get(playlistName);
        if (songs == null) {
            songs = new HashSet<>();
        }
        songs.add(songId);
        playlists.put(playlistName, songs);
        saveAllPlaylists(playlists);
    }

    // Xóa bài hát khỏi playlist
    public void removeFromPlaylist(String playlistName, String songId) {
        Map<String, Set<String>> playlists = getAllPlaylists();
        Set<String> songs = playlists.get(playlistName);
        if (songs != null) {
            songs.remove(songId);
            saveAllPlaylists(playlists);
        }
    }

    // Tạo playlist mới
    public void createPlaylist(String name) {
        Map<String, Set<String>> playlists = getAllPlaylists();
        if (!playlists.containsKey(name)) {
            playlists.put(name, new HashSet<>());
            saveAllPlaylists(playlists);
        }
    }

    // Xóa toàn bộ playlist
    public void deletePlaylist(String name) {
        Map<String, Set<String>> playlists = getAllPlaylists();
        playlists.remove(name);
        saveAllPlaylists(playlists);
    }
}
