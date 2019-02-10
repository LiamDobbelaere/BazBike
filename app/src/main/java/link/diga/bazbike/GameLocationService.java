package link.diga.bazbike;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.IBinder;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.room.Room;
import link.diga.bazbike.persistence.BazBikeDatabase;
import link.diga.bazbike.persistence.LocationGoal;

import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.Calendar;
import java.util.function.Consumer;

public class GameLocationService extends Service {
    public static final String ACTION_NO_TRACKING = "ACTION_NO_TRACKING";
    public static String TAG = GameLocationService.class.getSimpleName();

    private final String CHANNEL_ID = "TRACKER";
    private final String CHANNEL_DESC = "BazBike notifications";
    private final int NOTIFICATION_ID = 1;
    private final int STOP_TRACKING_REQUEST = 10;

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;

    private BazBikeDatabase bazBikeDatabase;

    private MediaPlayer mp;

    private int score;
    private LocationGoal mLastVisitedLocation;

    private double distanceTemp;
    private Location lastLocation;

    public GameLocationService() {
        super();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_DESC,
                NotificationManager.IMPORTANCE_LOW);

        channel.setSound(null, null);

        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

        startForeground(NOTIFICATION_ID, getNotification().setContentText("Starting...").build());

        return START_STICKY;
    }

    private NotificationCompat.Builder getNotification() {
        Intent notificationIntent = new Intent(this, MapsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Receiving location updates")
                .setColorized(true)
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .setSmallIcon(R.drawable.ic_stat_tracker)
                //.addAction(R.drawable.ic_stop_tracking, getString(R.string.stop_tracking), stopTrackingIntent)
                .setContentIntent(pendingIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);

        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        mLocationCallback = null;

        this.stopSelf();
    }

    private void broadcastLocationUpdate(double lat, double lng) {
        Intent intent = new Intent("location-update");
        intent.putExtra("lat", lat);
        intent.putExtra("lng", lng);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastScoreUpdate() {
        score += 1;

        Intent intent = new Intent("score-update");
        intent.putExtra("score", score);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (bazBikeDatabase == null) {
            bazBikeDatabase = Room
                    .databaseBuilder(getApplicationContext(), BazBikeDatabase.class, "bazbikedb")
                    .enableMultiInstanceInvalidation()
                    .build();
        }

        final Context self = this;
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                final Location location = locationResult.getLastLocation();

                Notification notification = getNotification()
                        .setShowWhen(true)
                        .setWhen(Calendar.getInstance().getTimeInMillis())
                        .setContentText(getString(R.string.notification_tracking)).build();

                NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.notify(NOTIFICATION_ID, notification);

                if (lastLocation != null) {
                    distanceTemp += lastLocation.distanceTo(location);
                }

                lastLocation = location;

                Log.i(TAG, "Location result: " + Double.toString(location.getLatitude()));

                AsyncTask.execute(() -> bazBikeDatabase.locationGoalDao().getAll().forEach(locationGoal -> {
                    Location locGoal = new Location("");
                    locGoal.setLatitude(locationGoal.lat);
                    locGoal.setLongitude(locationGoal.lng);

                    if (locGoal.distanceTo(location) <= 25f) {
                        if (mLastVisitedLocation != null && mLastVisitedLocation.equals(locationGoal)) return;

                        SharedPreferences sp = getSharedPreferences(getString(R.string.savedata_prefs), MODE_PRIVATE);
                        float distance = sp.getFloat("savedDistance", 0f);
                        distance += distanceTemp;
                        float xp = sp.getFloat("experience", 0f);
                        xp += distanceTemp / 2f;
                        sp.edit().putFloat("savedDistance", distance).putFloat("experience", xp).apply();

                        distanceTemp = 0f;

                        broadcastScoreUpdate();

                        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                        v.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE));

                        mLastVisitedLocation = locationGoal;
                    }
                }));

                broadcastLocationUpdate(location.getLatitude(), location.getLongitude());
            }
        };

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(6000);
        locationRequest.setFastestInterval(3000);

        LocationRequest locationRequestLow = LocationRequest.create();
        locationRequestLow.setPriority(LocationRequest.PRIORITY_LOW_POWER);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, null);
        } else {
            Log.d(TAG, "permission denied");
        }
    }
}