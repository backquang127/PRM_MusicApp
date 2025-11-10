package com.thapamusic.wetunes.ui;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

public class FavoritesManager {
    private static final String PREFS_NAME = "MusicAppPrefs";
    private static final String FAVORITES_KEY = "FavoriteSongs";
    private SharedPreferences sharedPreferences;
    private Gson gson = new Gson();

    public FavoritesManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public Set<String> getFavoriteSongs() {
        String json = sharedPreferences.getString(FAVORITES_KEY, null);
        if (json == null) {
            return new HashSet<>();
        }
        Type type = new TypeToken<HashSet<String>>() {}.getType();
        return gson.fromJson(json, type);
    }

    public void saveFavoriteSongs(Set<String> favoriteSongIds) {
        String json = gson.toJson(favoriteSongIds);
        sharedPreferences.edit().putString(FAVORITES_KEY, json).apply();
    }

    public boolean isFavorite(String songId) {
        return getFavoriteSongs().contains(songId);
    }

    public void addFavorite(String songId) {
        Set<String> favorites = getFavoriteSongs();
        favorites.add(songId);
        saveFavoriteSongs(favorites);
    }

    public void removeFavorite(String songId) {
        Set<String> favorites = getFavoriteSongs();
        favorites.remove(songId);
        saveFavoriteSongs(favorites);
    }
}
