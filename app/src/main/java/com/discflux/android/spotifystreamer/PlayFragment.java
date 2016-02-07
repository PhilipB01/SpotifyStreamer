package com.discflux.android.spotifystreamer;

import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.discflux.android.spotifystreamer.service.MediaPlaybackService;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;

/**
 * Created by Phil on 10/06/2015.
 */
public class PlayFragment extends Fragment{

    private TextView mArtistTextView, mAlbumTextView, mTrackTextView, mTrackElapsedTimeTextView, mTrackTotalDurationTextView;
    private ImageView mImageView;
    private ImageButton mPrevButton, mPlayButton, mNextButton;
    private SeekBar mSeekBar;
    private Handler mHandler;
    private MediaPlaybackService mService;
    private Intent mPlaybackService;

    private String mArtistName;
    private String mAlbumTitle;
    private String mTrackTitle;
    private String mImgUrl;
    private String mPreviewUrl;

    public static final String TRACK_TITLE_EXTRA = "song title";
    public static final String ARTIST_NAME_EXTRA = "artist name";
    public static final String ALBUM_TITLE_EXTRA = "album title";
    public static final String ALBUM_ART_EXTRA = "art url";
    public static final String TRACK_URL_EXTRA = "song url";

    private int mProgress = 0, trackDuration = 0;
    private boolean mPlay = false;
    private boolean mBound;

    private static final String LOG_TAG = PlayFragment.class.getSimpleName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(LOG_TAG, "View Created");
        View rootView = inflater.inflate(R.layout.fragment_play, container, false);
        Intent intent = getActivity().getIntent();
        if (intent != null) {
            if (intent.hasExtra(Intent.EXTRA_TEXT)) {
                ArrayList<String> trackDetails = intent.getStringArrayListExtra(Intent.EXTRA_TEXT);
                mArtistName = trackDetails.get(0);
                mTrackTitle = trackDetails.get(1);
                mAlbumTitle = trackDetails.get(2);
                mImgUrl = trackDetails.get(3);
                mPreviewUrl = trackDetails.get(4);
            } else {
                mTrackTitle = intent.getStringExtra(PlayFragment.TRACK_TITLE_EXTRA);
                mArtistName = intent.getStringExtra(PlayFragment.ARTIST_NAME_EXTRA);
                mAlbumTitle = intent.getStringExtra(PlayFragment.ALBUM_TITLE_EXTRA);
                mImgUrl = intent.getStringExtra(PlayFragment.ALBUM_ART_EXTRA);
                mPreviewUrl = intent.getStringExtra(PlayFragment.TRACK_URL_EXTRA);
            }
        }
        setup(rootView);

        prepareMediaIntent();

        mHandler = new Handler();

        mPlayButton.setOnClickListener(mediaListener);

        mSeekBar.setOnSeekBarChangeListener(seekBarListener);

