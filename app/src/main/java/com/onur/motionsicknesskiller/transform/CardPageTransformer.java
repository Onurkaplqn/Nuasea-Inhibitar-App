package com.onur.motionsicknesskiller.transform;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

public class CardPageTransformer implements ViewPager2.PageTransformer {
    private static final float MIN_SCALE = 0.95f;
    private static final float MIN_ALPHA = 0.8f;
    private static final float MAX_ROTATE = 3f;

    @Override
    public void transformPage(@NonNull View page, float position) {
        float absPosition = Math.abs(position);

        if (position < -1) { // [-Infinity,-1)
            // Sayfa ekranın sol kısmının dışında
            page.setAlpha(0f);
            page.setTranslationZ(-1f);
        } else if (position <= 1) { // [-1,1]
            // Sayfa ekrandayken geçiş efektlerini uygula
            
            // Ölçeklendirme (tam ekran olduğu için az ölçeklendirme)
            float scaleFactor = Math.max(MIN_SCALE, 1 - absPosition * 0.05f);
            page.setScaleX(scaleFactor);
            page.setScaleY(scaleFactor);
            
            // Yükseklik
            float elevation = 0;
            if (position == 0) {
                elevation = 8f;
                page.setTranslationZ(elevation);
            } else {
                elevation = 4f - absPosition * 2f;
                page.setTranslationZ(elevation);
            }
            
            // Döndürme (hafif döndürme)
            page.setRotationY(position * MAX_ROTATE);
            
            // Saydam geçiş efekti
            page.setAlpha(Math.max(MIN_ALPHA, 1 - absPosition * 0.2f));
            
        } else { // (1,+Infinity]
            // Sayfa ekranın sağ kısmının dışında
            page.setAlpha(0f);
            page.setTranslationZ(-1f);
        }
    }
} 