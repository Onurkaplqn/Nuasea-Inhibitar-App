package com.onur.motionsicknesskiller.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.onur.motionsicknesskiller.MainActivity;
import com.onur.motionsicknesskiller.R;
import com.onur.motionsicknesskiller.RelaxationActivity;
import com.onur.motionsicknesskiller.model.Sound;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Arka planda ses çalmak için servis sınıfı.
 * Bu servis, birden fazla sesi aynı anda çalabilir ve farklı ses seviyelerini destekler.
 */
public class SoundService extends Service {

    private static final String TAG = "SoundService";
    private static final String CHANNEL_ID = "sound_service_channel";
    private static final int NOTIFICATION_ID = 101;

    private static final int DEFAULT_VOLUME = 75;
    
    // Servis Action sabitleri
    private static final String ACTION_PLAY_SOUND = "PLAY_SINGLE_SOUND";
    private static final String ACTION_PAUSE_SOUND = "PAUSE_SINGLE_SOUND";
    private static final String ACTION_SET_MAIN_SOUND_VOLUME = "SET_MAIN_SOUND_VOLUME";
    private static final String ACTION_SET_MIXED_SOUND_VOLUME = "SET_MIXED_SOUND_VOLUME";
    private static final String ACTION_ADD_TO_MIX = "ADD_SOUND_TO_MIX";
    private static final String ACTION_REMOVE_FROM_MIX = "REMOVE_SOUND_FROM_MIX";
    private static final String ACTION_RESET_MIXER = "RESET_MIXER";
    private static final String ACTION_SET_TIMER = "SET_TIMER";
    private static final String ACTION_STOP_SERVICE = "STOP_SERVICE";

    // Extras sabitleri
    private static final String EXTRA_SOUND = "sound";
    private static final String EXTRA_VOLUME = "volume";
    private static final String EXTRA_TIMER_MINUTES = "minutes";

    // Çalınan tüm sesleri izlemek için
    private final Map<String, MediaPlayer> players = new HashMap<>();
    private final Map<String, Integer> volumes = new HashMap<>();
    private final Map<String, Integer> timerMinutes = new HashMap<>();

    // Ana ses (UI'da görüntülenen ve çalan ses)
    private Sound mainSound;
    private boolean isMainSoundPlaying = false;

    // Binder
    private final IBinder binder = new LocalBinder();

    // Zamanlayıcı
    private final Handler timerHandler = new Handler();
    private final Map<String, Runnable> timerRunnables = new HashMap<>();

    /**
     * Local binder sınıfı
     */
    public class LocalBinder extends Binder {
        public SoundService getService() {
            return SoundService.this;
        }
    }

    // Statik metotlar - Diğer bileşenlerden erişim için
    
    /**
     * Tek bir ses dosyasını çalmaya başlar.
     * 
     * @param context Uygulama bağlamı
     * @param sound Çalınacak ses
     */
    public static void playSingleSound(Context context, Sound sound) {
        Intent intent = new Intent(context, SoundService.class);
        intent.setAction(ACTION_PLAY_SOUND);
        intent.putExtra(EXTRA_SOUND, sound);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }
    
    /**
     * Çalan sesi duraklatır.
     * 
     * @param context Uygulama bağlamı
     * @param sound Duraklatılacak ses
     */
    public static void pauseSingleSound(Context context, Sound sound) {
        Intent intent = new Intent(context, SoundService.class);
        intent.setAction(ACTION_PAUSE_SOUND);
        intent.putExtra(EXTRA_SOUND, sound);
        context.startService(intent);
    }
    
    /**
     * Ana ses seviyesini ayarlar.
     * 
     * @param context Uygulama bağlamı
     * @param sound Ana ses
     * @param volume Ses seviyesi (0-100)
     */
    public static void setMainSoundVolume(Context context, Sound sound, int volume) {
        Intent intent = new Intent(context, SoundService.class);
        intent.setAction(ACTION_SET_MAIN_SOUND_VOLUME);
        intent.putExtra(EXTRA_SOUND, sound);
        intent.putExtra(EXTRA_VOLUME, volume);
        context.startService(intent);
    }
    
