package com.onur.motionsicknesskiller.motion;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.WindowManager;
import androidx.core.app.NotificationCompat;
import com.onur.motionsicknesskiller.R;

public class MotionOverlayService extends Service implements MotionSensorManager.MotionDataListener {
    private static final String CHANNEL_ID = "motion_overlay_channel";
    private static final int NOTIFICATION_ID = 3;

    private WindowManager windowManager;
    private MotionVisualizerView overlayView;
    private MotionSensorManager motionSensorManager;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        
        // Window manager ve overlay view başlat
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        overlayView = new MotionVisualizerView(this, null);
        
        // Sensör yöneticisini başlat
        motionSensorManager = new MotionSensorManager(this, this);

        // Overlay parametreleri
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
        );

        // Overlay'i ekle
        try {
            windowManager.addView(overlayView, params);
            // Sensörleri dinlemeye başla
            motionSensorManager.startListening();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, createNotification());
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlayView != null && overlayView.getParent() != null) {
            try {
                windowManager.removeView(overlayView);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (motionSensorManager != null) {
            motionSensorManager.stopListening();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onMotionDataChanged(float[] acceleration, float[] rotation) {
        if (overlayView != null) {
            overlayView.updateMotionData(acceleration, rotation);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Hareket Görselleştirici",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Hareket görselleştirici arka plan servisi");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Hareket Görselleştirici Aktif")
                .setContentText("Hareket görselleştirici arka planda çalışıyor")
                .setSmallIcon(R.drawable.ic_settings)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
} 