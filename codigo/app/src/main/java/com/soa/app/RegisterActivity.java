package com.soa.app;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

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
        private final String emptyFieldError = "Este campo no puede estar vacío";

        public void onClick(View v) {

            // perform validations
            String name = editTextName.getText().toString();
            String lastname = editTextLastname.getText().toString();
            String dni = editTextDni.getText().toString();
            String email = editTextEmail.getText().toString();
            String newPassword = editTextNewPassword.getText().toString();
            String commission = editTextCommission.getText().toString();
            String group = editTextGroup.getText().toString();

            if (name.isEmpty()) editTextName.setError(emptyFieldError);
            if (lastname.isEmpty()) editTextLastname.setError(emptyFieldError);
            if (dni.isEmpty()) editTextDni.setError(emptyFieldError);
            if (email.isEmpty()) editTextEmail.setError(emptyFieldError);
            if (newPassword.isEmpty()) editTextNewPassword.setError(emptyFieldError);
            if (commission.isEmpty()) editTextCommission.setError(emptyFieldError);
            if (group.isEmpty()) editTextGroup.setError(emptyFieldError);

        }

    };
}