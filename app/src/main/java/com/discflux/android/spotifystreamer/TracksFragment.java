package com.discflux.android.spotifystreamer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.RetrofitError;


/**
 * A placeholder fragment containing a simple view.
 */
public class TracksFragment extends Fragment {

    private static final String LOG_TAG = TracksFragment.class.getSimpleName();
    public static final String ARTIST_NAME = "ARTIST_NAME_DATA";
    public static final String SPOTIFY_ID = "SPOTIFY_ID_DATA";
    TrackAdapter mTracksAdapter;
    String mArtistName;
    private ArrayList<TrackInfo> mTracksList;

    public TracksFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_tracks, container, false);

        //String spotifyId = "12Chz98pHFMPJEknJQMWvI";
        String spotifyId = "";

        Bundle arguments = getArguments();
        if (arguments != null) {
            mArtistName = arguments.getString(ARTIST_NAME);
            spotifyId = arguments.getString(SPOTIFY_ID);
        }

        Intent intent = getActivity().getIntent();
        if (intent != null) {
            Log.d(LOG_TAG, "Is a phone fragment");
            if (intent.hasExtra(ARTIST_NAME)) {
                mArtistName = intent.getStringExtra(ARTIST_NAME);
            }
            if (intent.hasExtra(SPOTIFY_ID)) {
                spotifyId = intent.getStringExtra(SPOTIFY_ID);

            }
        }
        updateTracks(spotifyId);

        ActionBarActivity actionBarActivity = (ActionBarActivity)getActivity();
        ActionBar actionBar = actionBarActivity.getSupportActionBar();
        if (actionBar==null) {
            Log.e(LOG_TAG, "Action bar is null!");
        }
        else if(mArtistName != null){
            actionBar.setSubtitle(mArtistName);
        }

        /*// placeholder tracks
        String[] tracks = {
                "ABC",
                "XYXY",
                "Lose Yourself",
                "Knights of Cydonia",
                "Paradise"
        };

        List<String> trackList = new ArrayList<String>(Arrays.asList(tracks));*/

        mTracksAdapter = new TrackAdapter(
                getActivity(),
                R.layout.list_item_track,
                new ArrayList<TrackInfo>());

        ListView listView = (ListView) rootView.findViewById(R.id.list_view_tracks);
        listView.setAdapter(mTracksAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TrackInfo track = mTracksAdapter.getItem(position);
                ArrayList<String> trackDetails = new ArrayList<>();
                trackDetails.add(mArtistName);
                trackDetails.add(track.getTrackName());
                trackDetails.add(track.getAlbumName());
                trackDetails.add(track.getImageUrl());
                trackDetails.add(track.getPreviewUrl());
                Intent playTrackIntent = new Intent(getActivity(), PlayActivity.class);
                playTrackIntent.putExtra(PlayFragment.ARTIST_NAME_EXTRA, mArtistName);
                playTrackIntent.putParcelableArrayListExtra(PlayFragment.TRACK_INFO_EXTRA, mTracksList);
                playTrackIntent.putExtra(PlayFragment.TRACK_POSITION, position);
                //playTrackIntent.putStringArrayListExtra(Intent.EXTRA_TEXT, trackDetails);
                startActivity(playTrackIntent);
            }
        });

        return rootView;
    }

    private void updateTracks(String spotifyId) {
        if (spotifyId != "") {
            FetchTopTracksTask fetchTracks = new FetchTopTracksTask();
            fetchTracks.execute(spotifyId);
        } else {
            // Todo: error, no artist id provided
        }
    }

    public class FetchTopTracksTask extends AsyncTask<String, Void, List<TrackInfo>> {
        @Override
        protected List<TrackInfo> doInBackground(String... params) {

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            final String countryCode = preferences.getString(
                    getString(R.string.pref_locality_key),
                    getString(R.string.pref_locality_default));
            Log.d(LOG_TAG, countryCode);

            //final String countryCode = "GB";

            SpotifyApi api = new SpotifyApi();
            SpotifyService spotify = api.getService();

            /*Map map = new TreeMap();
            map.put("country", countryCode);*/

            Tracks data = null;
            try {
                Log.d(LOG_TAG, params[0] + ", " + countryCode);
                data = spotify.getArtistTopTrack(params[0], countryCode);
            } catch (RetrofitError error) {
                SpotifyError spotifyError = SpotifyError.fromRetrofitError(error);
                // handle error
            }

            List<Track> tracks = data.tracks;
            List<TrackInfo> trackInfoList = new ArrayList<>(tracks.size());
            for(Track track:tracks) {
                String trackName = track.name;
                String albumName = track.album.name;
                String imageUrl = "";
                String thumbUrl = "";
                String previewUrl = track.preview_url;

                if(track.album.images.size()<1) {
                    // no images
                    //Log.d(LOG_TAG, "Images are missing for the album " + albumName);
                } else {
                    imageUrl = thumbUrl = track.album.images.get(0).url;
                    for (Image image : track.album.images) {
                        if (image.width == 640 && image.height == 640) {
                            imageUrl = image.url;
                        } else if (image.width ==200 && image.height==200) {
                            thumbUrl = image.url;
                            break;
                        } else {
                            thumbUrl = image.url;
                        }
                    }
                }
                //Log.d(LOG_TAG, "Image url " + imageUrl);
                //Log.d(LOG_TAG, "Thumbnail url " + thumbUrl);

                TrackInfo trackInfo = new TrackInfo(trackName, albumName, imageUrl, thumbUrl, previewUrl);
                trackInfoList.add(trackInfo);
            }

            return trackInfoList;
        }

        @Override
        protected void onPostExecute(List<TrackInfo> tracks) {
            // update tracks
            if (tracks!= null) {
                mTracksList = new ArrayList<>(tracks);
                mTracksAdapter.clear();
                mTracksAdapter.addAll(tracks);
            }
        }
    }
}
