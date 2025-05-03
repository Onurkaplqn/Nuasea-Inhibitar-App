package com.onur.motionsicknesskiller;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;
import androidx.core.app.NotificationCompat;

public class BlueLightFilterService extends Service {
    private static final String CHANNEL_ID = "blue_light_filter_channel";
    private static final int NOTIFICATION_ID = 4;
    
    private View overlayView;
    private WindowManager windowManager;
    private boolean isOverlayActive = false;
    private static BlueLightFilterService instance;

    public static boolean isRunning() {
        return instance != null && instance.isOverlayActive;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        createNotificationChannel();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            int alpha = intent.getIntExtra("alpha", 100);
            startForeground(NOTIFICATION_ID, createNotification());
            
            if (isOverlayActive) {
                updateOverlayAlpha(alpha);
            } else {
                createSystemOverlay(alpha);
            }
        }
        return START_STICKY;
    }

    private void updateOverlayAlpha(int alpha) {
        if (overlayView != null) {
            overlayView.setBackgroundColor(android.graphics.Color.argb(alpha, 255, 100, 0));
        }
    }

    private void createSystemOverlay(int alpha) {
        if (isOverlayActive) return;

        overlayView = new View(this);
        overlayView.setBackgroundColor(android.graphics.Color.argb(alpha, 255, 100, 0));

        int overlayType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            overlayType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            overlayType = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }

        // Temel flag'ler - dokunmatik geçirgenliği için FLAG_NOT_TOUCHABLE eklendi
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                   WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                   WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                   WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                   WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                   WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;

        // Cihaz üreticisine göre özel ayarlar
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        if (manufacturer.contains("samsung")) {
            flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_OVERSCAN |
                    WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR;
        } else if (manufacturer.contains("huawei") || manufacturer.contains("honor")) {
            flags &= ~WindowManager.LayoutParams.FLAG_LAYOUT_IN_OVERSCAN;
        } else if (manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || 
                   manufacturer.contains("poco")) {
            flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_OVERSCAN;
        } else if (manufacturer.contains("oppo") || manufacturer.contains("realme") || 
                   manufacturer.contains("oneplus") || manufacturer.contains("vivo")) {
            flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_OVERSCAN |
                    WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                overlayType,
                flags,
                PixelFormat.TRANSLUCENT
        );

        // Z-Index ayarı (daha altta olması için)
        params.alpha = 0.95f;
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        params.buttonBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        
        // Önemli: Düşük z-order değeri ile diğer pencerelerin altında kalmasını sağla
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            params.setFitInsetsTypes(0); // Hiçbir inset'e uymaya çalışma
        }

        // Çentik/delik desteği
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        try {
            windowManager.addView(overlayView, params);
            isOverlayActive = true;
        } catch (Exception e) {
            e.printStackTrace();
            stopSelf();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Mavi Işık Filtresi",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Mavi ışık filtresi servisi");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Mavi Işık Filtresi Aktif")
                .setContentText("Filtre sistem genelinde uygulanıyor")
                .setSmallIcon(R.drawable.ic_settings)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlayView != null && overlayView.getParent() != null) {
            try {
                windowManager.removeViewImmediate(overlayView);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                overlayView = null;
                isOverlayActive = false;
                instance = null;
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
} 