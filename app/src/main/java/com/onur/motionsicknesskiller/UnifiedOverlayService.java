package com.onur.motionsicknesskiller;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Build;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

public class UnifiedOverlayService extends Service {
    private static final String CHANNEL_ID = "unified_overlay_channel";
    private static final int NOTIFICATION_ID = 5;
    
    private View overlayView;
    private WindowManager windowManager;
    private boolean isOverlayActive = false;
    private static UnifiedOverlayService instance;
    
    // Efekt durumları
    private boolean isNightModeActive = false;
    private boolean isBlueLightFilterActive = false;
    private float grayscaleIntensity = 0.5f;
    private int blueLightAlpha = 100;

    private View blueLightOverlay = null;
    
    public static boolean isRunning() {
        return instance != null && instance.isOverlayActive;
    }
    
    public static boolean isNightModeActive() {
        return instance != null && instance.isNightModeActive;
    }
    
    public static boolean isBlueLightFilterActive() {
        return instance != null && instance.isBlueLightFilterActive;
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
            String action = intent.getAction();
            
            if (action != null) {
                switch (action) {
                    case "START_NIGHT_MODE":
                        isNightModeActive = true;
                        grayscaleIntensity = intent.getFloatExtra("intensity", 0.5f);
                        break;
                        
                    case "UPDATE_NIGHT_MODE":
                        isNightModeActive = true;
                        grayscaleIntensity = intent.getFloatExtra("intensity", 0.5f);
                        break;
                        
                    case "STOP_NIGHT_MODE":
                        isNightModeActive = false;
                        break;
                        
                    case "START_BLUE_LIGHT_FILTER":
                        isBlueLightFilterActive = true;
                        blueLightAlpha = intent.getIntExtra("alpha", 100);
                        break;
                        
                    case "UPDATE_BLUE_LIGHT_FILTER":
                        isBlueLightFilterActive = true;
                        blueLightAlpha = intent.getIntExtra("alpha", 100);
                        break;
                        
                    case "STOP_BLUE_LIGHT_FILTER":
                        isBlueLightFilterActive = false;
                        break;
                }
            }
            
            // Herhangi bir efekt aktifse overlay'i göster
            if (isNightModeActive || isBlueLightFilterActive) {
                try {
                    // Önce foreground servisi başlat
                    startForeground(NOTIFICATION_ID, createNotification());
                    
                    // Sonra overlay'i oluştur veya güncelle
                    if (!isOverlayActive) {
                        createSystemOverlay();
                    } else {
                        updateOverlayEffects();
                        
                        // Dokunmatik geçirgenliği için flag'leri güncelle
                        if (overlayView != null) {
                            updateOverlayTouchFlags(overlayView);
                        }
                        
                        if (blueLightOverlay != null) {
                            updateOverlayTouchFlags(blueLightOverlay);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Servis başlatılamadı: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    cleanupAndStop();
                }
            } else {
                // Hiçbir efekt aktif değilse servisi durdur
                cleanupAndStop();
            }
        }
        
        return START_STICKY;
    }

    private void createSystemOverlay() {
        if (isOverlayActive) {
            return;
        }

        try {
            overlayView = new View(this) {
                @Override
                public boolean onTouchEvent(android.view.MotionEvent event) {
                    // Dokunma olaylarını tüketme, alttaki uygulamalara geçmesini sağla
                    return false;
                }
            };
            
            // Android O ve üzeri için TYPE_APPLICATION_OVERLAY, daha eski sürümler için TYPE_SYSTEM_ALERT kullan
            int overlayType = getOverlayType();
            
            // Overlay parametrelerini oluştur
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    overlayType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    PixelFormat.TRANSLUCENT
            );
            
            // Android 12+ için opaklığı azaltıyoruz (0.8'den az olmalı)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                params.alpha = 0.7f; // Android 12+ için daha düşük opaklık
            } else {
                params.alpha = 0.99f;
            }
            
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

            windowManager.addView(overlayView, params);
            isOverlayActive = true;
            updateOverlayEffects();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Overlay başlatılamadı: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            cleanupAndStop();
        }
    }

    private void updateOverlayEffects() {
        if (!isOverlayActive || overlayView == null) return;
        
        // Hiçbir efekt aktif değilse, servisi durdur
        if (!isNightModeActive && !isBlueLightFilterActive) {
            cleanupAndStop();
            return;
        }
        
        // Sadece mavi ışık filtresi aktifse
        if (isBlueLightFilterActive && !isNightModeActive) {
            // Sadece turuncu efekt uygula
            overlayView.setBackgroundColor(Color.argb(blueLightAlpha, 255, 100, 0));
            overlayView.setAlpha(0.99f); // 1.0f yerine 0.99f kullanarak z-index sorunlarını önle
            
            // Eğer ikinci overlay varsa kaldır
            removeBlueLightOverlay();
            
            // Dokunmatik geçirgenliği için flag'leri güncelle
            updateOverlayTouchFlags(overlayView);
            
            return;
        }
        
        // Sadece gece modu aktifse
        if (isNightModeActive && !isBlueLightFilterActive) {
            // Sadece gri tonlama efekti uygula
            applyGrayscaleEffect();
            
            // Eğer ikinci overlay varsa kaldır
            removeBlueLightOverlay();
            
            // Dokunmatik geçirgenliği için flag'leri güncelle
            updateOverlayTouchFlags(overlayView);
            
            return;
        }
        
        // Her iki efekt de aktifse
        if (isNightModeActive && isBlueLightFilterActive) {
            try {
                // Önce gece modu efektini uygula
                ShapeDrawable grayscaleShape = createGrayscaleDrawable();
                overlayView.setBackground(grayscaleShape);
                
                // Gece modu overlay'inin alpha değerini ayarla - 1.0f yerine 0.95f kullanarak z-index sorunlarını önle
                overlayView.setAlpha(Math.min(0.95f, grayscaleIntensity * 0.6f));
                
                // Dokunmatik geçirgenliği için flag'leri güncelle
                updateOverlayTouchFlags(overlayView);
                
                // Mavi ışık filtresi overlay'ini oluştur veya güncelle
                if (blueLightOverlay == null) {
                    // Yeni bir overlay oluştur ve mavi ışık filtresini uygula
                    blueLightOverlay = new View(this) {
                        @Override
                        public boolean onTouchEvent(android.view.MotionEvent event) {
                            // Dokunma olaylarını tüketme, alttaki uygulamalara geçmesini sağla
                            return false;
                        }
                    };
                    
                    // Mavi ışık filtresi rengini ayarla - tam ekranı kaplaması için
                    blueLightOverlay.setBackgroundColor(Color.argb(blueLightAlpha, 255, 100, 0));
                    
                    // Yeni overlay'i ekle - tam ekranı kaplaması için gece modu ile aynı parametreleri kullan
                    WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                            WindowManager.LayoutParams.MATCH_PARENT,
                            WindowManager.LayoutParams.MATCH_PARENT,
                            getOverlayType(),
                            getOverlayFlags() | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, // Dokunmatik geçirgenliği için FLAG_NOT_TOUCHABLE ekleyerek dokunuşların altındaki uygulamaya geçmesini sağla
                            PixelFormat.TRANSLUCENT
                    );
                    
                    // Z-Index ayarı - gece modu overlay'inin üstünde olması için
                    // Tam ekranı kaplaması için alpha değerini ayarla - 1.0f yerine 0.98f kullanarak z-index sorunlarını önle
                    // Android 12+ için opaklığı azaltıyoruz (0.8'den az olmalı)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        params.alpha = 0.7f; // Android 12+ için daha düşük opaklık
                    } else {
                        params.alpha = 0.98f;
                    }
                    
                    // Çentik/delik desteği
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                    }
                    
                    // Önemli: Yüksek z-order değeri ile diğer pencerelerin üstünde kalmasını sağla
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        params.setFitInsetsTypes(0); // Hiçbir inset'e uymaya çalışma
                    }
                    
                    // Ekran ve buton parlaklığı ayarları
                    params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
                    params.buttonBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
                    
                    // Tam ekranı kapladığından emin olmak için x, y, width ve height değerlerini ayarla
                    params.x = 0;
                    params.y = 0;
                    params.width = WindowManager.LayoutParams.MATCH_PARENT;
                    params.height = WindowManager.LayoutParams.MATCH_PARENT;
                    params.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
                    
                    // Yeni overlay'i ekle
                    windowManager.addView(blueLightOverlay, params);
                    
                    // Dokunmatik geçirgenliği için flag'leri güncelle
                    updateOverlayTouchFlags(blueLightOverlay);
                } else {
                    // Mevcut overlay'i güncelle
                    blueLightOverlay.setBackgroundColor(Color.argb(blueLightAlpha, 255, 100, 0));
                    
                    // Overlay'in tam ekranı kapladığından emin ol
                    if (blueLightOverlay.getParent() != null) {
                        try {
                            WindowManager.LayoutParams params = (WindowManager.LayoutParams) blueLightOverlay.getLayoutParams();
                            
                            // Tam ekranı kapladığından emin olmak için x, y, width ve height değerlerini ayarla
                            params.x = 0;
                            params.y = 0;
                            params.width = WindowManager.LayoutParams.MATCH_PARENT;
                            params.height = WindowManager.LayoutParams.MATCH_PARENT;
                            params.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
                            
                            // Z-Index ayarı - gece modu overlay'inin üstünde olması için
                            // 1.0f yerine 0.98f kullanarak z-index sorunlarını önle
                            params.alpha = 0.98f;
                            
                            // Çentik/delik desteği
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                            }
                            
                            // Flag'leri güncelle - dokunmatik geçirgenliği için FLAG_NOT_TOUCHABLE içeren flag'leri kullan
                            params.flags = getOverlayFlags() | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
                            
                            windowManager.updateViewLayout(blueLightOverlay, params);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    
                    // Dokunmatik geçirgenliği için flag'leri güncelle
                    updateOverlayTouchFlags(blueLightOverlay);
                }
            } catch (Exception e) {
                e.printStackTrace();
                // Hata durumunda en azından bir efekti göster
                applyGrayscaleEffect();
            }
        }
    }
    
    private void applyGrayscaleEffect() {
        ShapeDrawable grayscaleShape = createGrayscaleDrawable();
        overlayView.setBackground(grayscaleShape);
        overlayView.setAlpha(Math.min(0.95f, grayscaleIntensity * 0.6f));
    }
    
    private ShapeDrawable createGrayscaleDrawable() {
        // Yoğunluğu 0.1-1.5 arasında normalize et
        float normalizedIntensity = Math.max(0.1f, Math.min(1.5f, grayscaleIntensity));
        
        // Gri tonlama matrisi - doğrusal azalma
        ColorMatrix saturationMatrix = new ColorMatrix();
        // Sadece gri tonlama yap, renk değiştirme
        saturationMatrix.setSaturation(Math.max(0, 1 - normalizedIntensity)); 

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
        
        return shape;
    }

    private void removeBlueLightOverlay() {
        if (blueLightOverlay != null) {
            try {
                if (blueLightOverlay.getParent() != null) {
                    windowManager.removeViewImmediate(blueLightOverlay);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                blueLightOverlay = null;
            }
        }
    }

    private void cleanupAndStop() {
        // Gece modu overlay'ini temizle
        if (overlayView != null) {
            try {
                // Önce dokunmatik geçirgenliği için flag'leri güncelle
                updateOverlayTouchFlags(overlayView);
                
                if (overlayView.getParent() != null) {
                    windowManager.removeViewImmediate(overlayView);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                overlayView = null;
            }
        }
        
        // Mavi ışık filtresi overlay'ini temizle
        if (blueLightOverlay != null) {
            try {
                // Önce dokunmatik geçirgenliği için flag'leri güncelle
                updateOverlayTouchFlags(blueLightOverlay);
                
                if (blueLightOverlay.getParent() != null) {
                    windowManager.removeViewImmediate(blueLightOverlay);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                blueLightOverlay = null;
            }
        }
        
        // Servis durumunu güncelle
        isOverlayActive = false;
        isNightModeActive = false;
        isBlueLightFilterActive = false;
        
        // Servisi durdur
        stopForeground(true);
        stopSelf();
        instance = null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Ekran Efektleri",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Ekran efektleri servisi");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        String title = "Ekran Efektleri Aktif";
        String content = "";
        
        if (isNightModeActive && isBlueLightFilterActive) {
            content = "Gece modu ve mavi ışık filtresi aktif";
        } else if (isNightModeActive) {
            content = "Gece modu aktif";
        } else if (isBlueLightFilterActive) {
            content = "Mavi ışık filtresi aktif";
        }
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_settings)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanupAndStop();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private int getOverlayType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            return WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }
    }
    
    private int getOverlayFlags() {
        // Temel flag'ler
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                   WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                   WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        
        // FLAG_NOT_TOUCHABLE ekle - bu flag overlay'in dokunma olaylarını almamasını sağlar
        // ve alttaki uygulamalara geçmesini sağlar
        flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        
        // FLAG_NOT_TOUCH_MODAL'ı kaldır - bu flag overlay'in dokunma olaylarını almasını sağlar
        // ancak Android 12+ sürümlerinde sorunlara neden olabilir
        flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        
        // FLAG_HARDWARE_ACCELERATED bazı cihazlarda dokunmatik sorunlarına neden olabilir
        // Bu nedenle kaldırıldı
        
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
        
        return flags;
    }

    // Overlay'in dokunmatik geçirgenliği için flag'leri güncelleme metodu
    private void updateOverlayTouchFlags(View view) {
        if (view == null) return;
        
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) view.getLayoutParams();
        if (params != null) {
            // Dokunma olaylarının altındaki uygulamaya geçmesini sağla
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                         WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                         WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                         WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                         WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
            
            try {
                windowManager.updateViewLayout(view, params);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    // Uygulama arka planda olduğunda çağrılacak metot
    public static void onApplicationBackground() {
        if (instance != null) {
            instance.updateAllOverlayFlags();
        }
    }
    
    // Tüm overlay'lerin flag'lerini güncelleyen metot
    private void updateAllOverlayFlags() {
        // Ana overlay'in flag'lerini güncelle
        if (overlayView != null) {
            updateOverlayTouchFlags(overlayView);
        }
        
        // Mavi ışık filtresi overlay'inin flag'lerini güncelle
        if (blueLightOverlay != null) {
            updateOverlayTouchFlags(blueLightOverlay);
        }
    }
} 