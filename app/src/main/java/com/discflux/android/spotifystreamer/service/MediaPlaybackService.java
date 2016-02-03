package com.discflux.android.spotifystreamer.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.discflux.android.spotifystreamer.PlayActivity;
import com.discflux.android.spotifystreamer.R;

import java.io.IOException;

/**
 * Created by Phil on 02/02/2016.
 */
public class MediaPlaybackService extends Service implements MediaPlayer.OnPreparedListener {

    private static final String LOG_TAG = MediaPlaybackService.class.getSimpleName();
    public static final String ACTION_PLAY = "com.discflux.action.PLAY";
    public static final String ACTION_PAUSE = "com.discflux.action.PAUSE";
    private static final int NOTIFICATION_ID = 0;

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    private MediaPlayer mMediaPlayer;
    private Notification notification;
    private WifiManager.WifiLock mWifiLock;
    private String songUrl, songName;
    private int trackDuration = 0;
    private boolean initialStage = true;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        mMediaPlayer = new MediaPlayer();
        mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");

        // set notification and startForeground
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), PlayActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        notification = new Notification();
        notification.tickerText = "Now Playing Spotify Previews";
        notification.icon = R.mipmap.ic_launcher;
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        startForeground(NOTIFICATION_ID, notification);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        //...
        String action = "";
        if (intent != null) {
            action = intent.getAction();
        }

        if (action.equals(ACTION_PLAY)) {
            if (intent.hasExtra("song url") && intent.hasExtra("song name")) {
                if (!intent.getStringExtra("song url").equals(songUrl)) {
                    initialStage = true;
                }
                songUrl = intent.getStringExtra("song url");
                songName = intent.getStringExtra("song name");
            }
            if(initialStage) {
                setupMediaPlayer();
            } else {
                if (!mMediaPlayer.isPlaying()){
                    mMediaPlayer.start();
                }
            }

            // acquire cpu + wifi locks
            mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            mWifiLock.acquire();
        }
        if (action.equals(ACTION_PAUSE)) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
            }
            if (mWifiLock.isHeld()) {
                mWifiLock.release();
            }
        }
        return 0;
    }

    /** Setup MediaPlayer initially */
    private void setupMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
        }
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            mMediaPlayer.setDataSource(songUrl);

            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    //initialStage = true;
                    mMediaPlayer.pause();
                    //mMediaPlayer.reset();
                    if (mWifiLock.isHeld()) {
                        mWifiLock.release();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            Log.d("IllegarArgument", e.getMessage());
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

        mMediaPlayer.setOnPreparedListener(this);

        /*notification.setLatestEventInfo(getApplicationContext(), "MusicPlayerSample",
                "Playing: " + songName, pi);*/
        notification.tickerText = "Playing: " + songName;

        mMediaPlayer.prepareAsync(); // prepare async to not block main thread
    }

    /** Called when MediaPlayer is ready */
    public void onPrepared(MediaPlayer player) {
        initialStage = false;
        trackDuration = mMediaPlayer.getDuration();
        mMediaPlayer.start();
    }

    @Override
    public void onDestroy() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if (mWifiLock.isHeld()) {
            mWifiLock.release();
        }
        trackDuration = 0;
        initialStage = true;
        stopForeground(true);
    }

    public int getCurrentPosition() {
        if (mMediaPlayer != null) {
            Log.d(LOG_TAG, "Current Time: " + mMediaPlayer.getCurrentPosition());
            return mMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public void seekTo(int mSec) {
        Log.d(LOG_TAG, "Duration: " + getTrackDuration());
        Log.d(LOG_TAG, "Seek Time: " + mSec);
        if (mMediaPlayer != null && mSec <= getTrackDuration()) {

            mMediaPlayer.seekTo(mSec);
        }
    }

    public int getTrackDuration() {
        return trackDuration;
    }

    public class LocalBinder extends Binder {
        public MediaPlaybackService getService() {
            return MediaPlaybackService.this;
        }
    }
}
