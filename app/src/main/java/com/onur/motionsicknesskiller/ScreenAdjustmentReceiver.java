// Tek bir Receiver sınıfı
package com.onur.motionsicknesskiller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.view.WindowManager;

public class ScreenAdjustmentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        MainActivity mainActivity = (MainActivity) context;

        // Parlaklık ayarını yap
        if ("ADJUST_BRIGHTNESS".equals(intent.getAction())) {
            adjustBrightness(mainActivity);
        }
        // Mavi ışık filtresi ayarını yap
        else if ("ADJUST_FILTER".equals(intent.getAction())) {
            adjustBlueLightFilter(mainActivity);
        }
    }

    private void adjustBrightness(MainActivity mainActivity) {
        WindowManager.LayoutParams layoutParams = mainActivity.getWindow().getAttributes();
        layoutParams.screenBrightness = layoutParams.screenBrightness == 1.0f ? 0.5f : 1.0f; // 0.0 - 1.0 arası değer
        mainActivity.getWindow().setAttributes(layoutParams);
    }

    private void adjustBlueLightFilter(MainActivity mainActivity) {
        View overlayView = mainActivity.getOverlayView();
        int currentAlpha = overlayView.getBackground().getAlpha();
        int newAlpha = currentAlpha == 255 ? 100 : 255;
        overlayView.setBackgroundColor(Color.argb(newAlpha, 255, 100, 0)); // Transparan turuncu renk
    }
}
