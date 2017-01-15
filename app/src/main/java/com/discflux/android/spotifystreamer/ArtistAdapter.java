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
public class ArtistAdapter extends ArrayAdapter<ArtistInfo> {

    Context context;

    public ArtistAdapter(Context context, int resourceId, List<ArtistInfo> items) {
        super(context, resourceId, items);
        this.context = context;
    }

    /*private view holder class*/
    private class ViewHolder {
        ImageView imageView;
        TextView textView;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        ArtistInfo artist = getItem(position);

        LayoutInflater mInflater = (LayoutInflater) context
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.list_item_artist, null);
            holder = new ViewHolder();
            holder.textView = (TextView) convertView.findViewById(R.id.list_item_artist_textview);
            holder.imageView = (ImageView) convertView.findViewById(R.id.list_item_thumbnail);
            convertView.setTag(holder);
        } else
            holder = (ViewHolder) convertView.getTag();

        holder.textView.setText(artist.getArtistName());
        if (!artist.getThumbnailUrl().equals("")) {
            // debug picasso loading
            Picasso.with(context).setIndicatorsEnabled(false);
            Picasso.with(context)
                    .load(artist.getThumbnailUrl())
                    .into(holder.imageView);
        } else {
            Picasso.with(context)
                    .load(R.mipmap.ic_launcher)
                    .into(holder.imageView);
        }

        return convertView;
    }
}
