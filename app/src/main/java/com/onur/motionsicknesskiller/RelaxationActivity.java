package com.onur.motionsicknesskiller;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.onur.motionsicknesskiller.adapter.CategoryAdapter;
import com.onur.motionsicknesskiller.adapter.SoundAdapter;
import com.onur.motionsicknesskiller.fragment.SoundMixerFragment;
import com.onur.motionsicknesskiller.fragment.SoundPlayerFragment;
import com.onur.motionsicknesskiller.model.Sound;
import com.onur.motionsicknesskiller.model.SoundCategory;
import com.onur.motionsicknesskiller.service.SoundService;

import java.util.ArrayList;
import java.util.List;

public class RelaxationActivity extends AppCompatActivity implements 
        SoundAdapter.OnSoundClickListener,
        SoundPlayerFragment.OnSoundPlayerListener,
        SoundMixerFragment.OnSoundMixerListener {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private CategoryAdapter categoryAdapter;
    
    private List<SoundCategory> categories;

    // Servis bağlantısı
    private SoundService soundService;
    private boolean serviceBound = false;
    
    // Aktif çalan ses
    private Sound currentSound;
    
    // Şu anda ekranda gösterilen fragment
    private SoundPlayerFragment playerFragment;
    private SoundMixerFragment mixerFragment;
    
    // Tercihler
    private SharedPreferences preferences;
    private static final String PREF_NAME = "relaxation_preferences";
    private static final String PREF_LAST_CATEGORY = "last_category";

    // Servis bağlantı nesnesi
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SoundService.LocalBinder binder = (SoundService.LocalBinder) service;
            soundService = binder.getService();
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Tam ekran modu için bildirim panelini gizle
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
                
        setContentView(R.layout.activity_relaxation);

        // Prefs
        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        
        // UI bileşenlerini tanımla
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        
        // Geri butonunu ayarla
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        // Kategorileri ayarla
        setupCategories();
        
        // ViewPager adaptörünü ayarla
        categoryAdapter = new CategoryAdapter(this, categories, this);
        viewPager.setAdapter(categoryAdapter);
        
        // TabLayout ile ViewPager'ı bağla
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(categories.get(position).getName());
        }).attach();
        
        // Servis bağlantısı
        startSoundService();
        bindSoundService();
        
        // Son seçilen kategoriye git
        int lastCategory = preferences.getInt(PREF_LAST_CATEGORY, 0);
        if (lastCategory < categories.size()) {
            viewPager.setCurrentItem(lastCategory);
        }
    }
    
    private void setupCategories() {
        categories = new ArrayList<>();
        
        // Ortam Sesleri
        List<Sound> ambientSounds = new ArrayList<>();
        ambientSounds.add(new Sound("amb_rain", "Yağmur", SoundCategory.AMBIENT, R.drawable.bg_rain, "rain_sound", true));
        ambientSounds.add(new Sound("amb_forest", "Orman", SoundCategory.AMBIENT, R.drawable.bg_forest, "forest_sound", true));
        ambientSounds.add(new Sound("amb_ocean", "Okyanus", SoundCategory.AMBIENT, R.drawable.bg_ocean, "ocean_sound", true));
        ambientSounds.add(new Sound("amb_thunder", "Gök Gürültüsü", SoundCategory.AMBIENT, R.drawable.bg_thunder, "thunder_sound", true));
        ambientSounds.add(new Sound("amb_fireplace", "Şömine", SoundCategory.AMBIENT, R.drawable.bg_fireplace, "fireplace_sound", true));
        ambientSounds.add(new Sound("amb_night", "Gece", SoundCategory.AMBIENT, R.drawable.bg_night, "night_sound", true));
        ambientSounds.add(new Sound("amb_wind", "Rüzgar", SoundCategory.AMBIENT, R.drawable.bg_wind, "wind_sound", true));
        categories.add(new SoundCategory("Ortam Sesleri", ambientSounds));
        
        // Müzik
        List<Sound> musicSounds = new ArrayList<>();
        musicSounds.add(new Sound("mus_piano", "Piyano", SoundCategory.MUSIC, R.drawable.bg_piano, "piano_sound", true));
        musicSounds.add(new Sound("mus_flute", "Flüt", SoundCategory.MUSIC, R.drawable.bg_flute, "flute_sound", true));
        musicSounds.add(new Sound("mus_guitar", "Gitar", SoundCategory.MUSIC, R.drawable.bg_guitar, "guitar_sound", true));
        musicSounds.add(new Sound("mus_violin", "Keman", SoundCategory.MUSIC, R.drawable.bg_violin, "violin_sound", true));
        musicSounds.add(new Sound("mus_harp", "Arp", SoundCategory.MUSIC, R.drawable.bg_harp, "harp_sound", true));
        categories.add(new SoundCategory("Müzik", musicSounds));
        
        // Renkli Gürültü
        List<Sound> noisesSounds = new ArrayList<>();
        noisesSounds.add(new Sound("noise_white", "Beyaz Gürültü", SoundCategory.NOISE, R.drawable.bg_white_noise, "white_noise_sound", true));
        noisesSounds.add(new Sound("noise_pink", "Pembe Gürültü", SoundCategory.NOISE, R.drawable.bg_pink_noise, "ink_noise_sound", true));
        noisesSounds.add(new Sound("noise_brown", "Kahverengi Gürültü", SoundCategory.NOISE, R.drawable.bg_brown_noise, "brown_noise_sound", true));
        categories.add(new SoundCategory("Renkli Gürültü", noisesSounds));
        
        // Meditasyon
        List<Sound> meditationSounds = new ArrayList<>();
        meditationSounds.add(new Sound("med_chant", "Şanlar", SoundCategory.MEDITATION, R.drawable.bg_chant, "chant_sound", true));
        meditationSounds.add(new Sound("med_om", "Om", SoundCategory.MEDITATION, R.drawable.bg_om, "om_sound", true));
        meditationSounds.add(new Sound("med_bowl", "Tibet Kasesi", SoundCategory.MEDITATION, R.drawable.bg_bowl, "bowl_sound", true));
        categories.add(new SoundCategory("Meditasyon", meditationSounds));
    }
    
    private void startSoundService() {
        Intent intent = new Intent(this, SoundService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        Log.d("RelaxationActivity", "Sound service started");
    }

    private void bindSoundService() {
        Intent intent = new Intent(this, SoundService.class);
        boolean bound = bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        Log.d("RelaxationActivity", "Sound service bind attempt: " + (bound ? "successful" : "failed"));
    }
    
    // SoundAdapter.OnSoundClickListener implementasyonu
    @Override
    public void onSoundClick(Sound sound) {
        currentSound = sound;
        
        // Log ile bilgi yaz
        Log.d("RelaxationActivity", "Sound clicked: " + sound.getTitle() + ", soundPath: " + sound.getSoundPath());
        
        // Servis bağlantısını kontrol et
        if (!serviceBound) {
            Log.d("RelaxationActivity", "Service not bound, rebinding...");
            startSoundService();
            bindSoundService();
        }
        
        // Ses çalma fragmentini göster
        playerFragment = SoundPlayerFragment.newInstance(sound);
        playerFragment.show(getSupportFragmentManager(), "sound_player");
    }

    @Override
    public void onDownloadClick(Sound sound) {
        // Uzak sesin indirilmesi için mantık
        // Bu projede tüm sesler yerel olduğu için uygulanmadı
    }
    
    // SoundPlayerFragment.OnSoundPlayerListener implementasyonu
    @Override
    public void onClosePlayer() {
        if (playerFragment != null) {
            playerFragment.dismiss();
            playerFragment = null;
        }
    }

    @Override
    public void onShowMixer() {
        /* Mixer özelliği geçici olarak devre dışı bırakıldı
        if (playerFragment != null) {
            playerFragment.dismiss();
        }
        
        // Mixer fragmentini göster
        mixerFragment = SoundMixerFragment.newInstance(currentSound);
        mixerFragment.show(getSupportFragmentManager(), "sound_mixer");
        */
        
        // Mixer devre dışı olduğunda kullanıcıya bilgi ver
        Toast.makeText(this, "Ses karıştırma özelliği şu anda kullanılamıyor", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPlayStateChanged(boolean isPlaying) {
        try {
            Log.d("RelaxationActivity", "onPlayStateChanged called with isPlaying: " + isPlaying);
            if (currentSound == null) {
                Log.e("RelaxationActivity", "currentSound is null!");
                return;
            }
            
            Log.d("RelaxationActivity", "Attempting to " + (isPlaying ? "play" : "pause") + 
                       " sound: " + currentSound.getTitle() + " with path: " + currentSound.getSoundPath());
            
            // Servis bağlı değilse yeniden bağlan
            if (!serviceBound) {
                startSoundService();
                bindSoundService();
                
                // Servis bağlanana kadar kısa bir süre bekle
                new Handler().postDelayed(() -> {
                    if (serviceBound) {
                        applySoundPlaybackChange(isPlaying);
                    } else {
                        Log.e("RelaxationActivity", "Service could not be bound");
                        Toast.makeText(this, "Ses servisi başlatılamadı", Toast.LENGTH_SHORT).show();
                    }
                }, 500); // 500ms bekle
            } else {
                applySoundPlaybackChange(isPlaying);
            }
        } catch (Exception e) {
            Log.e("RelaxationActivity", "Error in onPlayStateChanged: " + e.getMessage(), e);
            Toast.makeText(this, "Ses çalma hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    // Ses çalma/durdurma işlemini uygula
    private void applySoundPlaybackChange(boolean isPlaying) {
        try {
            if (isPlaying) {
                // Müzik çalınmıyorsa başlat
                if (!isSoundPlaying(currentSound)) {
                    Log.d("RelaxationActivity", "Starting sound: " + currentSound.getTitle());
                    SoundService.playSingleSound(this, currentSound);
                } else {
                    Log.d("RelaxationActivity", "Sound already playing: " + currentSound.getTitle());
                }
            } else {
                // Müzik çalınıyorsa durdur
                if (isSoundPlaying(currentSound)) {
                    Log.d("RelaxationActivity", "Pausing sound: " + currentSound.getTitle());
                    SoundService.pauseSingleSound(this, currentSound);
                } else {
                    Log.d("RelaxationActivity", "Sound is already paused: " + currentSound.getTitle());
                }
            }
        } catch (Exception e) {
            Log.e("RelaxationActivity", "Error applying sound playback change: " + e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void onTimerSelected(int minutes) {
        SoundService.setTimer(this, currentSound, minutes);
    }
    
    // SoundMixerFragment.OnSoundMixerListener implementasyonu
    @Override
    public void onMixerClosed() {
        if (mixerFragment != null) {
            mixerFragment.dismiss();
            mixerFragment = null;
        }
        
        // Ana sesle ses oynatıcı fragmentini tekrar göster
        if (currentSound != null) {
            playerFragment = SoundPlayerFragment.newInstance(currentSound);
            playerFragment.show(getSupportFragmentManager(), "sound_player");
        }
    }

    @Override
    public void onMainSoundVolumeChanged(int volume) {
        if (currentSound != null) {
            SoundService.setMainSoundVolume(this, currentSound, volume);
        }
    }

    @Override
    public void onAddSound(Sound sound) {
        SoundService.addSoundToMix(this, sound);
    }

    @Override
    public void onRemoveSound(Sound sound) {
        SoundService.removeSoundFromMix(this, sound);
    }

    @Override
    public void onSoundVolumeChanged(Sound sound, int volume) {
        SoundService.setMixedSoundVolume(this, sound, volume);
    }
    
    /**
     * Belirtilen sesin şu anda çalıp çalmadığını kontrol eder.
     * Bu metot, SoundPlayerFragment tarafından çağrılır.
     * 
     * @param sound Kontrol edilecek ses
     * @return Ses çalıyorsa true, aksi halde false
     */
    public boolean isSoundPlaying(Sound sound) {
        if (soundService != null && serviceBound && sound != null) {
            return soundService.isSoundPlaying(sound.getId());
        }
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        
        // Son görüntülenen kategoriyi kaydet
        preferences.edit().putInt(PREF_LAST_CATEGORY, viewPager.getCurrentItem()).apply();
    }

    @Override
    protected void onDestroy() {
        unbindSoundService();
        super.onDestroy();
    }

    private void unbindSoundService() {
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }
} 