package com.onur.motionsicknesskiller;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class MotionControlActivity extends AppCompatActivity {

    private TextView infoTitle;
    private TextView infoDetail;
    private MaterialCardView motionVisualizerButton;
    private MaterialCardView refreshRateButton;
    private MaterialCardView nightModeButton;
    private MaterialCardView blueLightButton;
    private MaterialButton btnHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_motion_control);
        
        setupToolbar();
        initViews();
        setupFeatureButtons();
        setupHomeButton();
        
        setGeneralInfoText();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Hareket Hastalığı Bilgisi");
        }
    }
    
    private void initViews() {
        infoTitle = findViewById(R.id.info_title);
        infoDetail = findViewById(R.id.info_detail);
        motionVisualizerButton = findViewById(R.id.motion_visualizer_button);
        refreshRateButton = findViewById(R.id.refresh_rate_button);
        nightModeButton = findViewById(R.id.night_mode_button);
        blueLightButton = findViewById(R.id.blue_light_button);
        btnHome = findViewById(R.id.btn_home);
    }

    private void setupHomeButton() {
        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void setupFeatureButtons() {
        motionVisualizerButton.setOnClickListener(view -> {
            infoTitle.setText("Hareket Görselleştirici");
            infoDetail.setText(getMotionVisualizerInfo());
        });

        refreshRateButton.setOnClickListener(view -> {
            infoTitle.setText("Ekran Yenileme Hızı");
            infoDetail.setText(getRefreshRateInfo());
        });

        nightModeButton.setOnClickListener(view -> {
            infoTitle.setText("Gece Modu");
            infoDetail.setText(getNightModeInfo());
        });

        blueLightButton.setOnClickListener(view -> {
            infoTitle.setText("Mavi Işık Filtresi");
            infoDetail.setText(getBlueLightFilterInfo());
        });
    }

    private void setGeneralInfoText() {
        infoTitle.setText("Hareket Hastalığı Kontrol Özellikleri");
        infoDetail.setText("Bu sayfa, hareket hastalığını azaltmaya yönelik uygulamamızın sunduğu özellikleri bilimsel açıdan açıklar.\n\n" +
                "Aşağıdaki özellik butonlarına tıklayarak her bir özelliğin nasıl çalıştığı ve hareket hastalığını önlemeye nasıl yardımcı olduğu hakkında detaylı bilgi alabilirsiniz.");
    }

    private String getMotionVisualizerInfo() {
        return "Hareket Görselleştirici, cihazınızın sensörleri tarafından algılanan hareket verilerini görsel olarak temsil eden bir özelliktir.\n\n" +
                "Hareket hastalığı, beynin vestibüler sistem (iç kulak) tarafından algılanan hareket ile görsel sistemin algıladığı hareket arasındaki uyumsuzluktan kaynaklanır. Bu özellik, cihazın yönelimini ve hareketini gerçek zamanlı olarak görselleştirerek, kullanıcının görsel ve vestibüler sistemleri arasında daha iyi bir uyum sağlamasına yardımcı olur.\n\n" +
                "Araştırmalar, bu tür görsel geri bildirimlerin 'duyusal çelişki'yi azaltabildiğini ve bazı kullanıcılarda mide bulantısı, baş dönmesi gibi hareket hastalığı semptomlarının şiddetini hafifletebileceğini göstermiştir.";
    }

    private String getRefreshRateInfo() {
        return "Ekran Yenileme Hızı, ekranın görüntüyü ne sıklıkta güncellediğini kontrol eden bir özelliktir.\n\n" +
                "Modern cihazlar genellikle 60Hz veya daha yüksek yenileme hızlarına sahiptir. Yüksek yenileme hızları, daha akıcı görsel deneyim sağlarken, bazı insanların görsel sistemleri için zorlayıcı olabilir ve özellikle hareket halindeyken görsel yorgunluğa neden olabilir.\n\n" +
                "Düşük yenileme hızı, ekranın çok sık güncellenmemesini sağlayarak, hareket algısını değiştirir ve bazı durumlarda vestibüler sistemi ve görsel sistemler arasındaki uyumsuzluğu azaltabilir. Bu da araç, tekne veya uçak gibi ortamlarda hareket hastalığı semptomlarını azaltmaya yardımcı olabilir.\n\n" +
                "Ancak, çok düşük yenileme hızları, titreme etkisi oluşturabilir ve bazı kullanıcılarda görsel rahatsızlığa neden olabilir.";
    }

    private String getNightModeInfo() {
        return "Gece Modu, ekranın parlaklığını ve rengini ayarlayarak daha koyu ve gözlere daha yumuşak bir görüntü sağlayan bir özelliktir.\n\n" +
                "Hareket hastalığı, beyin tarafından işlenen bilişsel yükün artmasıyla kötüleşebilir. Parlak ve yüksek kontrastlı ekranlar, özellikle düşük ışık koşullarında, göz yorgunluğuna ve bilişsel yükün artmasına neden olabilir.\n\n" +
                "Gece modu, ekranın genel parlaklığını azaltır ve ekrandaki ışığın dalga boyunu daha sıcak tonlara kaydırır. Bu değişiklikler, görsel sistemi üzerindeki yükü azaltarak, hareket halindeyken beyninizin işlemesi gereken bilgi miktarını azaltabilir.\n\n" +
                "Araştırmalar, düşük parlaklık ve daha düşük kontrast seviyelerinin, uzun süreli ekran kullanımı sırasında göz yorgunluğunu ve buna bağlı mide bulantısı semptomlarını azaltabileceğini göstermiştir.";
    }

    private String getBlueLightFilterInfo() {
        return "Mavi Işık Filtresi, ekrandan yayılan mavi ışık miktarını azaltan bir özelliktir.\n\n" +
                "Dijital ekranlar, doğal olmayan miktarda mavi ışık yayar. Mavi ışık, kısa dalga boyuna ve yüksek enerjiye sahiptir ve göz yorgunluğuna, baş ağrısına ve uyku düzeninin bozulmasına neden olabilir.\n\n" +
                "Hareket hastalığı yaşayan insanlar, çoğu zaman görsel ve vestibüler girdiler arasındaki uyumsuzluğa karşı daha hassastır. Mavi ışık filtresi, gözlerinizin maruz kaldığı yüksek enerjili ışık miktarını azaltarak, görsel sistem üzerindeki stresi ve yorgunluğu azaltır.\n\n" +
                "Bu, özellikle hareket halindeyken (örneğin bir araçta yolculuk sırasında) ekran kullanırken faydalı olabilir, çünkü görsel sisteminiz üzerindeki yükü azaltmak, hareket hastalığı semptomlarını tetikleyebilecek duyusal çelişkilere karşı daha az hassas olmanızı sağlayabilir.";
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 