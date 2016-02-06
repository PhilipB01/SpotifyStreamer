package com.discflux.android.spotifystreamer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends ActionBarActivity implements SearchFragment.Callback {

    private static final String TRACKSFRAGMENT_TAG = "TFTAG";
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        if (findViewById(R.id.top_tracks_container) != null) {
            mTwoPane = true;
            Log.d(TRACKSFRAGMENT_TAG, "Two Pane!");
            if (savedInstanceState == null) {
                Log.d(TRACKSFRAGMENT_TAG, "Added Second Pane!");
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.top_tracks_container, new TracksFragment(), TRACKSFRAGMENT_TAG);
            }
        } else {
            Log.d(TRACKSFRAGMENT_TAG, "SIngle Pane!");
            mTwoPane = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //SearchFragment searchFragment = (SearchFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_search);
    }

    @Override
    public void onItemSelected(String artistName, String spotifyId) {
        if (!mTwoPane) {
            Intent tracksIntent = new Intent(this, TracksActivity.class);
            tracksIntent.putExtra(TracksFragment.ARTIST_NAME, artistName);
            tracksIntent.putExtra(TracksFragment.SPOTIFY_ID, spotifyId);
            startActivity(tracksIntent);
        } else {
            Bundle args = new Bundle();
            args.putString(TracksFragment.ARTIST_NAME, artistName);
            args.putString(TracksFragment.SPOTIFY_ID, spotifyId);

            TracksFragment tf = new TracksFragment();
            tf.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.top_tracks_container, tf, TRACKSFRAGMENT_TAG)
                    .commit();
        }
    }


}
