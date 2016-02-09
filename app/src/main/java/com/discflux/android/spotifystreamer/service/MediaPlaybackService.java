package com.discflux.android.spotifystreamer.service;

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
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.discflux.android.spotifystreamer.PlayActivity;
import com.discflux.android.spotifystreamer.PlayFragment;
import com.discflux.android.spotifystreamer.R;
import com.discflux.android.spotifystreamer.TrackInfo;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;


/**
 * Created by Phil on 02/02/2016.
 */
public class MediaPlaybackService extends Service implements MediaPlayer.OnPreparedListener {

    private static final String LOG_TAG = MediaPlaybackService.class.getSimpleName();
    private static final int NOTIFICATION_ID = 800;

    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_NEXT = "action_next";
    public static final String ACTION_PREVIOUS = "action_previous";
    public static final String ACTION_STOP = "action_stop";

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    private MediaPlayer mMediaPlayer;
    private MediaSessionCompat mSession;
    private MediaControllerCompat mController;
    private boolean mNotificationsEnabled;
    private WifiManager.WifiLock mWifiLock;
    private String mSongUrl, mSongTitle, mAlbumUrl, mArtistName, mAlbumTitle, mAction;
    private ArrayList<TrackInfo> mTracksList;
    private int mTrackPosition;
    private int mProgress = 0;
    private int mTrackDuration = 0;
    private boolean mInitialStage = true;
    private boolean mPrepared = false;


