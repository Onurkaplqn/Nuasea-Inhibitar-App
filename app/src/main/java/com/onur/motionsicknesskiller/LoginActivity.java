package com.onur.motionsicknesskiller;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Firebase Auth başlatma
        firebaseAuth = FirebaseAuth.getInstance();

        // Giriş yap butonu
        MaterialButton btnSignIn = findViewById(R.id.btn_sign_in);
        btnSignIn.setOnClickListener(v -> {
            Intent intent = new Intent(this, SignInActivity.class);
            startActivity(intent);
        });

        // Hesap oluştur butonu
        MaterialButton btnCreateAccount = findViewById(R.id.btn_create_account);
        btnCreateAccount.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Zaten giriş yapılmış mı kontrol et
        if (firebaseAuth.getCurrentUser() != null) {
            // MainActivity yerine HomeActivity'ye yönlendir
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        }
    }
} 