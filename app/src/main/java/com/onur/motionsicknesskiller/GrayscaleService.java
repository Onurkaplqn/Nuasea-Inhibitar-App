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
                        startGrayscaleEffect(intensity);
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
        return START_NOT_STICKY;
    }

    /**
     * Siyah-Beyaz (Grayscale) efektini başlatır.
     */
    private void startGrayscaleEffect(float intensity) {
        if (isOverlayActive) {
            return;
        }

        overlayView = new View(this);
        
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
        );

        // Android 12 ve üzeri için ek ayarlar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            params.setFitInsetsTypes(0); // Sistem çubuklarını dahil etme
            params.alpha = 0.99f; // Tam opak olmasını engelle
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
        // Yoğunluğu 0-1 arasında normalize et
        float normalizedIntensity = intensity;
        
        // Gri tonlama matrisi
        ColorMatrix saturationMatrix = new ColorMatrix();
        saturationMatrix.setSaturation(1 - normalizedIntensity);

        // Kontrast matrisi
        float scale = 0.8f + (0.2f * (1 - normalizedIntensity));
        float[] contrastMatrix = {
            scale, 0, 0, 0, 0,
            0, scale, 0, 0, 0,
            0, 0, scale, 0, 0,
            0, 0, 0, 1, 0
        };

        // Parlaklık matrisi
        float shift = -20 * normalizedIntensity;
        float[] brightnessMatrix = {
            1, 0, 0, 0, shift,
            0, 1, 0, 0, shift,
            0, 0, 1, 0, shift,
            0, 0, 0, 1, 0
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
                    "Siyah-Beyaz Modu",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Siyah-beyaz efekt servisi");
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
                .setContentTitle("Siyah-Beyaz Modu Aktif")
                .setContentText("Göz yorgunluğunu azaltmak için siyah-beyaz mod çalışıyor")
                .setSmallIcon(R.drawable.ic_settings)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
