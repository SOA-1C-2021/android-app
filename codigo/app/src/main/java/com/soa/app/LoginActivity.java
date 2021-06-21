package com.soa.app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.soa.app.models.LoginRequest;
import com.soa.app.models.LoginResponse;
import com.soa.app.services.AppExecutors;
import com.soa.app.services.NetworkConnectivity;
import com.soa.app.services.UNLaMSOAAPIService;
import com.soa.app.services.UNLaMSOAAPIServiceBuilder;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextEmailLogin = null;
    private EditText editTextPassword = null;
    private Button buttonLogin = null;
    private Button buttonRegistration = null;

    private NetworkConnectivity networkConnectivity;

    // shared preferences
    SharedPreferences tokenSharedPreferences = null;
    SharedPreferences.Editor tokenSharedPreferencesEditor = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // reference ui components
        editTextEmailLogin = findViewById(R.id.edit_text_email_login);
        editTextPassword = findViewById(R.id.edit_text_password);
        buttonLogin = findViewById(R.id.button_login);
        buttonRegistration = findViewById(R.id.button_registration);

        // add listeners
        buttonLogin.setOnClickListener(buttonLoginListener);
        buttonRegistration.setOnClickListener(buttonRegistrationListener);

        // instantiate network connectivity checker class
        networkConnectivity = new NetworkConnectivity(AppExecutors.getInstance(), this);

        // instantiate shared preferences files
        tokenSharedPreferences = getSharedPreferences(getString(R.string.shared_preferences_token), Context.MODE_PRIVATE);
        tokenSharedPreferencesEditor = tokenSharedPreferences.edit();

    }

    private final View.OnClickListener buttonLoginListener = v -> {
        if (getString(R.string.env).equals("TEST")) {
            Intent mainActivityIntent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(mainActivityIntent);
            return;
        }
        networkConnectivity.checkInternetConnection((isConnected) -> {
            if (isConnected) {
                login();
            } else {
                Toast.makeText(LoginActivity.this, "El dispositivo no puede conectarse a Internet, por favor revise la configuración de red", Toast.LENGTH_LONG).show();
            }
        });
    };

    private final View.OnClickListener buttonRegistrationListener = v -> {
        Intent registerActivityIntent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(registerActivityIntent);
    };

    private boolean validateInputs(String email, String password) {
        boolean valid = true;

        final int PASSWORD_MINIMUM_LENGTH = 8;
        final String EMPTY_FIELD_ERROR = "El campo no puede estar vacío";
        final String INVALID_EMAIL_ERROR = "El email ingresado es inválido";
        final String INVALID_PASSWORD_ERROR = "El password debe tener un mínimo de 8 caracteres";

        if (email.isEmpty()) {
            editTextEmailLogin.setError(EMPTY_FIELD_ERROR);
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmailLogin.setError(INVALID_EMAIL_ERROR);
            valid = false;
        }

        if (password.isEmpty()) {
            editTextPassword.setError(EMPTY_FIELD_ERROR);
            valid = false;
        } else if (password.length() < PASSWORD_MINIMUM_LENGTH) {
            editTextPassword.setError(INVALID_PASSWORD_ERROR);
            valid = false;
        }

        return valid;
    }

    private void login() {

        // get values
        String email = editTextEmailLogin.getText().toString();
        String password = editTextPassword.getText().toString();

        // validate
        boolean validInputs = validateInputs(email, password);
        if (!validInputs) return;

        // build request
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);

        // execute request
        UNLaMSOAAPIService service = UNLaMSOAAPIServiceBuilder.buildService(UNLaMSOAAPIService.class);
        Call<LoginResponse> call = service.login(loginRequest);

        call.enqueue(new Callback<LoginResponse>() {

            @Override
            public void onResponse(Call<LoginResponse> request, Response<LoginResponse> response) {

                if (response.isSuccessful()) {
                    tokenSharedPreferencesEditor.putString("token", response.body().getToken());
                    tokenSharedPreferencesEditor.putString("token_refresh", response.body().getTokenRefresh());
                    tokenSharedPreferencesEditor.apply();
                    Intent mainActivityIntent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(mainActivityIntent);
                } else {
                    Toast.makeText(LoginActivity.this, "Error, por favor verifique que el usuario y la contraseña ingresadas sean correctas" ,Toast.LENGTH_LONG).show();
                }

            }

            @Override
            public void onFailure(Call<LoginResponse> request, Throwable t) {
                Toast.makeText(LoginActivity.this, "Error, por favor verifique que el usuario y la contraseña ingresadas sean correctas", Toast.LENGTH_LONG).show();
            }
        });

    }
}
