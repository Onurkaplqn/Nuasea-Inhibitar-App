package com.onur.motionsicknesskiller;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.material.slider.Slider;
import com.onur.motionsicknesskiller.motion.MotionSensorManager;
import com.onur.motionsicknesskiller.motion.MotionVisualizerView;
import com.onur.motionsicknesskiller.motion.MotionOverlayService;
import com.onur.motionsicknesskiller.RelaxationActivity;

import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener, MotionSensorManager.MotionDataListener {

    private Switch switchDynamicRefresh;
    private Slider refreshRateSlider;
    private TextView refreshRateText;
    private Switch switchNightMode;
    private Slider nightModeSlider;
    private Switch switchBrightness;
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private boolean isBlueLightFilterActive = false;
    private float maxRefreshRate = 60f;
    private float minRefreshRate = 30f;

    private Slider beyazDengeSeekBar;

    // Motion özellikleri için yeni değişkenler
    private MotionSensorManager motionSensorManager;
    private MotionVisualizerView motionVisualizerView;
    private Switch switchMotionVisualizer;

    // Erişilebilirlik servisi sınıf adı
    private static final String ACCESSIBILITY_SERVICE_CLASS = "MyAccessibilityService";
    private static final String CHANNEL_ID = "motion_sickness_channel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupRefreshRateControls();
        setupOtherControls();
        setupMotionControls(); // Yeni motion kontrolleri
        enableAllControls(); // Tüm kontrolleri etkinleştir
        createNotificationChannel();
        showNotification();
    }

    private void initializeViews() {
        switchDynamicRefresh = findViewById(R.id.switch_dynamic_refresh);
        refreshRateSlider = findViewById(R.id.refresh_rate_slider);
        refreshRateText = findViewById(R.id.refresh_rate_text);
        switchNightMode = findViewById(R.id.switch_night_mode);
        nightModeSlider = findViewById(R.id.night_mode_slider);
        switchBrightness = findViewById(R.id.switch_brightness);
        beyazDengeSeekBar = findViewById(R.id.beyazDengeSeekBar);

        // Slider'ları başlat
        setupSliders();

        // Alt menü ayarları
        setupBottomNavigation();

        // Hesap ikonuna tıklama
        ImageView accountIcon = findViewById(R.id.account_image);
        accountIcon.setOnClickListener(v -> {
            Intent intent = new Intent(this, AccountActivity.class);
            startActivity(intent);
        });

        // Buy Me a Coffee linki için click listener
        ImageView buyMeCoffeeIcon = findViewById(R.id.icon_settings);
        buyMeCoffeeIcon.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://buymeacoffee.com/lestaim"));
            startActivity(intent);
        });

        // Motion kontrollerini başlat
        motionVisualizerView = findViewById(R.id.motion_visualizer);
        switchMotionVisualizer = findViewById(R.id.switch_motion_visualizer);
    }

    private void setupSliders() {
        // Gece Modu Slider ayarları
        if (nightModeSlider != null) {
            nightModeSlider.setValueFrom(0);
            nightModeSlider.setValueTo(100);
            nightModeSlider.setValue(50);
            nightModeSlider.setStepSize(1);
            nightModeSlider.setLabelFormatter(value -> String.format("%.0f%%", value));
        }

        // Parlaklık Slider ayarları
        if (beyazDengeSeekBar != null) {
            beyazDengeSeekBar.setValueFrom(0);
            beyazDengeSeekBar.setValueTo(100);
            beyazDengeSeekBar.setValue(50);
            beyazDengeSeekBar.setStepSize(1);
            beyazDengeSeekBar.setLabelFormatter(value -> String.format("%.0f%%", value));
        }
    }

    private void setupRefreshRateControls() {
        // Ekranın desteklediği maksimum yenileme hızını al
        DisplayManager dm = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        Display display = dm.getDisplay(Display.DEFAULT_DISPLAY);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            float[] refreshRates = display.getSupportedRefreshRates();
            if (refreshRates != null && refreshRates.length > 0) {
                maxRefreshRate = refreshRates[refreshRates.length - 1];
                // En yakın tam sayıya yuvarla
                maxRefreshRate = Math.round(maxRefreshRate);
            }
        }

        // Slider ayarları
        refreshRateSlider.setValueFrom(minRefreshRate);
        refreshRateSlider.setValueTo(maxRefreshRate);
        refreshRateSlider.setValue(60f);
        // Adım boyutunu 1 olarak ayarla ve değerleri tam sayıya yuvarla
        refreshRateSlider.setStepSize(1f);
        refreshRateSlider.setLabelFormatter(value -> String.format("%d FPS", Math.round(value)));

        // Switch kontrolü
        switchDynamicRefresh.setOnCheckedChangeListener((buttonView, isChecked) -> {
            refreshRateSlider.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            refreshRateText.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            
            if (isChecked) {
                float currentValue = Math.round(refreshRateSlider.getValue());
                setRefreshRate(currentValue);
                refreshRateText.setText(String.format("%d FPS", Math.round(currentValue)));
            } else {
                setRefreshRate(maxRefreshRate);
                refreshRateText.setText(String.format("%d FPS", Math.round(maxRefreshRate)));
            }
        });

        // Slider değişim kontrolü
        refreshRateSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                float roundedValue = Math.round(value);
                setRefreshRate(roundedValue);
                refreshRateText.setText(String.format("%d FPS", Math.round(roundedValue)));
            }
        });
    }

    private void setRefreshRate(float refreshRate) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            layoutParams.preferredRefreshRate = refreshRate;
            getWindow().setAttributes(layoutParams);
            
            // Kullanıcıya görsel feedback
            refreshRateText.setText(String.format("%d FPS", Math.round(refreshRate)));
            Toast.makeText(this, String.format("Yenileme hızı %d FPS olarak ayarlandı", Math.round(refreshRate)), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupOtherControls() {
        // Gece Modu ayarlarını yükle
        SharedPreferences grayscalePrefs = getSharedPreferences("GrayscalePrefs", MODE_PRIVATE);
        boolean isGrayscaleActive = grayscalePrefs.getBoolean("isGrayscaleActive", false);
        float grayscaleIntensity = grayscalePrefs.getFloat("grayscaleIntensity", 0.0f);

        // Gece Modu Switch ve Slider durumunu ayarla
        switchNightMode.setChecked(isGrayscaleActive);
        nightModeSlider.setVisibility(isGrayscaleActive ? View.VISIBLE : View.GONE);
        
        // Slider değerlerini tam sayı olarak ayarla ve sınırlar içinde tut
        nightModeSlider.setValueFrom(0);
        nightModeSlider.setValueTo(100);
        float sliderValue = Math.max(0, Math.min(100, Math.round(100 - (grayscaleIntensity * 100f / 1.5f))));
        nightModeSlider.setValue(sliderValue);
        nightModeSlider.setStepSize(1);
        nightModeSlider.setLabelFormatter(value -> String.format("%d%%", Math.round(value)));

        // Parlaklık ayarlarını yükle
        SharedPreferences sharedPreferences = getSharedPreferences("BlueLightFilterPrefs", MODE_PRIVATE);
        isBlueLightFilterActive = sharedPreferences.getBoolean("BlueLightFilterActive", false);
        switchBrightness.setChecked(isBlueLightFilterActive);

        // Parlaklık Slider ayarları
        beyazDengeSeekBar.setValueFrom(0);
        beyazDengeSeekBar.setValueTo(100);
        beyazDengeSeekBar.setValue(Math.round(sharedPreferences.getInt("SeekBarValue", 50)));
        beyazDengeSeekBar.setStepSize(1);
        beyazDengeSeekBar.setLabelFormatter(value -> String.format("%d%%", Math.round(value)));

        // Gece Modu Switch kontrolü
        switchNightMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return;

            // İzin kontrolü
            if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Gece modu için ekran üzerine çizim izni vermeniz gerekiyor", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                switchNightMode.setChecked(false);
                return;
            }

            nightModeSlider.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            nightModeSlider.setEnabled(isChecked);

            Intent serviceIntent = new Intent(this, UnifiedOverlayService.class);
            if (isChecked) {
                // Slider değerini 0.1-1.5 arasına doğrusal olarak normalize et
                // 100 = minimum karartma (0.1), 0 = maksimum karartma (1.5)
                float normalizedValue = 1.5f - (nightModeSlider.getValue() / 100f * 1.4f);
                
                serviceIntent.setAction("START_NIGHT_MODE");
                serviceIntent.putExtra("intensity", normalizedValue);
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent);
                    } else {
                        startService(serviceIntent);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Gece modu başlatılamadı: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                serviceIntent.setAction("STOP_NIGHT_MODE");
                try {
                    startService(serviceIntent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Ayarları kaydet
            SharedPreferences.Editor editor = getSharedPreferences("GrayscalePrefs", MODE_PRIVATE).edit();
            editor.putBoolean("isGrayscaleActive", isChecked);
            editor.putFloat("grayscaleIntensity", 0.2f); // Başlangıç karartma değerini kaydet
            editor.apply();
        });

    // Gece Modu Slider değişim kontrolü
    nightModeSlider.addOnChangeListener((slider, value, fromUser) -> {
        if (UnifiedOverlayService.isNightModeActive()) {
            // Slider değerini 0.1-1.5 arasına doğrusal olarak normalize et
            // 100 = minimum karartma (0.1), 0 = maksimum karartma (1.5)
            float normalizedValue = 1.5f - (value / 100f * 1.4f);
            
            Intent serviceIntent = new Intent(this, UnifiedOverlayService.class);
            serviceIntent.setAction("UPDATE_NIGHT_MODE");
            serviceIntent.putExtra("intensity", normalizedValue);
            startService(serviceIntent);

            // Ayarları kaydet
            SharedPreferences.Editor editor = getSharedPreferences("GrayscalePrefs", MODE_PRIVATE).edit();
            editor.putFloat("grayscaleIntensity", normalizedValue);
            editor.apply();

            // Kullanıcıya görsel feedback
            int karartmaYuzdesi = (int)((1.5f - normalizedValue) * 100f / 1.4f);
            Toast.makeText(this, String.format("Karartma: %%%d", karartmaYuzdesi), 
                Toast.LENGTH_SHORT).show();
        }
    });

    // Parlaklık Switch kontrolü
    switchBrightness.setOnCheckedChangeListener((buttonView, isChecked) -> {
        if (!buttonView.isPressed()) return;

        // İzin kontrolü
        if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Mavi ışık filtresi için ekran üzerine çizim izni vermeniz gerekiyor", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            switchBrightness.setChecked(false);
            return;
        }

        beyazDengeSeekBar.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        beyazDengeSeekBar.setEnabled(isChecked);

        Intent serviceIntent = new Intent(this, UnifiedOverlayService.class);
        if (isChecked) {
            isBlueLightFilterActive = true;
            int alpha = (int) (255 * (1 - 50 / 100f)); // Başlangıç değeri %50
            
            serviceIntent.setAction("START_BLUE_LIGHT_FILTER");
            serviceIntent.putExtra("alpha", alpha);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
                // Slider'ı %50 değerinde göster (orta seviye)
                beyazDengeSeekBar.setValue(50);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Mavi ışık filtresi başlatılamadı: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                isBlueLightFilterActive = false;
            }
        } else {
            isBlueLightFilterActive = false;
            serviceIntent.setAction("STOP_BLUE_LIGHT_FILTER");
            try {
                startService(serviceIntent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Ayarları kaydet
        SharedPreferences.Editor editor = getSharedPreferences("BlueLightFilterPrefs", MODE_PRIVATE).edit();
        editor.putBoolean("BlueLightFilterActive", isBlueLightFilterActive);
        editor.putInt("SeekBarValue", 50); // Varsayılan değer
        editor.apply();
    });

    // Parlaklık Slider değişim kontrolü
    beyazDengeSeekBar.addOnChangeListener((slider, value, fromUser) -> {
        if (isBlueLightFilterActive) {
            int roundedValue = Math.round(value);
            int alpha = (int) (255 * (1 - roundedValue / 100f));
            
            Intent serviceIntent = new Intent(this, UnifiedOverlayService.class);
            serviceIntent.setAction("UPDATE_BLUE_LIGHT_FILTER");
            serviceIntent.putExtra("alpha", alpha);
            startService(serviceIntent);

            // Ayarları kaydet
            SharedPreferences.Editor editor = getSharedPreferences("BlueLightFilterPrefs", MODE_PRIVATE).edit();
            editor.putInt("SeekBarValue", roundedValue);
            editor.apply();

            // Kullanıcıya görsel feedback
            Toast.makeText(this, String.format("Parlaklık: %%%d", roundedValue), 
                Toast.LENGTH_SHORT).show();
        }
    });

    sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
}

private void setupMotionControls() {
    motionSensorManager = new MotionSensorManager(this, this);

    switchMotionVisualizer.setOnCheckedChangeListener((buttonView, isChecked) -> {
        Intent serviceIntent = new Intent(this, MotionOverlayService.class);
        if (isChecked) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            motionVisualizerView.setVisibility(View.GONE); // Ana aktivitedeki view'ı gizle
        } else {
            stopService(serviceIntent);
        }
    });
}

