package com.onur.motionsicknesskiller;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;
import android.app.AlertDialog;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.accessibility.AccessibilityManager;
import java.util.List;

public class MyAccessibilityService extends AccessibilityService {

    private static final int SYSTEM_ALERT_WINDOW_PERMISSION_REQUEST_CODE = 1;
    private static final int WRITE_SETTINGS_PERMISSION_REQUEST_CODE = 2;
    private static final int ACCESSIBILITY_PERMISSION_REQUEST_CODE = 3;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Gerekli erişilebilirlik olaylarını burada işleyebilirsiniz
    }

    @Override
    public void onInterrupt() {
        // Servis kesildiğinde çağrılır
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        
        // Erişilebilirlik servisi yapılandırması
        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            // Sadece gerekli özellikleri etkinleştir, gereksiz izinler isteme
            info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED | 
                             AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
            
            // Bildirim gecikmesini azalt
            info.notificationTimeout = 50;
            
            // Geri bildirim türünü ayarla
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_VISUAL;
            
            // Pencere içeriğine erişimi etkinleştir (ekran filtreleri için gerekli)
            info.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            
            // Erişilebilirlik düğmesini etkinleştir (Android 9+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON;
            }
            
            // Yapılandırmayı uygula
            setServiceInfo(info);
            
            // Servis başarıyla başlatıldı
            Toast.makeText(this, "Motion Sickness Önleyici servis etkinleştirildi", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Servisi durdur
        stopService(new Intent(this, BlueLightFilterService.class));
    }

    private void checkAllPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                requestOverlayPermission();
            } else if (!Settings.System.canWrite(this)) {
                requestWriteSettingsPermission();
            } else if (!isAccessibilityServiceEnabled()) {
                requestAccessibilityPermission();
            } else {
                proceedWithAppFunctionality();
            }
        }
    }

    private void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void requestWriteSettingsPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void requestAccessibilityPermission() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void showPermissionExplanation(String permission) {
        String message = "Uygulamanın tüm işlevlerini kullanabilmeniz için " + permission + " iznini vermeniz gerekmektedir.";
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("İzin Gerekliliği")
               .setMessage(message)
               .setPositiveButton("Tamam", (dialog, which) -> dialog.dismiss())
               .show();
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

    private void proceedWithAppFunctionality() {
        // Kullanıcıya tüm izinlerin verildiğini bildir
        Toast.makeText(this, "Tüm izinler verildi, uygulama başlatılıyor.", Toast.LENGTH_SHORT).show();

        // Mavi ışık filtresini etkinleştir
        startBlueLightFilterService();

        // Kullanıcıya göz rahatlatma ve mide bulantısı önleme ipuçları göster
        showComfortTips();

        // Uygulamanın ana işlevselliğini başlat
        startMainFunctionality();
    }

    private void startBlueLightFilterService() {
        Intent serviceIntent = new Intent(this, BlueLightFilterService.class);
        int alpha = 100; // Varsayılan değer
        serviceIntent.putExtra("alpha", alpha);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        Toast.makeText(this, "Mavi ışık filtresi etkinleştirildi", Toast.LENGTH_SHORT).show();
    }

    private void showComfortTips() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Göz Rahatlatma ve Mide Bulantısı Önleme İpuçları")
               .setMessage("1. Ekran parlaklığını azaltın.\n2. Mavi ışık filtresini etkinleştirin.\n3. Düzenli aralıklarla gözlerinizi dinlendirin.")
               .setPositiveButton("Tamam", (dialog, which) -> dialog.dismiss())
               .show();
    }

    private void startMainFunctionality() {
        // Uygulamanın ana işlevselliğini burada başlatın
        // Örneğin, sensör verilerini izleyebilir veya kullanıcı etkileşimlerini işleyebilirsiniz
        // Bu kısımda uygulamanızın ana özelliklerini başlatabilirsiniz
    }
}
