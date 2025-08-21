package com.example.waykisafe;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Reporte extends AppCompatActivity {

    private EditText editIncidente, editDescripcion, editOtroTipo, editUbicacion;
    private AutoCompleteTextView autoTipo, autoNivel;
    private Button btnEnviar;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reporte);

        editIncidente = findViewById(R.id.editTextIncidente); // Este campo reemplaza a "Titulo"
        editDescripcion = findViewById(R.id.editTextDescripcion);
        editOtroTipo = findViewById(R.id.editTextOtroTipo);
        editUbicacion = findViewById(R.id.editTextUbicacion);
        autoTipo = findViewById(R.id.autoTipo);
        autoNivel = findViewById(R.id.autoNivel);
        btnEnviar = findViewById(R.id.btnEnviar);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        configurarDropdowns();
        obtenerUbicacion();

        btnEnviar.setOnClickListener(v -> enviarReporte());
    }

    @SuppressLint("ClickableViewAccessibility")
    private void configurarDropdowns() {
        String[] tipos = {
                "Robo",
                "Acoso",
                "Pérdida",
                "Violencia",
                "Emergencia médica",
                "Incendio",
                "Accidente de tránsito",
                "Vandalismo",
                "Consumo de drogas",
                "Armas visibles",
                "Persona sospechosa",
                "Otro"
        };

        String[] niveles = {"Rojo", "Naranja"}; // Eliminado "Verde"

        ArrayAdapter<String> adapterTipos = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, tipos);
        ArrayAdapter<String> adapterNiveles = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, niveles);

        autoTipo.setAdapter(adapterTipos);
        autoNivel.setAdapter(adapterNiveles);

        autoTipo.setOnTouchListener((v, event) -> {
            autoTipo.showDropDown();
            return false;
        });

        autoNivel.setOnTouchListener((v, event) -> {
            autoNivel.showDropDown();
            return false;
        });

        autoTipo.setOnItemClickListener((parent, view, position, id) -> {
            String seleccion = (String) parent.getItemAtPosition(position);
            View campoOtro = findViewById(R.id.layoutOtroTipo);
            campoOtro.setVisibility("otro".equalsIgnoreCase(seleccion) ? View.VISIBLE : View.GONE);
        });
    }

    private void obtenerUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 50);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                String ubicacionTexto = "Lat: " + location.getLatitude() + ", Lng: " + location.getLongitude();
                editUbicacion.setText(ubicacionTexto);
            }
        });
    }

    private void enviarReporte() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        String incidente = editIncidente.getText().toString().trim();
        String descripcion = editDescripcion.getText().toString().trim();
        String tipo = autoTipo.getText().toString().trim();
        String otroTipo = editOtroTipo.getText().toString().trim();
        String tipoFinal = "otro".equalsIgnoreCase(tipo) ? otroTipo : tipo;
        String nivel = autoNivel.getText().toString().trim();
        String ubicacionTexto = editUbicacion.getText().toString().trim();
        String usuarioId = user.getUid();

        // Validación de campos obligatorios
        if (incidente.isEmpty() || tipoFinal.isEmpty() || nivel.isEmpty() || ubicacionTexto.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos necesarios", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            double lat = (location != null) ? location.getLatitude() : 0.0;
            double lng = (location != null) ? location.getLongitude() : 0.0;

            Map<String, Object> reporte = new HashMap<>();
            reporte.put("incidente", incidente);
            reporte.put("descripcion", descripcion);
            reporte.put("tipo_incidente", tipoFinal);
            reporte.put("nivel_peligro", nivel);
            reporte.put("ubicacion_manual", ubicacionTexto);
            reporte.put("latitud", lat);
            reporte.put("longitud", lng);
            reporte.put("fecha_reporte", new Timestamp(new Date()));
            reporte.put("usuario_id", usuarioId);

            firestore.collection("reportes")
                    .add(reporte)
                    .addOnSuccessListener(doc -> {
                        Toast.makeText(this, "Reporte enviado", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }
}
