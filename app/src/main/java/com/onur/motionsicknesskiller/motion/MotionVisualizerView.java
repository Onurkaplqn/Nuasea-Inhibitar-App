package com.onur.motionsicknesskiller.motion;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class MotionVisualizerView extends View {
    private static final int HORIZONTAL_POINTS = 4; // Sağ ve sol kenarlarda 4'er nokta
    private static final int VERTICAL_POINTS = 3; // Üst ve alt kenarlarda 3'er nokta
    private static final float BASE_POINT_RADIUS = 9f; // Daha küçük noktalar
    private static final float MIN_POINT_RADIUS = 2f;
    private static final float MAX_SPEED = 1.5f; // Daha yavaş hareket
    private static final float SPAWN_INTERVAL = 800f; // Daha seyrek nokta oluşturma
    private static final float SECONDARY_SPAWN_THRESHOLD = 0.4f; // Ara noktaların oluşmaya başlayacağı alpha değeri
    private static final float CENTER_THRESHOLD = 0.25f; // Daha erken yok olma
    private static final float VELOCITY_DAMPING = 0.99f; // Daha az sönümleme

    private final Paint pointPaint;
    private final List<MotionPoint> points;
    private float[] currentAcceleration = new float[3];
    private float[] currentRotation = new float[3];
    private long lastSpawnTime = 0;
    private int viewWidth, viewHeight;

    private static class MotionPoint {
        PointF position;
        PointF startPosition; // Başlangıç pozisyonu
        PointF velocity;
        float radius;
        float baseRadius;
        EdgeType edge;
        float alpha = 255; // Başlangıç opaklığı
        boolean isSecondary;
        private static final float MAX_DISTANCE_DP = 90f; // Maksimum hareket mesafesi
        private static final float FADE_START_DP = 30f; // Sönmeye başlama mesafesi

        enum EdgeType {
            LEFT, RIGHT, TOP, BOTTOM
        }

        MotionPoint(float x, float y, EdgeType edge, float radius, boolean isSecondary) {
            position = new PointF(x, y);
            startPosition = new PointF(x, y); // Başlangıç pozisyonunu kaydet
            velocity = new PointF(0, 0);
            this.edge = edge;
            this.baseRadius = radius;
            this.radius = radius;
            this.isSecondary = isSecondary;
        }

        void update(Context context, float[] acceleration, float[] rotation, int width, int height) {
            float centerX = width / 2f;
            float centerY = height / 2f;

            // Sadece ilgili eksende hareket et
            switch (edge) {
                case LEFT:
                    velocity.x = MAX_SPEED;
                    velocity.y = 0;
                    break;
                case RIGHT:
                    velocity.x = -MAX_SPEED;
                    velocity.y = 0;
                    break;
                case TOP:
                    velocity.x = 0;
                    velocity.y = MAX_SPEED;
                    break;
                case BOTTOM:
                    velocity.x = 0;
                    velocity.y = -MAX_SPEED;
                    break;
            }

            // Hızı sönümle
            velocity.x *= VELOCITY_DAMPING;
            velocity.y *= VELOCITY_DAMPING;

            // Pozisyonu güncelle
            position.x += velocity.x;
            position.y += velocity.y;

            // Merkeze olan uzaklığı hesapla
            float distanceToCenter = (float) Math.sqrt(
                Math.pow((position.x - centerX) / (width * 0.5f), 2) +
                Math.pow((position.y - centerY) / (height * 0.5f), 2)
            );

            // Başlangıç noktasından olan uzaklığı hesapla (px cinsinden)
            float density = context.getResources().getDisplayMetrics().density;
            float distanceFromStart = (float) Math.sqrt(
                Math.pow(position.x - startPosition.x, 2) +
                Math.pow(position.y - startPosition.y, 2)
            );
            
            // dp cinsinden mesafe
            float distanceInDp = distanceFromStart / density;

            // Merkeze yakınlık ve hareket mesafesine göre opaklık ve boyut hesapla
            float centerScale = Math.max((distanceToCenter - CENTER_THRESHOLD) / (1 - CENTER_THRESHOLD), 0);
            float moveScale = 1f;
            
            // 11dp ile 16dp arasında kademeli sönme
            if (distanceInDp > FADE_START_DP) {
                moveScale = Math.max(0, 1 - (distanceInDp - FADE_START_DP) / (MAX_DISTANCE_DP - FADE_START_DP));
            }

            // İki faktörü birleştir (hem merkeze yakınlık hem hareket mesafesi etkisi)
            float finalScale = Math.min(centerScale, moveScale);
            
            // Boyut ve opaklığı güncelle
            radius = baseRadius * (0.3f + (0.7f * finalScale)); // Minimum %30 boyut
            alpha = Math.max(0, Math.min(255, 255 * finalScale)); // Opaklığı ayarla
        }

        boolean isOutOfBounds(int width, int height) {
            return position.x < -radius || position.x > width + radius ||
                   position.y < -radius || position.y > height + radius;
        }

        boolean isTooCloseToCenter(int width, int height) {
            float centerX = width / 2f;
            float centerY = height / 2f;
            float distanceToCenter = (float) Math.sqrt(
                Math.pow((position.x - centerX) / (width * 0.5f), 2) +
                Math.pow((position.y - centerY) / (height * 0.5f), 2)
            );
            return distanceToCenter < CENTER_THRESHOLD || alpha < 10; // Çok şeffaf olduğunda da kaldır
        }

        boolean hasMovedTooFar(Context context) {
            float density = context.getResources().getDisplayMetrics().density;
            float distanceMoved = (float) Math.sqrt(
                Math.pow(position.x - startPosition.x, 2) +
                Math.pow(position.y - startPosition.y, 2)
            );
            
            return distanceMoved > (MAX_DISTANCE_DP * density);
        }

        boolean shouldRemove(Context context, int width, int height) {
            return isOutOfBounds(width, height) || 
                   isTooCloseToCenter(width, height) || 
                   hasMovedTooFar(context) || 
                   alpha < 5; // Daha düşük alpha değerinde kaldır
        }
    }

    public MotionVisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointPaint.setColor(Color.rgb(40, 40, 40));
        pointPaint.setAlpha(180);
        pointPaint.setShadowLayer(6, 0, 0, Color.argb(150, 0, 0, 0));
        points = new ArrayList<>();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        viewHeight = h;
        initializePoints();
    }

    private void initializePoints() {
        points.clear();
        spawnEdgePoints();
    }

    private void spawnEdgePoints() {
        float margin = BASE_POINT_RADIUS * 2;
        
        // Yatay kenarlar için aralık hesaplama (4 nokta için 5 eşit bölme)
        float horizontalSpacing = (viewHeight - 2 * margin) / (HORIZONTAL_POINTS + 1f);
        // Dikey kenarlar için aralık hesaplama (3 nokta için 4 eşit bölme)
        float verticalSpacing = (viewWidth - 2 * margin) / (VERTICAL_POINTS + 1f);

        // Sol ve sağ kenarlar (4'er nokta)
        for (int i = 1; i <= HORIZONTAL_POINTS; i++) {
            float yPos = margin + (i * horizontalSpacing);
            // Sol kenar
            points.add(new MotionPoint(margin, yPos,
                MotionPoint.EdgeType.LEFT, BASE_POINT_RADIUS, false));
            
            // Sağ kenar
            points.add(new MotionPoint(viewWidth - margin, yPos,
                MotionPoint.EdgeType.RIGHT, BASE_POINT_RADIUS, false));
        }

        // Üst ve alt kenarlar (3'er nokta)
        for (int i = 1; i <= VERTICAL_POINTS; i++) {
            float xPos = margin + (i * verticalSpacing);
            // Üst kenar
            points.add(new MotionPoint(xPos, margin,
                MotionPoint.EdgeType.TOP, BASE_POINT_RADIUS, false));
            
            // Alt kenar
            points.add(new MotionPoint(xPos, viewHeight - margin,
                MotionPoint.EdgeType.BOTTOM, BASE_POINT_RADIUS, false));
        }
    }

    private void spawnNewPoints() {
        // Mevcut nokta sayısını kontrol et
        int currentLeftPoints = 0, currentRightPoints = 0;
        int currentTopPoints = 0, currentBottomPoints = 0;

        for (MotionPoint point : points) {
            switch (point.edge) {
                case LEFT: currentLeftPoints++; break;
                case RIGHT: currentRightPoints++; break;
                case TOP: currentTopPoints++; break;
                case BOTTOM: currentBottomPoints++; break;
            }
        }

        float margin = BASE_POINT_RADIUS * 2;
        // Yatay ve dikey kenarlar için eşit aralıkları hesapla
        float horizontalSpacing = (viewHeight - 2 * margin) / (HORIZONTAL_POINTS + 1f);
        float verticalSpacing = (viewWidth - 2 * margin) / (VERTICAL_POINTS + 1f);

        // Hareket yönüne göre yeni noktalar oluştur
        if (Math.abs(currentAcceleration[0]) > 0.2f || Math.abs(currentAcceleration[1]) > 0.2f) {
            // Yatay hareket için (4'er nokta)
            for (int i = 1; i <= HORIZONTAL_POINTS; i++) {
                float yPos = margin + (i * horizontalSpacing);
                if (currentAcceleration[0] > 0 && currentRightPoints < HORIZONTAL_POINTS) { 
                    points.add(new MotionPoint(viewWidth - margin, yPos,
                        MotionPoint.EdgeType.RIGHT, BASE_POINT_RADIUS, false));
                    currentRightPoints++;
                } else if (currentAcceleration[0] < 0 && currentLeftPoints < HORIZONTAL_POINTS) {
                    points.add(new MotionPoint(margin, yPos,
                        MotionPoint.EdgeType.LEFT, BASE_POINT_RADIUS, false));
                    currentLeftPoints++;
                }
            }

            // Dikey hareket için (3'er nokta)
            for (int i = 1; i <= VERTICAL_POINTS; i++) {
                float xPos = margin + (i * verticalSpacing);
                if (currentAcceleration[1] > 0 && currentBottomPoints < VERTICAL_POINTS) {
                    points.add(new MotionPoint(xPos, viewHeight - margin,
                        MotionPoint.EdgeType.BOTTOM, BASE_POINT_RADIUS, false));
                    currentBottomPoints++;
                } else if (currentAcceleration[1] < 0 && currentTopPoints < VERTICAL_POINTS) {
                    points.add(new MotionPoint(xPos, margin,
                        MotionPoint.EdgeType.TOP, BASE_POINT_RADIUS, false));
                    currentTopPoints++;
                }
            }
        }
    }

    public void updateMotionData(float[] acceleration, float[] rotation) {
        this.currentAcceleration = acceleration;
        this.currentRotation = rotation;
        updatePoints();
        invalidate();
    }

    private void updatePoints() {
        long currentTime = System.currentTimeMillis();
        List<MotionPoint> pointsToRemove = new ArrayList<>();
        boolean shouldSpawnSecondary = false;
        boolean hasMainPoints = false;
        boolean hasSecondaryPoints = false;

        // Mevcut noktaları kontrol et
        for (MotionPoint point : points) {
            point.update(getContext(), currentAcceleration, currentRotation, viewWidth, viewHeight);
            
            if (!point.isSecondary) {
                hasMainPoints = true;
                // Ana noktaların alpha değeri düştüğünde ara noktaları oluştur
                if (point.alpha <= 255 * SECONDARY_SPAWN_THRESHOLD && !hasSecondaryPoints) {
                    shouldSpawnSecondary = true;
                }
            } else {
                hasSecondaryPoints = true;
            }
            
            // Noktayı kaldırma koşullarını kontrol et
            if (point.shouldRemove(getContext(), viewWidth, viewHeight)) {
                pointsToRemove.add(point);
            }
        }

        points.removeAll(pointsToRemove);

        // Ara noktaları oluştur
        if (shouldSpawnSecondary && !hasSecondaryPoints) {
            spawnSecondaryPoints();
        }

        // Ana noktaları oluştur
        if (currentTime - lastSpawnTime > SPAWN_INTERVAL && !hasMainPoints) {
            spawnNewPoints();
            lastSpawnTime = currentTime;
        }
    }

    private void spawnSecondaryPoints() {
        List<MotionPoint> mainPoints = new ArrayList<>();
        
        // Aynı kenardaki ana noktaları grupla
        for (MotionPoint point : points) {
            if (!point.isSecondary) {
                mainPoints.add(point);
            }
        }

        // Her iki ana nokta arasına bir ara nokta ekle
        for (int i = 0; i < mainPoints.size() - 1; i++) {
            MotionPoint p1 = mainPoints.get(i);
            MotionPoint p2 = mainPoints.get(i + 1);
            
            // Sadece aynı kenardaki noktalar arasına ara nokta ekle
            if (p1.edge == p2.edge) {
                float x = (p1.position.x + p2.position.x) / 2;
                float y = (p1.position.y + p2.position.y) / 2;
                
                points.add(new MotionPoint(x, y, p1.edge, BASE_POINT_RADIUS * 0.7f, true));
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.TRANSPARENT);
        
        for (MotionPoint point : points) {
            // Noktanın kendi alpha değerini kullan
            pointPaint.setAlpha((int) point.alpha);
            
            // Gölge efekti için alpha değerini kullan
            pointPaint.setShadowLayer(4, 0, 0, Color.argb((int)(point.alpha * 0.6f), 0, 0, 0));
            canvas.drawCircle(point.position.x, point.position.y, 
                point.radius + 2, pointPaint);
            
            pointPaint.setShadowLayer(0, 0, 0, 0);
            canvas.drawCircle(point.position.x, point.position.y, 
                point.radius, pointPaint);
        }
        
        postInvalidateOnAnimation();
    }
} 