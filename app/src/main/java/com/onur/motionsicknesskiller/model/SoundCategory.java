package com.onur.motionsicknesskiller.model;

import java.io.Serializable;
import java.util.List;

/**
 * Ses kategorisi model sınıfı
 * Örnek: Ortam Sesleri, Müzik, Meditasyon, vb.
 */
public class SoundCategory implements Serializable {
    
    // Serileştirme için versiyon
    private static final long serialVersionUID = 1L;
    
    // Kategori türleri
    public static final int AMBIENT = 0;
    public static final int MUSIC = 1;
    public static final int NOISE = 2;
    public static final int MEDITATION = 3;
    
    private String name;
    private List<Sound> sounds;
    
    public SoundCategory(String name, List<Sound> sounds) {
        this.name = name;
        this.sounds = sounds;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public List<Sound> getSounds() {
        return sounds;
    }
    
    public void setSounds(List<Sound> sounds) {
        this.sounds = sounds;
    }
}