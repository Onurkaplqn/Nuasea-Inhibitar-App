package com.onur.motionsicknesskiller;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

public class MyAccessibilityService extends AccessibilityService {

    private View overlayView;
    private boolean isOverlayViewAttached = false;

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
        createOverlayView();
    }

    private void createOverlayView() {
        if (isOverlayViewAttached) {
            return;
        }

        // Erişilebilirlik servisinin bağlamında (context) overlay oluşturuyoruz
        overlayView = new View(this);
        overlayView.setBackgroundColor(Color.argb(100, 255, 100, 0));  // Transparan turuncu

        // WindowManager ile overlay'i eklemek için gerekli parametreler
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,  // Erişilebilirlik overlay
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        try {
            windowManager.addView(overlayView, params);  // Overlay'i ekleyin
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
}
