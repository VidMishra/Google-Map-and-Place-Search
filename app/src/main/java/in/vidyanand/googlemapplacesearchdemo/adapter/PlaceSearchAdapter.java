package in.vidyanand.googlemapplacesearchdemo.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import in.vidyanand.googlemapplacesearchdemo.R;

/**
 * Created by vidyanandmishra on 31/03/17.
 */

public class PlaceSearchAdapter extends ArrayAdapter<PlaceSearchAdapter.SearchedPlacesAutocomplete> implements Filterable {

    private Context mContext;
    private ArrayList<SearchedPlacesAutocomplete> mListPlaces;
    private GoogleApiClient mGoogleApiClient;
    private LatLngBounds mBounds;
    private AutocompleteFilter mPlaceFilter;

    public PlaceSearchAdapter(Context context, int resource, GoogleApiClient googleApiClient, LatLngBounds bounds, AutocompleteFilter filter) {

        super(context, resource);

        mListPlaces = new ArrayList<>();
        mContext = context;
        mGoogleApiClient = googleApiClient;
        mBounds = bounds;
        mPlaceFilter = filter;
    }

    @Override
    public int getCount() {
        return mListPlaces.size();
    }

    @Override
    public SearchedPlacesAutocomplete getItem(int position) {
        return mListPlaces.get(position);
    }

    @Override
    public Filter getFilter() {

        Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                FilterResults results = new FilterResults();

                if (constraint != null) {

                    mListPlaces = getAutocomplete(constraint);
                    if (mListPlaces != null) {

                        results.values = mListPlaces;
                        results.count = mListPlaces.size();
                    } else {
                        mListPlaces = new ArrayList<>();
                    }
                }
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {

                if (results != null && results.count > 0) {

                    ((Activity) mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            notifyDataSetChanged();

                        }
                    });

                } else {
                    notifyDataSetInvalidated();
                }
            }
        };
        return filter;
    }

    private ArrayList<SearchedPlacesAutocomplete> getAutocomplete(CharSequence constraint) {

        if (mGoogleApiClient.isConnected()) {

            PendingResult<AutocompletePredictionBuffer> results = Places.GeoDataApi
                    .getAutocompletePredictions(mGoogleApiClient, constraint.toString(),
                            mBounds, mPlaceFilter);

            AutocompletePredictionBuffer autocompletePredictions = results.await(60, TimeUnit.SECONDS);

            final Status status = autocompletePredictions.getStatus();
            if (!status.isSuccess()) {
                autocompletePredictions.release();
                return null;
            }

            Iterator<AutocompletePrediction> iterator = autocompletePredictions.iterator();
            ArrayList resultList = new ArrayList<>(autocompletePredictions.getCount());

            while (iterator.hasNext()) {
                AutocompletePrediction prediction = iterator.next();

                resultList.add(new SearchedPlacesAutocomplete(prediction.getPlaceId(), prediction.getDescription()));
            }

            autocompletePredictions.release();

            return resultList;
        }

        return null;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView tvPlaceDescription;
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.item_search_list, null);
        }
        final View view = convertView;
        tvPlaceDescription = (TextView) convertView.findViewById(R.id.txt_location);
        tvPlaceDescription.setText(mListPlaces.get(position).description);
        return convertView;
    }

    /**
     * Holder for Searched Places results.
     */
    public class SearchedPlacesAutocomplete {

        public CharSequence placeId;
        public CharSequence description;

        SearchedPlacesAutocomplete(CharSequence placeId, CharSequence description) {
            this.placeId = placeId;
            this.description = description;
        }

        @Override
        public String toString() {
            return description.toString();
        }
    }
}
