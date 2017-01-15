package com.discflux.android.spotifystreamer;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Image;


/**
 * A placeholder fragment containing a simple view.
 */
public class SearchFragment extends Fragment {

    private static final String LOG_TAG = SearchFragment.class.getSimpleName();
    private static final String SEACRH_VALUE = "search box string";
    private static final String ITEM_POSITION = "selected item osition";

    private int mPosition = ListView.INVALID_POSITION;
    private ListView mListView;
    private ArtistAdapter mArtistAdapter;
    private long delayTime = 500;
    private FetchArtistsTask mLastFetchTask;
    private EditText mSearchBox;
    private String mSearchString;

    public SearchFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if(savedInstanceState != null) {
            mSearchString = savedInstanceState.getString(SEACRH_VALUE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_search, container, false);

        mSearchBox = (EditText) rootView.findViewById(R.id.artist_search_edit_text);
        if(mSearchString != null) {
            mSearchBox.setText(mSearchString);
        }

        mArtistAdapter = new ArtistAdapter(
                    getActivity(),
                    R.layout.list_item_artist,
                    new ArrayList<ArtistInfo>());


        // set clear edit text 'x' button
        final Drawable x = getResources().getDrawable(R.drawable.ic_cancel_text);//your x image, this one from standard android images looks pretty good actually
        x.setBounds(0, 0, x.getIntrinsicWidth() / 2, x.getIntrinsicHeight() / 2);
        mSearchBox.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mSearchBox.getCompoundDrawables()[2] == null) {
                    return false;
                }
                if (event.getAction() != MotionEvent.ACTION_UP) {
                    return false;
                }
                if (event.getX() > mSearchBox.getWidth() - mSearchBox.getPaddingRight() - x.getIntrinsicWidth() / 2) {
                    mSearchBox.setText("");
                    mSearchBox.setCompoundDrawables(null, null, null, null);
                }
                return false;
            }
        });

        /**
         * Example of code for delaying TextWatcher taken from below:
         * http://stackoverflow.com/questions/5730609/is-it-possible-to-slowdown-reaction-of-edittext-listener
         */
        mSearchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mSearchBox.setCompoundDrawables(null, null, s.equals("") ? null : x, null);
            }

            @Override
            public void afterTextChanged(Editable s) {
                synchronized (this) {
                    if (mLastFetchTask != null) {
                        mLastFetchTask.cancel(true);
                    }
                    if (!s.toString().equals("")) {
                        String artist = s.toString();
                        //Log.i(LOG_TAG, "Text changed: " + artist);
                        mLastFetchTask = new FetchArtistsTask();
                        mLastFetchTask.execute(artist);
                    }
                }
            }
        });

        mListView = (ListView) rootView.findViewById(R.id.list_view_artists);
        mListView.setAdapter(mArtistAdapter);


        mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ArtistInfo artist = mArtistAdapter.getItem(position);
                String artistName = artist.getArtistName();
                String spotifyId = artist.getSpotifyId();
                // 0 is terminal value for no artists found message
                if (spotifyId != "0") {
                    ((Callback) getActivity()).onItemSelected(artistName, spotifyId);
                }
                mPosition = position;
            }
        });

        // hides keyboard on listview touch
        mListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                hideKeyboard(getActivity(), v);
                return false;
            }
        });

        if (savedInstanceState != null && savedInstanceState.containsKey(ITEM_POSITION)) {
            mPosition = savedInstanceState.getInt(ITEM_POSITION);
        }
        return rootView;
    }

    public void hideKeyboard(Activity activity, View v) {
        InputMethodManager inputMethodManager =(InputMethodManager)activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(SEACRH_VALUE ,mSearchString);
        if (mPosition != ListView.INVALID_POSITION) {
            outState.putInt(ITEM_POSITION, mPosition);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mPosition != ListView.INVALID_POSITION) {
            mListView.smoothScrollToPosition(mPosition);
        }
    }

    /**
     * Async background task to load Artists
     */
    public class FetchArtistsTask extends AsyncTask<String, Void, List<ArtistInfo>> {

        private final String LOG_TAG = FetchArtistsTask.class.getSimpleName();

        @Override
        protected List<ArtistInfo> doInBackground(String... params) {
            List<ArtistInfo> artistList;
            try {
                Thread.sleep(delayTime);
            } catch (InterruptedException e) {
                //Log.d(LOG_TAG, "Sleep interrupted: InterruptedException");
            }

            SpotifyApi api = new SpotifyApi();
            SpotifyService spotify = api.getService();
            if (params[0].length()<3) {
                return null;
            }
            ArtistsPager results = spotify.searchArtists(params[0]);
            //Log.d(LOG_TAG, "FetchArtistTask executed with string parameter: " + params[0]);
            List<Artist> artists = results.artists.items;
            mSearchString = params[0];
            if (artists.size()<1) {
                //Log.d(LOG_TAG, "Artist List is empty!");
                /*Toast.makeText(getActivity(),
                        "No artists could be found",
                        Toast.LENGTH_SHORT)
                        .show();*/
                // return empty artist array with message
                artistList = new ArrayList<ArtistInfo>(1);
                artistList.add(new ArtistInfo("No artists found, please try again", "0", ""));
                return artistList;
            }
            artistList = new ArrayList<ArtistInfo>(artists.size());

            for (int i=0; i<artists.size(); i++) {
                Artist a = artists.get(i);
                if(a!=null) {
                    String name;
                    String id;
                    String imageUrl = "";
                    name = a.name;
                    id = a.id;
                    // no images for this artist
                    if (a.images.size()==0) {
                        //Log.d(LOG_TAG, "No thumbnail for artist: " + name + " available.");
                    }
                    // only one image so use it
                    else if (a.images.size()==1) {
                        imageUrl=a.images.get(0).url;
                    }
                    // find an image under 300
                    else {
                        for (int j = 0; j < a.images.size(); j++) {
                            Image image = a.images.get(j);
                            if (image.height <= 300 && image.width <= 300) {
                                imageUrl = image.url;
                                /*Log.i(LOG_TAG, "thumbnail width x height: " +
                                                image.width + "x" + image.height +
                                                ", thumbnail was found for " + name +
                                                " under Url" + imageUrl
                                );*/
                                break; // should break from for loop only
                            }
                        }
                        // if size>1 but none selected
                        if (imageUrl=="") {
                            imageUrl=a.images.get(0).url;
                        }
                    }

                    artistList.add(new ArtistInfo(name, id, imageUrl));
                }
            }
            //Log.d(LOG_TAG, "Name is " + name + "\nID is " + id + "\nimage URL is " + imageUrl);
            return artistList;
        }

        @Override
        protected void onPostExecute(List<ArtistInfo> artists) {
            //super.onPostExecute(artists);
            // test loop
            /*for(ArtistInfo artist:artists) {
                Log.d(LOG_TAG, "name is " + artist.getArtistName() +
                        " and image is " + artist.getThumbnailUrl()
                );
            }*/
            mArtistAdapter.clear();
            if (artists != null) {
                mArtistAdapter.addAll(artists);
            }
        }
    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * SearchFragmentCallback for when an item has been selected.
         */
        void onItemSelected(String artistName, String spotifyId);
    }
}

