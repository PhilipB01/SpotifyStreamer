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

    private TextView artistText, albumText, trackText, trackCurrentDurationText, trackTotalDurationText;
    private ImageView imageView;
    private ImageButton prevButton, playButton, nextButton;
    private SeekBar seekBar;
    private Handler mHandler;
    private MediaPlaybackService mService;
    private Intent mPlaybackService;

    private String artistName;
    private String albumTitle;
    private String trackTitle;
    private String imgUrl;
    private String previewUrl;

    private int progress = 0, trackDuration = 0;
    private boolean play = false;
    private boolean mBound;

    private static final String LOG_TAG = PlayFragment.class.getSimpleName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(LOG_TAG, "View Created");
        View rootView = inflater.inflate(R.layout.fragment_play, container, false);
        Intent intent = getActivity().getIntent();
        if(intent!=null) {
            ArrayList<String> trackDetails = intent.getStringArrayListExtra(Intent.EXTRA_TEXT);
            artistName = trackDetails.get(0);
            trackTitle = trackDetails.get(1);
            albumTitle = trackDetails.get(2);
            imgUrl = trackDetails.get(3);
            previewUrl = trackDetails.get(4);
        }
        setup(rootView);

        playButton.setOnClickListener(mediaListener);

        mHandler = new Handler();

        seekBar.setOnSeekBarChangeListener(seekBarListener);

        return rootView;
    }

    /**
     ** Prepare layout and views
     **/
    private void setup(View view) {
        Log.d(LOG_TAG, "View setup");
        artistText = (TextView) view.findViewById(R.id.artist_textview);
        albumText = (TextView) view.findViewById(R.id.album_textview);
        trackText = (TextView) view.findViewById(R.id.track_textview);
        imageView = (ImageView) view.findViewById(R.id.album_art_imageview);

        seekBar = (SeekBar) view.findViewById(R.id.seekBar);
        trackCurrentDurationText = (TextView) view.findViewById(R.id.media_current_time_textview);
        trackTotalDurationText = (TextView) view.findViewById(R.id.media_end_time_textview);

        playButton = (ImageButton) view.findViewById(R.id.button_play);
        prevButton = (ImageButton) view.findViewById(R.id.button_previous);
        nextButton = (ImageButton) view.findViewById(R.id.button_next);

        playButton.setBackgroundResource(android.R.drawable.ic_media_play);
        prevButton.setBackgroundResource(android.R.drawable.ic_media_previous);
        nextButton.setBackgroundResource(android.R.drawable.ic_media_next);

        prevButton.setScaleX(1.5f);
        prevButton.setScaleY(1.5f);

        playButton.setScaleX(1.5f);
        playButton.setScaleY(1.5f);

        nextButton.setScaleX(1.5f);
        nextButton.setScaleY(1.5f);


        artistText.setText(artistName);
        albumText.setText(albumTitle);
        trackText.setText(trackTitle);
        if (imageView != null) {
            // vertical layout album art
            Picasso.with(getActivity()).load(imgUrl).into(imageView);
        } else {
            // horizontal layout album art
            final LinearLayout horizontalBackground = (LinearLayout) view.findViewById(R.id.linear_layout_background);
            Picasso.with(getActivity()).load(imgUrl).into(new Target() {
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

        Intent playbackService = new Intent(getActivity(), MediaPlaybackService.class);
        getActivity().bindService(playbackService, mConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Save instance state before orientation changes
     **/
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onSaveInstanceStateCalled");
        savedInstanceState.putInt("track duration", trackDuration);
        savedInstanceState.putInt("current time", mService.getCurrentPosition());
        savedInstanceState.putInt("current progress", progress);
        savedInstanceState.putBoolean("isPlaying", play);

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
            play = savedInstanceState.getBoolean("isPlaying");
            progress = savedInstanceState.getInt("current progress");
            seekBar.setProgress(progress);
            trackDuration = savedInstanceState.getInt("track duration");
            trackTotalDurationText.setText(milliToTimer(trackDuration));
            int currentTime = savedInstanceState.getInt("current time");
            trackCurrentDurationText.setText(milliToTimer(currentTime));

            if (play) {
                playButton.setBackgroundResource(android.R.drawable.ic_media_pause);
                updateProgressBar();
            } else {
                playButton.setBackgroundResource(android.R.drawable.ic_media_play);
            }
        }
    }

    private View.OnClickListener mediaListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!play) {
                playButton.setBackgroundResource(android.R.drawable.ic_media_pause);
                mPlaybackService = prepareMediaIntent(mPlaybackService, MediaPlaybackService.ACTION_PLAY);
                //getActivity().bindService(mPlaybackService, mConnection, Context.BIND_AUTO_CREATE);
                getActivity().startService(mPlaybackService);
                updateProgressBar();
                play = true;
            } else {
                playButton.setBackgroundResource(android.R.drawable.ic_media_play);
                mPlaybackService = prepareMediaIntent(mPlaybackService, MediaPlaybackService.ACTION_PAUSE);
                //getActivity().bindService(mPlaybackService, mConnection, Context.BIND_AUTO_CREATE);
                getActivity().startService(mPlaybackService);
                mHandler.removeCallbacks(mUpdateTimeTask);
                play = false;
            }
        }
    };

    private Intent prepareMediaIntent(Intent intent, String action) {
        if (intent == null) {
            intent = new Intent(getActivity(), MediaPlaybackService.class);
        }
        intent.setAction(action);
        intent.putExtra("song url", previewUrl);
        intent.putExtra("song name", trackTitle);
        intent.putExtra("icon url", imgUrl);
        return intent;
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
            trackCurrentDurationText.setText(milliToTimer(currentDuration));

            trackDuration =  mService.getTrackDuration();
            String durationText = trackTotalDurationText.getText().toString();
            if (durationText.equals("-") || durationText.contains("0:00")) {
                trackTotalDurationText.setText(milliToTimer(trackDuration));
            }

            // Updating progress bar
            if (trackDuration != 0) {
                progress = (int) (((currentDuration / (double) trackDuration) + 0.005) * 100);
                //Log.i("Progress", "" + progress);
                seekBar.setProgress(progress);
            }
            if (progress < 100) {
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
        playButton.setBackgroundResource(android.R.drawable.ic_media_play);
        trackCurrentDurationText.setText(milliToTimer(0));
        seekBar.setProgress(0);
        play = false;
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
            // remove message Handler from updating progress bar
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
            Log.d(LOG_TAG, "playing: " + play);

            if (mService.playerReady() && mService.getSongUrl().equals(previewUrl)) {
                Log.d(LOG_TAG, "Want to update progress bar: " + progress);
                updateProgressBar();
                if (mService.isPlaying()) {
                    playButton.setBackgroundResource(android.R.drawable.ic_media_pause);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
}
