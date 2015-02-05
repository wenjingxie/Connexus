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
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.apache.http.Header;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 *
 */
public class ViewSingleFragment extends Fragment {

    //instance variables
    private int NUM_PAGE;
    private String TAG = "ViewSingleFragment";
    private String StreamKeyUrl;
    private String StreamName;
    private AsyncHttpClient httpClient = new AsyncHttpClient();
    private static String REQUEST_URL;
    private OnViewSingleSelectedListener mCallback;
    private GridView GV;
    private ArrayList<String> URLS;
    private Button MORE;

    //call back
    public interface OnViewSingleSelectedListener {
        public void onUploadSelected(String streamKeyUrl, String streamName);
        public void onStreamsSelected();
    }

    //make sure main activity implements the callback interface
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (OnViewSingleSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnViewSingleSelectedListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_view_single, container, false);
        GV = (GridView) rootView.findViewById(R.id.pic_viewSingle_gridView);
        //set click callback for Streams Button
        final Button streamsButton = (Button) rootView.findViewById(R.id.streams_viewSingle_button);
        streamsButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                mCallback.onStreamsSelected();
            }
        });

        //set click callback for Upload Button
        final Button uploadButton = (Button) rootView.findViewById(R.id.upload_viewSingle_button);
        uploadButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                mCallback.onUploadSelected(StreamKeyUrl,StreamName);
            }
        });

        //set click callback for More pic Button
        MORE = (Button) rootView.findViewById(R.id.more_viewSingle_button);
        MORE.setEnabled(false);
        MORE.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if ((URLS.size() - NUM_PAGE * 16) <= 16) {
                    MORE.setEnabled(false);
                    GV.setAdapter(new GridViewAdapter(getActivity(), URLS.subList(16 * NUM_PAGE, URLS.size() - 1)));

                }
                else {
                    GV.setAdapter(new GridViewAdapter(getActivity(), URLS.subList(16 * NUM_PAGE, 16 * (NUM_PAGE + 1))));
                    NUM_PAGE += 1;
                }
            }
        });
        //set the grid view
        Bundle args = this.getArguments();
        StreamName = args.getString("streamName");
        StreamKeyUrl = args.getString("streamKeyUrl");
        REQUEST_URL = "http://aptconnexus11.appspot.com/viewSingleAPI/" + StreamKeyUrl;
        // get data from viewSingleAPI, set GridView Adapter
        httpClient.post(REQUEST_URL, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                String source = new String(response);
                //Log.v(TAG, "urls" + source);
                Type typeOf = new TypeToken<Map<String, List<String>>>(){}.getType();
                Gson gson = new Gson();
                Map<String, List<String>> urls = gson.fromJson(source, typeOf);
                URLS = new ArrayList<String> (urls.get("urls"));
                if (URLS.size() <= 16){
                    GV.setAdapter(new GridViewAdapter(getActivity(), URLS));
                }
                else {
                    NUM_PAGE += 1;
                    GV.setAdapter(new GridViewAdapter(getActivity(), URLS.subList(0, 16)));
                    MORE.setEnabled(true);
                }

                //Log.v(TAG, "viesingle" + source);
                // Log.v(TAG, "testGson" + streamInfos.get(0).keySet());
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                Log.e(TAG, "There was a problem in retrieving the url : " + e.toString());
            }
        });

        return rootView;
    }

}