@Override
public void onMotionDataChanged(float[] acceleration, float[] rotation) {
    if (motionVisualizerView != null) {
        motionVisualizerView.updateMotionData(acceleration, rotation);
    }
}

/**
 * Tüm kontrolleri etkinleştirir. İzin kontrolü olmadığı için doğrudan etkinleştiriyoruz.
 */
private void enableAllControls() {
    switchDynamicRefresh.setEnabled(true);
    switchNightMode.setEnabled(true);
    switchBrightness.setEnabled(true);
    beyazDengeSeekBar.setEnabled(switchBrightness.isChecked());
}

@Override
protected void onResume() {
    super.onResume();
    
    // Android 6.0+ için ekran üzerine çizim iznini kontrol et
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
        // İzin yoksa servisleri durdur
        if (UnifiedOverlayService.isNightModeActive() || UnifiedOverlayService.isBlueLightFilterActive()) {
            Intent serviceIntent = new Intent(this, UnifiedOverlayService.class);
            
            if (UnifiedOverlayService.isNightModeActive()) {
                serviceIntent.setAction("STOP_NIGHT_MODE");
                startService(serviceIntent);
                switchNightMode.setChecked(false);
            }
            
            if (UnifiedOverlayService.isBlueLightFilterActive()) {
                serviceIntent.setAction("STOP_BLUE_LIGHT_FILTER");
                startService(serviceIntent);
                switchBrightness.setChecked(false);
            }
            
            Toast.makeText(this, "Ekran üzerine çizim izni verilmediği için filtreler devre dışı bırakıldı", Toast.LENGTH_LONG).show();
        }
    }
    
    updateServiceStates();
}

