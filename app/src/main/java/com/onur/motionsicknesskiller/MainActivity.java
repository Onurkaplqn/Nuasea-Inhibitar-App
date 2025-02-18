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

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.material.slider.Slider;
import com.onur.motionsicknesskiller.motion.MotionSensorManager;
import com.onur.motionsicknesskiller.motion.MotionVisualizerView;
import com.onur.motionsicknesskiller.motion.MotionOverlayService;

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
    private View overlayView;
    private boolean isOverlayViewAttached = false;
    private boolean isBlueLightFilterActive = false;
    private int currentAlpha = 0;
    private float maxRefreshRate = 60f;
    private float minRefreshRate = 30f;

    private Slider beyazDengeSeekBar;
    private TextView textPermissionInfo;
    private Button buttonRequestPermissions;

    // Motion özellikleri için yeni değişkenler
    private MotionSensorManager motionSensorManager;
    private MotionVisualizerView motionVisualizerView;
    private Switch switchMotionVisualizer;

    private static final String CHANNEL_ID = "motion_sickness_channel";
    private static final int SYSTEM_ALERT_WINDOW_PERMISSION_REQUEST_CODE = 2;
    private static final int WRITE_SETTINGS_PERMISSION_REQUEST_CODE = 3;
    private static final int ACCESSIBILITY_PERMISSION_REQUEST_CODE = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupRefreshRateControls();
        setupOtherControls();
        setupMotionControls(); // Yeni motion kontrolleri
        checkPermissions();
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
        textPermissionInfo = findViewById(R.id.text_permission_info);
        buttonRequestPermissions = findViewById(R.id.button_request_permissions);

        // Slider'ları başlat
        setupSliders();

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
            }
        }

        // Slider ayarları
        refreshRateSlider.setValueFrom(minRefreshRate);
        refreshRateSlider.setValueTo(maxRefreshRate);
        refreshRateSlider.setValue(60f);
        refreshRateSlider.setStepSize(1f);
        refreshRateSlider.setLabelFormatter(value -> String.format("%.0f FPS", value));

        // Switch kontrolü
        switchDynamicRefresh.setOnCheckedChangeListener((buttonView, isChecked) -> {
            refreshRateSlider.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            refreshRateText.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            
            if (isChecked) {
                setRefreshRate(refreshRateSlider.getValue());
                refreshRateText.setText(String.format("%.0f FPS", refreshRateSlider.getValue()));
            } else {
                setRefreshRate(maxRefreshRate); // Varsayılan değere dön
                refreshRateText.setText(String.format("%.0f FPS", maxRefreshRate));
            }
        });

        // Slider değişim kontrolü
        refreshRateSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                setRefreshRate(value);
                refreshRateText.setText(String.format("%.0f FPS", value));
            }
        });
    }

    private void setRefreshRate(float refreshRate) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            layoutParams.preferredRefreshRate = refreshRate;
            getWindow().setAttributes(layoutParams);
            
            // Kullanıcıya görsel feedback
            refreshRateText.setText(String.format("%.0f FPS", refreshRate));
            Toast.makeText(this, String.format("Yenileme hızı %.0f FPS olarak ayarlandı", refreshRate), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupOtherControls() {
        switchNightMode.setEnabled(false);
        switchBrightness.setEnabled(false);
        beyazDengeSeekBar.setEnabled(false);

        SharedPreferences sharedPreferences = getSharedPreferences("BlueLightFilterPrefs", MODE_PRIVATE);
        isBlueLightFilterActive = sharedPreferences.getBoolean("BlueLightFilterActive", false);
        switchBrightness.setChecked(isBlueLightFilterActive);

        // Gece Modu Slider ayarları
        nightModeSlider.setValueFrom(0);
        nightModeSlider.setValueTo(100);
        nightModeSlider.setValue(sharedPreferences.getInt("NightModeIntensity", 50));
        nightModeSlider.setStepSize(1);
        nightModeSlider.setLabelFormatter(value -> String.format("%.0f%%", value));

        // Parlaklık Slider ayarları
        beyazDengeSeekBar.setValueFrom(0);
        beyazDengeSeekBar.setValueTo(100);
        beyazDengeSeekBar.setValue(sharedPreferences.getInt("SeekBarValue", 50));
        beyazDengeSeekBar.setStepSize(1);
        beyazDengeSeekBar.setLabelFormatter(value -> String.format("%.0f%%", value));

        // Gece Modu Switch kontrolü
        switchNightMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return;

            nightModeSlider.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            nightModeSlider.setEnabled(isChecked);

            Intent serviceIntent = new Intent(this, GrayscaleService.class);
            if (isChecked) {
                if (GrayscaleService.isRunning()) {
                    serviceIntent.setAction("STOP_GRAYSCALE");
                    stopService(serviceIntent);
                }
                
                serviceIntent.setAction("START_GRAYSCALE");
                serviceIntent.putExtra("intensity", nightModeSlider.getValue() / 100f);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
            } else {
                serviceIntent.setAction("STOP_GRAYSCALE");
                stopService(serviceIntent);
            }
        });

        // Gece Modu Slider değişim kontrolü
        nightModeSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (GrayscaleService.isRunning()) {
                Intent serviceIntent = new Intent(this, GrayscaleService.class);
                serviceIntent.setAction("UPDATE_INTENSITY");
                serviceIntent.putExtra("intensity", value / 100f);
                startService(serviceIntent);

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("NightModeIntensity", (int) value);
                editor.apply();
            }
        });

        // Parlaklık Switch kontrolü
        switchBrightness.setOnCheckedChangeListener((buttonView, isChecked) -> {
            beyazDengeSeekBar.setEnabled(isChecked);
            beyazDengeSeekBar.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (isChecked) {
                isBlueLightFilterActive = true;
                createOverlayView();
            } else {
                isBlueLightFilterActive = false;
                removeOverlayView();
            }
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("BlueLightFilterActive", isBlueLightFilterActive);
            editor.apply();
        });

        // Parlaklık Slider değişim kontrolü
        beyazDengeSeekBar.addOnChangeListener((slider, value, fromUser) -> {
            if (isBlueLightFilterActive) {
                currentAlpha = (int) (255 * (1 - value / 100));
                overlayView.setBackgroundColor(Color.argb(currentAlpha, 255, 100, 0));
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("SeekBarValue", (int) value);
                editor.apply();
            }
        });

        buttonRequestPermissions.setOnClickListener(v -> requestPermissionsManually());

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        overlayView = new View(this);
        overlayView.setBackgroundColor(Color.TRANSPARENT);
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

    private void requestPermissionsManually() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, SYSTEM_ALERT_WINDOW_PERMISSION_REQUEST_CODE);
            }

            if (!Settings.System.canWrite(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, WRITE_SETTINGS_PERMISSION_REQUEST_CODE);
            }

            if (!isAccessibilityServiceEnabled()) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivityForResult(intent, ACCESSIBILITY_PERMISSION_REQUEST_CODE);
            }
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (AccessibilityServiceInfo service : enabledServices) {
            if (service.getId().equals(getPackageName() + "/.MyAccessibilityService")) {
                return true;
            }
        }
        return false;
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasOverlayPermission = Settings.canDrawOverlays(this);
            boolean hasWritePermission = Settings.System.canWrite(this);
            boolean hasAccessibilityPermission = isAccessibilityServiceEnabled();
            boolean hasNotificationPermission = true;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                hasNotificationPermission = checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
            }

            if (hasOverlayPermission && hasWritePermission && hasAccessibilityPermission && hasNotificationPermission) {
                textPermissionInfo.setVisibility(View.GONE);
                buttonRequestPermissions.setVisibility(View.GONE);
                switchDynamicRefresh.setEnabled(true);
                switchNightMode.setEnabled(true);
                switchBrightness.setEnabled(true);
                beyazDengeSeekBar.setEnabled(switchBrightness.isChecked());
            } else {
                textPermissionInfo.setVisibility(View.VISIBLE);
                buttonRequestPermissions.setVisibility(View.VISIBLE);
                switchDynamicRefresh.setEnabled(false);
                switchNightMode.setEnabled(false);
                switchBrightness.setEnabled(false);
                beyazDengeSeekBar.setEnabled(false);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Artık servisi kullandığımız için buradaki sensör başlatmaya gerek yok
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Artık servisi kullandığımız için buradaki sensör durdurmaya gerek yok
    }

    private void createOverlayView() {
        if (isOverlayViewAttached) {
            return;
        }

        overlayView = new View(this);
        overlayView.setBackgroundColor(Color.argb(100, 255, 100, 0));  // Transparan turuncu

        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,  // Tüm ekran için overlay
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        try {
            windowManager.addView(overlayView, params);
            isOverlayViewAttached = true;
        } catch (WindowManager.BadTokenException e) {
            Toast.makeText(this, "Overlay eklenemedi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void removeOverlayView() {
        if (isOverlayViewAttached && overlayView.getParent() != null) {
            WindowManager windowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
            windowManager.removeView(overlayView);
            isOverlayViewAttached = false;
        }
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
            } else {
                notificationManager.notify(1, builder.build());
            }
        } else {
            notificationManager.notify(1, builder.build());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SYSTEM_ALERT_WINDOW_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Diğer uygulamaların üstüne çizim izni verildi.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Diğer uygulamaların üstüne çizim izni verilmedi.", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == WRITE_SETTINGS_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.System.canWrite(this)) {
                    Toast.makeText(this, "Sistem ayarları yazma izni verildi.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Sistem ayarları yazma izni verilmedi.", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == ACCESSIBILITY_PERMISSION_REQUEST_CODE) {
            if (isAccessibilityServiceEnabled()) {
                Toast.makeText(this, "Erişilebilirlik hizmeti izni verildi.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Erişilebilirlik hizmeti izni verilmedi.", Toast.LENGTH_SHORT).show();
            }
        }

        checkPermissions();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {}

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public View getOverlayView() {
        return overlayView;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Activity kapatılırken servisi de durdur
        if (GrayscaleService.isRunning()) {
            Intent serviceIntent = new Intent(this, GrayscaleService.class);
            serviceIntent.setAction("STOP_GRAYSCALE");
            stopService(serviceIntent);
        }
    }
}

