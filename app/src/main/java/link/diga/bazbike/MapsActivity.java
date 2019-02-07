package link.diga.bazbike;

import android.Manifest;
import androidx.room.Room;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toolbar;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import link.diga.bazbike.persistence.BazBikeDatabase;
import link.diga.bazbike.persistence.LocationGoal;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private final String TAG = "MapScreen";

    private GoogleMap mMap;

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;

    private BroadcastReceiver mLocationUpdateReceiver;
    private BroadcastReceiver mScoreUpdateReceiver;

    private TextView tvScore;

    private Marker mCurrentLocationMarker;
    private Marker mAddLocationMarker;
    private List<Marker> mLocationGoalMarkers;

    private boolean mEnableAddLocation;

    private BazBikeDatabase bazBikeDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setActionBar((Toolbar) findViewById(R.id.toolbar) );

        mLocationGoalMarkers = new ArrayList<>();
        tvScore = findViewById(R.id.score);

        bazBikeDatabase = Room
                .databaseBuilder(getApplicationContext(), BazBikeDatabase.class, "bazbikedb")
                .enableMultiInstanceInvalidation()
                .build();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (!LocationHelper.checkLocationPermissions(this)) requestPermissions();

        mLocationUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                double lat = intent.getDoubleExtra("lat", 0);
                double lng = intent.getDoubleExtra("lng", 0);

                if (mMap != null && mCurrentLocationMarker != null) {
                    mCurrentLocationMarker.setPosition(new LatLng(lat, lng));
                    if (!mEnableAddLocation) mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mCurrentLocationMarker.getPosition(), 18f));
                }

                Log.i(TAG, "Received location update! " + Double.toString(lat) + ", " + Double.toString(lng));
            }
        };

        mScoreUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                tvScore.setText(String.valueOf(intent.getIntExtra("score", 0)) + " pts");
            }
        };

        Intent intent = new Intent(this, GameLocationService.class);
        ContextCompat.startForegroundService(this, intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.maps_menu, menu);
        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_addlocation:
                mEnableAddLocation = !mEnableAddLocation;

                if (mEnableAddLocation) {
                    findViewById(R.id.action_addlocation).setBackgroundColor(getColor(R.color.colorPrimaryDark));
                } else {
                    if (mAddLocationMarker != null) {
                        mAddLocationMarker.remove();
                    }

                    findViewById(R.id.action_addlocation).setBackgroundColor(getColor(android.R.color.transparent));
                }

                return true;
        }

        return false;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.VIBRATE, Manifest.permission.ACCESS_COARSE_LOCATION},
                REQUEST_PERMISSIONS_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Permission granted.");
            } else {
                //Permission denied, do sth here
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mLocationUpdateReceiver,
                        new IntentFilter("location-update"));

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mScoreUpdateReceiver,
                        new IntentFilter("score-update"));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(mLocationUpdateReceiver);

        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(mScoreUpdateReceiver);

        super.onPause();
    }

    private void reloadLocationGoals() {
        mLocationGoalMarkers.forEach(new Consumer<Marker>() {
            @Override
            public void accept(Marker marker) {
                marker.remove();
            }
        });
        mLocationGoalMarkers.clear();

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                final List<LocationGoal> locationGoals = bazBikeDatabase.locationGoalDao().getAll();

                locationGoals.forEach(new Consumer<LocationGoal>() {
                    @Override
                    public void accept(LocationGoal locationGoal) {
                        final MarkerOptions mo = new MarkerOptions();
                        mo.position(new LatLng(locationGoal.lat, locationGoal.lng));
                        mo.draggable(false);
                        mo.title(locationGoal.locationName);
                        mo.icon(BitmapDescriptorFactory.defaultMarker(125f));

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mLocationGoalMarkers.add(mMap.addMarker(mo));
                            }
                        });
                    }
                });
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        reloadLocationGoals();

        MarkerOptions mo = new MarkerOptions();
        mo.position(new LatLng(0, 0));
        mo.draggable(false);
        mo.icon(BitmapDescriptorFactory.defaultMarker(200f));

        mCurrentLocationMarker = mMap.addMarker(mo);

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (mEnableAddLocation) {
                    if (mAddLocationMarker != null) {
                        mAddLocationMarker.remove();
                    }

                    MarkerOptions mo = new MarkerOptions();
                    mo.position(latLng);
                    mo.draggable(false);
                    mo.title(getString(R.string.new_location));
                    mo.icon(BitmapDescriptorFactory.defaultMarker(300f));

                    mAddLocationMarker = mMap.addMarker(mo);
                    mAddLocationMarker.showInfoWindow();
                }
            }
        });

        final Context self = this;
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                if (marker.equals(mAddLocationMarker)) {
                    final EditText txtLocationName = new EditText(self);

                    txtLocationName.setHint("Coffee shop");

                    new AlertDialog.Builder(self)
                            .setTitle("Add location")
                            .setMessage("Enter a name for this new location")
                            .setView(txtLocationName)
                            .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {

                                    final String name = txtLocationName.getText().toString();
                                    final double lat = mAddLocationMarker.getPosition().latitude;
                                    final double lng = mAddLocationMarker.getPosition().longitude;
                                    AsyncTask.execute(new Runnable() {
                                        @Override
                                        public void run() {
                                            LocationGoal locationGoal = new LocationGoal();
                                            locationGoal.locationName = name;
                                            locationGoal.lat = lat;
                                            locationGoal.lng = lng;
                                            bazBikeDatabase.locationGoalDao().insertAll(locationGoal);
                                        }
                                    });

                                    if (mAddLocationMarker != null) {
                                        mAddLocationMarker.remove();
                                    }

                                    reloadLocationGoals();
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {


                                }
                            })
                            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    if (mAddLocationMarker != null) {
                                        mAddLocationMarker.remove();
                                    }
                                }
                            })
                            .show();
                }
            }
        });
    }
}
