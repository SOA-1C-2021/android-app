package com.soa.app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.mikhaellopez.circularprogressbar.CircularProgressBar;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // ui
    private TextView textProgressValue = null;
    private TextView textGoalValue = null;
    private TextView textPercentageValue = null;
    CircularProgressBar circularProgressBar = null;
    private final float PROGRESS_BAR_WIDTH = 40;

    // shared preferences
    SharedPreferences historySharedPreferences = null;
    SharedPreferences settingsSharedPreferences = null;

    // sensor
    private SensorManager sensorManager = null;
    private boolean sensorActive = false;

    // step management
    private int dailySteps = 0;
    private int dailyGoal = 0;
    private int totalStepsSinceReboot = 0;
    private int loadedSteps = 0;
    private final int DEFAULT_DAILY_GOAL = 500;
    private boolean counterInitialized = false;

    // other
    SimpleDateFormat simpleDateFormat = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // reference ui components
        circularProgressBar = findViewById(R.id.circular_progress_bar);
        textProgressValue = findViewById(R.id.text_progress_value);
        textGoalValue = findViewById(R.id.text_goal_value);
        textPercentageValue = findViewById(R.id.text_percentage_value);

        // instantiate sensor manager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // instantiate date formatter
        simpleDateFormat = new SimpleDateFormat("yyyyMMdd");

        // instantiate shared preferences files
        historySharedPreferences = getSharedPreferences(getString(R.string.shared_preferences_history), Context.MODE_PRIVATE);
        settingsSharedPreferences = getSharedPreferences(getString(R.string.shared_preferences_settings), Context.MODE_PRIVATE);

        // add listeners
        settingsSharedPreferences.registerOnSharedPreferenceChangeListener(settingsSharedPreferencesListener);

        // perform initialization actions
        loadDailyGoal();
        loadSteps();
        initializeUi();

    }

    private SharedPreferences.OnSharedPreferenceChangeListener settingsSharedPreferencesListener = new SharedPreferences.OnSharedPreferenceChangeListener()
    {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (key.equals(getString(R.string.settings_step_goal_key))) {
                int goal = prefs.getInt(getString(R.string.settings_step_goal_key), 100);
                textGoalValue.setText(Integer.toString(goal));
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        Sensor stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if (stepSensor == null) {
            Toast.makeText(MainActivity.this, "El dispositivo no posee el sensor necesario para la aplicación", Toast.LENGTH_LONG).show();
        } else {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
        }

        // perform initialization actions
        loadDailyGoal();
        loadSteps();
        initializeUi();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSteps();
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveSteps();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveSteps();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        int stepSensorValue = (int) sensorEvent.values[0];
        Log.d(getClass().getName(), "stepSensorValue: " + Integer.toString(stepSensorValue));

        // if the activity has just been created, store the current sensor count in memory
        // to subtract it from subsequent reads (since its value is relative to the last phone reboot)
        if (!counterInitialized) {
            // initialize counter
            totalStepsSinceReboot = stepSensorValue;
            Log.d(getClass().getName(), "totalStepsSinceReboot: " + Integer.toString(totalStepsSinceReboot));
            dailySteps = loadedSteps;
            counterInitialized = true;
        } else {
            // update counter
            dailySteps = stepSensorValue - totalStepsSinceReboot;
            Log.d(getClass().getName(), "dailySteps: " + Integer.toString(dailySteps));
        }

        // update ui
        if (dailySteps >= dailyGoal) circularProgressBar.setProgressBarColor(Color.GREEN);
        circularProgressBar.setProgressWithAnimation(dailySteps);
        textProgressValue.setText(Integer.toString(dailySteps));
        textPercentageValue.setText(getPercentage(dailySteps, dailyGoal) + " %");

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        int id = item.getItemId();
        if (id == R.id.history) {
            showHistory();
            return true;
        } else if (id == R.id.settings) {
            showSettings();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void showHistory() {
        Intent historyActivityIntent = new Intent(MainActivity.this, HistoryActivity.class);
        startActivity(historyActivityIntent);
    }

    private void showSettings() {
        Intent settingsActivityIntent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivity(settingsActivityIntent);
    }

    private void saveSteps() {
        SharedPreferences.Editor editor = historySharedPreferences.edit();

        // get current date in the format "YYYYMMDD" and use it as key
        Date currentTime = Calendar.getInstance().getTime();
        String formattedDate = simpleDateFormat.format(currentTime);

        editor.putInt(formattedDate, dailySteps);
        editor.apply();

        Log.i(getClass().getName(), "Saved steps: " + Integer.toString(dailySteps));
    }

    private void loadDailyGoal() {
        // get current date in the format "YYYYMMDD" and use it as key
        Date currentTime = Calendar.getInstance().getTime();
        String formattedDate = simpleDateFormat.format(currentTime);

        // retrieve daily goal
        dailyGoal = settingsSharedPreferences.getInt(formattedDate, DEFAULT_DAILY_GOAL);
        Log.i(getClass().getName(), "Loaded daily goal: " + Integer.toString(dailyGoal));

    }

    private void loadSteps() {
        // get current date in the format "YYYYMMDD" and use it as key
        Date currentTime = Calendar.getInstance().getTime();
        String formattedDate = simpleDateFormat.format(currentTime);

        // retrieve daily steps
        loadedSteps = historySharedPreferences.getInt(formattedDate, 0);
        Log.i(getClass().getName(), "Loaded steps: " + Integer.toString(loadedSteps));

        if (dailySteps >= dailyGoal) circularProgressBar.setProgressBarColor(Color.GREEN);

    }

    private void initializeUi() {
        textGoalValue.setText(Integer.toString(dailyGoal));
        textProgressValue.setText(Integer.toString(dailySteps));
        textPercentageValue.setText(getPercentage(dailySteps, dailyGoal) + " %");
        circularProgressBar.setRoundBorder(true);
        circularProgressBar.setProgressBarWidth(PROGRESS_BAR_WIDTH);
        circularProgressBar.setBackgroundProgressBarWidth(PROGRESS_BAR_WIDTH);
        circularProgressBar.setBackgroundProgressBarColor(R.color.purple_500);
        circularProgressBar.setProgressBarColor(R.color.purple_700);
        circularProgressBar.setProgressMax(dailyGoal);
    }

    private String getPercentage(int dailySteps, int dailyGoal) {
        float result = (((float) dailySteps) / dailyGoal) * 100f;
        int percentage = Math.round(result);
        return Integer.toString(percentage);
    }

}