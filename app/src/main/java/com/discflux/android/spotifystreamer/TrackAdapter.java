package com.discflux.android.spotifystreamer;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;


/**
 * Tutorial code- URL:
 * http://theopentutorials.com/tutorials/android/listview/android-custom-listview-with-image-and-text-using-arrayadapter/
 */
public class TrackAdapter extends ArrayAdapter<TrackInfo> {

    Context context;

    public TrackAdapter(Context context, int resourceId, List<TrackInfo> items) {
        super(context, resourceId, items);
        this.context = context;
    }

    /*private view holder class*/
    private class ViewHolder {
        ImageView imageView;
        TextView albumTextView;
        TextView trackTextView;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        TrackInfo track = getItem(position);

        LayoutInflater mInflater = (LayoutInflater) context
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.list_item_track, null);
            holder = new ViewHolder();
            holder.albumTextView = (TextView) convertView.findViewById(R.id.list_item_track_album);
            holder.trackTextView = (TextView) convertView.findViewById(R.id.list_item_track_title);
            holder.imageView = (ImageView) convertView.findViewById(R.id.list_item_track_thumbnail);
            convertView.setTag(holder);
        } else
            holder = (ViewHolder) convertView.getTag();

        holder.albumTextView.setText(track.getAlbumName());
        holder.trackTextView.setText(track.getTrackName());
        if (track.getThumbnailUrl()!="") {
            Picasso.with(context).load(track.getThumbnailUrl()).into(holder.imageView);
        } else {
            Picasso.with(context).load(R.mipmap.ic_launcher).into(holder.imageView);
        }

        return convertView;
    }
}
