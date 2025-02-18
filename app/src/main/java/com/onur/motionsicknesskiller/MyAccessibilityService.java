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

    private View overlayView;
    private boolean isOverlayViewAttached = false;
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
        // createOverlayView(); // Bu satırı kaldır veya kontrol ekle
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isOverlayViewAttached && overlayView != null) {
            WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            windowManager.removeView(overlayView);  // Overlay'i kaldırın
            isOverlayViewAttached = false;
        }
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
        if (!isOverlayViewAttached) {
            createOverlayView();
        }

        // Kullanıcıya göz rahatlatma ve mide bulantısı önleme ipuçları göster
        showComfortTips();

        // Uygulamanın ana işlevselliğini başlat
        startMainFunctionality();
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
