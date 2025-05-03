package com.onur.motionsicknesskiller;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.content.SharedPreferences;

import androidx.core.app.NotificationCompat;

public class GrayscaleService extends Service {

    private static final String CHANNEL_ID = "grayscale_service_channel";
    private static final int NOTIFICATION_ID = 2;

    private View overlayView;
    private WindowManager windowManager;
    private boolean isOverlayActive = false;

    private static final String PREFS_NAME = "GrayscalePrefs";
    private static final String KEY_IS_ACTIVE = "isGrayscaleActive";

    // Servisin durumunu takip için
    private static GrayscaleService instance;

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
    public void onDestroy() {
        super.onDestroy();
        // onDestroy'da da temizlik yap
        if (overlayView != null) {
            try {
                if (overlayView.getParent() != null) {
                    windowManager.removeViewImmediate(overlayView);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            overlayView = null;
        }
        isOverlayActive = false;
        instance = null;
    }

    /**
     * Servise gelen START/STOP isteklerini yakalar.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            float intensity = intent.getFloatExtra("intensity", 0.5f);
            
            switch (intent.getAction()) {
                case "START_GRAYSCALE":
                    if (!isOverlayActive) {
                        startForeground(NOTIFICATION_ID, createNotification());
                        createSystemOverlay(intensity);
                    }
                    break;

                case "UPDATE_INTENSITY":
                    if (isOverlayActive && overlayView != null) {
                        updateGrayscaleIntensity(intensity);
                    }
                    break;

                case "STOP_GRAYSCALE":
                    cleanupAndStop();
                    break;
            }
        }
        return START_STICKY;
    }

    /**
     * Siyah-Beyaz (Grayscale) efektini başlatır.
     */
    private void createSystemOverlay(float intensity) {
        if (isOverlayActive) return;

        overlayView = new View(this);
        
        int overlayType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            overlayType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            overlayType = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        } else {
            overlayType = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
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
        if (manufacturer.contains("huawei") || manufacturer.contains("honor")) {
            flags &= ~WindowManager.LayoutParams.FLAG_LAYOUT_IN_OVERSCAN;
        } else if (manufacturer.contains("oppo") || manufacturer.contains("realme") || 
                   manufacturer.contains("oneplus") || manufacturer.contains("vivo")) {
            flags |= WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR |
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS |
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION |
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_OVERSCAN;
        } else if (manufacturer.contains("samsung")) {
            flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_OVERSCAN |
                    WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR;
        } else if (manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || 
                   manufacturer.contains("poco")) {
            flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_OVERSCAN;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                overlayType,
                flags,
                PixelFormat.TRANSLUCENT
        );

        // Z-Index ayarı (daha üstte olması için)
        params.alpha = 1.0f;
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        params.buttonBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        
        // Önemli: Yüksek z-order değeri ile diğer pencerelerin üstünde kalmasını sağla
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
            updateGrayscaleIntensity(intensity);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Gece modu başlatılamadı: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            cleanupAndStop();
        }
    }

    private void updateGrayscaleIntensity(float intensity) {
        // Yoğunluğu 0.1-1.5 arasında normalize et
        float normalizedIntensity = Math.max(0.1f, Math.min(1.5f, intensity));
        
        // Gri tonlama matrisi - doğrusal azalma
        ColorMatrix saturationMatrix = new ColorMatrix();
        saturationMatrix.setSaturation(Math.max(0, 1 - normalizedIntensity)); // Doğrusal gri tonlama

        // Kontrast matrisi - doğrusal azalma
        float scale = Math.max(0.2f, 1.0f - (normalizedIntensity * 0.5f));
        float[] contrastMatrix = {
            scale, 0, 0, 0, 0,
            0, scale, 0, 0, 0,
            0, 0, scale, 0, 0,
            0, 0, 0, 1, 0
        };

        // Parlaklık matrisi - doğrusal karartma
        float shift = -80 * normalizedIntensity; // Daha güçlü ve doğrusal karartma
        float[] brightnessMatrix = {
            1, 0, 0, 0, shift,
            0, 1, 0, 0, shift,
            0, 0, 1, 0, shift,
            0, 0, 0, 1, 0 // Sabit alpha değeri
        };

        // Matrisleri birleştir
        ColorMatrix finalMatrix = new ColorMatrix();
        finalMatrix.postConcat(new ColorMatrix(brightnessMatrix));
        finalMatrix.postConcat(new ColorMatrix(contrastMatrix));
        finalMatrix.postConcat(saturationMatrix);

        // Paint ve ShapeDrawable oluştur
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(finalMatrix));

        ShapeDrawable shape = new ShapeDrawable(new RectShape());
        shape.getPaint().set(paint);

        // Overlay'e uygula
        if (overlayView != null) {
            overlayView.setBackground(shape);
            // Alpha değerini doğrusal olarak ayarla
            overlayView.setAlpha(Math.min(0.95f, normalizedIntensity * 0.6f));
        }
    }

    /**
     * Eklenmiş overlay'i kaldırır ve servisi durdurur.
     */
    private void cleanupAndStop() {
        // Önce overlay'i temizle
        if (overlayView != null) {
            try {
                if (overlayView.getParent() != null) {
                    windowManager.removeViewImmediate(overlayView);
                }
            } catch (Exception e) {
                // Hata durumunda log
                e.printStackTrace();
            } finally {
                overlayView = null;
                isOverlayActive = false;
                instance = null; // instance'ı da temizle
            }
        }

        // Servisi durdur
        stopForeground(STOP_FOREGROUND_REMOVE); // STOP_FOREGROUND_REMOVE kullan
        stopSelf();
    }

    /**
     * Foreground servis için gerekli Notification Channel oluşturma (API 26+)
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Gece Modu",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Gece modu servisi");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Foreground serviste göstereceğimiz bildirimi oluşturur.
     */
    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Gece Modu Aktif")
                .setContentText("Göz yorgunluğunu azaltmak için gece modu çalışıyor")
                .setSmallIcon(R.drawable.ic_settings)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
