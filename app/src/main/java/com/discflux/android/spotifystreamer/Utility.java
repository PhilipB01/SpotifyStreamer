package com.discflux.android.spotifystreamer;

/**
 * Created by Phil on 09/02/2016.
 */
public class Utility {

    public static String milliToTimer(int duration) {
        int minutes = (duration / 60000);
        int seconds = (duration % 60000) / 1000;

        return String.format("%2d:%02d", minutes, seconds);
    }

}
