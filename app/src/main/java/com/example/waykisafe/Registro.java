package com.example.waykisafe;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Registro extends AppCompatActivity {

    private TextInputEditText editTextNombre, editTextApellido, editTextEmail, editTextPass, editTextCelular;
    private TextInputEditText editTextDni, editTextPasaporte;
    private Spinner spinnerNacionalidad, spinnerTipoDocumento;
    private Button btnRegistrar;
    private ImageView eyeIcon;

    private View nacionalidadLayout;

    private View dniLayout, pasaporteLayout;
    private boolean isPasswordVisible = false;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editTextNombre = findViewById(R.id.editTextNombre);
        editTextApellido = findViewById(R.id.editTextApellido);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPass = findViewById(R.id.editTextPass);
        editTextCelular = findViewById(R.id.editTextCelular);
        editTextDni = findViewById(R.id.editTextDni);
        editTextPasaporte = findViewById(R.id.editTextPasaporte);
        spinnerNacionalidad = findViewById(R.id.spinnerNacionalidad);
        spinnerTipoDocumento = findViewById(R.id.spinnerTipoDocumento);
        btnRegistrar = findViewById(R.id.btn_registrar_usuario);
        eyeIcon = findViewById(R.id.eyeIcon);
        dniLayout = findViewById(R.id.dniLayout);
        pasaporteLayout = findViewById(R.id.pasaporteLayout);
        nacionalidadLayout = findViewById(R.id.nacionalidadLayout);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.nacionalidades_array,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerNacionalidad.setAdapter(adapter);
        spinnerNacionalidad.setSelection(0);

        ArrayAdapter<String> tipoDocAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"DNI", "Pasaporte"});
        tipoDocAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTipoDocumento.setAdapter(tipoDocAdapter);

        // Bloque corregido para mostrar dinámicamente el campo de documento según el tipo seleccionado
        spinnerTipoDocumento.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) nacionalidadLayout.getLayoutParams();

                if (spinnerTipoDocumento.getSelectedItem().toString().equals("DNI")) {
                    dniLayout.setVisibility(View.VISIBLE);
                    pasaporteLayout.setVisibility(View.GONE);
                    editTextPasaporte.setText(""); // Limpia campo oculto
                    params.topToBottom = R.id.dniLayout;
                } else {
                    dniLayout.setVisibility(View.GONE);
                    pasaporteLayout.setVisibility(View.VISIBLE);
                    editTextDni.setText(""); // Limpia campo oculto
                    params.topToBottom = R.id.pasaporteLayout;
                }

                nacionalidadLayout.setLayoutParams(params);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });




        eyeIcon.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                editTextPass.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                eyeIcon.setImageResource(R.drawable.visibility);
            } else {
                editTextPass.setTransformationMethod(PasswordTransformationMethod.getInstance());
                eyeIcon.setImageResource(R.drawable.visibility_off);
            }
            editTextPass.setSelection(editTextPass.getText().length());
        });

        btnRegistrar.setOnClickListener(v -> validarCorreoYDocumento());
    }

    private void validarCorreoYDocumento() {
        String email = editTextEmail.getText().toString().trim();
        String tipoDoc = spinnerTipoDocumento.getSelectedItem().toString();
        String doc = tipoDoc.equals("DNI") ? editTextDni.getText().toString().trim() : editTextPasaporte.getText().toString().trim();
        String campo = tipoDoc.equals("DNI") ? "dni" : "pasaporte";

        if (!TextUtils.isEmpty(email)) {
            db.collection("usuarios")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            editTextEmail.setError("Este correo ya está registrado");
                            editTextEmail.requestFocus();
                        } else {
                            db.collection("usuarios")
                                    .whereEqualTo(campo, doc)
                                    .get()
                                    .addOnCompleteListener(docTask -> {
                                        if (docTask.isSuccessful() && !docTask.getResult().isEmpty()) {
                                            if (tipoDoc.equals("DNI")) {
                                                editTextDni.setError("DNI ya registrado");
                                                editTextDni.requestFocus();
                                            } else {
                                                editTextPasaporte.setError("Pasaporte ya registrado");
                                                editTextPasaporte.requestFocus();
                                            }
                                        } else {
                                            registrarUsuario();
                                        }
                                    });
                        }
                    });
        }
    }

    private void registrarUsuario() {
        String nombre = editTextNombre.getText().toString().trim();
        String apellido = editTextApellido.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPass.getText().toString().trim();
        String celular = editTextCelular.getText().toString().trim();
        String nacionalidad = spinnerNacionalidad.getSelectedItem().toString();
        String tipoDoc = spinnerTipoDocumento.getSelectedItem().toString();
        String dni = editTextDni.getText().toString().trim();
        String pasaporte = editTextPasaporte.getText().toString().trim();

        if (TextUtils.isEmpty(nombre) || !nombre.matches("[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+")) {
            editTextNombre.setError("Ingrese solo letras");
            editTextNombre.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(apellido) || !apellido.matches("[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+")) {
            editTextApellido.setError("Ingrese solo letras");
            editTextApellido.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Correo inválido");
            editTextEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password) || password.length() < 8 || !password.matches(".*\\d.*")) {
            editTextPass.setError("Mínimo 8 caracteres y un número");
            editTextPass.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(celular)) {
            editTextCelular.setError("Ingrese celular");
            editTextCelular.requestFocus();
            return;
        }

        if (tipoDoc.equals("DNI")) {
            if (TextUtils.isEmpty(dni) || dni.length() != 8 || !dni.matches("\\d{8}")) {
                editTextDni.setError("DNI inválido (8 dígitos)");
                editTextDni.requestFocus();
                return;
            }
        } else {
            if (TextUtils.isEmpty(pasaporte) || pasaporte.length() < 6) {
                editTextPasaporte.setError("Pasaporte inválido");
                editTextPasaporte.requestFocus();
                return;
            }
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        Map<String, Object> usuario = new HashMap<>();
                        usuario.put("nombre", nombre);
                        usuario.put("apellido", apellido);
                        usuario.put("email", email);
                        usuario.put("celular", celular);
                        usuario.put("nacionalidad", nacionalidad);
                        usuario.put("tipo_documento", tipoDoc);
                        if (tipoDoc.equals("DNI")) {
                            usuario.put("dni", dni);
                        } else {
                            usuario.put("pasaporte", pasaporte);
                        }

                        db.collection("usuarios").document(userId)
                                .set(usuario)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(Registro.this, "✅ Registro exitoso", Toast.LENGTH_LONG).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(Registro.this, "❌ Error al guardar datos: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Error desconocido";
                        Toast.makeText(Registro.this, "❌ Error en registro: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
