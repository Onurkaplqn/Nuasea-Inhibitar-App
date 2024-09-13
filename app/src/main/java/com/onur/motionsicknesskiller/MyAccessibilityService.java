package com.onur.motionsicknesskiller;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

public class MyAccessibilityService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Burada erişilebilirlik olaylarını işleyebilirsiniz
    }

    @Override
    public void onInterrupt() {
        // Bu yöntem, servis kesintiye uğradığında çağrılır
    }
}
