package com.onur.motionsicknesskiller;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

public class AccountActivity extends AppCompatActivity {
    
    private TextView accountInfoText;
    private TextView premiumInfoText;
    private Button googleSignInButton;
    private View premiumContainer;
    private GoogleSignInClient googleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        // View'ları başlat
        initializeViews();
        
        // Google Sign-In yapılandırması
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Mevcut hesap durumunu kontrol et
        checkCurrentAccount();

        // Google Sign-In butonu click listener
        googleSignInButton.setOnClickListener(v -> signIn());
    }

    private void initializeViews() {
        accountInfoText = findViewById(R.id.account_info_text);
        premiumInfoText = findViewById(R.id.premium_info_text);
        googleSignInButton = findViewById(R.id.google_sign_in_button);
        premiumContainer = findViewById(R.id.premium_container);

        // Geri butonu için click listener
        findViewById(R.id.back_button).setOnClickListener(v -> finish());
    }

    private void checkCurrentAccount() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            // Google hesabı ile giriş yapılmış
            updateUIForGoogleAccount(account);
        } else {
            // Misafir kullanıcı
            updateUIForGuestAccount();
        }
    }

    private void updateUIForGoogleAccount(GoogleSignInAccount account) {
        accountInfoText.setText("Hoş geldin, " + account.getDisplayName());
        googleSignInButton.setVisibility(View.GONE);
        premiumContainer.setVisibility(View.VISIBLE);
        
        // Premium durumunu kontrol et ve göster
        checkAndShowPremiumStatus();
    }

    private void updateUIForGuestAccount() {
        accountInfoText.setText("Misafir Kullanıcı");
        googleSignInButton.setVisibility(View.VISIBLE);
        premiumContainer.setVisibility(View.GONE);
        premiumInfoText.setText("Premium özellikleri kullanmak için Google hesabınız ile giriş yapın");
    }

    private void checkAndShowPremiumStatus() {
        // TODO: Backend'den premium durumunu kontrol et
        // Şimdilik örnek bir gösterim:
        premiumInfoText.setText("Premium üyelik aktif\nKalan süre: 30 gün");
    }

    private void signIn() {
        startActivityForResult(googleSignInClient.getSignInIntent(), RC_SIGN_IN);
    }
} 