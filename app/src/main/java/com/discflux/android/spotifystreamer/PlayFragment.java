package com.discflux.android.spotifystreamer;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by Phil on 10/06/2015.
 */
public class PlayFragment extends Fragment{

    private final String LOG_TAG = PlayFragment.class.getSimpleName();
    private String artistName;
    private String albumTitle;
    private String trackTitle;
    private String imgUrl;
    private String previewUrl;
    private boolean play = false;

    private TextView artistText, albumText, trackText;
    private ImageView imageView;
    private ImageButton prevButton, playButton, nextButton;

    @Nullable
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
        return rootView;
    }

    private void setup(View view) {
        artistText = (TextView) view.findViewById(R.id.artist_textview);
        albumText = (TextView) view.findViewById(R.id.album_textview);
        trackText = (TextView) view.findViewById(R.id.track_textview);
        imageView = (ImageView) view.findViewById(R.id.album_art_imageview);
        prevButton = (ImageButton) view.findViewById(R.id.button_previous);
        playButton = (ImageButton) view.findViewById(R.id.button_play);
        nextButton = (ImageButton) view.findViewById(R.id.button_next);

        artistText.setText(artistName);
        albumText.setText(albumTitle);
        trackText.setText(trackTitle);
        Picasso.with(getActivity()).load(imgUrl).into(imageView);
    }
}
