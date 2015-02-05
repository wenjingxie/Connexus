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
import android.widget.EditText;
import android.widget.GridView;
import android.content.Context;
import android.widget.BaseAdapter;
import android.widget.Toast;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.RequestParams;
import com.squareup.picasso.Picasso;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.google.gson.Gson;
import org.apache.http.Header;

import static android.widget.ImageView.ScaleType.CENTER_CROP;

public class ViewAllFragment extends Fragment {
    //instance variables
    private String TAG  = "ViewAllFragment";
    private AsyncHttpClient httpClient = new AsyncHttpClient();
    private static String REQUEST_URL_ALL = "http://aptconnexus11.appspot.com/viewAllAPI";
    private static String REQUEST_URL_SUB = "http://aptconnexus11.appspot.com/subStreamAPI";
    private ArrayList<String> CoverUrls = new ArrayList<String>();
    private ArrayList<String> StreamKeyUrls = new ArrayList<String>();
    private ArrayList<String> StreamNames = new ArrayList<String>();
    private OnStreamSelectedListener mCallback;
    private GridView GV;
    private List<List<String>> SUB_STREAMS_INFOS;
    //call back
    public interface OnStreamSelectedListener {
        public void onStreamSelected(String streamKeyUrl, String streamName);
        public void onSearchSelected(String keyword);
        public void onNearbySelected();
    }

    //make sure main activity implements the callback interface
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (OnStreamSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnStreamSelectedListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //Toast.makeText(getActivity(), "User: "+ MainActivity.USER +"test" +MainActivity.TEST, Toast.LENGTH_LONG).show();
        final View rootView = inflater.inflate(R.layout.fragment_view_all, container, false);
        GV = (GridView) rootView.findViewById(R.id.viewAll_grid_view);
        UpdateAllStreams();

        //set click callback for searchButton
        final Button searchButton = (Button) rootView.findViewById(R.id.search_viewAll_button);
        searchButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                final EditText keyword = (EditText) rootView.findViewById(R.id.keyword_viewAll_textView);
                String keywordText = keyword.getText().toString();
                mCallback.onSearchSelected(keywordText);
            }
        });

        //set click callback for nearbyButton
        final Button nearbyButton = (Button) rootView.findViewById(R.id.nearby_viewAll_button);
        nearbyButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                mCallback.onNearbySelected();
            }
        });

        // subscribe
        final Button subButton = (Button) rootView.findViewById(R.id.subscribe_viewAll_button);
        if (MainActivity.USER.length() == 0) {
            subButton.setVisibility(View.INVISIBLE);
        }
        subButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if (subButton.getText() != "View All Streams"){
                    UpdateSubStreams();
                    subButton.setText("View All Streams");
                }
                else {
                    UpdateAllStreams();
                    subButton.setText("My Subscribed Streams");
                }

            }
        });



        return rootView;
    }

    private void UpdateAllStreams() {
        StreamKeyUrls.clear();
        StreamNames.clear();
        CoverUrls.clear();
        // get data from viewAllAPI, set GridView Adapter
        httpClient.post(REQUEST_URL_ALL, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                String source = new String(response);
                Type typeOf = new TypeToken<List<Map<String, List<String>>>>(){}.getType();
                Gson gson = new Gson();
                List<Map<String,List<String>>> streamInfos = gson.fromJson(source,typeOf);
                for (Map<String, List<String>> info : streamInfos) {
                    for (String key : info.keySet()){
                        StreamKeyUrls.add(key);
                        StreamNames.add(info.get(key).get(0));
                        CoverUrls.add(info.get(key).get(1));
                        if (CoverUrls.size() <= 16){
                            GV.setAdapter(new GridViewAdapter(getActivity(), CoverUrls));

                        }
                        else{
                            GV.setAdapter(new GridViewAdapter(getActivity(), CoverUrls.subList(0, 16)));
                        }
                    }
                }

                GV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                        mCallback.onStreamSelected(StreamKeyUrls.get(position), StreamNames.get(position));
                    }
                });
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                Log.e(TAG, "There was a problem in retrieving the url : " + e.toString());
            }
        });
    }


    private void UpdateSubStreams(){
        RequestParams params = new RequestParams();
        params.put("user", MainActivity.USER);
        httpClient.get(REQUEST_URL_SUB, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                Log.v(TAG, "sub!!!!!!");
                String source = new String(response);
                Log.v(TAG, "sub streams info: " + source);
                Type typeOf = new TypeToken<List<List<String>>>() {}.getType();
                Gson gson = new Gson();
                SUB_STREAMS_INFOS = gson.fromJson(source, typeOf);
                if (SUB_STREAMS_INFOS.size() <= 16) {
                    GV.setAdapter(new NearbyGridViewAdapter(getActivity(), SUB_STREAMS_INFOS));
                } else {
                    GV.setAdapter(new NearbyGridViewAdapter(getActivity(), SUB_STREAMS_INFOS.subList(0, 16)));
                }
                GV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                        mCallback.onStreamSelected(SUB_STREAMS_INFOS.get(position).get(0), SUB_STREAMS_INFOS.get(position).get(2));
                    }
                });

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                Log.e(TAG, "There was a problem in retrieving the url : " + e.toString());
            }
        });

    }

}