package com.onur.motionsicknesskiller.helpers;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.List;

/**
 * Uygulama izinlerini yönetmek için yardımcı sınıf.
 * Bu sınıf, farklı Android sürümlerinde izin isteme ve kontrol etme işlemlerini soyutlar.
 */
public class PermissionHelper {

    // İzin istek kodları
    public static final int SYSTEM_ALERT_WINDOW_PERMISSION_REQUEST_CODE = 2;
    public static final int WRITE_SETTINGS_PERMISSION_REQUEST_CODE = 3;
    public static final int ACCESSIBILITY_PERMISSION_REQUEST_CODE = 4;
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 5;
    public static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 6;
    
    /**
     * System alert window (ekran üzerine çizim) iznini kontrol eder.
     * @param context Uygulama bağlamı
     * @return İzin verilmiş ise true, aksi halde false
     */
    public static boolean hasSystemAlertWindowPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true; // Android 6.0'dan önce otomatik olarak verilir
    }
    
    /**
     * Sistem ayarlarını değiştirme iznini kontrol eder.
     * @param context Uygulama bağlamı
     * @return İzin verilmiş ise true, aksi halde false
     */
    public static boolean hasWriteSettingsPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.System.canWrite(context);
        }
        return true; // Android 6.0'dan önce otomatik olarak verilir
    }
    
    /**
     * Erişilebilirlik servisinin etkin olup olmadığını kontrol eder
     * @param context Uygulama konteksti
     * @param serviceName Servis tam adı
     * @return Servis etkinse true, değilse false
     */
    public static boolean isAccessibilityServiceEnabled(Context context, String serviceName) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        
        ComponentName expectedComponentName = new ComponentName(context, serviceName);
        String expectedString = expectedComponentName.flattenToString();
        
        for (AccessibilityServiceInfo service : enabledServices) {
            ServiceInfo serviceInfo = service.getResolveInfo().serviceInfo;
            ComponentName componentName = new ComponentName(serviceInfo.packageName, serviceInfo.name);
            String flatName = componentName.flattenToString();
            
            if (flatName.equals(expectedString)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Erişilebilirlik servisi için uygun intent oluşturur
     * @param context Uygulama konteksti
     * @param serviceName Erişilebilirlik servisinin adı
     * @return Oluşturulan intent
     */
    public static Intent getAccessibilityServiceIntent(Context context, String serviceName) {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        
        // Android 11 ve üzeri için intent iyileştirmesi
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            String fullServiceName = context.getPackageName() + "/" + serviceName;
            intent.setData(Uri.parse("com.android.settings.AccessibilitySettings:" + fullServiceName));
        }
        
        return intent;
    }
    
    /**
     * Konum izinlerini kontrol eder.
     * @param context Uygulama bağlamı
     * @return İzinler verilmiş ise true, aksi halde false
     */
    public static boolean hasLocationPermissions(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Android 6.0'dan önce otomatik olarak verilir
    }
    
    /**
     * Bildirim iznini kontrol eder (Android 13+).
     * @param context Uygulama bağlamı
     * @return İzin verilmiş ise true, aksi halde false
     */
    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Android 13'ten önce otomatik olarak verilir
    }
    
    /**
     * System alert window (ekran üzerine çizim) izni için ayarlar sayfasını açar.
     * @param activity Geçerli aktivite
     */
    public static void requestSystemAlertWindowPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(activity)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, 
                Uri.parse("package:" + activity.getPackageName()));
            activity.startActivityForResult(intent, SYSTEM_ALERT_WINDOW_PERMISSION_REQUEST_CODE);
        }
    }
    
    /**
     * Sistem ayarlarını değiştirme izni için ayarlar sayfasını açar.
     * @param activity Geçerli aktivite
     */
    public static void requestWriteSettingsPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(activity)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, 
                Uri.parse("package:" + activity.getPackageName()));
            activity.startActivityForResult(intent, WRITE_SETTINGS_PERMISSION_REQUEST_CODE);
        }
    }
    
    /**
     * Erişilebilirlik izni isteme
     * @param activity Aktivite
     * @param serviceName Erişilebilirlik servisinin tam adı
     */
    public static void requestAccessibilityPermission(Activity activity, String serviceName) {
        try {
            // Doğrudan erişilebilirlik servisimize yönlendirmeyi deneyelim
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            
            // Android 11 ve üzeri için daha spesifik intent
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                String fullServiceName = activity.getPackageName() + "/" + activity.getPackageName() + serviceName;
                intent.setData(Uri.parse("com.android.settings.AccessibilitySettings:" + fullServiceName));
            } else {
                // Diğer cihazlar için
                // Bazı üreticilerde arama parametresi ile doğrudan uygulamayı bulmaya yardımcı olabiliriz
                intent.putExtra("android.provider.extra.QUERY", "Motion");
            }
            
            activity.startActivityForResult(intent, ACCESSIBILITY_PERMISSION_REQUEST_CODE);
        } catch (Exception e) {
            // Eğer özel intent başarısız olursa, kullanıcıya rehberlik diyaloğu göster
            showPermissionGuide(activity, PermissionGuideDialogFragment.PERMISSION_ACCESSIBILITY);
        }
    }
    
    /**
     * Konum izinlerini ister.
     * @param activity Geçerli aktivite
     */
    public static void requestLocationPermissions(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(activity, new String[]{
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            }, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }
    
    /**
     * Bildirim iznini ister (Android 13+).
     * @param activity Geçerli aktivite
     */
    public static void requestNotificationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(activity, 
                new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 
                NOTIFICATION_PERMISSION_REQUEST_CODE);
        }
    }
    
    /**
     * Tüm gereken izinleri tek seferde istemeye çalışır.
     * @param activity Geçerli aktivite
     * @param serviceClassName Erişilebilirlik hizmetinin tam sınıf adı
     * @return İzin istendi mi? İzin istemediyse tüm izinler zaten verilmiş demektir.
     */
    public static boolean requestAllPermissions(Activity activity, String serviceClassName) {
        if (!hasSystemAlertWindowPermission(activity)) {
            requestSystemAlertWindowPermission(activity);
            return true;
        }
        
        if (!hasWriteSettingsPermission(activity)) {
            requestWriteSettingsPermission(activity);
            return true;
        }
        
        if (!isAccessibilityServiceEnabled(activity, serviceClassName)) {
            requestAccessibilityPermission(activity, serviceClassName);
            return true;
        }
        
        if (!hasLocationPermissions(activity)) {
            requestLocationPermissions(activity);
            return true;
        }
        
        if (!hasNotificationPermission(activity)) {
            requestNotificationPermission(activity);
            return true;
        }
        
        return false; // Tüm izinler zaten verilmiş
    }
    
    /**
     * Tüm gereken izinlerin verilip verilmediğini kontrol eder.
     * @param context Uygulama bağlamı
     * @param serviceClassName Erişilebilirlik hizmetinin tam sınıf adı
     * @return Tüm izinler verilmiş ise true, aksi halde false
     */
    public static boolean hasAllPermissions(Context context, String serviceClassName) {
        return hasSystemAlertWindowPermission(context) &&
               hasWriteSettingsPermission(context) &&
               isAccessibilityServiceEnabled(context, serviceClassName) &&
               hasLocationPermissions(context) &&
               hasNotificationPermission(context);
    }
    
    /**
     * Önemli izinlerin verilmesi gerektiğinde kullanıcıya açıklama yapacak PermissionGuideDialogFragment gösterir.
     * @param activity Geçerli aktivite
     * @param permissionType İzin türü (SYSTEM_OVERLAY, WRITE_SETTINGS, ACCESSIBILITY, vb.)
     */
    public static void showPermissionGuide(Activity activity, String permissionType) {
        PermissionGuideDialogFragment dialog = PermissionGuideDialogFragment.newInstance(permissionType);
        dialog.show(activity.getFragmentManager(), "PermissionGuideDialog");
    }
} 