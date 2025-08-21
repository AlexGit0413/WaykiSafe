package com.example.waykisafe;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.*;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore firestore;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        firestore = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Cargar el mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, "No se pudo cargar el mapa", Toast.LENGTH_LONG).show();
        }

        findViewById(R.id.fab_sos).setOnClickListener(v -> {
            Intent intent = new Intent(MapsActivity.this, Alerta.class);
            startActivity(intent);
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        mMap.getUiSettings().setMyLocationButtonEnabled(true); // Elimina el botón de mi ubicación
        mMap.getUiSettings().setZoomControlsEnabled(false); // Elimina los controles de zoom (opcional)

        mMap.setMyLocationEnabled(true);

        // Obtener la ubicación actual
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {

                        // Obtener las coordenadas de la ubicación del usuario
                        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());

                        // Ajustar el nivel de zoom para mostrar un área más cercana centrada en la ubicación
                        float zoomLevel = 19.0f; // Ajusta el zoom para acercar más (puedes probar con 18.0f si es necesario)

                        // Mover la cámara al centro de la ubicación del usuario con el zoom adecuado
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, zoomLevel));

                        // Agregar marcador para la ubicación del usuario
                        mMap.addMarker(new MarkerOptions()
                                .position(userLocation)
                                .title("Tu ubicación actual")
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ubi))); // Ícono pequeño de ubicación

                        // Agregar un círculo alrededor de la ubicación del usuario (con un radio pequeño)
                        mMap.addCircle(new CircleOptions()
                                .center(userLocation)
                                .radius(8) // Radio pequeño
                                .strokeColor(Color.GRAY) // Color gris
                                .fillColor(Color.argb(50, 128, 128, 128))); // Color gris claro

                        // Verificar proximidad a zonas peligrosas
                        verificarProximidad(userLocation);
                    } else {
                        Toast.makeText(this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show();
                    }
                });

        // Cargar zonas peligrosas (si las hay)
        cargarZonasPeligrosas();
    }


    private void verificarProximidad(LatLng userLocation) {
        final double RADIUS = 5; // Radio de proximidad en metros (ajustable)

        // Verificar la proximidad con las zonas peligrosas
        firestore.collection("reportes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Double lat = doc.getDouble("latitud");
                        Double lng = doc.getDouble("longitud");
                        String nivel = doc.getString("nivel_peligro");

                        if (lat == null || lng == null || !"Rojo".equalsIgnoreCase(nivel)) continue;

                        // Calcular la distancia entre la ubicación del usuario y la zona peligrosa
                        Location userLoc = new Location("user");
                        userLoc.setLatitude(userLocation.latitude);
                        userLoc.setLongitude(userLocation.longitude);

                        Location zoneLoc = new Location("zone");
                        zoneLoc.setLatitude(lat);
                        zoneLoc.setLongitude(lng);

                        float distance = userLoc.distanceTo(zoneLoc); // Distancia en metros

                        // Si el usuario está cerca de una zona peligrosa (menos de 50 metros), muestra una alerta
                        if (distance < RADIUS) {
                            // Mostrar alerta
                            Toast.makeText(MapsActivity.this, "¡Estás ingresando a una zona peligrosa! Toma tus precauciones.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void cargarZonasPeligrosas() {
        final HashMap<String, Boolean> zonasOcupadas = new HashMap<>();

        // Cargar reportes de usuarios
        firestore.collection("reportes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Double lat = doc.getDouble("latitud");
                        Double lng = doc.getDouble("longitud");
                        String nivel = doc.getString("nivel_peligro");

                        if (lat == null || lng == null || nivel == null) continue;

                        LatLng ubicacion = new LatLng(lat, lng);

                        // Definir colores según nivel de peligro
                        int colorStroke = Color.RED;
                        int colorFill = 0x44FF0000; // Rojo transparente

                        if ("Naranja".equalsIgnoreCase(nivel)) {
                            colorStroke = Color.rgb(255, 165, 0);
                            colorFill = 0x44FFA500;
                        }

                        // Agregar círculo y marcador del usuario
                        mMap.addCircle(new CircleOptions()
                                .center(ubicacion)
                                .radius(8)
                                .strokeColor(colorStroke)
                                .fillColor(colorFill));

                        mMap.addMarker(new MarkerOptions()
                                .position(ubicacion)
                                .title("[Usuario] Zona reportada")
                                .snippet("Nivel: " + nivel)
                                .icon(BitmapDescriptorFactory.fromBitmap(
                                        Bitmap.createScaledBitmap(
                                                BitmapFactory.decodeResource(getResources(), R.drawable.human),
                                                50, 50, false))));

                        // Marcar la zona como ocupada
                        String key = Math.round(lat * 1000) + "," + Math.round(lng * 1000);
                        zonasOcupadas.put(key, true);
                    }

                    // Luego de cargar zonas de usuario, cargar reportes de IA
                    firestore.collection("reportes_ia")
                            .get()
                            .addOnSuccessListener(queryIA -> {
                                Map<String, Integer> conteoIA = new HashMap<>();
                                Map<String, LatLng> ubicacionesIA = new HashMap<>();
                                Map<String, String> textosIA = new HashMap<>();

                                for (QueryDocumentSnapshot doc : queryIA) {
                                    Double lat = doc.getDouble("lat");
                                    Double lng = doc.getDouble("lon");
                                    String texto = doc.getString("texto");
                                    String nivel = doc.getString("nivel_peligro"); // Obtenemos el nivel desde Firestore

                                    if (lat == null || lng == null || nivel == null) continue;

                                    String key = Math.round(lat * 1000) + "," + Math.round(lng * 1000);
                                    ubicacionesIA.put(key, new LatLng(lat, lng));
                                    textosIA.put(key, texto != null ? texto : "Zona IA reportada");
                                    zonasOcupadas.put(key, true);
                                    conteoIA.put(key, 1); // Este valor ya no se usará, pero lo dejamos por compatibilidad

                                    // Ahora, dibujamos inmediatamente el círculo según el nivel
                                    int colorStroke = Color.RED;
                                    int colorFill = 0x44FF0000; // Rojo por defecto

                                    if ("Naranja".equalsIgnoreCase(nivel)) {
                                        colorStroke = Color.rgb(255, 165, 0);
                                        colorFill = 0x44FFA500;
                                    }

                                    LatLng ubicacion = new LatLng(lat, lng);

                                    mMap.addCircle(new CircleOptions()
                                            .center(ubicacion)
                                            .radius(8)
                                            .strokeColor(colorStroke)
                                            .fillColor(colorFill));

                                    mMap.addMarker(new MarkerOptions()
                                            .position(ubicacion)
                                            .title("[IA] " + (texto != null ? texto.substring(0, Math.min(30, texto.length())) + "..." : "Zona IA"))
                                            .snippet("Nivel: " + nivel)
                                            .icon(BitmapDescriptorFactory.fromBitmap(
                                                    Bitmap.createScaledBitmap(
                                                            BitmapFactory.decodeResource(getResources(), R.drawable.ia),
                                                            50, 50, false))));
                                }



                                // Finalmente, mostrar zonas verdes (no reportadas)
                                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                    fusedLocationClient.getLastLocation()
                                            .addOnSuccessListener(location -> {
                                                if (location != null) {
                                                    double latCentro = location.getLatitude();
                                                    double lonCentro = location.getLongitude();

                                                    for (int latOffset = -3; latOffset <= 3; latOffset++) {
                                                        for (int lonOffset = -3; lonOffset <= 3; lonOffset++) {
                                                            double lat = latCentro + (latOffset * 0.0005);
                                                            double lon = lonCentro + (lonOffset * 0.0005);
                                                            String key = Math.round(lat * 1000) + "," + Math.round(lon * 1000);

                                                            if (!zonasOcupadas.containsKey(key)) {
                                                                LatLng ubicacion = new LatLng(lat, lon);
                                                                mMap.addGroundOverlay(new GroundOverlayOptions()
                                                                        .image(BitmapDescriptorFactory.fromResource(R.drawable.verde))
                                                                        .position(ubicacion, 60f)
                                                                        .transparency(0.4f));
                                                            }
                                                        }
                                                    }
                                                }
                                            });
                                }
                            });
                });
    }
}