package com.soa.app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;

public class HistoryActivity extends AppCompatActivity {

    // ui
    ListView listView = null;
    TextView textDate = null;
    TextView textSteps = null;
    TextView textGoal = null;
    TextView textCompleted = null;

    // shared preferences
    SharedPreferences historySharedPreferences = null;
    SharedPreferences settingsSharedPreferences = null;

    // list
    ArrayList<String> list = null;
    ArrayAdapter arrayAdapter = null;

    // other
    private final int DEFAULT_DAILY_GOAL = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // reference ui components
        listView = findViewById(R.id.list_view);
        textDate = findViewById(R.id.text_date);
        textSteps = findViewById(R.id.text_steps);
        textGoal = findViewById(R.id.text_goal);
        textCompleted = findViewById(R.id.text_completed);

        // instantiate shared preferences files
        historySharedPreferences = getSharedPreferences(getString(R.string.shared_preferences_history), Context.MODE_PRIVATE);
        settingsSharedPreferences = getSharedPreferences(getString(R.string.shared_preferences_settings), Context.MODE_PRIVATE);

        // instantiate list components
        list = new ArrayList<String>();
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list);
        listView.setAdapter(arrayAdapter);

        // perform initialization actions
        if (getString(R.string.env).equals("TEST")) mockSharedPreferences();
        loadList();
    }

    private void loadList() {
        Map<String,?> historySharedPreferencesKeys = historySharedPreferences.getAll();

        for(Map.Entry<String,?> historySharedPreferencesKeysEntry : historySharedPreferencesKeys.entrySet()){
            Log.d(getClass().getName(),historySharedPreferencesKeysEntry.getKey() + ": " +
                    historySharedPreferencesKeysEntry.getValue().toString());

            // retrieve necessary values
            String dateYYYYMMDD = historySharedPreferencesKeysEntry.getKey();
            int goal = settingsSharedPreferences.getInt(dateYYYYMMDD, DEFAULT_DAILY_GOAL);
            int steps = (int) historySharedPreferencesKeysEntry.getValue();
            String completed = (steps >= goal) ? "SI" : "NO";

            // build row
            String formattedDate = dateYYYYMMDD.substring(6, 8) + "/" + dateYYYYMMDD.substring(4, 6) + "/" + dateYYYYMMDD.substring(0, 4);
            String row = String.format("%10s\t\t\t\t\t\t%-4s\t\t\t\t\t\t%-4s\t\t\t\t\t\t%-2s", formattedDate, steps, goal, completed);
            list.add(row);
            Collections.sort(list, new DateComparator());
        }
    }

    private void mockSharedPreferences() {
        SharedPreferences.Editor historySharedPreferencesEditor = historySharedPreferences.edit();
        SharedPreferences.Editor settingsSharedPreferencesEditor = settingsSharedPreferences.edit();

        historySharedPreferencesEditor.putInt("20210615", 50);
        historySharedPreferencesEditor.putInt("20210616", 90);
        historySharedPreferencesEditor.putInt("20210617", 150);
        historySharedPreferencesEditor.putInt("20210618", 190);
        historySharedPreferencesEditor.putInt("20210619", 250);
        historySharedPreferencesEditor.apply();

        settingsSharedPreferencesEditor.putInt("20210615", 50);
        settingsSharedPreferencesEditor.putInt("20210616", 100);
        settingsSharedPreferencesEditor.putInt("20210617", 150);
        settingsSharedPreferencesEditor.putInt("20210618", 200);
        settingsSharedPreferencesEditor.putInt("20210619", 250);
        settingsSharedPreferencesEditor.apply();
    }

    private class DateComparator implements Comparator<String> {

        @Override
        public int compare(String a, String b) {
            String dateA = a.substring(0, 10);
            String dateB = b.substring(0, 10);

            try {
                Date formattedDateA = new SimpleDateFormat("dd/MM/yyyy").parse(dateA);
                Date formattedDateB = new SimpleDateFormat("dd/MM/yyyy").parse(dateB);

                return formattedDateA.compareTo(formattedDateB);

            } catch (ParseException e) {
                e.printStackTrace();
            }

            return 0;
        }
    }
}
