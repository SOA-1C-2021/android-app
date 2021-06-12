package com.soa.app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.andrognito.patternlockview.PatternLockView;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextUser = null;
    private EditText editTextPassword = null;
    private Button buttonLogin = null;
    private Button buttonRegistration = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // reference ui components
        editTextUser = findViewById(R.id.edit_text_user);
        editTextPassword = findViewById(R.id.edit_text_password);
        buttonLogin = findViewById(R.id.button_login);
        buttonRegistration = findViewById(R.id.button_registration);

        // add listeners
        buttonRegistration.setOnClickListener(buttonRegistrationListener);

    }

    private View.OnClickListener buttonRegistrationListener = new View.OnClickListener()
    {
        public void onClick(View v) {
            Intent registerActivityIntent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(registerActivityIntent);
        }

    };
}