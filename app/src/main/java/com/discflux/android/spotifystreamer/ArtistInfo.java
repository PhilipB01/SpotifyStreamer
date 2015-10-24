package com.discflux.android.spotifystreamer;

/**
 * Created by Phil on 09/06/2015.
 */
public class ArtistInfo {
    private String artistName;
    private String spotifyId;
    private String thumbnailUrl;

    public ArtistInfo(String name, String id, String path) {
        artistName = name;
        spotifyId = id;
        thumbnailUrl = path;
    }

    public String getArtistName() {
        return artistName;
    }

    public String getSpotifyId() {
        return spotifyId;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    @Override
    public String toString() {
        return artistName + "  " + spotifyId;
    }
}