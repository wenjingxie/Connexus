package com.example.wenjing.connexus;



import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class NearbyFragment extends Fragment {
    private String TAG  = "NearbyFragment";
    private AsyncHttpClient httpClient = new AsyncHttpClient();
    private static String REQUEST_URL = "http://aptconnexus11.appspot.com/nearbyAPI";
    private OnNearbySelectedListener mCallback;
    private ArrayList<String> STREAM_KEY_URLS;
    private ArrayList<String> STREAM_NAMES;
    private List<List<String>> IMAGE_INFOS;
    private GridView GV;
    private Button MORE;
    private int NUM_PAGE;
    private int CUR_PAGE;

    //call back
    public interface OnNearbySelectedListener {
        public void onStreamsSelected();
        public void onPicSelected(String streamKeyUrl, String streamName);
    }

    //make sure main activity implements the callback interface
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (OnNearbySelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnNearbySelectedListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        CUR_PAGE = 0;
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_nearby, container, false);
        GV = (GridView) rootView.findViewById(R.id.pic_nearby_gridView);

        //set click for streams button
        final Button streamsButton = (Button) rootView.findViewById(R.id.streams_nearby_button);
        streamsButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                mCallback.onStreamsSelected();
            }
        });

        //set click for more button
        MORE = (Button) rootView.findViewById(R.id.more_nearby_button);
        MORE.setEnabled(false);
        MORE.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                CUR_PAGE += 1;
                if ((IMAGE_INFOS.size() - NUM_PAGE * 16) <= 16) {
                    MORE.setEnabled(false);
                    GV.setAdapter(new NearbyGridViewAdapter(getActivity(), IMAGE_INFOS.subList(16 * NUM_PAGE, IMAGE_INFOS.size() - 1)));

                }
                else {
                    GV.setAdapter(new NearbyGridViewAdapter(getActivity(), IMAGE_INFOS.subList(16 * NUM_PAGE, 16 * (NUM_PAGE + 1))));
                    NUM_PAGE += 1;
                }
            }
        });

        // get nearby pics from nearby API
        RequestParams params = new RequestParams();
        params.put("latitude", MainActivity.CUR_LATITUDE);
        params.put("longitude", MainActivity.CUR_LONGITUDE);
        httpClient.post(REQUEST_URL, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                Log.v(TAG, "nearby!!!!!!");
                String source = new String(response);
                Log.v(TAG, "image_infos" + source);
                Type typeOf = new TypeToken<List<List<String>>>() {
                }.getType();
                Gson gson = new Gson();
                IMAGE_INFOS = gson.fromJson(source, typeOf);
                if (IMAGE_INFOS.size() <= 16) {
                    GV.setAdapter(new NearbyGridViewAdapter(getActivity(), IMAGE_INFOS));
                } else {
                    NUM_PAGE += 1;
                    GV.setAdapter(new NearbyGridViewAdapter(getActivity(), IMAGE_INFOS.subList(0, 16)));
                    MORE.setEnabled(true);
                }
                GV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                        mCallback.onPicSelected(IMAGE_INFOS.get(CUR_PAGE * 16 + position).get(2), IMAGE_INFOS.get(CUR_PAGE * 16 + position).get(3));
                    }
                });

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                Log.e(TAG, "There was a problem in retrieving the url : " + e.toString());
            }
        });



        return rootView;
    }


}
