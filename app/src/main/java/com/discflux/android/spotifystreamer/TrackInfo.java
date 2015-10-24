package com.discflux.android.spotifystreamer;

/**
 * Created by Phil on 09/06/2015.
 */
public class TrackInfo {

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
}
