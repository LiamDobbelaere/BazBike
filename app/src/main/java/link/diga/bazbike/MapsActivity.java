package link.diga.bazbike;

import android.Manifest;
import android.arch.persistence.room.Room;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toolbar;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import link.diga.bazbike.persistence.BazBikeDatabase;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private final String TAG = "MapScreen";

    private GoogleMap mMap;

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;

    private BroadcastReceiver mLocationUpdateReceiver;

    private Marker mCurrentLocationMarker;
    private Marker mAddLocationMarker;

    private boolean mEnableAddLocation;

    private BazBikeDatabase bazBikeDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setActionBar((Toolbar) findViewById(R.id.toolbar) );

        /*bazBikeDatabase = Room
                .databaseBuilder(getApplicationContext(), BazBikeDatabase.class, "bazbikedb")
                .ena
                .build();*/

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
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
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
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(mLocationUpdateReceiver);

        super.onPause();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

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
                                    String name = txtLocationName.getText().toString();

                                    if (mAddLocationMarker != null) {
                                        mAddLocationMarker.remove();
                                    }

                                    Log.i(TAG, "Add location happens now for " + name);
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
