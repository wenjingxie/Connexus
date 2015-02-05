package com.example.wenjing.connexus;



import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.squareup.picasso.Picasso;

import org.apache.http.Header;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class UploadFragment extends Fragment {

    private String TAG = "Upload Fragment";
    private String StreamKeyUrl;
    private String StreamName;
    private OnUploadSelectedListener mCallback;
    private static final int SELECT_PICTURE = 1;
    private String selectedImagePath;
    private AsyncHttpClient httpClient = new AsyncHttpClient();
    private String BLOB_UPLOAD_URL;
    private String REQUEST_UPLOAD_URL;


    //call back
    public interface OnUploadSelectedListener {
        public void onFinishUploadSelected(String streamKeyUrl, String streamName);
        public void onCameraSelected(String streamKeyUrl, String streamName);
    }

    //make sure main activity implements the callback interface
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (OnUploadSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnUploadSelectedListener");
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_upload, container, false);

        // update the textView with the name of stream
        Bundle args = this.getArguments();
        StreamKeyUrl = args.getString("streamKeyUrl");
        StreamName = args.getString("streamName");
        selectedImagePath = args.getString("path");
        TextView streamText = (TextView) rootView.findViewById(R.id.name_upload_textView);
        streamText.setText(StreamName);


        // set the click listener of button choose from library
        final Button imageLibraryButton = (Button) rootView.findViewById(R.id.image_library_upload_button);
        imageLibraryButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(Intent.createChooser(intent,
                        "Select Picture"), SELECT_PICTURE);
            }
        });

        // set click listener of button using camera
        final Button cameraButton = (Button) rootView.findViewById(R.id.camera_upload_button);
        cameraButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                mCallback.onCameraSelected(StreamKeyUrl,StreamName);
            }

        });

        // set the click listener of button upload
        final Button uploadButton = (Button) rootView.findViewById(R.id.upload_upload_button);
        uploadButton.setEnabled(false);
        uploadButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
            // upload image file to blobstore
                REQUEST_UPLOAD_URL = "http://aptconnexus11.appspot.com/getBlobUrl/" + StreamKeyUrl;
                // get data from viewSingleAPI, set GridView Adapter
                httpClient.post(REQUEST_UPLOAD_URL, new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                        String source = new String(response);
                        Log.v(TAG, "url" + source);
                        Type typeOf = new TypeToken<Map<String, String>>(){}.getType();
                        Gson gson = new Gson();
                        Map<String, String> urls = gson.fromJson(source, typeOf);
                        BLOB_UPLOAD_URL = urls.get("url");
                        Log.v(TAG, "upload url" + BLOB_UPLOAD_URL);
                        UploadImage(BLOB_UPLOAD_URL);


                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                        Log.e(TAG, "There was a problem in retrieving the url : " + e.toString());
                    }
                });
            }
        });

        if (selectedImagePath != null) {
            UpdateImageView(rootView, selectedImagePath);
        }
        return rootView;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == getActivity().RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                Uri selectedImageUri = data.getData();
                selectedImagePath = getPath(selectedImageUri);
                Log.v(TAG, selectedImagePath);
                UpdateImageView(getView(), selectedImagePath);

            }
        }
    }
    public String getPath(Uri uri) {
        // just some safety built in
        if( uri == null ) {
            // TODO perform some logging or show user feedback
            return null;
        }
        // try to retrieve the image from the media store first
        // this will only work for images selected from gallery
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = getActivity().getContentResolver().query(uri, projection, null, null, null);
        if( cursor != null ){
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        // this is our fallback here
        return uri.getPath();
    }

    public void UpdateImageView(View v, String path){
        Log.v(TAG, "path" + path);
        ImageView imageView = (ImageView) v.findViewById(R.id.imageView_upload_imageView);
        File image = new File(path);
        Picasso.with(getActivity()).load(image).resize(100,120).into(imageView);
        final Button uploadButton = (Button) v.findViewById(R.id.upload_upload_button);
        uploadButton.setEnabled(true);
    }
    public void UploadImage(String url){
        // add image file to request parameter
        File myFile = new File(selectedImagePath);
        RequestParams params = new RequestParams();
        try {
            params.put("file", myFile);
        } catch(FileNotFoundException e) {}

        // add geo location to request parameter
        Log.v(TAG, "latitude" + MainActivity.CUR_LATITUDE);
        Log.v(TAG, "longitude" + MainActivity.CUR_LONGITUDE);
        params.put("latitude", MainActivity.CUR_LATITUDE);
        params.put("longitude", MainActivity.CUR_LONGITUDE);

        httpClient.post(url, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                Log.v(TAG, "upload!!!!!!");
                mCallback.onFinishUploadSelected(StreamKeyUrl, StreamName);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                Log.e(TAG, "There was a problem in retrieving the url : " + e.toString());
            }
        });
    }
}
