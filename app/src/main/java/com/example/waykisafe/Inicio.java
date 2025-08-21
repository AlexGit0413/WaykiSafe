package com.example.waykisafe;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class Inicio extends AppCompatActivity {

    Button btnSignIn, btnSignUp;
    Animation bounce, slideUpFade;
    LinearLayout containerBienvenido;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio);

        btnSignIn = findViewById(R.id.btnSignIn);
        btnSignUp = findViewById(R.id.btnSignUp);
        containerBienvenido = findViewById(R.id.containerBienvenido);

        bounce = AnimationUtils.loadAnimation(this, R.anim.bounce);
        slideUpFade = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade);

        containerBienvenido.startAnimation(slideUpFade);

        btnSignIn.setOnClickListener(v -> {
            v.startAnimation(bounce);
            startActivity(new Intent(Inicio.this, MainActivity.class));
        });

        btnSignUp.setOnClickListener(v -> {
            v.startAnimation(bounce);
            startActivity(new Intent(Inicio.this, Registro.class));
        });
    }
}
