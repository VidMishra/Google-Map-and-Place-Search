package in.vidyanand.googlemapplacesearchdemo.services;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import in.vidyanand.googlemapplacesearchdemo.R;
import in.vidyanand.googlemapplacesearchdemo.utils.Constants;

/**
 * Created by vidyanandmishra on 01/04/17.
 */

public class ReverseGeoIntentService extends IntentService {

    private static final String TAG = "ReverseGeoIntentService";

    protected ResultReceiver mResultReceiver;

    public ReverseGeoIntentService() {
        super(TAG);
    }


    @Override
    protected void onHandleIntent(Intent intent) {

        String errorMessage = "";
        mResultReceiver = intent.getParcelableExtra(Constants.REGISTER_RECEIVER_KEY);

        if (mResultReceiver == null) {
            Log.e(TAG, "No receiver received");
            return;
        }

        Location location = intent.getParcelableExtra(Constants.REV_GEO_DATA_BUNDLE_KEY);

        if (location == null) {
            errorMessage = getString(R.string.str_no_location_data_provided);
            Log.wtf(TAG, errorMessage);
            broadcastToReceiver(Constants.FAILURE_RESULT_KEY, errorMessage, null);
            return;
        }

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses = null;

        try {
            addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

        } catch (IOException ioException) {

            errorMessage = getString(R.string.str_service_not_available);
            Log.e(TAG, errorMessage, ioException);
        } catch (IllegalArgumentException illegalArgumentException) {

            errorMessage = getString(R.string.str_invalid_lat_long_used);
        }

        if (addresses == null || addresses.size() == 0) {

            if (errorMessage.isEmpty()) {
                errorMessage = getString(R.string.str_no_address_found);
                Log.e(TAG, errorMessage);
            }
            broadcastToReceiver(Constants.FAILURE_RESULT_KEY, errorMessage, null);

        } else {
            Address address = addresses.get(0);
            ArrayList<String> addressFragments = new ArrayList<String>();

            for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                addressFragments.add(address.getAddressLine(i));
            }

            broadcastToReceiver(Constants.SUCCESS_RESULT_KEY, TextUtils.join(System.getProperty("line.separator"), addressFragments), address);
        }
    }

    /**
     * Sending result to receiver
     *
     * @param resultCode
     * @param message
     * @param address
     */
    private void broadcastToReceiver(int resultCode, String message, Address address) {

        try {
            Bundle bundle = new Bundle();

            bundle.putString(Constants.SEARCH_RESULT_DATA_KEY, message);
            bundle.putString(Constants.GEO_AREA_BUNDLE_KEY, address.getSubLocality());
            bundle.putString(Constants.GEO_CITY_BUNDLE_KEY, address.getLocality());
            bundle.putString(Constants.GEO_STREET_BUNDLE_KEY, address.getAddressLine(0));

            mResultReceiver.send(resultCode, bundle);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
