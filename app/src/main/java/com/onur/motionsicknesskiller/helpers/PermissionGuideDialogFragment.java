package com.onur.motionsicknesskiller.helpers;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.onur.motionsicknesskiller.R;

/**
 * İzin gerektiren özellikler için kullanıcıya rehberlik eden dialog fragment.
 * Bu dialog, izinlerin nasıl verileceğini görsel ve yazılı olarak anlatır.
 */
public class PermissionGuideDialogFragment extends DialogFragment {

    // İzin Türleri
    public static final String PERMISSION_SYSTEM_OVERLAY = "system_overlay";
    public static final String PERMISSION_WRITE_SETTINGS = "write_settings";
    public static final String PERMISSION_ACCESSIBILITY = "accessibility";
    public static final String PERMISSION_LOCATION = "location";
    public static final String PERMISSION_NOTIFICATION = "notification";

    private static final String ARG_PERMISSION_TYPE = "permission_type";
    
    // Dialog içeriği
    private ImageView imageGuide;
    private TextView titleText;
    private TextView descriptionText;
    private TextView stepText1;
    private TextView stepText2;
    private TextView stepText3;
    private Button buttonPositive;
    private Button buttonNegative;
    
    public static PermissionGuideDialogFragment newInstance(String permissionType) {
        PermissionGuideDialogFragment fragment = new PermissionGuideDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PERMISSION_TYPE, permissionType);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_permission_guide, null);
        
        // View bileşenlerini tanımla
        imageGuide = view.findViewById(R.id.image_guide);
        titleText = view.findViewById(R.id.text_title);
        descriptionText = view.findViewById(R.id.text_description);
        stepText1 = view.findViewById(R.id.text_step1);
        stepText2 = view.findViewById(R.id.text_step2);
        stepText3 = view.findViewById(R.id.text_step3);
        buttonPositive = view.findViewById(R.id.button_positive);
        buttonNegative = view.findViewById(R.id.button_negative);
        
        // İzin türüne göre içeriği ayarla
        String permissionType = getArguments().getString(ARG_PERMISSION_TYPE);
        configureDialog(permissionType);
        
        builder.setView(view);
        return builder.create();
    }
    
    private void configureDialog(String permissionType) {
        switch (permissionType) {
            case PERMISSION_SYSTEM_OVERLAY:
                configureSystemOverlayPermission();
                break;
            case PERMISSION_WRITE_SETTINGS:
                configureWriteSettingsPermission();
                break;
            case PERMISSION_ACCESSIBILITY:
                configureAccessibilityPermission();
                break;
            case PERMISSION_LOCATION:
                configureLocationPermission();
                break;
            case PERMISSION_NOTIFICATION:
                configureNotificationPermission();
                break;
        }
        
        // İptal butonu her durumda aynı
        buttonNegative.setOnClickListener(v -> dismiss());
    }
    
    private void configureSystemOverlayPermission() {
        titleText.setText(getString(R.string.permission_title_system_overlay));
        descriptionText.setText(getString(R.string.permission_desc_system_overlay));
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            imageGuide.setImageResource(R.drawable.guide_overlay_permission);
            
            // Android 10+ için
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                stepText1.setText(getString(R.string.permission_guide_system_overlay_q));
                stepText2.setVisibility(View.GONE);
                stepText3.setVisibility(View.GONE);
            } 
            // Android 6.0 - 9.0 için
            else {
                stepText1.setText(getString(R.string.permission_guide_system_overlay_m));
                stepText2.setVisibility(View.GONE);
                stepText3.setVisibility(View.GONE);
            }
            
            buttonPositive.setText("Ayarlara Git");
            buttonPositive.setOnClickListener(v -> {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.setData(android.net.Uri.parse("package:" + getActivity().getPackageName()));
                startActivity(intent);
                dismiss();
            });
        } else {
            stepText1.setText("Bu Android sürümünde otomatik olarak izin verilmiştir.");
            stepText2.setVisibility(View.GONE);
            stepText3.setVisibility(View.GONE);
            buttonPositive.setText("Tamam");
            buttonPositive.setOnClickListener(v -> dismiss());
        }
    }
    
    private void configureWriteSettingsPermission() {
        titleText.setText(getString(R.string.permission_title_write_settings));
        descriptionText.setText(getString(R.string.permission_desc_write_settings));
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            imageGuide.setImageResource(R.drawable.guide_settings_permission);
            
            stepText1.setText(getString(R.string.permission_guide_write_settings));
            stepText2.setVisibility(View.GONE);
            stepText3.setVisibility(View.GONE);
            
            buttonPositive.setText("Ayarlara Git");
            buttonPositive.setOnClickListener(v -> {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(android.net.Uri.parse("package:" + getActivity().getPackageName()));
                startActivity(intent);
                dismiss();
            });
        } else {
            stepText1.setText("Bu Android sürümünde otomatik olarak izin verilmiştir.");
            stepText2.setVisibility(View.GONE);
            stepText3.setVisibility(View.GONE);
            buttonPositive.setText("Tamam");
            buttonPositive.setOnClickListener(v -> dismiss());
        }
    }
    
    private void configureAccessibilityPermission() {
        titleText.setText(getString(R.string.permission_title_accessibility));
        descriptionText.setText(getString(R.string.permission_desc_accessibility));
        
        imageGuide.setImageResource(R.drawable.guide_accessibility_permission);
        
        stepText1.setText(getString(R.string.permission_guide_accessibility));
        stepText2.setText("MotionSicknessKiller uygulamasını listede bulun ve dokunun");
        stepText3.setText("Hizmeti açık duruma getirin ve onaylayın");
        
        stepText2.setVisibility(View.VISIBLE);
        stepText3.setVisibility(View.VISIBLE);
        
        buttonPositive.setText("Ayarlara Git");
        buttonNegative.setText("Daha Fazla Yardım");
        
        buttonPositive.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                
                // API 30+ için android 11 ve üzeri
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    String packageName = getActivity().getPackageName();
                    String serviceName = packageName + ".MyAccessibilityService";
                    String fullName = packageName + "/" + serviceName;
                    
                    intent.setData(Uri.parse("package:" + packageName));
                    intent.putExtra(":settings:fragment_args_key", fullName);
                    intent.putExtra(":settings:show_fragment_args", true);
                }
                
                // Android 11'den düşük API seviyesi için
                else {
                    // Varsayılan erişilebilirlik ayarları ekranı
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                }
                
                startActivity(intent);
                dismiss();
            } catch (Exception e) {
                // Eğer özel intent çalışmazsa, genel erişilebilirlik ayarlarını aç
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
                dismiss();
            }
        });
        
        buttonNegative.setOnClickListener(v -> {
            String deviceBrand = getDeviceBrand();
            showDeviceSpecificAccessibilityHelp(deviceBrand);
        });
    }
    
    /**
     * Cihaz markasını belirler
     * @return Algılanan cihaz markası
     */
    private String getDeviceBrand() {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        String brand = Build.BRAND.toLowerCase();
        
        if (manufacturer.contains("xiaomi") || brand.contains("xiaomi") || 
            manufacturer.contains("redmi") || brand.contains("redmi") || 
            manufacturer.contains("poco") || brand.contains("poco")) {
            return "xiaomi";
        } else if (manufacturer.contains("samsung") || brand.contains("samsung")) {
            return "samsung";
        } else if (manufacturer.contains("huawei") || brand.contains("huawei") || 
                  manufacturer.contains("honor") || brand.contains("honor")) {
            return "huawei";
        } else if (manufacturer.contains("oppo") || brand.contains("oppo") || 
                  manufacturer.contains("realme") || brand.contains("realme")) {
            return "oppo";
        } else if (manufacturer.contains("oneplus") || brand.contains("oneplus")) {
            return "oneplus";
        } else if (manufacturer.contains("vivo") || brand.contains("vivo")) {
            return "vivo";
        } else {
            return "generic";
        }
    }
    
    /**
     * Cihaza özel erişilebilirlik yardım diyaloğunu gösterir
     * @param deviceBrand Cihaz markası
     */
    private void showDeviceSpecificAccessibilityHelp(String deviceBrand) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        
        switch (deviceBrand) {
            case "xiaomi":
                showXiaomiAccessibilityHelpDialog();
                return;
            case "samsung":
                builder.setTitle(getString(R.string.samsung_accessibility_guide_title))
                       .setMessage(getString(R.string.samsung_accessibility_guide_message));
                break;
            case "huawei":
                builder.setTitle(getString(R.string.huawei_accessibility_guide_title))
                       .setMessage(getString(R.string.huawei_accessibility_guide_message));
                break;
            case "oppo":
                builder.setTitle(getString(R.string.oppo_accessibility_guide_title))
                       .setMessage(getString(R.string.oppo_accessibility_guide_message));
                break;
            case "oneplus":
                builder.setTitle(getString(R.string.oneplus_accessibility_guide_title))
                       .setMessage(getString(R.string.oneplus_accessibility_guide_message));
                break;
            case "vivo":
                builder.setTitle(getString(R.string.vivo_accessibility_guide_title))
                       .setMessage(getString(R.string.vivo_accessibility_guide_message));
                break;
            default:
                builder.setTitle(getString(R.string.search_in_settings_title))
                       .setMessage(getString(R.string.search_in_settings_message));
                break;
        }
        
        builder.setPositiveButton("Ayarlara Git", (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
                dialog.dismiss();
                dismiss();
            })
            .setNeutralButton("Arama ile Bul", (dialog, which) -> {
                try {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("android.provider.extra.QUERY", "Motion");
                    startActivity(intent);
                    dialog.dismiss();
                    dismiss();
                } catch (Exception e) {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(intent);
                    dialog.dismiss();
                    dismiss();
                }
            })
            .setNegativeButton("İptal", (dialog, which) -> {
                dialog.dismiss();
            });
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    // Xiaomi cihazlar için özel yardım dialogu
    private void showXiaomiAccessibilityHelpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.xiaomi_accessibility_guide_title))
               .setMessage(getString(R.string.xiaomi_accessibility_guide_message))
               .setPositiveButton("Ayarlara Git", (dialog, which) -> {
                   Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                   startActivity(intent);
                   dialog.dismiss();
                   dismiss();
               })
               .setNeutralButton("Erişilebilirlik Servisi Ara", (dialog, which) -> {
                   try {
                       Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                       intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                       intent.putExtra("android.provider.extra.QUERY", "Motion");
                       startActivity(intent);
                       dialog.dismiss();
                       dismiss();
                   } catch (Exception e) {
                       Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                       startActivity(intent);
                       dialog.dismiss();
                       dismiss();
                   }
               })
               .setNegativeButton("İptal", (dialog, which) -> {
                   dialog.dismiss();
               });
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    // Erişilebilirlik yardım dialogu
    private void showAccessibilityHelpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.accessibility_find_service_title))
               .setMessage(getString(R.string.accessibility_find_service_message))
               .setPositiveButton("Anladım", (dialog, which) -> {
                   dialog.dismiss();
               })
               .setNegativeButton("Ayarlara Git", (dialog, which) -> {
                   Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                   startActivity(intent);
                   dialog.dismiss();
                   dismiss();
               });
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private void configureLocationPermission() {
        titleText.setText(getString(R.string.permission_title_location));
        descriptionText.setText(getString(R.string.permission_desc_location));
        
        imageGuide.setImageResource(R.drawable.guide_location_permission);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            stepText1.setText(getString(R.string.permission_guide_location));
            stepText2.setVisibility(View.GONE);
            stepText3.setVisibility(View.GONE);
            
            buttonPositive.setText("İzin İste");
            buttonPositive.setOnClickListener(v -> {
                PermissionHelper.requestLocationPermissions(getActivity());
                dismiss();
            });
        } else {
            stepText1.setText("Bu Android sürümünde otomatik olarak izin verilmiştir.");
            stepText2.setVisibility(View.GONE);
            stepText3.setVisibility(View.GONE);
            buttonPositive.setText("Tamam");
            buttonPositive.setOnClickListener(v -> dismiss());
        }
    }
    
    private void configureNotificationPermission() {
        titleText.setText(getString(R.string.permission_title_notification));
        descriptionText.setText(getString(R.string.permission_desc_notification));
        
        imageGuide.setImageResource(R.drawable.guide_notification_permission);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            stepText1.setText(getString(R.string.permission_guide_notification));
            stepText2.setVisibility(View.GONE);
            stepText3.setVisibility(View.GONE);
            
            buttonPositive.setText("İzin İste");
            buttonPositive.setOnClickListener(v -> {
                PermissionHelper.requestNotificationPermission(getActivity());
                dismiss();
            });
        } else {
            stepText1.setText("Bu Android sürümünde otomatik olarak izin verilmiştir.");
            stepText2.setVisibility(View.GONE);
            stepText3.setVisibility(View.GONE);
            buttonPositive.setText("Tamam");
            buttonPositive.setOnClickListener(v -> dismiss());
        }
    }
    
    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null) {
            getDialog().getWindow().setBackgroundDrawableResource(R.drawable.bg_rounded_dialog);
        }
    }
} 