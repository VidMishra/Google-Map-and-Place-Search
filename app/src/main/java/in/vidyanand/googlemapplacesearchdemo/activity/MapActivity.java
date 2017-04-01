package in.vidyanand.googlemapplacesearchdemo.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import in.vidyanand.googlemapplacesearchdemo.R;
import in.vidyanand.googlemapplacesearchdemo.adapter.PlaceSearchAdapter;
import in.vidyanand.googlemapplacesearchdemo.services.ReverseGeoIntentService;
import in.vidyanand.googlemapplacesearchdemo.utils.Constants;
import in.vidyanand.googlemapplacesearchdemo.utils.Utilities;

/**
 * Created by vidyanandmishra on 01/04/17.
 */

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMapClickListener, GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnCameraChangeListener {


    private static final String TAG = "MapActivity";

    private TextView txtBanner;
    private AutoCompleteTextView txtAutoSearch;
    private TextView txtAddress;

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LatLng mLatLong;
    private PlaceSearchAdapter mPlaceSearchAdapter;
    private LocationRequest mLocationRequest;
    private SupportMapFragment mapFragment;

    private ReverseGeoReceiver mResultReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_map);

        Utilities.checkAndRequestPermissions(this);

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, 0, this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .build();

        txtBanner = (TextView) findViewById(R.id.txt_baner);
        txtAddress = (TextView) findViewById(R.id.txt_address);
        txtAutoSearch = (AutoCompleteTextView) findViewById(R.id.txt_search);
        final ImageView imgClear = (ImageView) findViewById(R.id.img_clear);

        imgClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                txtAutoSearch.setText("");
            }
        });

        txtAutoSearch.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.length() != 0) {
                    imgClear.setVisibility(View.VISIBLE);
                } else {
                    imgClear.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        mPlaceSearchAdapter = new PlaceSearchAdapter(this, R.layout.item_search_list, mGoogleApiClient, null, null);
        txtAutoSearch.setAdapter(mPlaceSearchAdapter);
        txtAutoSearch.setOnItemClickListener(mItemSearchListOnClick);

        mapFragment.getMapAsync(this);
        mResultReceiver = new ReverseGeoReceiver(new Handler());

        checkLocationService();
    }

    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback = new ResultCallback<PlaceBuffer>() {

        @Override
        public void onResult(PlaceBuffer places) {

            if (!places.getStatus().isSuccess()) {
                places.release();
                return;
            }

            final Place place = places.get(0);
            Utilities.hideKeyboard(MapActivity.this);
            LatLng latlng = new LatLng(place.getLatLng().latitude, place.getLatLng().longitude);

            CameraPosition cameraPosition = new CameraPosition.Builder().target(latlng).zoom(19f).build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    };

    private AdapterView.OnItemClickListener mItemSearchListOnClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            final PlaceSearchAdapter.SearchedPlacesAutocomplete item = mPlaceSearchAdapter.getItem(position);
            final String placeId = String.valueOf(item.placeId);

            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
        }
    };

    private void checkLocationService() {

        if (isPlayServicesAvailable()) {
            if (!Utilities.isLocationEnabled(this)) {

                AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                dialog.setMessage("LOCATION IS NOT ENABLE!");
                dialog.setPositiveButton("ENABLE LOCATION FROM SETTINGS!", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(myIntent);
                    }
                });

                dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        // TODO Auto-generated method stub
                    }
                });
                dialog.show();
            }

        } else {
            Toast.makeText(this, "YOUR DEVICE DOES NOT SUPPORT LOCATION SERVICES!", Toast.LENGTH_SHORT).show();
        }
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(Constants.UPDATE_INTERVAL_IN_MILLI);
        mLocationRequest.setFastestInterval(Constants.FASTEST_UPDATE_INTERVAL_IN_MILLI);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapClickListener(this);
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnCameraChangeListener(this);

        setMyLocationButtonPosition();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Utilities.checkAndRequestPermissions(this);
            return;
        }

        mMap.setMyLocationEnabled(true);

        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {

            @Override
            public void onCameraChange(CameraPosition cameraPosition) {

                mLatLong = cameraPosition.target;
                mMap.clear();

                try {

                    Location mLocation = new Location("");
                    mLocation.setLatitude(mLatLong.latitude);
                    mLocation.setLongitude(mLatLong.longitude);
                    callReverseGeoService(mLocation);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        if (!Utilities.checkAndRequestPermissions(this)) {
            return;
        }
    }

    @Override
    public void onConnected(Bundle bundle) {

        Log.e(TAG, "onConnected: ");

        if (!Utilities.checkAndRequestPermissions(this)) {
            return;
        }

        createLocationRequest();
        requestLocationUpdate();
    }

    private void setMyLocationButtonPosition() {

        View mapView = mapFragment.getView();
        View locationButton = ((View) mapView.findViewById(1).getParent()).findViewById(2);
        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();

        rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        rlp.setMargins(0, 0, 50, 50);
    }

    public void requestLocationUpdate() {

        if (mGoogleApiClient.isConnected()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Utilities.checkAndRequestPermissions(this);
                return;
            }
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {

        Log.e(TAG, "onLocationChanged: ");

        try {
            if (location != null)
                loadMap(location);
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "onConnectionFailed: ");
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            mGoogleApiClient.connect();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {

        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    private boolean isPlayServicesAvailable() {

        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, Constants.PLAY_SERVICES_REQ_KEY).show();
            } else {
                //TODO: Do something
            }
            return false;
        }
        return true;
    }

    private void loadMap(Location location) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Utilities.checkAndRequestPermissions(this);
            return;
        }

        if (mMap != null) {

            mMap.getUiSettings().setZoomControlsEnabled(false);

            LatLng latLong = new LatLng(location.getLatitude(), location.getLongitude());
            CameraPosition cameraPosition = new CameraPosition.Builder().target(latLong).zoom(19f).build();

            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

            callReverseGeoService(location);
        } else {
            Toast.makeText(getApplicationContext(), "UNABLE TO LOAD MAP!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

    @Override
    public void onMapClick(LatLng latLng) {

    }

    @Override
    public boolean onMyLocationButtonClick() {
        return false;
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {

    }

    class ReverseGeoReceiver extends ResultReceiver {

        public ReverseGeoReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            Log.i(TAG, "onReceiveResult: " + resultCode);

            txtBanner.setVisibility(View.VISIBLE);

            if (resultCode == Constants.SUCCESS_RESULT_KEY && resultData != null) {

                String address = resultData.getString(Constants.SEARCH_RESULT_DATA_KEY);
                String area = resultData.getString(Constants.GEO_AREA_BUNDLE_KEY);
                String city = resultData.getString(Constants.GEO_CITY_BUNDLE_KEY);
                String state = resultData.getString(Constants.GEO_STREET_BUNDLE_KEY);

                Log.e(TAG, "onReceiveResult: " + address + " " + area + " " + city + " " + state);

                if (address == null && area == null && city == null && state == null) {
                    txtAddress.setText("Address Not Found");
                    txtBanner.setText("Address Not Found");
                } else {
                    txtBanner.setText("Pick This Location");
                    txtAddress.setText(address);
                }
            }
        }
    }

    private void callReverseGeoService(Location mLocation) {

        txtBanner.setVisibility(View.GONE);

        Intent intent = new Intent(this, ReverseGeoIntentService.class);
        intent.putExtra(Constants.REGISTER_RECEIVER_KEY, mResultReceiver);
        intent.putExtra(Constants.REV_GEO_DATA_BUNDLE_KEY, mLocation);
        startService(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == Constants.LOCATION_PERM_KEY) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission Granted.");
            } else {
                Log.d(TAG, "Permission Denied.");
            }

            createLocationRequest();
            requestLocationUpdate();
        }
    }

}