    @Override
    public void onCreate() {
        mMediaPlayer = new MediaPlayer();

        mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mNotificationsEnabled = sharedPref.getBoolean(getString(R.string.pref_notification_key), true);
        if (!mNotificationsEnabled) {
            stopForeground(true);
        }
        mAction = "";
        if (intent != null) {
            mAction = intent.getAction();
        }

        if (mInitialStage) {
            mInitialStage = false;
            initMediaSessions();
        }

        if (mAction.equals(ACTION_PLAY)) {
            if (intent.hasExtra(PlayFragment.TRACK_URL_EXTRA)) {
                if (!intent.getStringExtra(PlayFragment.TRACK_URL_EXTRA).equals(mSongUrl)) {
                    mInitialStage = true;
                }
                mProgress = intent.getIntExtra(PlayFragment.SEEKBAR_PROGRESS, 0);
                mSongUrl = intent.getStringExtra(PlayFragment.TRACK_URL_EXTRA);
            }
            if(mInitialStage) {
                setupMediaPlayer();
                saveSongState(intent);
            } else {
                if (!mMediaPlayer.isPlaying()){
                    mMediaPlayer.start();
                }
            }
            mediaNotifier(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));

            // acquire cpu + wifi locks
            mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            mWifiLock.acquire();

        } else if (mAction.equals(ACTION_PAUSE)) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
            }
            mediaNotifier(generateAction(android.R.drawable.ic_media_play, "Play", ACTION_PLAY));

            if (mWifiLock.isHeld()) {
                mWifiLock.release();
            }

        } else if (mAction.equals(ACTION_PREVIOUS)) {
            mMediaPlayer.stop();
            saveSongState(intent);
            mProgress = intent.getIntExtra(PlayFragment.SEEKBAR_PROGRESS, 0);
            mSongUrl = intent.getStringExtra(PlayFragment.TRACK_URL_EXTRA);
            setupMediaPlayer();

        }else if (mAction.equals(ACTION_NEXT)) {
            mMediaPlayer.stop();
            saveSongState(intent);
            mProgress = intent.getIntExtra(PlayFragment.SEEKBAR_PROGRESS, 0);
            mSongUrl = intent.getStringExtra(PlayFragment.TRACK_URL_EXTRA);
            setupMediaPlayer();

        }
        if (mController != null) {
            handleIntent(intent);
        }
        return 0;
    }

    private void saveSongState(Intent intent) {
        mSongTitle = intent.getStringExtra(PlayFragment.TRACK_TITLE_EXTRA);
        mArtistName = intent.getStringExtra(PlayFragment.ARTIST_NAME_EXTRA);
        mAlbumTitle = intent.getStringExtra(PlayFragment.ALBUM_TITLE_EXTRA);
        mAlbumUrl = intent.getStringExtra(PlayFragment.ALBUM_ART_EXTRA);
        mTrackPosition = intent.getIntExtra(PlayFragment.TRACK_POSITION, 0);
        mProgress = intent.getIntExtra(PlayFragment.SEEKBAR_PROGRESS, 0);
        mTracksList = intent.getParcelableArrayListExtra(PlayFragment.TRACK_INFO_EXTRA);

    }

    private NotificationCompat.Action generateAction( int icon, String title, String intentAction ) {
        Intent intent = new Intent( getApplicationContext(), MediaPlaybackService.class );
        intent.setAction( intentAction );
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);
        return new NotificationCompat.Action.Builder( icon, title, pendingIntent ).build();
    }

    /** Build notification */
    private void mediaNotifier(NotificationCompat.Action action) {
        if(!mNotificationsEnabled) {
            return;
        }
        NotificationCompat.MediaStyle style = new NotificationCompat.MediaStyle();

        NotificationCompat.Builder mBuilder =
                (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Spotify Streamer")
                    .setContentText("Now Playing: \"" + mSongTitle + "\"")
                    .setTicker("Can you hear the music?")
                    .setStyle(style)
                    // Add media control buttons that invoke intents in your media service
                    .addAction(generateAction(android.R.drawable.ic_media_previous, "Previous", ACTION_PREVIOUS)) // #0
                    .addAction(action) // #1
                    .addAction(generateAction(android.R.drawable.ic_media_next, "Next", ACTION_NEXT)) // #2
                    .addAction(generateAction(R.drawable.ic_media_stop, "Stop", ACTION_STOP)); // #3
        style.setShowActionsInCompactView(0, 1, 2);

        Intent intent = new Intent(this, PlayActivity.class);
        intent.putExtra(PlayFragment.TRACK_TITLE_EXTRA, mSongTitle);
        intent.putExtra(PlayFragment.ARTIST_NAME_EXTRA, mArtistName);
        intent.putExtra(PlayFragment.ALBUM_TITLE_EXTRA, mAlbumTitle);
        intent.putExtra(PlayFragment.ALBUM_ART_EXTRA, mAlbumUrl);
        intent.putExtra(PlayFragment.TRACK_URL_EXTRA, mSongUrl);
        intent.putParcelableArrayListExtra(PlayFragment.TRACK_INFO_EXTRA, mTracksList);
        intent.putExtra(PlayFragment.TRACK_POSITION, mTrackPosition);
        intent.putExtra(PlayFragment.SEEKBAR_PROGRESS, mProgress);

        /*intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);*/

        /*TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(PlayActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(intent);

        PendingIntent pi =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );*/

        PendingIntent pi = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        mBuilder.setContentIntent(pi);
        mBuilder.setOngoing(true);

        /** code source for using picasso
         *  http://stackoverflow.com/questions/26888247/easiest-way-to-use-picasso-in-notification-icon
         **/

        Bitmap albumArt = loadBitMapWithPicasso();

        if (albumArt != null) {
            mBuilder.setLargeIcon(albumArt);
        } else {
            mBuilder.setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher));
        }

        startForeground(NOTIFICATION_ID, mBuilder.build());
    }

    private Bitmap loadBitMapWithPicasso() {
        Bitmap albumArt = null;
        try {
            albumArt = new AsyncTask<Void, Void, Bitmap>() {
                @Override
                protected Bitmap doInBackground(Void... params) {
                    try {
                        return Picasso.with(getApplicationContext()).load(mAlbumUrl)
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
        return albumArt;
    }

    /** Setup MediaPlayer initially */
    private void setupMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
        }
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            mMediaPlayer.setDataSource(mSongUrl);

            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    //mInitialStage = true;
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
        mMediaPlayer.prepareAsync(); // prepare async to not block main thread
    }

    /** Called when MediaPlayer is ready */
    @Override
    public void onPrepared(MediaPlayer player) {
        mInitialStage = false;
        mPrepared = true;
        mTrackDuration = mMediaPlayer.getDuration();
        if (mProgress != 0) {
            int mSec = (int)((double)mProgress/100 * mMediaPlayer.getDuration());
            mMediaPlayer.seekTo(mSec);
        }
        mMediaPlayer.start();
    }

    public boolean isPlaying() {
        if(mAction.equals(ACTION_PLAY)) {
            return true;
        }
        return false;
    }

    public int getCurrentPosition() {
        if (mMediaPlayer != null) {
            return mMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public void seekTo(int mSec) {
        //Log.d(LOG_TAG, "Duration: " + getTrackDuration());
        //Log.d(LOG_TAG, "Seek Time: " + mSec);
        if (mMediaPlayer != null && mSec <= getTrackDuration()) {

            mMediaPlayer.seekTo(mSec);
        }
    }

    public int getTrackDuration() {
        return mTrackDuration;
    }

    public boolean playerReady() {
        return mPrepared;
    }

    public String getSongUrl() {
        return mSongUrl;
    }

    private void initMediaSessions() {

        mSession = new MediaSessionCompat(getApplicationContext(), "media player session", null, null);
        try {
            mController = new MediaControllerCompat(getApplicationContext(), mSession.getSessionToken());
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        mSession.setCallback(new MediaSessionCompat.Callback(){
             @Override
             public void onPlay() {
                 super.onPlay();
                 Log.e("MediaPlayerService", "onPlay");
             }

             @Override
             public void onPause() {
                 super.onPause();
                 Log.e("MediaPlayerService", "onPause");
             }

             @Override
             public void onSkipToNext() {
                 super.onSkipToNext();
                 Log.e("MediaPlayerService", "onSkipToNext");
                 //Change media here
                 mediaNotifier(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
             }

             @Override
             public void onSkipToPrevious() {
                 super.onSkipToPrevious();
                 Log.e("MediaPlayerService", "onSkipToPrevious");
                 //Change media here
                 mediaNotifier(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
             }

             @Override
             public void onStop() {
                 super.onStop();
                 Log.e( "MediaPlayerService", "onStop");
                 //Stop media player here
                 mMediaPlayer.pause();
                 mInitialStage = true;
                 stopForeground(true);
                 stopSelf();
             }

         }
        );
    }

    private void handleIntent( Intent intent ) {
        if( intent == null || intent.getAction() == null )
            return;

        String action = intent.getAction();

        if( action.equalsIgnoreCase( ACTION_PLAY ) ) {
            mController.getTransportControls().play();
        } else if( action.equalsIgnoreCase( ACTION_PAUSE ) ) {
            mController.getTransportControls().pause();
        } else if( action.equalsIgnoreCase( ACTION_PREVIOUS ) ) {
            mController.getTransportControls().skipToPrevious();
        } else if( action.equalsIgnoreCase( ACTION_NEXT ) ) {
            mController.getTransportControls().skipToNext();
        } else if( action.equalsIgnoreCase( ACTION_STOP ) ) {
            mController.getTransportControls().stop();
        }
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
        mTrackDuration = 0;
        mInitialStage = true;
        mPrepared = false;
        stopForeground(true);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (mSession != null) {
            mSession.release();
        }
        return super.onUnbind(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public MediaPlaybackService getService() {
            return MediaPlaybackService.this;
        }
    }

}
