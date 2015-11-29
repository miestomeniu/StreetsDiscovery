package com.example.test.cityexplorer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapsActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, OnMapReadyCallback, LocationListener {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private PolylineOptions mPolylineOptions;
    LocationManager locationManager;
    ArrayList<Double> latitudes = new ArrayList<Double>();
    ArrayList<Double> longitudes = new ArrayList<Double>();
    ArrayList<Integer> points = new ArrayList<Integer>();
    private LatLng mLatLng;
    private boolean mRequestingLocationUpdates = false;
    double mLatitude = 54.5567776;
    double mLongitude = 23.3521873;
    private GoogleApiClient mGoogleApiClient;
    double latitude, longitude;
    private Location mLastLocation;
    private int taskai;
    private SharedPreferences sp;
    private LocationRequest mLocationRequest;
    private static int UPDATE_INTERVAL = 1000; // 1 sec
    private static int FATEST_INTERVAL = 5000; // 5 sec
    private static int DISPLACEMENT = 1; // 1 meters

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
       // setUpMapIfNeeded();
        //  LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //this.locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        latitudes.add(54.7246625); latitudes.add(54.724867); latitudes.add(54.7243527); latitudes.add(54.7234972); latitudes.add(54.7234943);
        longitudes.add(25.3413606); longitudes.add(25.3409542); longitudes.add(25.3414746); longitudes.add(25.3374738); longitudes.add(25.3367553);
        points.add(1); points.add(3); points.add(6); points.add(2); points.add(7);
        sp = getSharedPreferences("My_prefs", Context.MODE_PRIVATE);
        taskai = sp.getInt("points", 0);
        setUpMapIfNeeded();
        initializeMap();
        for(int i = 0; i < 5; i++)
            mMap.addMarker(new MarkerOptions().position(new LatLng(latitudes.get(i), longitudes.get(i))));

        buildGoogleApiClient();

        createLocationRequest();
//        togglePeriodicLocationUpdates();
        //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Resuming the periodic location updates
        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void displayLocation() {

        mLastLocation = LocationServices.FusedLocationApi
                .getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            double latitude = mLastLocation.getLatitude();
            double longitude = mLastLocation.getLongitude();
            mLatLng = new LatLng(latitude, longitude);
            for(int i = 0; i < 5; i++){
                double distance = CalculationByDistance(mLatLng, new LatLng(latitudes.get(i), longitudes.get(i)));
                if (distance < 5) {
                    send_alert(i);
                }
            }
            SharedPreferences.Editor editor = sp.edit();
            editor.putInt("points", taskai);
            Toast.makeText(getApplicationContext(), "Display: " + latitude + ", " + longitude, Toast.LENGTH_LONG).show();
            updatePolyline();
                    updateCamera();
                   // updateMarker();
        } else {
            Toast.makeText(getApplicationContext(), "Couldn't get the location. Make sure location is enabled on the device", Toast.LENGTH_LONG).show();

        }
    }

    public void send_alert(final int j){
        AlertDialog.Builder dialog = new AlertDialog.Builder(MapsActivity.this);
        dialog
                .setTitle("Šaunuolis! Pasiekei!")
                .setPositiveButton("Gerai", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        taskai += points.get(j);
                        Toast.makeText(getApplicationContext(), "Taškų skaičius: " + taskai, Toast.LENGTH_LONG).show();
                    }
                });
        AlertDialog alert_dialog = dialog.create();
        alert_dialog.show();
    }

    /*public void onLocationChanged(Location location) {
        longitude = location.getLongitude();
        latitude = location.getLatitude();
        mLatLng = new LatLng(latitude, longitude);
        Toast.makeText(getApplicationContext(), "Taskas: " + latitude + " ir " + longitude, Toast.LENGTH_SHORT).show();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updatePolyline();
                updateCamera();
                updateMarker();
            }
        });
    }*/

    @Override
    public void onMapReady(GoogleMap map) {
        //mMap = map;

    }

    private void updatePolyline() {
     //   mMap.clear();
       // mMap.addPolyline(new PolylineOptions()
//                .add(mLatLng, new LatLng(latitude, longitude))
//                .width(5)
//                .color(Color.RED));
        mMap.addPolyline(mPolylineOptions.add(mLatLng));
    }

    private void updateMarker() {
        mMap.addMarker(new MarkerOptions().position(mLatLng));
    }

    private void updateCamera() {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mLatLng, 16));
    }

    private void initializeMap() {
        mPolylineOptions = new PolylineOptions();
        mPolylineOptions.color(Color.BLUE).width(10);
    }

 /*   @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }
*/
    public double CalculationByDistance(LatLng StartP, LatLng EndP) {
        int Radius = 6371;// radius of earth in Km
        double lat1 = StartP.latitude;
        double lat2 = EndP.latitude;
        double lon1 = StartP.longitude;
        double lon2 = EndP.longitude;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        double valueResult = Radius * c;
        double km = valueResult / 1;
        DecimalFormat newFormat = new DecimalFormat("####");
        int kmInDec = Integer.valueOf(newFormat.format(km));
        double meter = valueResult % 1000;
        int meterInDec = Integer.valueOf(newFormat.format(meter));
        Log.i("Radius Value", "" + valueResult + "   KM  " + kmInDec
                + " Meter   " + meterInDec);

        return 1000 * Radius * c;
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();

            mMap.setMyLocationEnabled(true);
            mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                @Override
                public boolean onMyLocationButtonClick() {
                    // Try to obtain the map from the SupportMapFragment.
                    LocationManager lm = null;
                    boolean gps_enabled = false, network_enabled = false;
                    if (lm == null)
                        lm = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
                    try {
                        gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
                    } catch (Exception ex) {
                    }
                    try {
                        network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                    } catch (Exception ex) {
                    }
                    if (!gps_enabled && !network_enabled) {
                        AlertDialog.Builder dialog = new AlertDialog.Builder(MapsActivity.this);
                        dialog
                                .setTitle("Nėra gps")
                                .setPositiveButton("Atšaukti", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {

                                    }
                                })
                                .setNegativeButton("Atidaryti nustatymus", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                        MapsActivity.this.startActivity(myIntent);
                                    }
                                });
                        AlertDialog alert_dialog = dialog.create();
                        alert_dialog.show();

                    }
                    togglePeriodicLocationUpdates();
                    return false;
                }
            });
        }
    }

    private void togglePeriodicLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            // Changing the button text
      //      btnStartLocationUpdates
        ///            .setText(getString(R.string.btn_stop_location_updates));

            mRequestingLocationUpdates = true;

            // Starting the location updates
            startLocationUpdates();

           // Log.d(TAG, "Periodic location updates started!");

        } else {
            // Changing the button text
           // btnStartLocationUpdates
              //      .setText(getString(R.string.btn_start_location_updates));

            mRequestingLocationUpdates = false;

            // Stopping the location updates
            stopLocationUpdates();

            //Log.d(TAG, "Periodic location updates stopped!");
        }
    }

    protected void startLocationUpdates() {
       // setUpMapIfNeeded();
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);

    }

    /**
     * Stopping location updates
     */
    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    /**
     * Creating google api client object
     * */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    /**
     * Creating location request object
     * */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.d("connected", "fail");
      //  Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
        //        + result.getErrorCode());
    }

    @Override
    public void onConnected(Bundle arg0) {

        // Once connected with google api, get the location
        displayLocation();
        Log.d("connected!", "jdjd");
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        Log.d("connection suspended!", "jdjd");

        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        // Assign the new location
        mLastLocation = location;

        Toast.makeText(getApplicationContext(), "Location changed!",
                Toast.LENGTH_SHORT).show();

        // Displaying the new location on UI
        displayLocation();
    }
}