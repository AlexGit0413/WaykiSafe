package com.example.waykisafe;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class Bienvenido extends AppCompatActivity {

    private TextView txtWelcome;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CardView btnNext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bienvenido);

        txtWelcome = findViewById(R.id.txtWelcome);
        btnNext = findViewById(R.id.btnInfo);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            String uid = currentUser.getUid();

            db.collection("usuarios").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String nombre = documentSnapshot.getString("nombre");
                            String apellido = documentSnapshot.getString("apellido");

                            if (nombre == null) nombre = "";
                            if (apellido == null) apellido = "";

                            String saludo = "¡Hola, " + nombre + " " + apellido + "!";
                            txtWelcome.setText(saludo);
                        } else {
                            txtWelcome.setText("¡Hola, Usuario!");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Bienvenido", "Error al obtener datos", e);
                        txtWelcome.setText("¡Hola, Usuario!");
                    });
        } else {
            txtWelcome.setText("¡Hola, Usuario!");
        }

        btnNext.setOnClickListener(v -> {
            Intent intent = new Intent(Bienvenido.this, MapsActivity.class);
            startActivity(intent);
        });
    }
}
