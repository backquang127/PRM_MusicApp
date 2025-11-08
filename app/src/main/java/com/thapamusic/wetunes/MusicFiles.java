package com.thapamusic.wetunes;

import android.os.Parcel;
import android.os.Parcelable;

// SỬA LỖI: Thêm "implements Parcelable" vào khai báo lớp
public class MusicFiles implements Parcelable {
    private String path;
    private String title;
    private String artist;
    private String album;
    private String duration;
    private String id;

    // Constructor chính vẫn giữ nguyên
    public MusicFiles(String path, String title, String artist, String album, String duration, String id) {
        this.path = path;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.duration = duration;
        this.id = id;
    }

    // Constructor rỗng cũng giữ nguyên
    public MusicFiles() {
    }

    // --- BẮT ĐẦU PHẦN CODE CỦA PARCELABLE ĐƯỢC THÊM VÀO ---

    // Constructor đặc biệt này dùng để tái tạo lại đối tượng từ một Parcel.
    protected MusicFiles(Parcel in) {
        path = in.readString();
        title = in.readString();
        artist = in.readString();
        album = in.readString();
        duration = in.readString();
        id = in.readString();
    }

    // Phương thức tĩnh CREATOR là bắt buộc. Nó tạo ra các instance mới của lớp Parcelable của bạn.
    public static final Creator<MusicFiles> CREATOR = new Creator<MusicFiles>() {
        @Override
        public MusicFiles createFromParcel(Parcel in) {
            return new MusicFiles(in);
        }

        @Override
        public MusicFiles[] newArray(int size) {
            return new MusicFiles[size];
        }
    };

    @Override
    public int describeContents() {
        return 0; // Thường trả về 0 trừ khi có các đối tượng đặc biệt
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // Ghi các thuộc tính của đối tượng vào Parcel theo một thứ tự nhất định.
        // Thứ tự này phải khớp với thứ tự đọc trong constructor MusicFiles(Parcel in).
        dest.writeString(path);
        dest.writeString(title);
        dest.writeString(artist);
        dest.writeString(album);
        dest.writeString(duration);
        dest.writeString(id);
    }

    // --- KẾT THÚC PHẦN CODE CỦA PARCELABLE ---


    // Các phương thức getter và setter của bạn vẫn được giữ nguyên
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
