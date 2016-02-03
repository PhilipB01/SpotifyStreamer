package com.discflux.android.spotifystreamer;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.discflux.android.spotifystreamer.service.MediaPlaybackService;
import com.squareup.picasso.Picasso;

import java.io.IOException;
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

    private String artistName;
    private String albumTitle;
    private String trackTitle;
    private String imgUrl;
    private String previewUrl;

    private MediaPlayer mMediaPlayer;
    private boolean play = false;
    private boolean initialStage = true;


    private static final String ACTION_PLAY = "com.discflux.action.PLAY";
    private static final String ACTION_PAUSE = "com.discflux.action.PAUSE";
    private static final String LOG_TAG = PlayFragment.class.getSimpleName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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

    private void setup(View view) {
        artistText = (TextView) view.findViewById(R.id.artist_textview);
        albumText = (TextView) view.findViewById(R.id.album_textview);
        trackText = (TextView) view.findViewById(R.id.track_textview);
        imageView = (ImageView) view.findViewById(R.id.album_art_imageview);

        seekBar = (SeekBar) view.findViewById(R.id.seekBar);
        trackCurrentDurationText = (TextView) view.findViewById(R.id.media_current_time_textview);
        trackTotalDurationText = (TextView) view.findViewById(R.id.media_end_time_textview);

        playButton = (ImageButton) view.findViewById(R.id.button_play);
        playButton.setBackgroundResource(R.drawable.ic_media_play);

        prevButton = (ImageButton) view.findViewById(R.id.button_previous);
        nextButton = (ImageButton) view.findViewById(R.id.button_next);

        artistText.setText(artistName);
        albumText.setText(albumTitle);
        trackText.setText(trackTitle);
        Picasso.with(getActivity()).load(imgUrl).into(imageView);
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mUpdateTimeTask);
    }

    private View.OnClickListener mediaListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!play) {
                playButton.setBackgroundResource(R.drawable.ic_media_pause);
                if (initialStage) {
                    Intent playbackService = new Intent(getActivity(), MediaPlaybackService.class);
                    playbackService.setAction(MediaPlaybackService.ACTION_PLAY);
                    playbackService.putExtra("song url", previewUrl);
                    playbackService.putExtra("song name", trackTitle);
                    getActivity().startService(playbackService);
                    //Log.d(LOG_TAG, previewUrl);
                    //new Player().execute(previewUrl);
                }/* else {
                    if (!mMediaPlayer.isPlaying()) {
                        mMediaPlayer.start();
                    }
                }*/
                play = true;
            } else {
                playButton.setBackgroundResource(R.drawable.ic_media_play);
                /*if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.pause();
                }*/
                Intent playbackService = new Intent(getActivity(), MediaPlaybackService.class);
                playbackService.setAction(MediaPlaybackService.ACTION_PAUSE);
                playbackService.putExtra("song url", previewUrl);
                playbackService.putExtra("song name", trackTitle);
                getActivity().startService(playbackService);
                play = false;
            }
        }
    };

    /**
     * Update timer on seekbar
     **/
    public void updateProgressBar() {
        mHandler.postDelayed(mUpdateTimeTask, 100);
    }

    /**
     * Background Runnable thread
     **/
    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            long currentDuration = mMediaPlayer.getCurrentPosition();

            // Displaying time completed playing
            trackCurrentDurationText.setText(milliToTimer(currentDuration));

            // Updating progress bar
            int progress = (int) (currentDuration / 1000);
            //Log.d("Progress", ""+progress);
            if(mMediaPlayer != null) {
                seekBar.setProgress(progress);
            }

            // Running this thread after 100 milliseconds
            mHandler.postDelayed(this, 100);
        }
    };

    private SeekBar.OnSeekBarChangeListener seekBarListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                mMediaPlayer.seekTo(progress * 1000);
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

    private String milliToTimer(long duration) {
        int minutes = (int)(duration / 60000);
        int seconds = (int)(duration % 60000) / 1000;

        return String.format("%2d:%02d", minutes, seconds);
    }

    /**
     * preparing mediaplayer will take sometime to buffer the content so prepare it inside the background thread and starting it on UI thread.
     * @author piyush
     *
     */
    class Player extends AsyncTask<String, Void, Boolean> {
        private ProgressDialog progress;

        @Override
        protected Boolean doInBackground(String... params) {
            Boolean prepared;
            try {

                mMediaPlayer.setDataSource(params[0]);

                mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        initialStage = true;
                        play = false;
                        playButton.setBackgroundResource(R.drawable.ic_media_play);
                        mMediaPlayer.stop();
                        mMediaPlayer.reset();
                        // remove message Handler from updating progress bar
                        mHandler.removeCallbacks(mUpdateTimeTask);
                        seekBar.setProgress(0);
                        trackTotalDurationText.setText("-");
                        trackCurrentDurationText.setText("0:00");
                    }
                });
                mMediaPlayer.prepare();
                prepared = true;
            } catch (IllegalArgumentException e) {
                Log.d("IllegarArgument", e.getMessage());
                prepared = false;
                e.printStackTrace();
            } catch (SecurityException e) {
                prepared = false;
                e.printStackTrace();
            } catch (IllegalStateException e) {
                prepared = false;
                e.printStackTrace();
            } catch (IOException e) {
                prepared = false;
                e.printStackTrace();
            }
            return prepared;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (progress.isShowing()) {
                progress.cancel();
            }
            Log.d("Prepared", "//" + result);
            seekBar.setMax(mMediaPlayer.getDuration() / 1000); // where mFileDuration is mMediaPlayer.getDuration();
            trackTotalDurationText.setText(milliToTimer(mMediaPlayer.getDuration()));
            mMediaPlayer.start();
            updateProgressBar();
            initialStage = false;
        }

        public Player() {
            progress = new ProgressDialog(getActivity());
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            this.progress.setMessage("Buffering...");
            this.progress.show();

        }
    }
}
