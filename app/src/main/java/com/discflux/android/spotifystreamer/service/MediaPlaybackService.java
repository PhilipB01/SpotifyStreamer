package com.discflux.android.spotifystreamer.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.discflux.android.spotifystreamer.PlayActivity;
import com.discflux.android.spotifystreamer.R;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Created by Phil on 02/02/2016.
 */
public class MediaPlaybackService extends Service implements MediaPlayer.OnPreparedListener {

    private static final String LOG_TAG = MediaPlaybackService.class.getSimpleName();
    private static final int NOTIFICATION_ID = 800;
    public static final String ACTION_PLAY = "com.discflux.action.PLAY";
    public static final String ACTION_PAUSE = "com.discflux.action.PAUSE";

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    private MediaPlayer mMediaPlayer;
    private Notification mNotification;
    private WifiManager.WifiLock mWifiLock;
    private String songUrl, songName, albumUrl, action;
    private int trackDuration = 0;
    private boolean initialStage = true;
    private boolean prepared = false;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        mMediaPlayer = new MediaPlayer();
        mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        //...
        action = "";
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
                albumUrl = intent.getStringExtra("icon url");
            }
            if(initialStage) {
                setupMediaPlayer();
            } else {
                if (!mMediaPlayer.isPlaying()){
                    mMediaPlayer.start();
                }
            }
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            boolean notificationsEnabled = sharedPref.getBoolean(getString(R.string.pref_notification_key), true);
            if (notificationsEnabled) {
                mediaNotifier();
            }
            // acquire cpu + wifi locks
            mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            mWifiLock.acquire();
            return 1;
        }
        if (action.equals(ACTION_PAUSE)) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
            }
            if (mWifiLock.isHeld()) {
                mWifiLock.release();
            }
            return 0;
        }
        return -1;
    }

    /** Build notification */
    private void mediaNotifier() {

        /*
        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(PlayActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(nowPlayingIntent);

        PendingIntent nowPlayingPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(songName)
                        .setContentText(songUrl)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        // Add media control buttons that invoke intents in your media service
                        .addAction(17301541, "", nowPlayingPendingIntent) // #0
                        .addAction( 17301539, "", nowPlayingPendingIntent)  // #1
                        .addAction(17301538, "", nowPlayingPendingIntent);     // #2

        mBuilder.setContentIntent(nowPlayingPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());*/

        NotificationCompat.Builder mBuilder =
                (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Spotify Streamer")
                    .setContentText("Now Playing: \"" + songName + "\"")
                    .setTicker("Can you hear the music?")
                    // Add media control buttons that invoke intents in your media service
                    .addAction(android.R.drawable.ic_media_previous, "", null) // #0
                    .addAction(android.R.drawable.ic_media_pause, "", null) // #1
                    .addAction(android.R.drawable.ic_media_next, "", null); // #2

        Intent intent = new Intent(this, PlayActivity.class);

        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(pi);

        //notification.flags |= Notification.FLAG_NO_CLEAR;
        //notification.flags |= Notification.FLAG_ONGOING_EVENT;

        /** code source for using picasso
         *  http://stackoverflow.com/questions/26888247/easiest-way-to-use-picasso-in-notification-icon
         **/
        Bitmap albumArt = null;

        try {
            albumArt = new AsyncTask<Void, Void, Bitmap>() {
                @Override
                protected Bitmap doInBackground(Void... params) {
                    try {
                        return Picasso.with(getApplicationContext()).load(albumUrl)
                                .resize(200, 200)
                                .placeholder(R.mipmap.ic_launcher)
                                .error(R.mipmap.ic_launcher)
                                .get();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            }.execute().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        if (albumArt != null) {
            mBuilder.setLargeIcon(albumArt);
        } else {
            mBuilder.setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher));
        }

        startForeground(NOTIFICATION_ID, mBuilder.build());
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

        //mNotification.tickerText = "Playing: " + songName;

        mMediaPlayer.prepareAsync(); // prepare async to not block main thread
    }

    /** Called when MediaPlayer is ready */
    @Override
    public void onPrepared(MediaPlayer player) {
        initialStage = false;
        prepared = true;
        trackDuration = mMediaPlayer.getDuration();
        mMediaPlayer.start();
    }

    /** Clean up service on completion */
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
        prepared = false;
        stopForeground(true);
    }

    public boolean isPlaying() {
        if(action.equals(ACTION_PLAY)) {
            return true;
        }
        return false;
    }

    public int getCurrentPosition() {
        if (mMediaPlayer != null) {
            //Log.i(LOG_TAG, "Current Time: " + mMediaPlayer.getCurrentPosition()/1000);
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

    public boolean playerReady() {
        return prepared;
    }

    public String getSongUrl() {
        return songUrl;
    }

    public class LocalBinder extends Binder {
        public MediaPlaybackService getService() {
            return MediaPlaybackService.this;
        }
    }
}
