package com.onur.motionsicknesskiller;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2500; // 2.5 saniye
    private FirebaseAuth firebaseAuth;
    private LottieAnimationView splashAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Firebase Auth başlatma
        firebaseAuth = FirebaseAuth.getInstance();
        
        // Lottie animasyonunu ayarla
        splashAnimation = findViewById(R.id.splash_animation);
        
        // Animasyon yüklenirken hata oluşursa
        splashAnimation.setFailureListener(exception -> {
            // Hata durumunda kullanıcıyı bilgilendir ve yedek görünümü kullan
            Toast.makeText(this, "Animasyon yüklenemedi", Toast.LENGTH_SHORT).show();
            exception.printStackTrace();
        });
        
        // Animasyon yükleme başarılı ise
        splashAnimation.addAnimatorUpdateListener(animation -> {
            // Animasyon güncelleme işlemleri buraya yazılabilir
        });

        // Kısa bir bekleme süresi sonra kontrol yap
        new Handler().postDelayed(this::checkUserAndRedirect, SPLASH_DURATION);
    }

    private void checkUserAndRedirect() {
        try {
            // Kullanıcı durumunu kontrol et
            FirebaseUser currentUser = firebaseAuth.getCurrentUser();

            if (currentUser != null) {
                // Kullanıcı giriş yapmış, kart seçme ekranına yönlendir
                startActivity(new Intent(SplashActivity.this, HomeActivity.class));
            } else {
                // Kullanıcı giriş yapmamış, login ekranına yönlendir
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }
            
            // Mevcut aktiviteyi kapat
            finish();
        } catch (Exception e) {
            // Hata durumunda
            Toast.makeText(this, "Bir hata oluştu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            // Hata sonrası varsayılan olarak login ekranına yönlendir
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            finish();
        }
    }
}