    /**
     * Karışımdaki bir sesin seviyesini ayarlar.
     * 
     * @param context Uygulama bağlamı
     * @param sound Ses
     * @param volume Ses seviyesi (0-100)
     */
    public static void setMixedSoundVolume(Context context, Sound sound, int volume) {
        Intent intent = new Intent(context, SoundService.class);
        intent.setAction(ACTION_SET_MIXED_SOUND_VOLUME);
        intent.putExtra(EXTRA_SOUND, sound);
        intent.putExtra(EXTRA_VOLUME, volume);
        context.startService(intent);
    }
    
    /**
     * Karışıma yeni bir ses ekler.
     * 
     * @param context Uygulama bağlamı
     * @param sound Eklenecek ses
     */
    public static void addSoundToMix(Context context, Sound sound) {
        Intent intent = new Intent(context, SoundService.class);
        intent.setAction(ACTION_ADD_TO_MIX);
        intent.putExtra(EXTRA_SOUND, sound);
        context.startService(intent);
    }
    
    /**
     * Karışımdan bir sesi kaldırır.
     * 
     * @param context Uygulama bağlamı
     * @param sound Kaldırılacak ses
     */
    public static void removeSoundFromMix(Context context, Sound sound) {
        Intent intent = new Intent(context, SoundService.class);
        intent.setAction(ACTION_REMOVE_FROM_MIX);
        intent.putExtra(EXTRA_SOUND, sound);
        context.startService(intent);
    }
    
    /**
     * Tüm karışım ayarlarını sıfırlar.
     * 
     * @param context Uygulama bağlamı
     */
    public static void resetMixer(Context context) {
        Intent intent = new Intent(context, SoundService.class);
        intent.setAction(ACTION_RESET_MIXER);
        context.startService(intent);
    }
    
