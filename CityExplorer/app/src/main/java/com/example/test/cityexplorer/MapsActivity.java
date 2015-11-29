package com.example.test.cityexplorer;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, OnMapReadyCallback, LocationListener {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private PolylineOptions mPolylineOptions;
    private ArrayList<MarkerOptions> mMarkerOptions;
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
    private static int ACTIVITY_CAMERA = 1;
    private int currentIndex;
    private boolean[] isVisited = new boolean[5];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        ActionBar actionBar = getActionBar();
        //actionBar.show();

        // setUpMapIfNeeded();
        //  LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //this.locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        latitudes.add(54.7229141); latitudes.add(54.724867); latitudes.add(54.7243527); latitudes.add(54.7234972); latitudes.add(54.7234943);
        longitudes.add(25.3374497); longitudes.add(25.3409542); longitudes.add(25.3414746); longitudes.add(25.3374738); longitudes.add(25.3367553);
        points.add(1); points.add(3); points.add(6); points.add(2); points.add(7);
        sp = getSharedPreferences("My_prefs", Context.MODE_PRIVATE);
        taskai = sp.getInt("points", 0);
        setUpMapIfNeeded();
        initializeMap();
        for(int i = 0; i < 5; i++) {
            mMap.addMarker(new MarkerOptions().position(new LatLng(latitudes.get(i), longitudes.get(i))));
        }

        buildGoogleApiClient();

        createLocationRequest();

        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        builder.setTitle("Select a mode")
                .setItems(R.array.testArray, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                    }
                });
        builder.create();
//        togglePeriodicLocationUpdates();
        //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main_actions, menu);

        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        // Take appropriate action for each action item click
        switch (item.getItemId()) {
            case R.id.action_check_updates:

                // search action
                return true;
            /*case R.id.action_location_found:
                // location found
                LocationFound();
                return true;
            case R.id.action_refresh:
                // refresh
                return true;
            case R.id.action_help:
                // help action
                return true;
            case R.id.action_check_updates:
                // check for updates action
                return true;*/
            default:
                return super.onOptionsItemSelected(item);
        }
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
                if (distance < 20 && !isVisited[i]) {
                    currentIndex = i;
                    send_alert(i);
                    isVisited[i] = true;
                    Intent camera_intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(camera_intent, ACTIVITY_CAMERA);
                }
            }
            SharedPreferences.Editor editor = sp.edit();
            editor.putInt("points", taskai);
            //Toast.makeText(getApplicationContext(), "Display: " + latitude + ", " + longitude, Toast.LENGTH_LONG).show();
            updatePolyline();
            updateCamera();
            //updateMarker();
        } else {
            //Toast.makeText(getApplicationContext(), "Couldn't get the location. Make sure location is enabled on the device", Toast.LENGTH_LONG).show();

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
                        //Toast.makeText(getApplicationContext(), "Taškų skaičius: " + taskai, Toast.LENGTH_LONG).show();
                    }
                });
        AlertDialog alert_dialog = dialog.create();
        alert_dialog.show();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((resultCode == RESULT_OK) && (requestCode == ACTIVITY_CAMERA)) {
            String[] projection = new String[]{
                    MediaStore.Images.ImageColumns._ID,
                    MediaStore.Images.ImageColumns.DATA,
                    MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
                    MediaStore.Images.ImageColumns.DATE_TAKEN,
                    MediaStore.Images.ImageColumns.MIME_TYPE
            };
            final Cursor cursor = getContentResolver()
                    .query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null,
                            null, MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");
            if (cursor.moveToFirst()) {
                String imageLocation = cursor.getString(1);
                File imageFile = new File(imageLocation);
                if (imageFile.exists()) {   // TODO: is there a better way to do this?
                    Bitmap bm = BitmapFactory.decodeFile(imageLocation);
                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(bm, 200, 200, true);
                    scaledBitmap = getRoundedCornerBitmap(scaledBitmap);
                    Matrix matrix = new Matrix();
                    matrix.postRotate(270);
                    Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap , 0, 0, scaledBitmap .getWidth(), scaledBitmap .getHeight(), matrix, true);
                    BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(rotatedBitmap);

                    //mMarkers.get(currentIndex).setIcon(icon);

                    MarkerOptions markerOptions = new MarkerOptions().position(new LatLng(latitudes.get(currentIndex), longitudes.get(currentIndex)))
                            .title("Current Location")
                            .snippet("Thinking of finding some thing...")
                            .icon(icon);

                    mMap.addMarker(markerOptions);
                }
            }
        }
    }

    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = 12;

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
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

        //Toast.makeText(getApplicationContext(), "Location changed!",
                //Toast.LENGTH_SHORT).show();

        // Displaying the new location on UI
        displayLocation();
    }
}