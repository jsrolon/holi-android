package com.holisolutions.holi;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
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

    private static final String TAG = "HoliLocator";

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


    private TextView textLatitudeText;
    private TextView textLongitudeText;
    private TextView textSpeedText;
    private TextView textAltitudeText;


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
        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(3 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1000); // 1 second, in milliseconds


        textLatitudeText = (TextView) findViewById(R.id.LatitudeText);
        textLongitudeText = (TextView) findViewById(R.id.LongitudeText);
        textAltitudeText = (TextView) findViewById(R.id.AltitudeText);
        textSpeedText = (TextView) findViewById(R.id.SpeedText);

        ToggleButton toggleTCP = (ToggleButton) findViewById(R.id.togglebuttonEnviarTCP);
        toggleTCP.setText("TCP");
        toggleTCP.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled
                    final Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            try {
                                currentLatitude = location.getLatitude();
                                currentLongitude = location.getLongitude();
                                currentAltitude = location.getAltitude();
                                currentSpeed = location.getSpeed();

                                //textLatitudeText.setText("Latitude: "+currentLatitude);
                                //textLongitudeText.setText("Longitude: "+currentLongitude);
                                //textAltitudeText.setText("Altitude: "+currentAltitude);
                                //textSpeedText.setText("Speed: "+currentSpeed);


                                Log.w("holi", currentLongitude + ""  + currentLatitude);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            handler.postDelayed(this, 1000);
                        }
                    }, 1000);
                } else {
                    // The toggle is disabled

                }
            }
        });

        ToggleButton toggleUDP = (ToggleButton) findViewById(R.id.togglebuttonEnviarUDP);
        toggleUDP.setText("UDP");
        toggleUDP.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            try {
                                Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                                currentLatitude = location.getLatitude();
                                currentLongitude = location.getLongitude();
                                currentAltitude = location.getAltitude();
                                currentSpeed = location.getSpeed();

                                textLatitudeText.setText("Latitude: "+currentLatitude);
                                textLongitudeText.setText("Longitude: "+currentLongitude);
                                textAltitudeText.setText("Altitude: "+currentAltitude);
                                textSpeedText.setText("Speed: "+currentSpeed);

                                Log.w("holi", currentLongitude + " " + currentLatitude);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            handler.postDelayed(this, 1000);
                        }
                    }, 1000);
                } else {
                    // The toggle is disabled

                }
            }
        });




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
        // TODO: Start making API requests.
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

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    private void handleNewLocation(Location location) {
        Toast.makeText(getApplicationContext(), "Latitude: " + location.toString(), Toast.LENGTH_LONG).show();
    }

    private class SendUDPTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... param) {
            String udpMsg = "hello world from UDP client " + 1337;
            DatagramSocket ds = null;
            Log.w("holi", "entra");
            try {

                ds = new DatagramSocket();
                InetAddress serverAddr = InetAddress.getByName("192.168.0.12");
                DatagramPacket dp;
                dp = new DatagramPacket(udpMsg.getBytes(), udpMsg.length(), serverAddr, 1337);
                ds.send(dp);
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
            return null;
        }
    }

    private class SendTCPTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... param) {
            try {
                Socket s = new Socket("192.168.0.12", 1337);
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
                //send output msg
                String outMsg = "TCP connecting to " + 1337 + System.getProperty("line.separator");
                out.write(outMsg);
                out.flush();
                Log.i("TcpClient", "sent: " + outMsg);
                //accept server response
                //String inMsg = in.readLine() + System.getProperty("line.separator");
                //Log.i("TcpClient", "received: " + inMsg);
                //close connection
                s.close();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private void runUdpClient() {
        new SendUDPTask().execute();
    }

    private void runTcpClient() {
        new SendTCPTask().execute();
    }
}
