package com.soa.app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.gson.JsonObject;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;
import com.soa.app.models.RefreshResponse;
import com.soa.app.services.AppExecutors;
import com.soa.app.services.NetworkConnectivity;
import com.soa.app.services.UNLaMSOAAPIService;
import com.soa.app.services.UNLaMSOAAPIServiceBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // ui
    private TextView textProgressValue = null;
    private TextView textGoalValue = null;
    private TextView textPercentageValue = null;
    CircularProgressBar circularProgressBar = null;
    private final float PROGRESS_BAR_WIDTH = 40;

    // shared preferences
    SharedPreferences historySharedPreferences = null;
    SharedPreferences.Editor historySharedPreferencesEditor = null;
    SharedPreferences settingsSharedPreferences = null;
    SharedPreferences.Editor settingsSharedPreferencesEditor = null;
    SharedPreferences tokenSharedPreferences = null;
    SharedPreferences.Editor tokenSharedPreferencesEditor = null;

    // sensor
    private SensorManager sensorManager = null;
    Sensor stepSensor = null;
    private boolean sensorActive = false;

    // step management
    private int dailySteps = 0;
    private int dailyGoal = 0;
    private int totalStepsSinceReboot = 0;
    private int loadedSteps = 0;
    private final int DEFAULT_DAILY_GOAL = 500;
    private boolean counterInitialized = false;

    // event management
    private AsyncTask eventTask;
    private NetworkConnectivity networkConnectivity;
    private boolean eventSent;
    private boolean tokenRefreshed;

    // other
    SimpleDateFormat simpleDateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // reference ui components
        circularProgressBar = findViewById(R.id.circular_progress_bar);
        textProgressValue = findViewById(R.id.text_progress_value);
        textGoalValue = findViewById(R.id.text_goal_value);
        textPercentageValue = findViewById(R.id.text_percentage_value);

        // instantiate network connectivity checker class
        networkConnectivity = new NetworkConnectivity(AppExecutors.getInstance(), this);

        // instantiate date formatter
        simpleDateFormat = new SimpleDateFormat("yyyyMMdd");

        // instantiate shared preferences files
        historySharedPreferences = getSharedPreferences(getString(R.string.shared_preferences_history), Context.MODE_PRIVATE);
        historySharedPreferencesEditor = historySharedPreferences.edit();
        settingsSharedPreferences = getSharedPreferences(getString(R.string.shared_preferences_settings), Context.MODE_PRIVATE);
        settingsSharedPreferencesEditor = settingsSharedPreferences.edit();
        tokenSharedPreferences = getSharedPreferences(getString(R.string.shared_preferences_history), Context.MODE_PRIVATE);
        tokenSharedPreferencesEditor = tokenSharedPreferences.edit();

        // add listeners
        settingsSharedPreferences.registerOnSharedPreferenceChangeListener(settingsSharedPreferencesListener);

        // perform initialization actions
        counterInitialized = false;
        loadDailyGoal();
        loadSteps();
        initializeUi();

        // instantiate sensor manager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (stepSensor == null) {
            Toast.makeText(MainActivity.this, "El dispositivo no posee el sensor necesario para la aplicación", Toast.LENGTH_LONG).show();
        }
    }

    private SharedPreferences.OnSharedPreferenceChangeListener settingsSharedPreferencesListener = (prefs, key) -> {
        // get current date in the format "YYYYMMDD" and use it as key
        Date currentTime = Calendar.getInstance().getTime();
        String formattedDate = simpleDateFormat.format(currentTime);

        if (key.equals(formattedDate)) {
            eventSent = false;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (stepSensor != null) {
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
        if (stepSensor != null) {
            sensorManager.unregisterListener(this, stepSensor);
        }
        saveSteps();
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveSteps();
    }

    @Override
    protected void onDestroy() {
        if (eventTask != null) {
            eventTask.cancel(true);
        }
        super.onDestroy();
        saveSteps();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        int stepSensorValue = (int) sensorEvent.values[0];
        Log.d(getClass().getName(), "stepSensorValue: " + stepSensorValue);

        // if the activity has just been created, store the current sensor count in memory
        // to subtract it from subsequent reads (since its value is relative to the last phone reboot)
        if (!counterInitialized) {
            // initialize counter
            totalStepsSinceReboot = stepSensorValue;
            Log.d(getClass().getName(), "totalStepsSinceReboot: " + totalStepsSinceReboot);
            dailySteps = loadedSteps;
            counterInitialized = true;
        } else {
            // update counter
            dailySteps = stepSensorValue - totalStepsSinceReboot;
            if (dailySteps >= dailyGoal && !eventSent) launchThread(); // send goal accomplished event to API
            Log.d(getClass().getName(), "dailySteps: " + dailySteps);
        }

        // update ui
        initializeUi();

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
        // get current date in the format "YYYYMMDD" and use it as key
        Date currentTime = Calendar.getInstance().getTime();
        String formattedDate = simpleDateFormat.format(currentTime);

        historySharedPreferencesEditor.putInt(formattedDate, dailySteps);
        historySharedPreferencesEditor.apply();

        Log.i(getClass().getName(), "Saved steps: " + dailySteps);
    }

    private void loadDailyGoal() {
        // get current date in the format "YYYYMMDD" and use it as key
        Date currentTime = Calendar.getInstance().getTime();
        String formattedDate = simpleDateFormat.format(currentTime);

        // retrieve daily goal, if daily goal does not exist, create entry
        if (settingsSharedPreferences.contains(formattedDate)) {
            dailyGoal = settingsSharedPreferences.getInt(formattedDate, DEFAULT_DAILY_GOAL);
            Log.i(getClass().getName(), "Loaded daily goal: " + dailyGoal);
        } else {
            settingsSharedPreferencesEditor.putInt(formattedDate, DEFAULT_DAILY_GOAL);
            settingsSharedPreferencesEditor.apply();
            Log.i(getClass().getName(), "Daily goal entry not found, created entry for today with default value");
        }

    }

    private void loadSteps() {
        // get current date in the format "YYYYMMDD" and use it as key
        Date currentTime = Calendar.getInstance().getTime();
        String formattedDate = simpleDateFormat.format(currentTime);

        // retrieve daily steps
        loadedSteps = historySharedPreferences.getInt(formattedDate, 0);
        Log.i(getClass().getName(), "Loaded steps: " + loadedSteps);

        dailySteps = loadedSteps;
    }

    private void initializeUi() {
        textGoalValue.setText(Integer.toString(dailyGoal));
        textProgressValue.setText(Integer.toString(dailySteps));
        textPercentageValue.setText(getPercentage(dailySteps, dailyGoal) + " %");

        circularProgressBar.setRoundBorder(true);
        circularProgressBar.setProgressBarWidth(PROGRESS_BAR_WIDTH);
        circularProgressBar.setBackgroundProgressBarWidth(PROGRESS_BAR_WIDTH);
        circularProgressBar.setBackgroundProgressBarColor(R.color.purple_500);

        if (dailySteps >= dailyGoal) circularProgressBar.setProgressBarColor(Color.GREEN);
        else circularProgressBar.setProgressBarColor(R.color.purple_700);

        circularProgressBar.setProgressMax(dailyGoal);
        circularProgressBar.setProgressWithAnimation(dailySteps);
    }

    private String getPercentage(int dailySteps, int dailyGoal) {
        float result = (((float) dailySteps) / dailyGoal) * 100f;
        int percentage = Math.round(result);
        return Integer.toString(percentage);
    }

    private void launchThread() {
        eventSent = true;
        networkConnectivity.checkInternetConnection((isConnected) -> {
            if (isConnected) {
                eventTask = new ThreadAsyncTask();
                eventTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                Toast.makeText(MainActivity.this, "El dispositivo no puede conectarse a Internet, por favor revise la configuración de red", Toast.LENGTH_LONG).show();
            }
        });
    }

    // Proceso en background
    @SuppressWarnings("rawtypes")
    private class ThreadAsyncTask extends AsyncTask  {
        @Override
        protected Object doInBackground(Object[] objects) {
            // Hilo secundario!
            // En este caso usamos un mecanismo de request
            // sincronico para justificar el uso de AsyncTask
            JsonObject obj = new JsonObject();
            obj.addProperty("env", "TEST");
            obj.addProperty("type_events", "dayly_goal_reached");
            obj.addProperty("description", "Meta de pasos alcanzada!!");
            try {
                URL url = new URL("http://so-unlam.net.ar/api/api/event");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setRequestProperty("Content-Type", "application/json");
                    urlConnection.setRequestProperty("Authorization", "Bearer " + getToken());
                    String body = obj.toString();
                    OutputStream out = urlConnection.getOutputStream();
                    out.write(body.getBytes());
                    out.flush();
                    out.close();
                    int responseCode = urlConnection.getResponseCode();
                    publishProgress(responseCode);
                } finally {
                    urlConnection.disconnect();
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Object[] values) {
            int responseCode = (int)values[0];
            if (responseCode == HTTP_OK || responseCode == HTTP_CREATED) {
                Toast.makeText(MainActivity.this, "Meta completada!! Tu logro se ha enviado al servidor!!", Toast.LENGTH_LONG).show();
            } else if (responseCode == HTTP_UNAUTHORIZED) {
                Toast.makeText(MainActivity.this, "Token vencido, ejecutando request para obtener uno nuevo...", Toast.LENGTH_LONG).show();
                executeRefreshTokenRequest();
            } else {
                Toast.makeText(MainActivity.this, "Meta completada!! Tu logro no pudo enviarse al servidor :(", Toast.LENGTH_LONG).show();
            }
        }

        private String getToken() {
            return tokenSharedPreferences.getString("token", "");
        }
    }

    private void executeRefreshTokenRequest() {
        UNLaMSOAAPIService service = UNLaMSOAAPIServiceBuilder.buildService(UNLaMSOAAPIService.class);
        Call<RefreshResponse> call = service.refresh();

        call.enqueue(new Callback<RefreshResponse>() {

            @Override
            public void onResponse(Call<RefreshResponse> request, Response<RefreshResponse> response) {

                if (response.isSuccessful()) {
                    tokenSharedPreferencesEditor.putString("token", response.body().getToken());
                    tokenSharedPreferencesEditor.putString("token_refresh", response.body().getTokenRefresh());
                    tokenSharedPreferencesEditor.apply();
                    Toast.makeText(MainActivity.this, "Token actualizado" ,Toast.LENGTH_LONG).show();
                    launchThread();
                } else {
                    Toast.makeText(MainActivity.this, "Error al refrescar token" ,Toast.LENGTH_LONG).show();
                }

            }

            @Override
            public void onFailure(Call<RefreshResponse> request, Throwable t) {
                Toast.makeText(MainActivity.this, "Error al refrescar token", Toast.LENGTH_LONG).show();
            }
        });
    }

}
