package com.holisolutions.holi;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.location.Location;
import com.google.android.gms.location.LocationListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;


public class HoliLocator extends Activity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static final String TAG = "Holi";

    private static final String KEY_IN_RESOLUTION = "is_in_resolution";

    /**
     * Request code for auto Google Play Services error resolution.
     */
    protected static final int REQUEST_CODE_RESOLUTION = 1;

    /**
     * Google API client.
     */
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    /**
     * Determines if the client is in a resolution state, and
     * waiting for resolution intent to return.
     */
    private boolean mIsInResolution;

    private double currentLatitude;
    private double currentLongitude;
    private double currentAltitude;
    private double currentSpeed;

    /**
     * Which protocol is used for sending
     */
    private boolean mIsUDPSelected;

    private TextView textLatitudeText;
    private TextView textLongitudeText;
    private TextView textSpeedText;
    private TextView textAltitudeText;

    private String mNetAddress;

    /**
     * Called when the activity is starting. Restores the activity state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState != null) {
            mIsInResolution = savedInstanceState.getBoolean(KEY_IN_RESOLUTION, false);
        }

        // Interface text fields
        textLatitudeText = (TextView) findViewById(R.id.LatitudeText);
        textLongitudeText = (TextView) findViewById(R.id.LongitudeText);
        textAltitudeText = (TextView) findViewById(R.id.AltitudeText);
        textSpeedText = (TextView) findViewById(R.id.SpeedText);

        // Capture button
        ToggleButton toggleCapture = (ToggleButton) findViewById(R.id.toggleCapture);
        toggleCapture.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled
                    startLocationUpdates();
                } else {
                    // The toggle is disabled
                    stopLocationUpdates();
                }
            }
        });
    }

    private void startLocationUpdates() {
        ((EditText)findViewById(R.id.addressText)).setEnabled(false);
        mNetAddress = ((EditText)findViewById(R.id.addressText)).getText().toString();
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    private void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        ((EditText)findViewById(R.id.addressText)).setEnabled(true);
    }

    /**
     *
     * @param view
     */
    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radioUDP:
                if (checked)
                    mIsUDPSelected = true;
                    break;
            case R.id.radioTCP:
                if (checked)
                    mIsUDPSelected = false;
                    break;
        }
    }

    /**
     * Called when the Activity is made visible.
     * A connection to Play Services need to be initiated as
     * soon as the activity is visible. Registers {@code ConnectionCallbacks}
     * and {@code OnConnectionFailedListener} on the
     * activities itself.
     */
    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        mGoogleApiClient.connect();

    }

    /**
     * Called when activity gets invisible. Connection to Play Services needs to
     * be disconnected as soon as an activity is invisible.
     */
    @Override
    protected void onStop() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        stopLocationUpdates();
        super.onStop();
    }

    /**
     * Saves the resolution state.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IN_RESOLUTION, mIsInResolution);
    }

    /**
     * Handles Google Play Services resolution callbacks.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_RESOLUTION:
                retryConnecting();
                break;
        }
    }

    private void retryConnecting() {
        mIsInResolution = false;
        if (!mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.connect();
        }
    }

    /**
     * Called when {@code mGoogleApiClient} is connected.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "GoogleApiClient connected");
        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1000); // 1 second, in milliseconds
        Log.i(TAG, "LocationRequest configured successfully");
    }


    /**
     * Called when {@code mGoogleApiClient} connection is suspended.
     */
    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended");
        retryConnecting();
    }

    /**
     * Called when {@code mGoogleApiClient} is trying to connect but failed.
     * Handle {@code result.getResolution()} if there is a resolution
     * available.
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
            // Show a localized error dialog.
            GooglePlayServicesUtil.getErrorDialog(
                    result.getErrorCode(), this, 0, new OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            retryConnecting();
                        }
                    }).show();
            return;
        }
        // If there is an existing resolution error being displayed or a resolution
        // activity has started before, do nothing and wait for resolution
        // progress to be completed.
        if (mIsInResolution) {
            return;
        }
        mIsInResolution = true;
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
            retryConnecting();
        }
    }

    /**
     * Executed everytime there's a new location update reported by the FusedLocationAPI
     * @param location the new reported location
     */
    @Override
    public void onLocationChanged(Location location) {
        // send the info
        if(mIsUDPSelected) {
            new SendUDPTask().execute(location);
        } else {
            new SendTCPTask().execute(location);
        }
        // update UI
        textAltitudeText.setText("Altitude: " + location.getAltitude());
        textLatitudeText.setText("Latitude: " + location.getLatitude());
        textLongitudeText.setText("Longitude: " + location.getLongitude());
        textSpeedText.setText("Speed: " + location.getSpeed());
    }

    private class SocketConfig extends AsyncTask<Boolean, Void, Void> {

        /**
         * Send info through TCP
         * @param booleans
         * @return
         */
        @Override
        protected Void doInBackground(Boolean... booleans) {
            double start = System.currentTimeMillis();

            Log.i(TAG, "Elapsed: " + (System.currentTimeMillis() - start) + "us");
            return null;
        }
    }

    private class SendUDPTask extends AsyncTask<Location, Void, Void> {

        /**
         * Sends info through UDP
         * @param locations a singular location, as the calling method only uses one
         * @return null
         */
        @Override
        protected Void doInBackground(Location... locations) {
            double start = System.nanoTime();
            Location loc = locations[0];
            String udpMsg = loc.getAltitude() + ";" + loc.getLatitude() + ";" + loc.getLongitude() + ";" + loc.getSpeed();
            DatagramSocket ds = null;
            try {
                ds = new DatagramSocket();
                InetAddress serverAddr = InetAddress.getByName(mNetAddress);
                DatagramPacket dp;
                dp = new DatagramPacket(udpMsg.getBytes(), udpMsg.length(), serverAddr, 1337);
                ds.send(dp);
                Log.w(TAG, "UDP Sent Location");
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (ds != null) {
                    ds.close();
                }
            }
            Log.i(TAG, "Elapsed: " + (System.nanoTime() - start) * 1000 + "us");
            return null;
        }
    }

    private class SendTCPTask extends AsyncTask<Location, Void, Void> {

        /**
         * Send info through TCP
         * @param locations
         * @return
         */
        @Override
        protected Void doInBackground(Location... locations) {
            double start = System.currentTimeMillis();
            Location loc = locations[0];
            try {
                Socket s = new Socket(mNetAddress, 1337);
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
                String outMsg = loc.getAltitude() + ";" + loc.getLatitude() + ";" + loc.getLongitude() + ";" + loc.getSpeed();
                double startInner = System.currentTimeMillis();
                out.write(outMsg);
                out.flush();
                Log.i(TAG, "Elapsed Inner: " + (System.currentTimeMillis() - startInner) + "us");
                s.close();
                Log.w(TAG, "TCP Sent Location");
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.i(TAG, "Elapsed: " + (System.currentTimeMillis() - start) + "us");
            return null;
        }
    }
}
