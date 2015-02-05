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
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.apache.http.Header;
import org.apache.http.entity.StringEntity;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class SearchFragment extends Fragment {
    private String TAG  = "SearchFragment";
    private AsyncHttpClient httpClient = new AsyncHttpClient();
    private static String REQUEST_URL = "http://aptconnexus11.appspot.com/searchAPI";
    private ArrayList<String> CoverUrls = new ArrayList<String>();
    private ArrayList<String> StreamKeyUrls = new ArrayList<String>();
    private ArrayList<String> StreamNames = new ArrayList<String>();
    private OnStreamSearchResultSelectedListener mCallback;

    //call back
    public interface OnStreamSearchResultSelectedListener {
        public void onStreamSelected(String streamKeyUrl, String streamName);
    }

    //make sure main activity implements the callback interface
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (OnStreamSearchResultSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnStreamSearchResultSelectedListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_search, container, false);
        // hide all ui components about search results
        final Button moreButton = (Button) rootView.findViewById(R.id.more_search_button);
        moreButton.setVisibility(View.INVISIBLE);
        final TextView clickHint = (TextView) rootView.findViewById(R.id.hint_click_search_textView);
        clickHint.setVisibility(View.INVISIBLE);
        final TextView searchResult = (TextView) rootView.findViewById(R.id.result_search_textView);
        searchResult.setText("");
        //initial search
        Bundle args = this.getArguments();
        final String keywordText = args.getString("keyword");
        final EditText keyword = (EditText) rootView.findViewById(R.id.keyword_search_editText);
        keyword.setText(keywordText);
        if (keywordText.length() != 0){
            UpdateSearchResult(keywordText);
        }

//        final TextView resultText = (TextView) rootView.findViewById(R.id.result_search_textView);
//        resultText.setVisibility(View.INVISIBLE);
        // set click of search button
        final Button searchButton = (Button) rootView.findViewById(R.id.search_search_button);
        searchButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                String newKeyword = keyword.getText().toString();
                if (newKeyword.length() != 0){
                    UpdateSearchResult(newKeyword);
                }
                else {
                    searchResult.setText("");
                    clickHint.setVisibility(View.INVISIBLE);
                    StreamKeyUrls.clear();
                    StreamNames.clear();
                    CoverUrls.clear();
                    GridView gv = (GridView) rootView.findViewById(R.id.result_search_gridView);
                    gv.setAdapter(new GridViewAdapter(getActivity(), CoverUrls));
                }

            }
        });

        return rootView;
    }

    // send request to API get search result and update views
    public void UpdateSearchResult(final String keyword) {
        Gson gson = new Gson();
        TreeMap<String, String> keywordMap = new TreeMap<String, String>();
        keywordMap.put("keyword", keyword);
        String keywordJson = gson.toJson(keywordMap);

        try{
            StringEntity entity = new StringEntity(keywordJson);
            httpClient.post(getActivity(),REQUEST_URL, entity, "application/json", new AsyncHttpResponseHandler(){
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                    String source = new String(response);
                    Log.v(TAG, "search result" + source);
                    Type typeOf = new TypeToken<List<Map<String, List<String>>>>(){}.getType();
                    Gson gson = new Gson();
                    List<Map<String,List<String>>> streamInfos = gson.fromJson(source,typeOf);
                    String resultText = streamInfos.size() + " results for " + keyword;
                    if (streamInfos.size() != 0){
                        for (Map<String, List<String>> info : streamInfos) {
                            for (String key : info.keySet()){
                                StreamKeyUrls.add(key);
                                StreamNames.add(info.get(key).get(0));
                                CoverUrls.add(info.get(key).get(1));
                                //Log.v(TAG, "test key" + key);
                                //Log.v(TAG, "test url" + info.get(key).get(1));
                            }
                        }
                        TextView hintClick = (TextView) getView().findViewById(R.id.hint_click_search_textView);
                        hintClick.setVisibility(View.VISIBLE);
                    }
                    else {
                        StreamKeyUrls.clear();
                        StreamNames.clear();
                        CoverUrls.clear();
                        TextView hintClick = (TextView) getView().findViewById(R.id.hint_click_search_textView);
                        hintClick.setVisibility(View.INVISIBLE);
                    }
                    TextView resultView = (TextView) getView().findViewById(R.id.result_search_textView);
                    resultView.setText(resultText);
                    GridView gv = (GridView) getActivity().findViewById(R.id.result_search_gridView);
                    gv.setAdapter(new GridViewAdapter(getActivity(), CoverUrls));

                    //Log.v(TAG, "viewall" + source);
                    // Log.v(TAG, "testGson" + streamInfos.get(0).keySet());

                    gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                            Toast.makeText(getActivity(), "" + StreamKeyUrls.get(position), Toast.LENGTH_SHORT).show();
                            mCallback.onStreamSelected(StreamKeyUrls.get(position), StreamNames.get(position));
                        }
                    });
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                    Log.e(TAG, "There was a problem in retrieving the url : " + e.toString());
                }
            });
        }catch (UnsupportedEncodingException e){
            e.printStackTrace();
        };



    }


}
