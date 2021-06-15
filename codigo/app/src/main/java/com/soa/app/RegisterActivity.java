package com.soa.app;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.soa.app.models.RegisterResponse;
import com.soa.app.models.RegisterRequest;
import com.soa.app.services.UNLaMSOAAPIService;
import com.soa.app.services.UNLaMSOAAPIServiceBuilder;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextName = null;
    private EditText editTextLastname = null;
    private EditText editTextDni = null;
    private EditText editTextEmail = null;
    private EditText editTextNewPassword = null;
    private EditText editTextCommission = null;
    private EditText editTextGroup = null;
    private Button buttonRegister = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // reference ui components
        editTextName = findViewById(R.id.edit_text_name);
        editTextLastname = findViewById(R.id.edit_text_last_name);
        editTextDni = findViewById(R.id.edit_text_dni);
        editTextEmail = findViewById(R.id.edit_text_email);
        editTextNewPassword = findViewById(R.id.edit_text_new_password);
        editTextCommission = findViewById(R.id.edit_text_commission);
        editTextGroup = findViewById(R.id.edit_text_group);
        buttonRegister = findViewById(R.id.button_register);

        // add listeners
        buttonRegister.setOnClickListener(buttonRegisterListener);
    }

    private View.OnClickListener buttonRegisterListener = new View.OnClickListener()
    {

        public void onClick(View v) {

            // get values
            String name = editTextName.getText().toString();
            String lastname = editTextLastname.getText().toString();
            String dni = editTextDni.getText().toString();
            String email = editTextEmail.getText().toString();
            String newPassword = editTextNewPassword.getText().toString();
            String commission = editTextCommission.getText().toString();
            String group = editTextGroup.getText().toString();

            // validate
            boolean validInputs = validateInputs(name, lastname, dni, email, newPassword, commission, group);
            if (!validInputs) return;

            // build request
            RegisterRequest registerRequest = new RegisterRequest();
            registerRequest.setEnv(getString(R.string.env));
            registerRequest.setName(name);
            registerRequest.setLastname(lastname);
            registerRequest.setDni(Integer.parseInt(dni));
            registerRequest.setEmail(email);
            registerRequest.setPassword(newPassword);
            registerRequest.setCommission(Integer.parseInt(commission));
            registerRequest.setGroup(Integer.parseInt(group));

            // execute request
            UNLaMSOAAPIService service = UNLaMSOAAPIServiceBuilder.buildService(UNLaMSOAAPIService.class);
            Call<RegisterResponse> call = service.createUser(registerRequest);

            call.enqueue(new Callback<RegisterResponse>() {

                @Override
                public void onResponse(Call<RegisterResponse> request, Response<RegisterResponse> response) {
                    Toast.makeText(RegisterActivity.this, "Usuario creado." ,Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onFailure(Call<RegisterResponse> request, Throwable t) {
                    Toast.makeText(RegisterActivity.this, "Error al crear usuario." ,Toast.LENGTH_SHORT).show();
                    finish();
                }
            });

        }

    };

    private boolean validateInputs(String name, String lastname, String dni, String email, String newPassword, String commission, String group) {
        boolean valid = true;

        final int VALID_DNI_LENGTH = 8;
        final int PASSWORD_MINIMUM_LENGTH = 8;
        final String EMPTY_FIELD_ERROR = "El campo no puede estar vacío";
        final String INVALID_DNI_ERROR = "El DNI ingresado es inválido";
        final String INVALID_EMAIL_ERROR = "El e-mail ingresado es inválido";
        final String INVALID_PASSWORD_ERROR = "El password debe tener un mínimo de 8 caracteres";

        if (name.isEmpty()) {
            editTextName.setError(EMPTY_FIELD_ERROR);
            valid = false;
        }

        if (lastname.isEmpty()) {
            editTextLastname.setError(EMPTY_FIELD_ERROR);
            valid = false;
        }

        if (dni.isEmpty()) {
            editTextDni.setError(EMPTY_FIELD_ERROR);
            valid = false;
        } else if (dni.length() != VALID_DNI_LENGTH) {
            editTextDni.setError(INVALID_DNI_ERROR);
            valid = false;
        }

        if (email.isEmpty()) {
            editTextEmail.setError(EMPTY_FIELD_ERROR);
            valid = false;
        } else if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError(INVALID_EMAIL_ERROR);
            valid = false;
        }

        if (newPassword.isEmpty()) {
            editTextNewPassword.setError(EMPTY_FIELD_ERROR);
            valid = false;
        } else if (newPassword.length() < PASSWORD_MINIMUM_LENGTH) {
            editTextNewPassword.setError(INVALID_PASSWORD_ERROR);
            valid = false;
        }

        if (commission.isEmpty()) {
            editTextCommission.setError(EMPTY_FIELD_ERROR);
            valid = false;
        }

        if (group.isEmpty()) {
            editTextGroup.setError(EMPTY_FIELD_ERROR);
            valid = false;
        }

        return valid;
    }
}