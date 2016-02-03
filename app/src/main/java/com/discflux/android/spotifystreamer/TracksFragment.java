package com.discflux.android.spotifystreamer;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
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
import java.util.Map;
import java.util.TreeMap;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;


/**
 * A placeholder fragment containing a simple view.
 */
public class TracksFragment extends Fragment {

    private static final String LOG_TAG = TracksFragment.class.getSimpleName();
    TrackAdapter mTracksAdapter;
    String artistName;

    public TracksFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_tracks, container, false);

        String spotifyId = "";

        Intent intent = getActivity().getIntent();
        if(intent!=null && intent.hasExtra(Intent.EXTRA_TITLE)) {
            artistName = intent.getStringExtra(Intent.EXTRA_TITLE);
        }
        if(intent!=null && intent.hasExtra(Intent.EXTRA_TEXT)) {
            spotifyId = intent.getStringExtra(Intent.EXTRA_TEXT);
        }
        updateTracks(spotifyId);

        ActionBarActivity actionBarActivity = (ActionBarActivity)getActivity();
        ActionBar actionBar = actionBarActivity.getSupportActionBar();
        if (actionBar==null) {
            Log.e(LOG_TAG, "Action bar is null!");
        }
        else if(artistName!=null){
            actionBar.setSubtitle(artistName);
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
                trackDetails.add(artistName);
                trackDetails.add(track.getTrackName());
                trackDetails.add(track.getAlbumName());
                trackDetails.add(track.getImageUrl());
                trackDetails.add(track.getPreviewUrl());
                Intent playTrack = new Intent(getActivity(), PlayActivity.class);
                playTrack.putStringArrayListExtra(Intent.EXTRA_TEXT, trackDetails);
                startActivity(playTrack);
            }
        });

        return rootView;
    }

    private void updateTracks(String spotifyId) {
        if(spotifyId!="") {
            FetchTopTracksTask fetchTracks = new FetchTopTracksTask();
            fetchTracks.execute(spotifyId);
        } else {
            // Todo: error, no artist id provided
        }
    }

    public class FetchTopTracksTask extends AsyncTask<String, Void, List<TrackInfo>> {
        @Override
        protected List<TrackInfo> doInBackground(String... params) {
            final String countryCode = "GB";
            SpotifyApi api = new SpotifyApi();
            SpotifyService spotify = api.getService();

            Map map = new TreeMap();
            map.put("country", countryCode);
            Tracks data = spotify.getArtistTopTrack(params[0], map);
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
                mTracksAdapter.clear();
                mTracksAdapter.addAll(tracks);
            }
        }
    }
}
