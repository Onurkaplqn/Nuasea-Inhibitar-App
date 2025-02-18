// Tek bir Receiver sınıfı
package com.onur.motionsicknesskiller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.widget.Toast;

public class ScreenAdjustmentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case "ADJUST_BRIGHTNESS":
                    try {
                        // Sistem ayarlarını değiştirme izni kontrolü
                        if (Settings.System.canWrite(context)) {
                            // Mevcut parlaklık değerini al
                            int currentBrightness = Settings.System.getInt(
                                    context.getContentResolver(),
                                    Settings.System.SCREEN_BRIGHTNESS
                            );
                            
                            // Parlaklığı değiştir (örnek olarak %50)
                            int newBrightness = Math.min(255, currentBrightness + 25);
                            Settings.System.putInt(
                                    context.getContentResolver(),
                                    Settings.System.SCREEN_BRIGHTNESS,
                                    newBrightness
                            );
                            
                            Toast.makeText(context, "Parlaklık ayarlandı", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "Sistem ayarları izni gerekli", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Settings.SettingNotFoundException e) {
                        Toast.makeText(context, "Parlaklık ayarlanamadı", Toast.LENGTH_SHORT).show();
                    }
                    break;

                case "ADJUST_FILTER":
                    // Mavi ışık filtresi ayarları
                    Intent filterIntent = new Intent(context, MainActivity.class);
                    filterIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(filterIntent);
                    break;
            }
        }
    }
}
