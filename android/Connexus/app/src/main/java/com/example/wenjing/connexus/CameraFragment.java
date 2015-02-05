package com.example.wenjing.connexus;



import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class CameraFragment extends Fragment {


    private String TAG = "Camera Fragment";
    private String StreamKeyUrl;
    private String StreamName;
    private OnCameraSelectedListener mCallback;
    private static String mCurrentPhotoPath;
    private static final int REQUEST_TAKE_PHOTO = 1;


    //call back
    public interface OnCameraSelectedListener {
        public void onStreamsSelected();
        public void onUsePicSelected(String streamKeyUrl, String streamName, String path);

    }

    //make sure main activity implements the callback interface
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (OnCameraSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnCameraSelectedListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_camera, container, false);
        Bundle args = this.getArguments();
        StreamKeyUrl = args.getString("streamKeyUrl");
        StreamName = args.getString("streamName");

        //set click for streams button
        final Button streamsButton = (Button) rootView.findViewById(R.id.streams_camera_button);
        streamsButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                mCallback.onStreamsSelected();
            }
        });

        //set click for camera button
        final Button cameraButton = (Button) rootView.findViewById(R.id.camera_camera_button);
        cameraButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });

        //set click for use this picture button
        final Button usePicButton = (Button) rootView.findViewById(R.id.usePic_camera_button);
        usePicButton.setEnabled(false);
        usePicButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                mCallback.onUsePicSelected(StreamKeyUrl,StreamName,mCurrentPhotoPath);
            }
        });

        return rootView;
    }


    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        Log.v(TAG, "start intent");
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            Log.v(TAG, "test");
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
                Log.v(TAG, "start create file");
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.v(TAG, "io error");
                ex.printStackTrace();

            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Log.v(TAG, "start take pic");
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == getActivity().RESULT_OK) {
//            Bundle extras = data.getExtras();
//            Bitmap imageBitmap = (Bitmap) extras.get("data");
            Log.v(TAG, "end of pic" + mCurrentPhotoPath);
            File image = new File(mCurrentPhotoPath);
            ImageView mImageView = (ImageView) getView().findViewById(R.id.imageView_camera);
            Picasso.with(getActivity()).load(image).resize(200,200).into(mImageView);
           // mImageView.setImageBitmap(imageBitmap);
            final Button usePic = (Button) getView().findViewById(R.id.usePic_camera_button);
            Log.v(TAG, "test enable button");
            usePic.setEnabled(true);
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        if (!storageDir.getParentFile().exists()) {
            Log.v(TAG, "so");
            storageDir.getParentFile().mkdirs();
        }
        if (!storageDir.exists()) {
            storageDir.mkdirs();
            storageDir.createNewFile();
            Log.v(TAG,"sd");
        }

        Log.v(TAG, storageDir.toString());
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        Log.v(TAG, image.toString());
        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        Log.v(TAG, mCurrentPhotoPath);
        return image;
    }
}