        return rootView;
    }

    /**
     ** Prepare layout and views
     **/
    private void setup(View view) {
        Log.d(LOG_TAG, "View setup");
        mArtistTextView = (TextView) view.findViewById(R.id.artist_textview);
        mAlbumTextView = (TextView) view.findViewById(R.id.album_textview);
        mTrackTextView = (TextView) view.findViewById(R.id.track_textview);
        mImageView = (ImageView) view.findViewById(R.id.album_art_imageview);

        mSeekBar = (SeekBar) view.findViewById(R.id.seekBar);
        mTrackElapsedTimeTextView = (TextView) view.findViewById(R.id.media_current_time_textview);
        mTrackTotalDurationTextView = (TextView) view.findViewById(R.id.media_end_time_textview);

        mPlayButton = (ImageButton) view.findViewById(R.id.button_play);
        mPrevButton = (ImageButton) view.findViewById(R.id.button_previous);
        mNextButton = (ImageButton) view.findViewById(R.id.button_next);

        mPlayButton.setBackgroundResource(android.R.drawable.ic_media_play);
        mPrevButton.setBackgroundResource(android.R.drawable.ic_media_previous);
        mNextButton.setBackgroundResource(android.R.drawable.ic_media_next);

        mPrevButton.setScaleX(1.5f);
        mPrevButton.setScaleY(1.5f);

        mPlayButton.setScaleX(1.5f);
        mPlayButton.setScaleY(1.5f);

        mNextButton.setScaleX(1.5f);
        mNextButton.setScaleY(1.5f);


        mArtistTextView.setText(mArtistName);
        mAlbumTextView.setText(mAlbumTitle);
        mTrackTextView.setText(mTrackTitle);
        if (mImageView != null) {
            // vertical layout album art
            Picasso.with(getActivity()).load(mImgUrl).into(mImageView);
        } else {
            // horizontal layout album art
            final LinearLayout horizontalBackground = (LinearLayout) view.findViewById(R.id.linear_layout_background);
            Picasso.with(getActivity()).load(mImgUrl).into(new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        horizontalBackground.setBackground(new BitmapDrawable(bitmap));
                    }
                }

                @Override
                public void onBitmapFailed(Drawable errorDrawable) {
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {
                }
            });

        }
    }

    private View.OnClickListener mediaListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!mPlay) {
                mPlay = true;
                mPlayButton.setBackgroundResource(android.R.drawable.ic_media_pause);
                mPlaybackService.setAction(MediaPlaybackService.ACTION_PLAY);
                //getActivity().bindService(mPlaybackService, mConnection, Context.BIND_AUTO_CREATE);
                getActivity().startService(mPlaybackService);
                updateProgressBar();
            } else {
                mPlay = false;
                mPlayButton.setBackgroundResource(android.R.drawable.ic_media_play);
                mHandler.removeCallbacks(mUpdateTimeTask);

                if (mPlaybackService == null) {
                    mPlaybackService = new Intent(getActivity(), MediaPlaybackService.class);
                }
                mPlaybackService.setAction(MediaPlaybackService.ACTION_PAUSE);
                getActivity().startService(mPlaybackService);
            }
        }
    };

    private void prepareMediaIntent() {
        if (mPlaybackService == null) {
            mPlaybackService = new Intent(getActivity(), MediaPlaybackService.class);
        }
        mPlaybackService.putExtra(PlayFragment.TRACK_URL_EXTRA, mPreviewUrl);
        mPlaybackService.putExtra(PlayFragment.TRACK_TITLE_EXTRA, mTrackTitle);
        mPlaybackService.putExtra(PlayFragment.ARTIST_NAME_EXTRA, mArtistName);
        mPlaybackService.putExtra(PlayFragment.ALBUM_TITLE_EXTRA, mAlbumTitle);
        mPlaybackService.putExtra(PlayFragment.ALBUM_ART_EXTRA, mImgUrl);
        /*mPlaybackService.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);*/
    }

    /** Update scrub bar every 100 mSec */
    public void updateProgressBar() {
        mHandler.postDelayed(mUpdateTimeTask, 100);
    }

    /**
     * Background Runnable thread for handling scrub bar updates
     **/
    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            int currentDuration = mService.getCurrentPosition();

            // Displaying time completed playing
            mTrackElapsedTimeTextView.setText(milliToTimer(currentDuration));

            trackDuration =  mService.getTrackDuration();
            String durationText = mTrackTotalDurationTextView.getText().toString();
            if (durationText.equals("-") || durationText.contains("0:00")) {
                mTrackTotalDurationTextView.setText(milliToTimer(trackDuration));
            }

            // Updating mProgress bar
            if (trackDuration != 0) {
                mProgress = (int) (((currentDuration / (double) trackDuration) + 0.005) * 100);
                //Log.i("Progress", "" + mProgress);
                mSeekBar.setProgress(mProgress);
            }
            if (mProgress < 100) {
                // Running this thread after 100 milliseconds
                updateProgressBar();
            } else {
                resetScrubBar();
            }
        }
    };

    /**
     * Called on song completion to reset scrub bar to start of track
     **/
    private void resetScrubBar() {
        Log.d(LOG_TAG, "Track playback complete");
        mHandler.removeCallbacks(mUpdateTimeTask);
        mPlayButton.setBackgroundResource(android.R.drawable.ic_media_play);
        mTrackElapsedTimeTextView.setText(milliToTimer(0));
        mSeekBar.setProgress(0);
        mPlay = false;
    }

    private SeekBar.OnSeekBarChangeListener seekBarListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                int mSec = (int)((double)progress/100 * mService.getTrackDuration());
                mService.seekTo(mSec);
            }
        }
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // remove message Handler from updating mProgress bar
            mHandler.removeCallbacks(mUpdateTimeTask);
        }
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // resume updating seek bar
            updateProgressBar();
        }
    };

    private String milliToTimer(int duration) {
        int minutes = (duration / 60000);
        int seconds = (duration % 60000) / 1000;

        return String.format("%2d:%02d", minutes, seconds);
    }


    @Override
    public void onPause() {
        super.onPause();
        Log.d(LOG_TAG, "Activity Paused");
        mHandler.removeCallbacks(mUpdateTimeTask);
        if (mBound) {
            getActivity().unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "Activity Resumed");

        getActivity().bindService(mPlaybackService, mConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Save instance state before orientation changes
     **/
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onSaveInstanceStateCalled");
        savedInstanceState.putInt("track duration", trackDuration);
        savedInstanceState.putInt("current time", mService.getCurrentPosition());
        savedInstanceState.putInt("current progress", mProgress);
        savedInstanceState.putBoolean("isPlaying", mPlay);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Restores saved instance state
     **/
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "Restoring instance state");
        Log.d(LOG_TAG, "hasInstanceState: " + savedInstanceState);
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            // Restore value of members from saved state
            mPlay = savedInstanceState.getBoolean("isPlaying");
            mProgress = savedInstanceState.getInt("current progress");
            mSeekBar.setProgress(mProgress);
            trackDuration = savedInstanceState.getInt("track duration");
            mTrackTotalDurationTextView.setText(milliToTimer(trackDuration));
            int currentTime = savedInstanceState.getInt("current time");
            mTrackElapsedTimeTextView.setText(milliToTimer(currentTime));

            if (mPlay) {
                mPlayButton.setBackgroundResource(android.R.drawable.ic_media_pause);
                updateProgressBar();
            } else {
                mPlayButton.setBackgroundResource(android.R.drawable.ic_media_play);
            }
        }
    }

    /**
     *  Defines callbacks for service binding, passed to bindService()
     **/
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MediaPlaybackService.LocalBinder binder = (MediaPlaybackService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            Log.d(LOG_TAG, "Service connected");
            Log.d(LOG_TAG, "player ready: " + mService.playerReady());
            Log.d(LOG_TAG, "playing: " + mPlay);

            if (mService.playerReady() && mService.getSongUrl().equals(mPreviewUrl)) {
                Log.d(LOG_TAG, "Want to update progress bar: " + mProgress);
                updateProgressBar();
                if (mService.isPlaying()) {
                    mPlayButton.setBackgroundResource(android.R.drawable.ic_media_pause);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
}
