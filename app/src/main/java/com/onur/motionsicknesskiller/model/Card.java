package com.onur.motionsicknesskiller.model;

import android.graphics.Color;

public class Card {
    // Kart tipleri
    public static final int TYPE_RELAXATION = 0;
    public static final int TYPE_NIGHT_MODE = 1;
    public static final int TYPE_BLUE_LIGHT = 2;

    // Kart renkleri
    public static final int COLOR_RELAXATION = Color.parseColor("#000000"); // Siyah
    public static final int COLOR_NIGHT_MODE = Color.parseColor("#000000"); // Siyah

    private String title;
    private String description;
    private int iconResourceId;
    private int colorResourceId;
    private int type;
    private String lottieFileName; // Yeni eklenen alan

    public Card(String title, String description, int iconResourceId, int colorResourceId, int type, String lottieFileName) {
        this.title = title;
        this.description = description;
        this.iconResourceId = iconResourceId;
        this.colorResourceId = colorResourceId;
        this.type = type;
        this.lottieFileName = lottieFileName;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getIconResourceId() {
        return iconResourceId;
    }

    public int getColorResourceId() {
        return colorResourceId;
    }

    public int getType() {
        return type;
    }
    
    public String getLottieFileName() {
        return lottieFileName;
    }
} 