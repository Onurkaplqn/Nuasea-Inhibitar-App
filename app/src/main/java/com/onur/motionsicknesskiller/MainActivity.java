package com.onur.motionsicknesskiller;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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
import android.widget.RelativeLayout;
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
    private int currentAlpha = 0; // Mevcut opaklık değeri

    private SeekBar beyazDengeSeekBar;
    private TextView textPermissionInfo;
    private Button buttonRequestPermissions;

    private static final String CHANNEL_ID = "motion_sickness_channel";
    private static final int SYSTEM_ALERT_WINDOW_PERMISSION_REQUEST_CODE = 2;
    private static final int ACCESSIBILITY_PERMISSION_REQUEST_CODE = 3;

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

        // İzinlerin ilk başta kapalı olmasını sağlamak için switch'leri pasif hale getiriyoruz.
        switchDynamicRefresh.setEnabled(false);
        switchNightMode.setEnabled(false);
        switchBrightness.setEnabled(false);
        beyazDengeSeekBar.setEnabled(false);

        beyazDengeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (isBlueLightFilterActive) {
                    currentAlpha = (int) (255 * (1 - (float) progress / 100)); // 0-255 aralığında opaklık değeri
                    overlayView.setBackgroundColor(Color.argb(currentAlpha, 255, 100, 0)); // Transparan turuncu renk
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // SeekBar bırakıldığında da aynı opaklık değerini koruyacak
                overlayView.setBackgroundColor(Color.argb(currentAlpha, 255, 100, 0));
            }
        });

        switchDynamicRefresh.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                enableDynamicRefreshRate();
            }
        });

        switchNightMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                toggleNightMode();
            }
        });

        switchBrightness.setOnCheckedChangeListener((buttonView, isChecked) -> {
            beyazDengeSeekBar.setEnabled(isChecked);
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.canDrawOverlays(this)) {
                        Toast.makeText(this, "Gerekli izinleri vermelisiniz.", Toast.LENGTH_SHORT).show();
                        switchBrightness.setChecked(false);
                    } else {
                        isBlueLightFilterActive = true;
                        createOverlayView();
                        overlayView.setBackgroundColor(Color.TRANSPARENT); // Başlangıçta saydam olacak
                    }
                }
            } else {
                isBlueLightFilterActive = false;
                removeOverlayView();
            }
        });

        buttonRequestPermissions.setOnClickListener(v -> {
            requestPermissionsManually();
        });

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        overlayView = new View(this);
        overlayView.setBackgroundColor(Color.TRANSPARENT); // Başlangıçta tamamen saydam

        createNotificationChannel();
        showNotification();

        // İlk başlatmada izin bilgisi ve izin isteme butonunu göster
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (!Settings.canDrawOverlays(this))) {
            textPermissionInfo.setVisibility(View.VISIBLE);
            buttonRequestPermissions.setVisibility(View.VISIBLE);
        }

        // Uygulama başlatıldığında izin kontrolü yap
        checkPermissions();
    }

    private void requestPermissionsManually() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, SYSTEM_ALERT_WINDOW_PERMISSION_REQUEST_CODE);
            }
            if (!isAccessibilityServiceEnabled()) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivityForResult(intent, ACCESSIBILITY_PERMISSION_REQUEST_CODE);
                Toast.makeText(this, "Lütfen uygulama için erişilebilirlik izni verin", Toast.LENGTH_LONG).show();
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
            if (Settings.canDrawOverlays(this) && isAccessibilityServiceEnabled()) {
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SYSTEM_ALERT_WINDOW_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    createOverlayView();
                } else {
                    Toast.makeText(this, "Permission denied to draw over other apps", Toast.LENGTH_SHORT).show();
                }
            }
        }

        // İzinler verildikten sonra switch'leri aktif hale getirin
        checkPermissions();
    }

    private void enableDynamicRefreshRate() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            layoutParams.preferredRefreshRate = 120.0f; // Örneğin 120Hz
            getWindow().setAttributes(layoutParams);
        }
    }

    private void toggleNightMode() {
        int nightModeFlags =
                getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        switch (nightModeFlags) {
            case Configuration.UI_MODE_NIGHT_YES:
                break;

            case Configuration.UI_MODE_NIGHT_NO:
                break;

            case Configuration.UI_MODE_NIGHT_UNDEFINED:
                break;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float ambientLight = event.values[0];
            if (isBlueLightFilterActive) {
                adjustBlueLight(ambientLight);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void adjustBlueLight(float ambientLight) {
        int alpha = (int) (255 * (ambientLight < 1000 ? 0.65f : 0.45f));
        overlayView.setBackgroundColor(Color.argb(alpha, 255, 100, 0)); // Transparan turuncu renk
    }
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        removeOverlayView();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        removeOverlayView();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Aktivitenin geçerli olup olmadığını kontrol et
        if (!isFinishing() && !isDestroyed() && isBlueLightFilterActive) {
            createOverlayView();
        }
    }

    private void createOverlayView() {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        if (!isOverlayViewAttached && overlayView.getParent() == null) {
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

            windowManager.addView(overlayView, params);
            isOverlayViewAttached = true;
        }
    }



    private void removeOverlayView() {
        if (isOverlayViewAttached) {
            WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showNotification();
            } else {
                Toast.makeText(this, "Permission denied to post notifications", Toast.LENGTH_SHORT).show();
            }
        }

        // İzinler verildikten sonra switch'leri aktif hale getirin
        checkPermissions();
    }

    public View getOverlayView() {
        return overlayView;
    }


}
