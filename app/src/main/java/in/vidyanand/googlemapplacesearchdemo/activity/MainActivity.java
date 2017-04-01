package in.vidyanand.googlemapplacesearchdemo.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import in.vidyanand.googlemapplacesearchdemo.R;
import in.vidyanand.googlemapplacesearchdemo.utils.Constants;
import in.vidyanand.googlemapplacesearchdemo.utils.Utilities;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Utilities.checkAndRequestPermissions(this);

        TextView txtLoadMap = (TextView) findViewById(R.id.txt_load_map);

        txtLoadMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, MapActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == Constants.LOCATION_PERM_KEY) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission Granted.");
            } else {
                Log.d(TAG, "Permission Denied.");
            }
        }
    }
}
