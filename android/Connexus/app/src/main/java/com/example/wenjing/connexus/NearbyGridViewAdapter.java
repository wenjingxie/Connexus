package com.example.wenjing.connexus;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.squareup.picasso.Picasso;

import java.util.List;

import static android.widget.ImageView.ScaleType.CENTER_CROP;

/**
 * Created by wenjing on 14-11-9.
 */
// grid adapter class
final class NearbyGridViewAdapter extends BaseAdapter {
    private final Context CONTEXT;
    private List<List<String>> IMAGE_INFOS;

    public NearbyGridViewAdapter(Context context, List<List<String>> imageInfos) {
        this.CONTEXT = context;
        this.IMAGE_INFOS = imageInfos;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SquaredImageView view = (SquaredImageView) convertView;
        if (view == null) {
            view = new SquaredImageView(CONTEXT);
            view.setScaleType(CENTER_CROP);
        }

        // Get the image URL for the current position.
        String url = getItem(position);

        // Trigger the download of the URL asynchronously into the image view.
        if (url.length() != 0) {
            Picasso.with(CONTEXT) //
                    .load(url) //
                    .placeholder(R.drawable.placeholder) //
                    .error(R.drawable.error) //
                    .fit() //
                    .tag(CONTEXT) //
                    .into(view);
        }
        else {
            Picasso.with(CONTEXT).load(R.drawable.error).into(view);
        }


        return view;
    }

    @Override public int getCount() {
        return IMAGE_INFOS.size();
    }

    @Override public String getItem(int position) {
        return IMAGE_INFOS.get(position).get(1);
    }

    @Override public long getItemId(int position) {
        return position;
    }
}
