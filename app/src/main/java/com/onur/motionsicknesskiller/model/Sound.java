package com.onur.motionsicknesskiller.model;

import android.net.Uri;

import androidx.annotation.NonNull;

import java.io.Serializable;

/**
 * Ses modelini temsil eden sınıf.
 */
public class Sound implements Serializable {
    
    private String id;
    private String title;
    private String category;
    private int imageResId;
    private String soundPath;
    private transient Uri soundUri;
    private boolean isPlaying;
    private boolean isLocal;
    private int type;

    /**
     * Varsayılan yapıcı metod.
     */
    public Sound() {
        // Boş yapıcı 
    }

    /**
     * Kaynak sesler için yapıcı metod.
     * 
     * @param id Benzersiz tanımlayıcı
     * @param title Ses başlığı
     * @param category Ses kategorisi
     * @param imageResId Görsel kaynak ID'si
     * @param soundPath Raw klasöründeki ses dosyası adı (.mp3 uzantısı olmadan)
     */
    public Sound(String id, String title, String category, int imageResId, String soundPath) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.imageResId = imageResId;
        this.soundPath = soundPath;
        this.isPlaying = false;
        this.isLocal = true;
    }
    
    /**
     * Kaynak sesler için yapıcı metod (kategori tipi ile).
     * 
     * @param id Benzersiz tanımlayıcı
     * @param title Ses başlığı
     * @param type Kategori tipi (SoundCategory.AMBIENT, SoundCategory.MUSIC, vb.)
     * @param imageResId Görsel kaynak ID'si
     * @param soundPath Raw klasöründeki ses dosyası adı (.mp3 uzantısı olmadan)
     * @param isLocal Dosyanın yerel olup olmadığı
     */
    public Sound(String id, String title, int type, int imageResId, String soundPath, boolean isLocal) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.imageResId = imageResId;
        this.soundPath = soundPath;
        this.isPlaying = false;
        this.isLocal = isLocal;
        
        // Kategori tipine göre kategori adını ata
        switch (type) {
            case SoundCategory.AMBIENT:
                this.category = "ambient";
                break;
            case SoundCategory.MUSIC:
                this.category = "music";
                break;
            case SoundCategory.NOISE:
                this.category = "noise";
                break;
            case SoundCategory.MEDITATION:
                this.category = "meditation";
                break;
            default:
                this.category = "other";
        }
    }

    /**
     * Uzak serverdan indirilen sesler için yapıcı metod.
     * 
     * @param id Benzersiz tanımlayıcı
     * @param title Ses başlığı
     * @param category Ses kategorisi
     * @param imageResId Görsel kaynak ID'si
     * @param soundUri Ses dosyasının URI'si
     */
    public Sound(String id, String title, String category, int imageResId, Uri soundUri) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.imageResId = imageResId;
        this.soundUri = soundUri;
        this.isPlaying = false;
        this.isLocal = false;
    }

    // Getter ve Setter metotları

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
    
    public int getType() {
        return type;
    }
    
    public void setType(int type) {
        this.type = type;
    }

    public int getImageResId() {
        return imageResId;
    }

    public void setImageResId(int imageResId) {
        this.imageResId = imageResId;
    }

    public String getSoundPath() {
        return soundPath;
    }

    public void setSoundPath(String soundPath) {
        this.soundPath = soundPath;
    }

    public Uri getSoundUri() {
        return soundUri;
    }

    public void setSoundUri(Uri soundUri) {
        this.soundUri = soundUri;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }

    public boolean isLocal() {
        return isLocal;
    }

    public void setLocal(boolean local) {
        isLocal = local;
    }

    @NonNull
    @Override
    public String toString() {
        return "Sound{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", category='" + category + '\'' +
                ", isPlaying=" + isPlaying +
                '}';
    }
} 