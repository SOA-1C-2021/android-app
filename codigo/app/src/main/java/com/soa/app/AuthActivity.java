package com.soa.app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.andrognito.patternlockview.PatternLockView;
import com.andrognito.patternlockview.listener.PatternLockViewListener;
import com.andrognito.patternlockview.utils.PatternLockUtils;
import com.andrognito.patternlockview.utils.ResourceUtils;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class AuthActivity extends AppCompatActivity {

    private TextView textWelcome = null;
    private PatternLockView patternLockView = null;
    private final String neededPattern = "678543012";
    private TextView textBatteryStatus = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        // reference ui components
        textWelcome = findViewById(R.id.text_welcome);
        patternLockView = (PatternLockView) findViewById(R.id.pattern_lock_view);
        textBatteryStatus = findViewById(R.id.text_battery_status);

        // add listeners
        patternLockView.addPatternLockListener(patternLockViewListener);

        // register battery status receiver
        registerReceiver(this.batteryStatusReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

    }

    private PatternLockViewListener patternLockViewListener = new PatternLockViewListener() {
        @Override
        public void onStarted() {
            Log.d(getClass().getName(), "Pattern drawing started");
        }

        @Override
        public void onProgress(List<PatternLockView.Dot> progressPattern) {
            Log.d(getClass().getName(), "Pattern progress: " +
                    PatternLockUtils.patternToString(patternLockView, progressPattern));
        }

        @Override
        public void onComplete(List<PatternLockView.Dot> pattern) {
            String stringPattern = PatternLockUtils.patternToString(patternLockView, pattern);
            Log.d(getClass().getName(), "Pattern complete: " + stringPattern);

            if (stringPattern.equals(neededPattern)) {
                Log.d(getClass().getName(), "Correct pattern entered");
                patternLockView.setCorrectStateColor(ResourceUtils.getColor(AuthActivity.this, R.color.green));
                Intent loginActivityIntent = new Intent(AuthActivity.this, LoginActivity.class);
                startActivity(loginActivityIntent);
            } else {
                Log.d(getClass().getName(), "Incorrect pattern entered");
                patternLockView.setCorrectStateColor(ResourceUtils.getColor(AuthActivity.this, R.color.red));
                Toast.makeText(AuthActivity.this, "Patrón incorrecto", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onCleared() {
            Log.d(getClass().getName(), "Pattern has been cleared");
        }
    };

    private BroadcastReceiver batteryStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctxt, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            float batteryPct = level * 100 / (float) scale;
            textBatteryStatus.setText("Estado de carga de la batería: " + String.valueOf(batteryPct) + "%");
        }
    };
}