    /**
     * Zamanlayıcıyı ayarlar. Bu süre sonunda tüm sesler duracak.
     * 
     * @param context Uygulama bağlamı
     * @param sound Ana ses
     * @param minutes Kaç dakika sonra duracak (0 = zamanlayıcı kapalı)
     */
    public static void setTimer(Context context, Sound sound, int minutes) {
        Intent intent = new Intent(context, SoundService.class);
        intent.setAction(ACTION_SET_TIMER);
        intent.putExtra(EXTRA_SOUND, sound);
        intent.putExtra(EXTRA_TIMER_MINUTES, minutes);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        
        // App başlatılırken timeout olmaması için immediate notification göster
        Notification defaultNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Ses Servisi")
                .setContentText("Servis başlatılıyor...")
                .setSmallIcon(R.drawable.ic_play)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
                
        startForeground(NOTIFICATION_ID, defaultNotification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand called");
        
        // Servisin hızlıca başlatılması için bildirim oluştur
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Ses Servisi")
                .setContentText("Servis başlatılıyor...")
                .setSmallIcon(R.drawable.ic_play)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
        
        // Servisi hemen önplan olarak başlat
        startForeground(NOTIFICATION_ID, notification);
        
        if (intent != null) {
            final String action = intent.getAction();
            
            // Arka planda işlemleri yap - UI thread'i bloklama
            new Thread(() -> {
                try {
                    if (action != null) {
                        switch (action) {
                            case ACTION_PLAY_SOUND:
                                handlePlaySingleSound(intent);
                                break;
                            case ACTION_PAUSE_SOUND:
                                handlePauseSingleSound(intent);
                                break;
                            case ACTION_SET_MAIN_SOUND_VOLUME:
                                handleSetMainSoundVolume(intent);
                                break;
                            case ACTION_SET_MIXED_SOUND_VOLUME:
                                handleSetMixedSoundVolume(intent);
                                break;
                            case ACTION_ADD_TO_MIX:
                                handleAddSoundToMix(intent);
                                break;
                            case ACTION_REMOVE_FROM_MIX:
                                handleRemoveSoundFromMix(intent);
                                break;
                            case ACTION_RESET_MIXER:
                                handleResetMixer();
                                break;
                            case ACTION_SET_TIMER:
                                handleSetTimer(intent);
                                break;
                            case ACTION_STOP_SERVICE:
                                stopSelf();
                                break;
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing service action: " + action, e);
                }
            }).start();
        }
        
        return START_STICKY;
    }

    private void handlePlaySingleSound(Intent intent) {
        Sound sound = (Sound) intent.getSerializableExtra(EXTRA_SOUND);
        if (sound == null) {
            Log.e(TAG, "handlePlaySingleSound: Sound is null");
            return;
        }
        
        Log.d(TAG, "handlePlaySingleSound - Attempting to play sound: " + sound.getTitle() + ", path: " + sound.getSoundPath());
        
        String soundId = sound.getId();
        
        // Tüm diğer sesleri durdur (gerçekten durduğundan emin ol)
        stopAllSounds();
        
        // Ana ses olarak bu sesi ayarla
        mainSound = sound;
        isMainSoundPlaying = true;
        
        // MediaPlayer zaten varsa, durumunu kontrol et
        if (players.containsKey(soundId)) {
            MediaPlayer player = players.get(soundId);
            if (player != null) {
                try {
                    Log.d(TAG, "Starting existing MediaPlayer");
                    // Eğer player daha önce hazırlandıysa ve şu anda çalmıyorsa
                    if (!player.isPlaying()) {
                        player.seekTo(0);
                        player.start();
                        Log.d(TAG, "MediaPlayer started successfully");
                    } else {
                        Log.d(TAG, "MediaPlayer is already playing");
                    }
                    
                    // Bildirim güncelle
                    updateNotification();
                } catch (Exception e) {
                    Log.e(TAG, "Error playing with existing MediaPlayer: " + e.getMessage(), e);
                    // Hata durumunda bu player'ı sil ve yeni oluştur
                    player.release();
                    players.remove(soundId);
                    playNewSound(sound);
                }
            } else {
                Log.e(TAG, "MediaPlayer is null despite being in players map");
                players.remove(soundId);
                playNewSound(sound);
            }
        } else {
            // Yeni MediaPlayer oluştur
            playNewSound(sound);
        }
    }
    
    /**
     * Tüm çalan sesleri durdurur.
     * Bu metot, tüm MediaPlayer nesnelerini durdurur ve sıfırlar.
     */
    private void stopAllSounds() {
        Log.d(TAG, "Stopping all sounds");
        
        // Tüm player'ları döngüyle durdur
        for (Map.Entry<String, MediaPlayer> entry : new HashMap<>(players).entrySet()) {
            try {
                String id = entry.getKey();
                MediaPlayer player = entry.getValue();
                
                if (player != null) {
                    if (player.isPlaying()) {
                        player.pause();
                        player.seekTo(0);
                        Log.d(TAG, "Stopped and reset player for sound ID: " + id);
                    }
                } else {
                    // Null player'ları map'ten kaldır
                    players.remove(id);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error stopping player: " + e.getMessage(), e);
            }
        }
        
        // Ana sesin durumu güncelle
        if (mainSound != null) {
            isMainSoundPlaying = false;
        }
    }

    private void playNewSound(Sound sound) {
        try {
            Log.d(TAG, "Creating new MediaPlayer for: " + sound.getTitle());
            MediaPlayer player = createMediaPlayer(sound);
            
            String soundId = sound.getId();
            
            if (player != null) {
                player.setOnPreparedListener(mp -> {
                    Log.d(TAG, "MediaPlayer prepared asynchronously, ready to play: " + sound.getTitle());
                    
                    // Player'ı kaydet
                    players.put(soundId, mp);
                    
                    // Varsayılan ses seviyesini ayarla
                    int volume = DEFAULT_VOLUME;
                    volumes.put(soundId, volume);
                    setPlayerVolume(mp, volume);
                    
                    // Sesi başlat
                    if (isMainSoundPlaying) {
                        mp.start();
                        Log.d(TAG, "MediaPlayer started playing: " + sound.getTitle());
                        
                        // Bildirim göster
                        startForeground(NOTIFICATION_ID, createNotification(sound));
                    }
                });
                
                player.setOnErrorListener((mp, what, extra) -> {
                    Log.e(TAG, "MediaPlayer error: what=" + what + ", extra=" + extra);
                    players.remove(soundId);
                    return false;
                });
                
                // Hazırlama işlemini başlat
                player.prepareAsync();
            } else {
                Log.e(TAG, "Failed to create MediaPlayer for: " + sound.getTitle());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating and starting MediaPlayer: " + e.getMessage(), e);
        }
    }

    private void handlePauseSingleSound(Intent intent) {
        Sound sound = (Sound) intent.getSerializableExtra(EXTRA_SOUND);
        if (sound == null) return;
        
        isMainSoundPlaying = false;
        
        String soundId = sound.getId();
        
        // MediaPlayer varsa, çalışıyorsa durdur
        if (players.containsKey(soundId)) {
            MediaPlayer player = players.get(soundId);
            if (player != null && player.isPlaying()) {
                player.pause();
            }
        }
        
        // Tüm sesler durduysa, bildirim güncelle veya servisi arka plandan çıkar
        boolean anyPlaying = isAnyMediaPlayerPlaying();
        if (!anyPlaying) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE);
            } else {
                stopForeground(true);
            }
        } else {
            // Bildirim güncelle
            updateNotification();
        }
    }

    private void handleSetMainSoundVolume(Intent intent) {
        Sound sound = (Sound) intent.getSerializableExtra(EXTRA_SOUND);
        int volume = intent.getIntExtra(EXTRA_VOLUME, DEFAULT_VOLUME);
        
        if (sound == null) return;
        
        String soundId = sound.getId();
        
        // Ses seviyesini kaydet
        volumes.put(soundId, volume);
        
        // MediaPlayer varsa, ses seviyesini ayarla
        if (players.containsKey(soundId)) {
            MediaPlayer player = players.get(soundId);
            if (player != null) {
                setPlayerVolume(player, volume);
            }
        }
    }

    private void handleSetMixedSoundVolume(Intent intent) {
        Sound sound = (Sound) intent.getSerializableExtra(EXTRA_SOUND);
        int volume = intent.getIntExtra(EXTRA_VOLUME, DEFAULT_VOLUME);
        
        if (sound == null) return;
        
        String soundId = sound.getId();
        
        // Ses seviyesini kaydet
        volumes.put(soundId, volume);
        
        // MediaPlayer varsa, ses seviyesini ayarla
        if (players.containsKey(soundId)) {
            MediaPlayer player = players.get(soundId);
            if (player != null) {
                setPlayerVolume(player, volume);
            }
        }
    }

    private void handleAddSoundToMix(Intent intent) {
        Sound sound = (Sound) intent.getSerializableExtra(EXTRA_SOUND);
        if (sound == null) return;
        
        String soundId = sound.getId();
        
        // Ana ses çalmıyorsa, mikse ses ekleme
        if (mainSound == null || !isMainSoundPlaying) {
            Log.d(TAG, "Main sound is not playing, cannot add to mix");
            return;
        }
        
        // Ana ses ile aynı sesi tekrar eklemeyi önle
        if (mainSound.getId().equals(soundId)) {
            Log.d(TAG, "Cannot add main sound to mix again");
            return;
        }
        
        // MediaPlayer zaten varsa, çalışmıyorsa başlat
        if (players.containsKey(soundId)) {
            MediaPlayer player = players.get(soundId);
            if (player != null && !player.isPlaying()) {
                player.start();
            }
        } else {
            // Yeni MediaPlayer oluştur
            try {
                Log.d(TAG, "Creating new MediaPlayer for mix: " + sound.getTitle());
                MediaPlayer player = createMediaPlayer(sound);
                
                if (player != null) {
                    player.setOnPreparedListener(mp -> {
                        Log.d(TAG, "Mix MediaPlayer prepared asynchronously, ready to play: " + sound.getTitle());
                        
                        // Player'ı kaydet
                        players.put(soundId, mp);
                        
                        // Karışım için daha düşük ses seviyesini ayarla
                        int volume = 50; // Karışımdaki sesler için daha düşük varsayılan değer
                        volumes.put(soundId, volume);
                        setPlayerVolume(mp, volume);
                        
                        // Sesi başlat
                        mp.start();
                        Log.d(TAG, "Mix MediaPlayer started playing: " + sound.getTitle());
                        
                        // Bildirim güncelle
                        updateNotification();
                    });
                    
                    player.setOnErrorListener((mp, what, extra) -> {
                        Log.e(TAG, "Mix MediaPlayer error: what=" + what + ", extra=" + extra);
                        players.remove(soundId);
                        return false;
                    });
                    
                    // Hazırlama işlemini başlat
                    player.prepareAsync();
                } else {
                    Log.e(TAG, "Failed to create mix MediaPlayer for: " + sound.getTitle());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error adding sound to mix: " + e.getMessage(), e);
            }
        }
    }

    private void handleRemoveSoundFromMix(Intent intent) {
        Sound sound = (Sound) intent.getSerializableExtra(EXTRA_SOUND);
        if (sound == null) return;
        
        String soundId = sound.getId();
        
        // Ana ses değilse sil
        if (mainSound != null && !soundId.equals(mainSound.getId())) {
            // MediaPlayer varsa, durdur ve sil
            if (players.containsKey(soundId)) {
                MediaPlayer player = players.get(soundId);
                if (player != null) {
                    if (player.isPlaying()) {
                        player.stop();
                    }
                    player.release();
                }
                
                players.remove(soundId);
                volumes.remove(soundId);
                
                // Zamanlayıcı varsa kaldır
                if (timerRunnables.containsKey(soundId)) {
                    timerHandler.removeCallbacks(timerRunnables.get(soundId));
                    timerRunnables.remove(soundId);
                    timerMinutes.remove(soundId);
                }
            }
        }
    }

    private void handleResetMixer() {
        // Ana sesi koru, diğerlerini temizle
        String mainSoundId = mainSound != null ? mainSound.getId() : null;
        
        // Tüm sesleri tek tek kontrol et
        for (String soundId : new HashMap<>(players).keySet()) {
            // Ana ses değilse sil
            if (mainSoundId == null || !soundId.equals(mainSoundId)) {
                MediaPlayer player = players.get(soundId);
                if (player != null) {
                    if (player.isPlaying()) {
                        player.stop();
                    }
                    player.release();
                }
                
                players.remove(soundId);
                volumes.remove(soundId);
                
                // Zamanlayıcı varsa kaldır
                if (timerRunnables.containsKey(soundId)) {
                    timerHandler.removeCallbacks(timerRunnables.get(soundId));
                    timerRunnables.remove(soundId);
                    timerMinutes.remove(soundId);
                }
            }
        }
        
        // Ana ses seviyesini varsayılana ayarla
        if (mainSoundId != null) {
            volumes.put(mainSoundId, DEFAULT_VOLUME);
            MediaPlayer player = players.get(mainSoundId);
            if (player != null) {
                setPlayerVolume(player, DEFAULT_VOLUME);
            }
        }
        
        // Bildirim güncelle
        updateNotification();
    }

    private void handleSetTimer(Intent intent) {
        Sound sound = (Sound) intent.getSerializableExtra(EXTRA_SOUND);
        int minutes = intent.getIntExtra(EXTRA_TIMER_MINUTES, 0);
        
        if (sound == null) return;
        
        String soundId = sound.getId();
        
        // Önceki zamanlayıcıyı iptal et
        if (timerRunnables.containsKey(soundId)) {
            timerHandler.removeCallbacks(timerRunnables.get(soundId));
            timerRunnables.remove(soundId);
        }
        
        // Yeni zamanlayıcı ayarla
        if (minutes > 0) {
            timerMinutes.put(soundId, minutes);
            
            Runnable timerRunnable = () -> {
                // Belirtilen süre sonunda sesi durdur
                if (players.containsKey(soundId)) {
                    MediaPlayer player = players.get(soundId);
                    if (player != null && player.isPlaying()) {
                        player.pause();
                    }
                }
                
                // Ana ses ise bildirim güncelle veya servisi arka plandan çıkar
                if (mainSound != null && soundId.equals(mainSound.getId())) {
                    isMainSoundPlaying = false;
                    
                    boolean anyPlaying = isAnyMediaPlayerPlaying();
                    if (!anyPlaying) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            stopForeground(STOP_FOREGROUND_REMOVE);
                        } else {
                            stopForeground(true);
                        }
                    } else {
                        // Bildirim güncelle
                        updateNotification();
                    }
                }
                
                timerRunnables.remove(soundId);
                timerMinutes.remove(soundId);
            };
            
            timerRunnables.put(soundId, timerRunnable);
            timerHandler.postDelayed(timerRunnable, TimeUnit.MINUTES.toMillis(minutes));
        } else {
            timerMinutes.remove(soundId);
        }
    }

    private MediaPlayer createMediaPlayer(Sound sound) throws IOException {
        MediaPlayer mediaPlayer = new MediaPlayer();
        
        try {
            Log.d(TAG, "Setting up MediaPlayer for sound: " + sound.getTitle());
            
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            
            // Ses kaynağını ayarla
            if (sound.isLocal()) {
                // En basit yaklaşım: Doğrudan dosya adını kullan
                String soundFileName = sound.getSoundPath();
                
                // .mp3 uzantısı kaldır
                if (soundFileName.endsWith(".mp3")) {
                    soundFileName = soundFileName.substring(0, soundFileName.length() - 4);
                }
                
                Log.d(TAG, "Trying to load sound file: " + soundFileName);
                
                // Raw ID'yi bul
                int resId = getResources().getIdentifier(soundFileName, "raw", getPackageName());
                
                // Eğer bulunamazsa, raw-nodpi klasöründe ara
                if (resId == 0) {
                    Log.d(TAG, "Resource not found in raw, trying raw-nodpi folder");
                    resId = getResources().getIdentifier(soundFileName, "raw-nodpi", getPackageName());
                }
                
                if (resId != 0) {
                    // Dosya bulundu
                    Log.d(TAG, "Found resource ID: " + resId + " for " + soundFileName);
                    Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + resId);
                    mediaPlayer.setDataSource(getApplicationContext(), uri);
                } else {
                    // Dosya bulunamadı
                    Log.e(TAG, "Resource not found for " + soundFileName);
                    throw new IOException("Resource not found: " + soundFileName);
                }
            } else {
                // Uri'den ses çalma
                Log.d(TAG, "Using external URI: " + sound.getSoundUri());
                mediaPlayer.setDataSource(this, sound.getSoundUri());
            }
            
            // Sesi sürekli döngüde çalması için ayarlama
            mediaPlayer.setLooping(true);
            
            // Kısa ses dosyaları için çalma tamamlandığında yeniden başlatma
            mediaPlayer.setOnCompletionListener(mp -> {
                if (mp != null && !mp.isLooping()) {
                    mp.seekTo(0);
                    mp.start();
                }
            });
            
            mediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
            
            Log.d(TAG, "MediaPlayer configured successfully");
            
            return mediaPlayer;
        } catch (Exception e) {
            Log.e(TAG, "Error creating MediaPlayer: " + e.getMessage(), e);
            mediaPlayer.release();
            throw e;
        }
    }

