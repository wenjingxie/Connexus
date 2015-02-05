package com.example.wenjing.connexus;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.content.IntentSender.SendIntentException;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

/**
 * Created by wenjing on 11/7/14.
 */
public class LoginFragment extends Fragment implements
        ConnectionCallbacks, OnConnectionFailedListener{

    private static String TAG = "Login Fragment";
    private OnLoginSelectedListener mCallback;
    private static final int RC_SIGN_IN = 0;
    private GoogleApiClient mGoogleApiClient;
    private boolean mIntentInProgress;
    private boolean mSignInClicked;
    private ConnectionResult mConnectionResult;


    public LoginFragment() {
    }

    //call back
    public interface OnLoginSelectedListener {
        public void onStreamsSelected();
    }


    //make sure main activity implements the callback interface
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (OnLoginSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnLoginSelectedListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .build();
        Log.v(TAG, "Client" +mGoogleApiClient);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_login, container, false);

        // streamsButton
        final Button streamsButton = (Button) rootView.findViewById(R.id.streams_login_button);
        streamsButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                mCallback.onStreamsSelected();
            }
        });

        rootView.findViewById(R.id.signIn_login_button).setOnClickListener(new View.OnClickListener(){
            public void onClick(View view) {
                if (view.getId() == R.id.signIn_login_button
                        && !mGoogleApiClient.isConnecting()) {
                    mSignInClicked = true;
                    resolveSignInError();
                }
            }
        });

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Toast.makeText(getActivity(), "connect error!"+ MainActivity.USER, Toast.LENGTH_LONG).show();
        if (!mIntentInProgress) {
            // Store the ConnectionResult so that we can use it later when the user clicks
            // 'sign-in'.
            mConnectionResult = result;

            if (mSignInClicked) {
                // The user has already clicked 'sign-in' so we attempt to resolve all
                // errors until the user is signed in, or they cancel.
                resolveSignInError();
            }
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.v(TAG, "connected!!!!");
        mSignInClicked = false;
        String email = Plus.AccountApi.getAccountName(mGoogleApiClient);
        MainActivity.USER = email.substring(0, email.indexOf("@"));
        MainActivity.TEST = email;
        //MainActivity.USER = email;
        Log.v(TAG, "user name"+ MainActivity.USER);
        Toast.makeText(getActivity(), "User is connected!"+ MainActivity.USER, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onActivityResult(int requestCode, int responseCode, Intent intent) {
        if (requestCode == RC_SIGN_IN) {
            if (responseCode != getActivity().RESULT_OK) {
                mSignInClicked = false;
            }

            mIntentInProgress = false;

            if (!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        mGoogleApiClient.connect();
    }

    /* A helper method to resolve the current ConnectionResult error. */
    private void resolveSignInError() {
        if (mConnectionResult != null){
            if (mConnectionResult.hasResolution()) {
                try {
                    Log.v(TAG, "resolve try");
                    mIntentInProgress = true;
                    getActivity().startIntentSenderForResult(mConnectionResult.getResolution().getIntentSender(),
                            RC_SIGN_IN, null, 0, 0, 0);
                } catch (SendIntentException e) {
                    // The intent was canceled before it was sent.  Return to the default
                    // state and attempt to connect to get an updated ConnectionResult.
                    Log.v(TAG, "exception");
                    mIntentInProgress = false;
                    mGoogleApiClient.connect();
                }
            }
        }

    }

}
