package com.onur.motionsicknesskiller;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.onur.motionsicknesskiller.adapter.CardAdapter;
import com.onur.motionsicknesskiller.model.Card;
import com.onur.motionsicknesskiller.transform.CardPageTransformer;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private LinearLayout indicatorContainer;
    private CardAdapter adapter;
    private List<View> indicators;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Tam ekran modu
        hideSystemUI();

        // Firebase Auth başlat
        mAuth = FirebaseAuth.getInstance();

        // ViewPager ve göstergeleri başlat
        setupViews();
        setupCards();
        setupIndicators();
    }

    // Sistem UI'ı gizle - tam ekran mod
    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void setupViews() {
        viewPager = findViewById(R.id.viewPager);
        indicatorContainer = findViewById(R.id.indicatorsContainer);
        
        // Sayfa dönüşüm efektini ayarla
        viewPager.setPageTransformer(new CardPageTransformer());
        
        // Sayfa değişim dinleyicisi
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateIndicators(position);
                
                // Seçilen kart için animasyonu oynat, diğerleri için durdur
                adapter.playAnimationForPosition(position);
            }
        });
    }

    private void setupCards() {
        List<Card> cards = new ArrayList<>();
        
        // Kartları oluştur - istenilen yeni JSON dosyalarıyla
        cards.add(new Card(
                "Rahatlama",
                "",  // Açıklama silindi
                0,   // İkon silindi
                Color.BLACK,
                Card.TYPE_RELAXATION,
                "lefttag.json"));  // İstenilen rahatlama için yeni JSON
        
        cards.add(new Card(
                "Ekran-Hareket",  // İsim güncellendi
                "",  // Açıklama silindi
                0,   // İkon silindi
                Color.BLACK,
                Card.TYPE_NIGHT_MODE,
                "eyefiltersplash.json"));  // İstenilen açılış ekranı için yeni JSON
        
        // Adaptörü oluştur ve ViewPager'a ayarla
        adapter = new CardAdapter(this, cards);
        viewPager.setAdapter(adapter);
        
        // İlk kart için animasyonu başlat
        adapter.playAnimationForPosition(0);
    }

    private void setupIndicators() {
        indicators = new ArrayList<>();
        
        // Kart sayısı kadar gösterge oluştur
        for (int i = 0; i < adapter.getItemCount(); i++) {
            View indicator = new View(this);
            
            // Gösterge boyutunu ve kenar boşluğunu ayarla
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    getResources().getDimensionPixelSize(R.dimen.indicator_size),
                    getResources().getDimensionPixelSize(R.dimen.indicator_size)
            );
            params.setMargins(
                    getResources().getDimensionPixelSize(R.dimen.indicator_margin),
                    0,
                    getResources().getDimensionPixelSize(R.dimen.indicator_margin),
                    0
            );
            indicator.setLayoutParams(params);
            
            // İlk göstergeyi aktif yap, diğerlerini pasif
            indicator.setBackgroundResource(i == 0 ? 
                    R.drawable.indicator_active : 
                    R.drawable.indicator_inactive);
            
            // Göstergeyi container'a ekle
            indicatorContainer.addView(indicator);
            indicators.add(indicator);
        }
    }

    private void updateIndicators(int position) {
        // Tüm göstergeleri güncelle
        for (int i = 0; i < indicators.size(); i++) {
            indicators.get(i).setBackgroundResource(
                    i == position ? R.drawable.indicator_active : R.drawable.indicator_inactive
            );
        }
    }
    
    // Kullanıcının çıkış yapmasını sağla
    public void logout(View view) {
        mAuth.signOut();
        finish();
        startActivity(getIntent());
    }
} 