package com.discflux.android.spotifystreamer;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Phil on 09/06/2015.
 */
public class TrackInfo implements Parcelable {

    private String trackName;
    private String albumName;
    private String imageUrl;
    private String thumbnailUrl;
    private String previewUrl;

    public TrackInfo(String trackName, String albumName, String imageUrl, String thumbnailUrl, String previewUrl) {
        this.trackName = trackName;
        this.albumName = albumName;
        this.imageUrl = imageUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.previewUrl = previewUrl;
    }

    public String getTrackName() {
        return trackName;
    }

    public String getAlbumName() {
        return albumName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    @Override
    public String toString() {
        return trackName + ", " + albumName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(trackName);
        dest.writeString(albumName);
        dest.writeString(imageUrl);
        dest.writeString(thumbnailUrl);
        dest.writeString(previewUrl);
    }

    private TrackInfo(Parcel in) {
        trackName = in.readString();
        albumName = in.readString();
        imageUrl = in.readString();
        thumbnailUrl = in.readString();
        previewUrl = in.readString();
    }

    public static final Parcelable.Creator<TrackInfo> CREATOR = new Parcelable.Creator<TrackInfo>() {
        public TrackInfo createFromParcel(Parcel in) {
            return new TrackInfo(in);
        }

        public TrackInfo[] newArray(int size) {
            return new TrackInfo[size];
        }
    };
}
