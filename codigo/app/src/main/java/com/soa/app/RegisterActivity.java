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
import com.soa.app.services.AppExecutors;
import com.soa.app.services.NetworkConnectivity;
import com.soa.app.services.UNLaMSOAAPIService;
import com.soa.app.services.UNLaMSOAAPIServiceBuilder;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// TODO: hacer que el grupo y la comision sean spinners con valores predeterminados
// TODO: hacer que los labels sean flotantes

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextName = null;
    private EditText editTextLastname = null;
    private EditText editTextDni = null;
    private EditText editTextEmailRegister = null;
    private EditText editTextNewPassword = null;
    private EditText editTextCommission = null;
    private EditText editTextGroup = null;
    private Button buttonRegister = null;

    private NetworkConnectivity networkConnectivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // reference ui components

        editTextName = findViewById(R.id.edit_text_name);
        editTextLastname = findViewById(R.id.edit_text_last_name);
        editTextDni = findViewById(R.id.edit_text_dni);
        editTextEmailRegister = findViewById(R.id.edit_text_email_register);
        editTextNewPassword = findViewById(R.id.edit_text_new_password);
        editTextCommission = findViewById(R.id.edit_text_commission);
        editTextGroup = findViewById(R.id.edit_text_group);
        buttonRegister = findViewById(R.id.button_register);

        // add listeners
        buttonRegister.setOnClickListener(buttonRegisterListener);

        // instantiate network connectivity checker class
        networkConnectivity = new NetworkConnectivity(AppExecutors.getInstance(), this);
    }

    private View.OnClickListener buttonRegisterListener = new View.OnClickListener()
    {

        public void onClick(View v) {

            networkConnectivity.checkInternetConnection((isConnected) -> {
                if (isConnected) {
                    register();
                } else {
                    Toast.makeText(RegisterActivity.this, "El dispositivo no puede conectarse a Internet, por favor revise la configuraci??n de red", Toast.LENGTH_LONG).show();
                }
            });

        }

    };

    private boolean validateInputs(String name, String lastname, String dni, String email, String newPassword, String commission, String group) {
        boolean valid = true;

        final int VALID_DNI_LENGTH = 8;
        final int PASSWORD_MINIMUM_LENGTH = 8;
        final String EMPTY_FIELD_ERROR = "El campo no puede estar vac??o";
        final String INVALID_DNI_ERROR = "El DNI ingresado es inv??lido";
        final String INVALID_EMAIL_ERROR = "El email ingresado es inv??lido";
        final String INVALID_PASSWORD_ERROR = "El password debe tener un m??nimo de 8 caracteres";
        final String INVALID_COMMISSION_ERROR = "La comisi??n s??lo puede tener los valores 2900 o 3900";

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
            editTextEmailRegister.setError(EMPTY_FIELD_ERROR);
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmailRegister.setError(INVALID_EMAIL_ERROR);
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
        } else if (!commission.equals("2900") && !commission.equals("3900")) {
            editTextCommission.setError(INVALID_COMMISSION_ERROR);
            valid = false;
        }

        if (group.isEmpty()) {
            editTextGroup.setError(EMPTY_FIELD_ERROR);
            valid = false;
        }

        return valid;
    }

    private void register() {

        // get values
        String name = editTextName.getText().toString();
        String lastname = editTextLastname.getText().toString();
        String dni = editTextDni.getText().toString();
        String email = editTextEmailRegister.getText().toString();
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
        Call<RegisterResponse> call = service.register(registerRequest);

        call.enqueue(new Callback<RegisterResponse>() {

            @Override
            public void onResponse(Call<RegisterResponse> request, Response<RegisterResponse> response) {

                if (response.isSuccessful()) {
                    Toast.makeText(RegisterActivity.this, "Usuario creado" ,Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(RegisterActivity.this, "Error al crear usuario, por favor verifique los valores ingresados e intente nuevamente" ,Toast.LENGTH_LONG).show();
                }

            }

            @Override
            public void onFailure(Call<RegisterResponse> request, Throwable t) {
                Toast.makeText(RegisterActivity.this, "Error al crear usuario, por favor verifique los valores ingresados e intente nuevamente" ,Toast.LENGTH_LONG).show();
            }
        });
    }
}