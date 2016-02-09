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
public class PlayFragment extends Fragment {

    private TextView mArtistTextView, mAlbumTextView, mTrackTextView, mTrackElapsedTimeTextView, mTrackTotalDurationTextView;
    private ImageView mImageView;
    private LinearLayout mLandscapeBackground;
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
    private ArrayList<TrackInfo> mTracksList;
    private int mTrackPosition;
    private int mTrackCount;

    public static final String TRACK_POSITION = "track position";
    public static final String TRACK_INFO_EXTRA = "track info";
    public static final String ARTIST_NAME_EXTRA = "artist name";
    public static final String TRACK_TITLE_EXTRA = "song title";
    public static final String ALBUM_TITLE_EXTRA = "album title";
    public static final String ALBUM_ART_EXTRA = "art url";
    public static final String TRACK_URL_EXTRA = "song url";

    public static final String SEEKBAR_PROGRESS = "seekbar progress";
    private int mElapsedTime = 0;
    private int mProgress = 0, mTrackDuration = 0;

    private boolean mPlay = false;

    private boolean mBound;
    private static final String LOG_TAG = PlayFragment.class.getSimpleName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_play, container, false);
        Intent intent = getActivity().getIntent();
        if (intent != null) {
            if (intent.hasExtra(TRACK_INFO_EXTRA) && intent.hasExtra(TRACK_POSITION)) {

                mTrackPosition = intent.getIntExtra(TRACK_POSITION, 0);
                mTracksList = intent.getParcelableArrayListExtra(TRACK_INFO_EXTRA);
                mTrackCount = mTracksList.size();
                TrackInfo trackDetails = mTracksList.get(mTrackPosition);

                mTrackTitle = trackDetails.getTrackName();
                mAlbumTitle = trackDetails.getAlbumName();
                mImgUrl = trackDetails.getImageUrl();
                mPreviewUrl = trackDetails.getPreviewUrl();

                if (intent.hasExtra(ARTIST_NAME_EXTRA)) {
                    mArtistName = intent.getStringExtra(ARTIST_NAME_EXTRA);
                }
            } else {

                mTrackTitle = intent.getStringExtra(PlayFragment.TRACK_TITLE_EXTRA);
                mArtistName = intent.getStringExtra(PlayFragment.ARTIST_NAME_EXTRA);
                mAlbumTitle = intent.getStringExtra(PlayFragment.ALBUM_TITLE_EXTRA);
                mImgUrl = intent.getStringExtra(PlayFragment.ALBUM_ART_EXTRA);
                mPreviewUrl = intent.getStringExtra(PlayFragment.TRACK_URL_EXTRA);
                mProgress = intent.getIntExtra(PlayFragment.SEEKBAR_PROGRESS, 0);
                mTrackPosition = intent.getIntExtra(PlayFragment.TRACK_POSITION, 0);
                mTracksList = intent.getParcelableArrayListExtra(TRACK_INFO_EXTRA);
                mTrackCount = mTracksList.size();
            }
        }
        setup(rootView);
        prepareMediaIntent();

        mHandler = new Handler();
        mPlayButton.setOnClickListener(mediaListener);
        mPrevButton.setOnClickListener(prevListener);
        mNextButton.setOnClickListener(nextListener);
        mSeekBar.setOnSeekBarChangeListener(seekBarListener);

        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
        //Log.d(LOG_TAG, "Activity Paused");
        mHandler.removeCallbacks(mUpdateTimeTask);
        if (mBound) {
            getActivity().unbindService(mConnection);
            mBound = false;
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        //Log.d(LOG_TAG, "Activity Resumed");

        getActivity().bindService(mPlaybackService, mConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     ** Prepare layout and views
     **/
    private void setup(View view) {
        mArtistTextView = (TextView) view.findViewById(R.id.artist_textview);
        mAlbumTextView = (TextView) view.findViewById(R.id.album_textview);
        mTrackTextView = (TextView) view.findViewById(R.id.track_textview);
        mImageView = (ImageView) view.findViewById(R.id.album_art_imageview);
        mLandscapeBackground = (LinearLayout) view.findViewById(R.id.linear_layout_background);

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
        updateImage();
    }

    private void prepareMediaIntent() {
        if (mPlaybackService == null) {
            mPlaybackService = new Intent(getActivity(), MediaPlaybackService.class);
        }
        mPlaybackService.putExtra(PlayFragment.TRACK_URL_EXTRA, mPreviewUrl);
        mPlaybackService.putExtra(PlayFragment.TRACK_TITLE_EXTRA, mTrackTitle);
        mPlaybackService.putExtra(PlayFragment.ARTIST_NAME_EXTRA, mArtistName);
        mPlaybackService.putExtra(PlayFragment.ALBUM_TITLE_EXTRA, mAlbumTitle);
        mPlaybackService.putExtra(PlayFragment.ALBUM_ART_EXTRA, mImgUrl);
        mPlaybackService.putExtra(TRACK_POSITION, mTrackPosition);
        mPlaybackService.putParcelableArrayListExtra(TRACK_INFO_EXTRA, mTracksList);
        /*mPlaybackService.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);*/
    }

    private View.OnClickListener mediaListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!mPlay) {
                onPlayerPlay();

                mPlaybackService.setAction(MediaPlaybackService.ACTION_PLAY);
                getActivity().startService(mPlaybackService);
            } else {
                onPlayerPause();

                if (mPlaybackService == null) {
                    mPlaybackService = new Intent(getActivity(), MediaPlaybackService.class);
                }
                mPlaybackService.setAction(MediaPlaybackService.ACTION_PAUSE);
                getActivity().startService(mPlaybackService);
            }
        }
    };

    private View.OnClickListener prevListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onPlayerPrevious();
        }
    };

    private View.OnClickListener nextListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onPlayerNext();
        }
    };

    /** Update scrub bar every 100 mSec */
    public void updateProgressBar() {
        mHandler.postDelayed(mUpdateTimeTask, 100);
    }

    /**
     * Background Runnable thread for handling scrub bar updates
     **/
    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {

            if (mService.playerReady()) {
                mElapsedTime = mService.getCurrentPosition();
            }

            // Displaying time completed playing
            mTrackElapsedTimeTextView.setText(Utility.milliToTimer(mElapsedTime));

            mTrackDuration =  mService.getTrackDuration();
            String durationText = mTrackTotalDurationTextView.getText().toString();
            if (durationText.equals("-") || durationText.contains("0:00")) {
                mTrackTotalDurationTextView.setText(Utility.milliToTimer(mTrackDuration));
            }

            // Updating mProgress bar
            if (mTrackDuration != 0) {
                mProgress = (int) (((mElapsedTime / (double) mTrackDuration) + 0.005) * 100);
                //Log.i("Progress", "" + mProgress);
                mSeekBar.setProgress(mProgress);
            }
            if (mProgress < 100) {
                // Running this thread after 100 milliseconds
                updateProgressBar();
            } else {
                // plays next track by default
                mTrackElapsedTimeTextView.setText(mTrackTotalDurationTextView.getText().toString());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                onPlayerNext();
            }
        }
    };

    private SeekBar.OnSeekBarChangeListener seekBarListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser && isSameTrack()) {
                int mSec = (int)((double)progress/100 * mService.getTrackDuration());
                mService.seekTo(mSec);
                //Log.d(LOG_TAG, "Seek progress" + progress);
            }
            mProgress = progress;
        }
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // remove message Handler from updating mProgress bar
            mHandler.removeCallbacks(mUpdateTimeTask);
        }
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // resume updating seek bar
            mPlaybackService.putExtra(PlayFragment.SEEKBAR_PROGRESS, mProgress);
            if (isSameTrack()) {
                updateProgressBar();
            }
        }
    };

    /**
     * Called on song completion to reset scrub bar to start of track
     **/
    private void resetScrubBar() {
        Log.d(LOG_TAG, "Track playback complete");
        mTrackElapsedTimeTextView.setText(Utility.milliToTimer(0));
        mProgress = 0;
        mPlaybackService.putExtra(SEEKBAR_PROGRESS, mProgress);
        mSeekBar.setProgress(0);
    }

    public void onPlayerPlay() {
        mPlay = true;
        mPlayButton.setBackgroundResource(android.R.drawable.ic_media_pause);
        updateProgressBar();
    }

    public void onPlayerPause() {
        mPlay = false;
        mPlayButton.setBackgroundResource(android.R.drawable.ic_media_play);
        mHandler.removeCallbacks(mUpdateTimeTask);
    }

    public void onPlayerNext() {
        onPlayerStop();
        mTrackPosition = (mTrackPosition + 1) % mTrackCount;
        updateTrack(mTrackPosition);
        prepareMediaIntent();
        mPlaybackService.setAction(MediaPlaybackService.ACTION_NEXT);
        getActivity().startService(mPlaybackService);
        onPlayerPlay();
    }

    public void onPlayerPrevious() {
        if (mProgress > 9) {
            resetScrubBar();
            mService.seekTo(0);
        } else {
            onPlayerStop();
            mTrackPosition = (mTrackPosition - 1) % mTrackCount;
            if (mTrackPosition < 0) {
                mTrackPosition = mTrackCount - 1;
            }
            updateTrack(mTrackPosition);
            prepareMediaIntent();
            mPlaybackService.setAction(MediaPlaybackService.ACTION_PREVIOUS);
            getActivity().startService(mPlaybackService);
            onPlayerPlay();
        }
    }

    public void onPlayerStop() {
        onPlayerPause();
        resetScrubBar();
    }

    private boolean isSameTrack() {
        if (!mPreviewUrl.equals(mService.getSongUrl())) {
            return false;
        }
        return true;
    }

    private void updateTrack(int i) {
        TrackInfo newTrack = mTracksList.get(i);
        mTrackTitle = newTrack.getTrackName();
        mAlbumTitle = newTrack.getAlbumName();
        mImgUrl = newTrack.getImageUrl();
        mPreviewUrl = newTrack.getPreviewUrl();

        mAlbumTextView.setText(mAlbumTitle);
        mTrackTextView.setText(mTrackTitle);
        updateImage();
    }

    private void updateImage() {
        if (mImageView != null) {
            // vertical layout album art
            Picasso.with(getActivity()).load(mImgUrl).into(mImageView);
        } else {
            // horizontal layout album art
            Picasso.with(getActivity()).load(mImgUrl).into(new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        mLandscapeBackground.setBackground(new BitmapDrawable(bitmap));
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
            //Log.d(LOG_TAG, "Service connected");
            //Log.d(LOG_TAG, "player ready: " + mService.playerReady());
            //Log.d(LOG_TAG, "playing: " + mPlay);

            if (mService.playerReady() && mService.getSongUrl().equals(mPreviewUrl)) {
                //Log.d(LOG_TAG, "Want to update progress bar: " + mProgress);
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


    /**
     * Save instance state before orientation changes
     **/
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onSaveInstanceStateCalled");
        savedInstanceState.putInt("track duration", mTrackDuration);
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
            mTrackDuration = savedInstanceState.getInt("track duration");
            mTrackTotalDurationTextView.setText(Utility.milliToTimer(mTrackDuration));
            int currentTime = savedInstanceState.getInt("current time");
            mTrackElapsedTimeTextView.setText(Utility.milliToTimer(currentTime));

            if (mPlay) {
                mPlayButton.setBackgroundResource(android.R.drawable.ic_media_pause);
                updateProgressBar();
            } else {
                mPlayButton.setBackgroundResource(android.R.drawable.ic_media_play);
            }
        }
    }

}
