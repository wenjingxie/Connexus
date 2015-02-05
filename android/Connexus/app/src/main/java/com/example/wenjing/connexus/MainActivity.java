package com.example.wenjing.connexus;

import android.content.IntentSender;
import android.location.Location;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;


public class MainActivity extends ActionBarActivity
       implements LoginFragment.OnLoginSelectedListener,
        ViewAllFragment.OnStreamSelectedListener,
        ViewSingleFragment.OnViewSingleSelectedListener,
        UploadFragment.OnUploadSelectedListener,
        CameraFragment.OnCameraSelectedListener,
        SearchFragment.OnStreamSearchResultSelectedListener,
        NearbyFragment.OnNearbySelectedListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener{

    public static double CUR_LATITUDE;
    public static double CUR_LONGITUDE;
    public static String USER = "";
    public static String TEST;
    private final static int
            CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private String TAG = "Main Activity";
    private LocationClient mLocationClient;



    // Callback of stream cover selected on ViewAll Fragment
    @Override
    public void onStreamSelected(String streamKeyUrl, String streamName) {
        ViewSingleFragment stream = new ViewSingleFragment ();
        Bundle args = new Bundle();
        args.putString("streamKeyUrl", streamKeyUrl);
        args.putString("streamName", streamName);
        stream.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, stream)
                .commit();


    }
    // Callback of search button on ViewAll Fragment
    @Override
    public void onSearchSelected(String keyword) {
        SearchFragment search = new SearchFragment ();
        Bundle args = new Bundle();
        args.putString("keyword", keyword);
        search.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, search)
                .addToBackStack(null)
                .commit();
    }

    // Callback of nearby button on ViewAll Fragment
    @Override
    public void onNearbySelected() {
        NearbyFragment nearby = new NearbyFragment ();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, nearby)
                .addToBackStack(null)
                .commit();
    }

    //callback of pic item selected on Nearby Fragment
    @Override
    public void onPicSelected(String streamKeyUrl, String streamName) {
        ViewSingleFragment stream = new ViewSingleFragment ();
        Bundle args = new Bundle();
        args.putString("streamKeyUrl", streamKeyUrl);
        args.putString("streamName", streamName);
        stream.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, stream)
                .commit();
    }

    // Callback of Streams Button on Login, View Single,Camera and Nearby Fragment
    @Override
    public void onStreamsSelected() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, new ViewAllFragment())
                .commit();
    }


    // Callback of upload_view Button on View Single Fragment
    @Override
    public void onUploadSelected(String streamKeyUrl, String streamName) {
        UploadFragment upload = new UploadFragment();
        Bundle args = new Bundle();
        args.putString("streamKeyUrl", streamKeyUrl);
        args.putString("streamName", streamName);
        upload.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, upload)
                .commit();
    }

    //callback of using camera button on upload fragment
    @Override
    public void onCameraSelected(String streamKeyUrl, String streamName) {
        CameraFragment camera = new CameraFragment();
        Bundle args = new Bundle();
        args.putString("streamKeyUrl", streamKeyUrl);
        args.putString("streamName", streamName);
        camera.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, camera)
                .commit();
    }

    //callback of finish upload on upload fragment
    @Override
    public void onFinishUploadSelected(String streamKeyUrl, String streamName) {
        ViewSingleFragment stream = new ViewSingleFragment ();
        Bundle args = new Bundle();
        args.putString("streamKeyUrl", streamKeyUrl);
        args.putString("streamName", streamName);
        stream.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, stream)
                .commit();
    }

    //callback of use pic button on Camera Fragment
    @Override
    public void onUsePicSelected(String streamKeyUrl, String streamName, String path) {
        UploadFragment upload = new UploadFragment();
        Bundle args = new Bundle();
        args.putString("streamKeyUrl", streamKeyUrl);
        args.putString("streamName", streamName);
        args.putString("path", path);
        upload.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, upload)
                .commit();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //location
        mLocationClient = new LocationClient(this, this, this);
        // set login fragment
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new LoginFragment())
                    .commit();
        }

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    // connect to location service
    @Override
    public void onConnected(Bundle savedInstanceState) {
        // Display the connection status
        Location curLocation = mLocationClient.getLastLocation();
        CUR_LATITUDE = curLocation.getLatitude();
        CUR_LONGITUDE = curLocation.getLongitude();
        //Toast.makeText(this, "Latitude: "+ CUR_LATITUDE + "Longitude: " + CUR_LONGITUDE, Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onDisconnected() {
        Toast.makeText(this, "Disconnected. Please re-connect.",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);

            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {

            Log.v(TAG, "connect error");
        }

    }


    @Override
    protected void onStart() {
        super.onStart();
        mLocationClient.connect();
    }

    @Override
    protected void onStop() {
        mLocationClient.disconnect();
        super.onStop();
    }

}
