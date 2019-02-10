package link.diga.bazbike;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.util.SharedPreferencesUtils;

import java.text.DecimalFormat;

public class ProfileActivity extends AppCompatActivity {
    private BroadcastReceiver mScoreUpdateReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.profile_title);

        mScoreUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateProgressViews();
            }
        };

        updateProgressViews();
    }

    private void updateProgressViews() {
        SharedPreferences sp = getSharedPreferences(getString(R.string.savedata_prefs), MODE_PRIVATE);

        DecimalFormat kmFormat = new DecimalFormat("0.00");

        float xp = sp.getFloat("experience", 0f);
        String totalDistance = kmFormat.format(sp.getFloat("savedDistance", 0f) / 1000f);
        float level = XPHelper.XPToLevel(xp);
        float xpThisLevel = XPHelper.LevelToXP(level);
        float xpNeeded = XPHelper.LevelToXP(level + 1f);
        float xpToGo = xpNeeded - xp;
        float progress = (xp - xpThisLevel) / (xpNeeded - xpThisLevel) * 100f;

        TextView currentLevel = findViewById(R.id.currentLevel);
        TextView xpRequired = findViewById(R.id.xpRequired);
        ProgressBar xpProgress = findViewById(R.id.xpProgress);
        TextView distanceTravelled = findViewById(R.id.distanceTravelled);

        currentLevel.setText(String.format(getString(R.string.levelPlaceholder), (int) level));
        xpRequired.setText(String.format(getString(R.string.xpPlaceholder), (int) xp, (int) xpToGo));
        xpProgress.setProgress((int) progress, true);
        distanceTravelled.setText(String.format(getString(R.string.distanceTravelled), totalDistance));
    }


    @Override
    protected void onResume() {
        super.onResume();

        updateProgressViews();

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mScoreUpdateReceiver,
                        new IntentFilter("score-update"));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(mScoreUpdateReceiver);

        super.onPause();
    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }
}