    private void setPlayerVolume(MediaPlayer player, int volumePercent) {
        if (player == null) return;
        
        // Ses seviyesini 0-1 aralığına dönüştür
        float volume = volumePercent / 100f;
        player.setVolume(volume, volume);
    }

    /**
     * Belirtilen ID'ye sahip sesin çalıp çalmadığını kontrol eder.
     * 
     * @param soundId Kontrol edilecek ses ID'si
     * @return Ses çalıyorsa true, aksi halde false
     */
    public boolean isSoundPlaying(String soundId) {
        if (players.containsKey(soundId)) {
            MediaPlayer player = players.get(soundId);
            return player != null && player.isPlaying();
        }
        return false;
    }

    private boolean isAnyMediaPlayerPlaying() {
        for (MediaPlayer player : players.values()) {
            if (player != null && player.isPlaying()) {
                return true;
            }
        }
        return false;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Ses Servisi Kanalı",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Ses çalma servisi için bildirim kanalı");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification(Sound sound) {
        // Ana aktiviteye geri dönmek için intent
        Intent intent = new Intent(this, RelaxationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Oynatma/duraklatma için intent
        Intent playPauseIntent = new Intent(this, SoundService.class);
        playPauseIntent.setAction(isMainSoundPlaying ? ACTION_PAUSE_SOUND : ACTION_PLAY_SOUND);
        playPauseIntent.putExtra(EXTRA_SOUND, sound);
        PendingIntent playPausePendingIntent = PendingIntent.getService(this, 1, playPauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Servisin durdurulması için intent
        Intent stopIntent = new Intent(this, SoundService.class);
        stopIntent.setAction(ACTION_STOP_SERVICE);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 2, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Bildirim oluştur
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_play)
                .setContentTitle(sound.getTitle())
                .setContentText("Çalınıyor")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent)
                .addAction(isMainSoundPlaying ? R.drawable.ic_pause : R.drawable.ic_play, 
                          isMainSoundPlaying ? "Duraklat" : "Oynat", playPausePendingIntent)
                .addAction(R.drawable.ic_back, "Kapat", stopPendingIntent);

        return builder.build();
    }

    private void updateNotification() {
        if (mainSound != null) {
            NotificationManager notificationManager = 
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFICATION_ID, createNotification(mainSound));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Tüm sesleri ve zamanlayıcıları temizle
        for (MediaPlayer player : players.values()) {
            if (player != null) {
                if (player.isPlaying()) {
                    player.stop();
                }
                player.release();
            }
        }
        
        players.clear();
        volumes.clear();
        
        for (Runnable runnable : timerRunnables.values()) {
            timerHandler.removeCallbacks(runnable);
        }
        
        timerRunnables.clear();
        timerMinutes.clear();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
} 