private void updateServiceStates() {
    // Gece modu durumunu kontrol et
    if (UnifiedOverlayService.isNightModeActive()) {
        switchNightMode.setChecked(true);
        nightModeSlider.setVisibility(View.VISIBLE);
        nightModeSlider.setEnabled(true);
    } else {
        switchNightMode.setChecked(false);
        nightModeSlider.setVisibility(View.GONE);
        nightModeSlider.setEnabled(false);
    }
    
    // Mavi ışık filtresi durumunu kontrol et
    if (UnifiedOverlayService.isBlueLightFilterActive()) {
        switchBrightness.setChecked(true);
        beyazDengeSeekBar.setVisibility(View.VISIBLE);
        beyazDengeSeekBar.setEnabled(true);
    } else {
        switchBrightness.setChecked(false);
        beyazDengeSeekBar.setVisibility(View.GONE);
        beyazDengeSeekBar.setEnabled(false);
    }
}

@Override
protected void onPause() {
    super.onPause();
    // Uygulama arka plana alındığında overlay flag'lerini güncelle
    UnifiedOverlayService.onApplicationBackground();
}

private void createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        CharSequence name = "Motion Sickness Channel";
        String description = "Channel for motion sickness notifications";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }
}

private void showNotification() {
    // Ana aktiviteye doğrudan intent oluştur
    Intent mainIntent = new Intent(this, MainActivity.class);
    mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    PendingIntent mainPendingIntent = PendingIntent.getActivity(this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

    // Parlaklık ayarları için doğrudan intent
    Intent brightnessIntent = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
    PendingIntent brightnessPendingIntent = PendingIntent.getActivity(this, 1, brightnessIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_settings)
            .setContentTitle("Motion Sickness Killer")
            .setContentText("Ayarları düzenlemek için dokunun")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(mainPendingIntent)
            .addAction(R.drawable.ic_brightness, "Parlaklık Ayarları", brightnessPendingIntent);

    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

    try {
        notificationManager.notify(1, builder.build());
    } catch (Exception e) {
        e.printStackTrace();
    }
}

@Override
public void onSensorChanged(SensorEvent event) {}

@Override
public void onAccuracyChanged(Sensor sensor, int accuracy) {}

public View getOverlayView() {
    return null; // Artık overlay view kullanmıyoruz, servisler yönetiyor
}

@Override
protected void onDestroy() {
    super.onDestroy();
    // Activity kapatılırken servisleri durdurma
    Intent serviceIntent = new Intent(this, UnifiedOverlayService.class);
    if (UnifiedOverlayService.isNightModeActive()) {
        serviceIntent.setAction("STOP_NIGHT_MODE");
        startService(serviceIntent);
    }
    
    if (UnifiedOverlayService.isBlueLightFilterActive()) {
        serviceIntent.setAction("STOP_BLUE_LIGHT_FILTER");
        startService(serviceIntent);
    }
}

@Override
public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    // İzin kontrolleri kaldırıldı
}

@Override
public void onBackPressed() {
    // Ana ekrana dönmek yerine kart seçme ekranına dön
    Intent intent = new Intent(this, HomeActivity.class);
    startActivity(intent);
    finish();
}

private void setupBottomNavigation() {
    com.google.android.material.bottomnavigation.BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
    bottomNavigationView.setOnItemSelectedListener(item -> {
        int itemId = item.getItemId();
        if (itemId == R.id.navigation_home) {
            // Zaten ana sayfadayız
            return true;
        } else if (itemId == R.id.navigation_motion_control) {
            // Motion Control Activity'e git
            Intent intent = new Intent(this, MotionControlActivity.class);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.navigation_relax) {
            // Relax Activity'e git
            Intent intent = new Intent(this, RelaxationActivity.class);
            startActivity(intent);
            return true;
        }
        return false;
    });
    
    // Ana sayfa seçilmiş olarak ayarla
    bottomNavigationView.setSelectedItemId(R.id.navigation_home);
}
}

