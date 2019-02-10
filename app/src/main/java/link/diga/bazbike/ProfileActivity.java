package link.diga.bazbike;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.util.SharedPreferencesUtils;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.profile_title);

        float xp = getSharedPreferences(getString(R.string.savedata_prefs), MODE_PRIVATE).getFloat("experience", 0f);
        float level = XPHelper.XPToLevel(xp);
        float xpNeeded = XPHelper.LevelToXP(level + 1f);
        float xpToGo = xpNeeded - xp;
        float progress = xp/xpNeeded;

        TextView currentLevel = findViewById(R.id.currentLevel);
        TextView xpRequired = findViewById(R.id.xpRequired);
        ProgressBar xpProgress = findViewById(R.id.xpProgress);

        currentLevel.setText(String.format(getString(R.string.levelPlaceholder), (int) level));
        xpRequired.setText(String.format(getString(R.string.xpPlaceholder), (int) xp, (int) xpToGo));
        xpProgress.setProgress((int) progress, true);
    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }
}
