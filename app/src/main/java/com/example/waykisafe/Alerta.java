// Alerta.java
package com.example.waykisafe;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Alerta extends AppCompatActivity implements SeguroDialogFragment.OnSeguroSeleccionadoListener {

    private static final int LOCATION_PERMISSION_REQUEST = 100;
    private ImageButton panicButton;
    private Button btnWhatsapp;
    private Button btnReporte;
    private CardView btnInfo;
    private FusedLocationProviderClient fusedLocationClient;

    private SensorManager sensorManager;
    private float aceleracion, aceleracionActual, aceleracionUltima;
    private long lastShakeTime = 0;
    private static final int SHAKE_COOLDOWN_MS = 5000;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String userName = "", userPhone = "", userDocumento = "", userNacionalidad = "", userTipoDoc = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_alerta);

            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            sensorManager.registerListener(sensorListener,
                    sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL);

            aceleracion = 10f;
            aceleracionActual = SensorManager.GRAVITY_EARTH;
            aceleracionUltima = SensorManager.GRAVITY_EARTH;

            panicButton = findViewById(R.id.panicButton);
            btnWhatsapp = findViewById(R.id.btnWhatsapp);
            btnReporte = findViewById(R.id.btnReporte);
            MaterialCardView icAmbulancia = findViewById(R.id.btnCentroMedico);
            MaterialCardView icPolicia = findViewById(R.id.btnPolicia);
            MaterialCardView icBombero = findViewById(R.id.btnBomberos);
            btnInfo = findViewById(R.id.btnInfo);

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();

            cargarDatosUsuario();

            TextView locationText = findViewById(R.id.locationText);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                    if (location != null) {
                        Geocoder geocoder = new Geocoder(this);
                        try {
                            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                            if (addresses != null && !addresses.isEmpty()) {
                                String direccion = addresses.get(0).getAddressLine(0);
                                locationText.setText("📍 " + direccion);
                            } else {
                                locationText.setText("📍 Lat: " + location.getLatitude() + " Lon: " + location.getLongitude());
                            }
                        } catch (IOException e) {
                            locationText.setText("📍 Lat: " + location.getLatitude() + " Lon: " + location.getLongitude());
                        }
                    } else {
                        locationText.setText("Ubicación no disponible");
                    }
                });
            }

            panicButton.setOnClickListener(view -> sendEmergencyMessage(true));
            btnWhatsapp.setOnClickListener(view -> sendEmergencyMessage(false));
            btnReporte.setOnClickListener(view -> startActivity(new Intent(Alerta.this, Reporte.class)));

            icPolicia.setOnClickListener(v -> mostrarDialogoEmergencia("Policía", "105"));
            icBombero.setOnClickListener(v -> mostrarDialogoEmergencia("Bomberos", "116"));

            findViewById(R.id.btnInfo).setOnClickListener(v -> {
                Intent intent = new Intent(Alerta.this, MapsActivity.class);
                startActivity(intent);
            });

            icAmbulancia.setOnClickListener(v -> {
                if (getSupportFragmentManager().findFragmentByTag("SeguroDialog") == null) {
                    new SeguroDialogFragment().show(getSupportFragmentManager(), "SeguroDialog");
                }
            });


        } catch (Exception e) {
            Log.e("Alerta", "Error al crear la pantalla de alerta", e);
            Toast.makeText(this, "Ocurrió un error inesperado", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void cargarDatosUsuario() {
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            db.collection("usuarios").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String nombre = documentSnapshot.getString("nombre");
                            String apellido = documentSnapshot.getString("apellido");
                            String celular = documentSnapshot.getString("celular");
                            String nacionalidad = documentSnapshot.getString("nacionalidad");
                            String tipoDoc = documentSnapshot.getString("tipo_documento");

                            // Validar tipo_documento nulo
                            if (tipoDoc == null || tipoDoc.isEmpty()) {
                                tipoDoc = "DNI"; // valor por defecto
                                Log.w("Alerta", "tipo_documento nulo o vacío, usando 'DNI' por defecto");
                            }

                            String docId = "No definido";
                            if ("DNI".equals(tipoDoc)) {
                                docId = documentSnapshot.getString("dni");
                            } else if ("Pasaporte".equals(tipoDoc)) {
                                docId = documentSnapshot.getString("pasaporte");
                            }

                            userName = (nombre != null ? nombre : "") + " " + (apellido != null ? apellido : "");
                            userPhone = celular != null ? celular : "No registrado";
                            userNacionalidad = nacionalidad != null ? nacionalidad : "No especificada";
                            userTipoDoc = tipoDoc;
                            userDocumento = docId != null ? docId : "Desconocido";

                        } else {
                            Toast.makeText(this, "No se encontraron los datos del usuario", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error al cargar datos del usuario", Toast.LENGTH_SHORT).show();
                        Log.e("Alerta", "Firestore error: ", e);
                    });
        }
    }

    private void guardarAlertaEnFirestore(String tipoAlerta, Location location) {
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();

            // Crear el mapa de datos con la información de la alerta
            Map<String, Object> alerta = new HashMap<>();
            alerta.put("nombre", userName);
            alerta.put("ubicacion", "Lat: " + location.getLatitude() + " Lon: " + location.getLongitude());
            alerta.put("tipo_alerta", tipoAlerta); // "Sacudida" o "Botón de pánico"
            alerta.put("dni", userDocumento);
            alerta.put("telefono", userPhone);
            alerta.put("timestamp", System.currentTimeMillis());

            // Guardar los datos en la colección de Firestore
            FirebaseFirestore.getInstance()
                    .collection("Alertas")  // Nombre de la colección
                    .add(alerta)
                    .addOnSuccessListener(documentReference -> {
                        Log.d("Firestore", "Alerta guardada con éxito");
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firestore", "Error al guardar alerta", e);
                        Toast.makeText(Alerta.this, "Error al guardar la alerta", Toast.LENGTH_SHORT).show();
                    });
        }
    }



    private void sendEmergencyMessage(boolean toFixedNumber) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                String message = buildMessageWithLocation(location);
                if (toFixedNumber) {
                    sendWhatsAppToFixedNumber("51926628290", message);
                } else {
                    sendWhatsAppToAnyContact(message);
                }

                // Guardar la alerta en Firestore con la ubicación obtenida
                guardarAlertaEnFirestore("Botón de pánico", location);  // Aquí pasas la ubicación
            } else {
                Toast.makeText(this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show();
            }
        });
    }



    private String buildMessageWithLocation(Location location) {
        String mapsUrl = "https://www.google.com/maps?q=" + location.getLatitude() + "," + location.getLongitude();
        return "🚨 ¡Emergencia!\n\n"
                + "👤 Nombre: " + userName + "\n"
                + "📄 " + userTipoDoc + ": " + userDocumento + "\n"
                + "🌍 Nacionalidad: " + userNacionalidad + "\n"
                + "📱 Teléfono: " + userPhone + "\n"
                + "📍 Ubicación: " + mapsUrl + "\n"
                + "Necesito ayuda inmediata.";
    }

    private void sendWhatsAppToFixedNumber(String phoneNumber, String message) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://api.whatsapp.com/send?phone=" + phoneNumber + "&text=" + Uri.encode(message)));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "WhatsApp no está instalado", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendWhatsAppToAnyContact(String message) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.setPackage("com.whatsapp");
            intent.putExtra(Intent.EXTRA_TEXT, message);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "WhatsApp no está instalado", Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarDialogoEmergencia(String servicio, String numeroEmergencia) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Emergencia: " + servicio)
                .setMessage("¿Qué desea hacer?")
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Llamar", (dialog, which) -> {
                    registrarNotificacion(servicio, "No aplica", "No aplica", "Llamada");
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + numeroEmergencia));
                    startActivity(intent);
                })
                .setNeutralButton("Mensaje", (dialog, which) -> {
                    registrarNotificacion(servicio, "No aplica", "No aplica", "Mensaje");
                    String mensaje = "🚨 Solicito ayuda de " + servicio + ". Por favor, enviar asistencia urgente.";
                    enviarMensajeWhatsApp(numeroEmergencia, mensaje);
                })
                .show();
    }


    private void enviarMensajeWhatsApp(String numero, String mensaje) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String url = "https://api.whatsapp.com/send?phone=51" + numero + "&text=" + Uri.encode(mensaje);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "WhatsApp no está instalado", Toast.LENGTH_SHORT).show();
        }
    }

    private final SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0], y = event.values[1], z = event.values[2];
            aceleracionUltima = aceleracionActual;
            aceleracionActual = (float) Math.sqrt(x * x + y * y + z * z);
            float delta = aceleracionActual - aceleracionUltima;
            aceleracion = aceleracion * 0.9f + delta;

            if (aceleracion > 12) {
                long now = System.currentTimeMillis();
                if (now - lastShakeTime > SHAKE_COOLDOWN_MS) {
                    lastShakeTime = now;
                    sendEmergencyMessage(true);  // Enviar el mensaje de emergencia

                    // Obtener la ubicación y guardar la alerta en Firestore
                    if (ActivityCompat.checkSelfPermission(Alerta.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(Alerta.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // Solicitar permisos si no se tienen
                        ActivityCompat.requestPermissions(Alerta.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
                        return;
                    }

                    // Obtener la ubicación solo si tenemos los permisos
                    fusedLocationClient.getLastLocation().addOnSuccessListener(Alerta.this, location -> {
                        if (location != null) {
                            guardarAlertaEnFirestore("Sacudida", location);  // Guardar la alerta por sacudida
                            Toast.makeText(Alerta.this, "⚠️ Emergencia activada por sacudida", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(Alerta.this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(sensorListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(sensorListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSeguroConfirmado(String tipoSeguro, String clinica, String numero) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Map<String, Object> datos = new HashMap<>();
        datos.put("tipo_seguro", tipoSeguro);
        datos.put("clinica_preferida", clinica);
        datos.put("numero_clinica", numero); // ✅ lo añadimos ahora también

        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(uid)
                .update(datos)
                .addOnSuccessListener(aVoid -> {
                    String mensajeFinal = "🚨 Emergencia médica\n" +
                            "Tipo de seguro: " + tipoSeguro;
                    if (!clinica.isEmpty()) {
                        mensajeFinal += "\nClínica preferida: " + clinica;
                    }
                    mensajeFinal += "\nPor favor, enviar ayuda lo antes posible.";

                    registrarNotificacion("Clínica", tipoSeguro, clinica, "Llamada");

                    switch (tipoSeguro) {
                        case "SIS":
                            mostrarDialogoEmergenciaPersonalizado("Clínica", "084582050", mensajeFinal);
                            break;
                        case "EsSalud":
                            mostrarDialogoEmergenciaPersonalizado("Clínica", "084231664", mensajeFinal);
                            break;
                        case "Privado":
                            String finalMensajeFinal = mensajeFinal;
                            obtenerNumeroPrivadoDesdeFirestore(uid, numeroObtenido -> {
                                mostrarDialogoEmergenciaPersonalizado("Clínica", numeroObtenido, finalMensajeFinal);
                            });
                            break;
                        default:
                            mostrarDialogoEmergenciaPersonalizado("Clínica", "106", mensajeFinal);
                            break;
                    }
                });
    }

    private void mostrarDialogoEmergenciaPersonalizado(String servicio, String numeroEmergencia, String mensajePersonalizado) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Emergencia: " + servicio)
                .setMessage("¿Qué desea hacer?")
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Llamar", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + numeroEmergencia));
                    startActivity(intent);
                })
                .setNeutralButton("Mensaje", (dialog, which) -> {
                    enviarMensajeWhatsApp(numeroEmergencia, mensajePersonalizado);
                })
                .show();
    }

    private void registrarNotificacion(String tipoAutoridad, String tipoSeguro, String clinicaPreferida, String metodo) {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        Map<String, Object> datos = new HashMap<>();
        datos.put("usuario_id", userId);
        datos.put("autoridad", tipoAutoridad);
        datos.put("tipo_seguro", tipoSeguro);
        datos.put("clinica_preferida", clinicaPreferida);
        datos.put("metodo_contacto", metodo); // "Llamada" o "Mensaje"
        datos.put("timestamp", System.currentTimeMillis());

        FirebaseFirestore.getInstance()
                .collection("NotificacionesAutoridades")
                .add(datos)
                .addOnSuccessListener(documentReference -> Log.d("Firestore", "Notificación registrada"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error al registrar notificación", e));
    }

    private void obtenerNumeroPrivadoDesdeFirestore(String uid, OnNumeroObtenidoListener callback) {
        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String numero = documentSnapshot.getString("numero_clinica");
                    if (numero != null && !numero.isEmpty()) {
                        callback.onNumeroObtenido(numero);
                    } else {
                        callback.onNumeroObtenido("106"); // número genérico si no hay
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error al obtener número privado", e);
                    callback.onNumeroObtenido("106");
                });
    }

    // Interface para manejar el callback
    public interface OnNumeroObtenidoListener {
        void onNumeroObtenido(String numero);
    }


}
