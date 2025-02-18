package com.onur.motionsicknesskiller.motion;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class MotionSensorManager implements SensorEventListener {
    private static final float DEFAULT_ALPHA = 0.8f; // Varsayılan düşük geçiren filtre katsayısı
    private static final float NOISE_THRESHOLD = 0.1f; // Gürültü eşiği
    private static final float MOVEMENT_THRESHOLD = 0.2f; // Hareket eşiği
    
    private float currentAlpha = DEFAULT_ALPHA; // Dinamik filtre katsayısı
    private final SensorManager sensorManager;
    private final Sensor accelerometer;
    private final Sensor gyroscope;
    private final Sensor rotationVector;
    private final MotionDataListener listener;

    // Kalman Filtresi parametreleri
    private static final float Q_ANGLE = 0.001f;  // Process noise covariance for the accelerometer
    private static final float Q_BIAS = 0.003f;   // Process noise covariance for the gyroscope bias
    private static final float R_MEASURE = 0.03f;  // Measurement noise covariance

    // Kalman durumları
    private float[] angle = new float[3];  // Tahmin edilen açı
    private float[] bias = new float[3];   // Tahmin edilen sapma
    private float[][] P = new float[3][2];  // Hata kovaryans matrisi

    // Sensör verileri
    private float[] filteredAcceleration = new float[3];
    private float[] filteredRotation = new float[3];
    private float[] lastAcceleration = new float[3];
    private float[] rotationMatrix = new float[9];
    private float[] orientationAngles = new float[3];

    public interface MotionDataListener {
        void onMotionDataChanged(float[] acceleration, float[] rotation);
    }

    public MotionSensorManager(Context context, MotionDataListener listener) {
        this.listener = listener;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        
        // Kalman filtresi başlangıç değerleri
        for (int i = 0; i < 3; i++) {
            P[i][0] = 1.0f;
            P[i][1] = 1.0f;
        }
    }

    public void startListening() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_GAME);
    }

    public void stopListening() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                processAccelerometerData(event.values);
                break;
            case Sensor.TYPE_GYROSCOPE:
                processGyroscopeData(event.values);
                break;
            case Sensor.TYPE_ROTATION_VECTOR:
                processRotationVectorData(event.values);
                break;
        }

        // Sensör füzyonu ve filtreleme sonrası verileri gönder
        float[] fusedData = getFusedMotionData();
        listener.onMotionDataChanged(fusedData, orientationAngles);
    }

    private void processAccelerometerData(float[] values) {
        // Düşük geçiren filtre uygula
        for (int i = 0; i < 3; i++) {
            filteredAcceleration[i] = currentAlpha * filteredAcceleration[i] + (1 - currentAlpha) * values[i];
        }

        // Gürültü filtreleme
        for (int i = 0; i < 3; i++) {
            if (Math.abs(filteredAcceleration[i] - lastAcceleration[i]) < NOISE_THRESHOLD) {
                filteredAcceleration[i] = lastAcceleration[i];
            }
            lastAcceleration[i] = filteredAcceleration[i];
        }
    }

    private void processGyroscopeData(float[] values) {
        // Kalman filtresi uygula
        for (int i = 0; i < 3; i++) {
            float rate = values[i] - bias[i];
            angle[i] += rate * 0.01f; // dt = 0.01 saniye (100Hz örnekleme hızı varsayımı)

            // Hata kovaryans matrisini güncelle
            P[i][0] += -(P[i][1] + P[i][0]) * 0.01f;
            P[i][1] += -P[i][1] * 0.01f;
            P[i][0] += Q_ANGLE * 0.01f;
            P[i][1] += Q_BIAS * 0.01f;

            // Kalman kazancını hesapla
            float S = P[i][0] + R_MEASURE;
            float K0 = P[i][0] / S;
            float K1 = P[i][1] / S;

            // Açı ve sapma tahminlerini güncelle
            float innovation = filteredAcceleration[i] - angle[i];
            angle[i] += K0 * innovation;
            bias[i] += K1 * innovation;

            // Hata kovaryans matrisini güncelle
            P[i][0] *= (1 - K0);
            P[i][1] -= P[i][0] * K1;

            filteredRotation[i] = angle[i];
        }
    }

    private void processRotationVectorData(float[] values) {
        // Rotasyon vektöründen yönelim açılarını hesapla
        SensorManager.getRotationMatrixFromVector(rotationMatrix, values);
        SensorManager.getOrientation(rotationMatrix, orientationAngles);
    }

    private float[] getFusedMotionData() {
        float[] fusedData = new float[3];
        
        // Sensör verilerini birleştir
        for (int i = 0; i < 3; i++) {
            // Ağırlıklı ortalama ile füzyon
            fusedData[i] = 0.7f * filteredAcceleration[i] + 
                          0.2f * filteredRotation[i] + 
                          0.1f * orientationAngles[i];
            
            // Hareket eşiği uygula
            if (Math.abs(fusedData[i]) < MOVEMENT_THRESHOLD) {
                fusedData[i] = 0;
            }
        }
        
        return fusedData;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Sensör hassasiyeti değiştiğinde gerekli ayarlamaları yap
        if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            // Düşük hassasiyet durumunda filtreleme parametrelerini ayarla
            currentAlpha = 0.9f; // Daha agresif filtreleme
        } else {
            currentAlpha = DEFAULT_ALPHA; // Normal filtreleme
        }
    }
} 