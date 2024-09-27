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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private Switch switchDynamicRefresh;
    private Switch switchNightMode;
    private Switch switchBrightness;
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private View overlayView;
    private boolean isOverlayViewAttached = false;
    private boolean isBlueLightFilterActive = false;
    private int currentAlpha = 0;

    private SeekBar beyazDengeSeekBar;
    private TextView textPermissionInfo;
    private Button buttonRequestPermissions;

    private static final String CHANNEL_ID = "motion_sickness_channel";
    private static final int SYSTEM_ALERT_WINDOW_PERMISSION_REQUEST_CODE = 2;
    private static final int WRITE_SETTINGS_PERMISSION_REQUEST_CODE = 3;
    private static final int ACCESSIBILITY_PERMISSION_REQUEST_CODE = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        switchDynamicRefresh = findViewById(R.id.switch_dynamic_refresh);
        switchNightMode = findViewById(R.id.switch_night_mode);
        switchBrightness = findViewById(R.id.switch_brightness);

        beyazDengeSeekBar = findViewById(R.id.beyazDengeSeekBar);
        textPermissionInfo = findViewById(R.id.text_permission_info);
        buttonRequestPermissions = findViewById(R.id.button_request_permissions);

        switchDynamicRefresh.setEnabled(false);
        switchNightMode.setEnabled(false);
        switchBrightness.setEnabled(false);
        beyazDengeSeekBar.setEnabled(false);

        SharedPreferences sharedPreferences = getSharedPreferences("BlueLightFilterPrefs", MODE_PRIVATE);
        int savedSeekBarProgress = sharedPreferences.getInt("SeekBarValue", 50);
        beyazDengeSeekBar.setProgress(savedSeekBarProgress);

        beyazDengeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (isBlueLightFilterActive) {
                    currentAlpha = (int) (255 * (1 - (float) progress / 100));
                    overlayView.setBackgroundColor(Color.argb(currentAlpha, 255, 100, 0));
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt("SeekBarValue", progress);
                    editor.apply();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                overlayView.setBackgroundColor(Color.argb(currentAlpha, 255, 100, 0));
            }
        });

        switchBrightness.setOnCheckedChangeListener((buttonView, isChecked) -> {
            beyazDengeSeekBar.setEnabled(isChecked);  // Seekbar sadece filtre aktif olduğunda aktif olsun
            if (isChecked) {
                // Eğer filtre butonu aktifse overlay'i oluştur
                isBlueLightFilterActive = true;
                createOverlayView();  // Filtreyi etkinleştir
            } else {
                // Eğer filtre butonu pasifse overlay'i kaldır
                isBlueLightFilterActive = false;
                removeOverlayView();  // Filtreyi devre dışı bırak
            }
        });

        buttonRequestPermissions.setOnClickListener(v -> requestPermissionsManually());

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        overlayView = new View(this);
        overlayView.setBackgroundColor(Color.TRANSPARENT);

        createNotificationChannel();
        showNotification();

        checkPermissions();
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
            if (Settings.canDrawOverlays(this) && Settings.System.canWrite(this) && isAccessibilityServiceEnabled()) {
                textPermissionInfo.setVisibility(View.GONE);
                buttonRequestPermissions.setVisibility(View.GONE);
                switchDynamicRefresh.setEnabled(true);
                switchNightMode.setEnabled(true);
                switchBrightness.setEnabled(true);
            } else {
                textPermissionInfo.setVisibility(View.VISIBLE);
                buttonRequestPermissions.setVisibility(View.VISIBLE);
                switchDynamicRefresh.setEnabled(false);
                switchNightMode.setEnabled(false);
                switchBrightness.setEnabled(false);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isFinishing() && !isDestroyed() && isBlueLightFilterActive) {
            createOverlayView();
        }
    }

    private void createOverlayView() {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        if (isOverlayViewAttached || overlayView.getParent() != null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Overlay izni verilmedi.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        WindowManager windowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);

        WindowManager.LayoutParams params;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
            );
        } else {
            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
            );
        }

        try {
            windowManager.addView(overlayView, params);
            isOverlayViewAttached = true;
        } catch (WindowManager.BadTokenException e) {
            Toast.makeText(this, "Overlay eklenemedi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // removeOverlayView(); Kaldırılmadı, overlay arka planda da devam etmeli
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
        Intent brightnessIntent = new Intent(this, ScreenAdjustmentReceiver.class);
        brightnessIntent.setAction("ADJUST_BRIGHTNESS");
        PendingIntent brightnessPendingIntent = PendingIntent.getBroadcast(this, 0, brightnessIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent filterIntent = new Intent(this, ScreenAdjustmentReceiver.class);
        filterIntent.setAction("ADJUST_FILTER");
        PendingIntent filterPendingIntent = PendingIntent.getBroadcast(this, 1, filterIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_settings)
                .setContentTitle("Motion Sickness Killer")
                .setContentText("Adjust screen settings")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .addAction(R.drawable.ic_brightness, "Brightness", brightnessPendingIntent)
                .addAction(R.drawable.ic_filter, "Blue Light Filter", filterPendingIntent);

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
                    createOverlayView();
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
}
