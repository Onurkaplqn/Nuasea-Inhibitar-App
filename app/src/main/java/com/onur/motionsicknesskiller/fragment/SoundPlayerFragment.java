package com.onur.motionsicknesskiller.fragment;

import android.app.Dialog;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.onur.motionsicknesskiller.R;
import com.onur.motionsicknesskiller.RelaxationActivity;
import com.onur.motionsicknesskiller.model.Sound;
import com.onur.motionsicknesskiller.service.SoundService;

/**
 * Ses oynatıcı ekranı için Fragment sınıfı.
 * Bu sınıf, tam ekran bir dialog fragment olarak çalışır.
 */
public class SoundPlayerFragment extends DialogFragment {

    private static final String ARG_SOUND = "sound";
    
    private Sound sound;
    private boolean isPlaying = false;
    
    private ImageView imageBackground;
    private TextView textTitle;
    private TextView textSubtitle;
    private TextView textTimer;
    private FloatingActionButton fabPlayPause;
    private FloatingActionButton fabMixer;
    private ImageView buttonBack;
    private ImageView buttonDown;
    
    private OnSoundPlayerListener listener;
    
    private int timerMinutes = 30;
    
    /**
     * Ses oynatıcı ekranındaki olayları dinleyen arayüz.
     */
    public interface OnSoundPlayerListener {
        void onClosePlayer();
        void onShowMixer();
        void onPlayStateChanged(boolean isPlaying);
        void onTimerSelected(int minutes);
    }
    
    /**
     * Yeni bir fragment örneği oluşturur.
     * 
     * @param sound Oynatılacak ses
     * @return Yeni fragment örneği
     */
    public static SoundPlayerFragment newInstance(Sound sound) {
        SoundPlayerFragment fragment = new SoundPlayerFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_SOUND, sound);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        
        if (getArguments() != null) {
            sound = (Sound) getArguments().getSerializable(ARG_SOUND);
        }
    }
    
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnSoundPlayerListener) {
            listener = (OnSoundPlayerListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnSoundPlayerListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sound_player, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // View'leri bul
        imageBackground = view.findViewById(R.id.image_background);
        textTitle = view.findViewById(R.id.text_title);
        textSubtitle = view.findViewById(R.id.text_subtitle);
        textTimer = view.findViewById(R.id.text_timer);
        fabPlayPause = view.findViewById(R.id.fab_play_pause);
        fabMixer = view.findViewById(R.id.fab_mixer);
        buttonBack = view.findViewById(R.id.button_back);
        buttonDown = view.findViewById(R.id.button_down);
        
        // UI'yi ayarla
        setupUI();
        
        // Olayları ayarla
        setupListeners();
        
        // Fragment her görünür olduğunda oynatma durumunu kontrol et
        updatePlaybackState();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Fragment tekrar görünür olduğunda, çalma durumunu kontrol et
        updatePlaybackState();
    }
    
    // Gerçek çalma durumunu kontrol et ve UI'yi güncelle
    private void updatePlaybackState() {
        // Activity'den çalma durumunu al
        if (listener != null && sound != null) {
            try {
                // MediaPlayer'ın durumunu kontrol etmek için özel bir metot çağır
                boolean isCurrentlyPlaying = checkIfSoundIsPlaying();
                
                // UI ile gerçek durumu senkronize et
                if (isPlaying != isCurrentlyPlaying) {
                    Log.d("SoundPlayerFragment", "Updating UI playback state to: " + isCurrentlyPlaying);
                    isPlaying = isCurrentlyPlaying;
                    updatePlayPauseButton();
                }
            } catch (Exception e) {
                Log.e("SoundPlayerFragment", "Error updating playback state: " + e.getMessage(), e);
            }
        }
    }
    
    // Sesin şu anda çalıp çalmadığını kontrol et
    private boolean checkIfSoundIsPlaying() {
        if (getActivity() instanceof RelaxationActivity && sound != null) {
            RelaxationActivity activity = (RelaxationActivity) getActivity();
            return activity.isSoundPlaying(sound);
        }
        return false;
    }
    
    private void setupUI() {
        if (sound != null) {
            // Arkaplan resmini ayarla
            imageBackground.setImageResource(sound.getImageResId());
            
            // Başlıkları ayarla
            textTitle.setText("dinleyin");
            textSubtitle.setText(sound.getTitle());
            
            // Zaman ayarını göster
            textTimer.setText("30dk.");
        }
    }
    
    private void setupListeners() {
        // Geri düğmesi
        buttonBack.setOnClickListener(v -> dismiss());
        
        // Aşağı düğmesi
        buttonDown.setOnClickListener(v -> dismiss());
        
        // Oynat/Durdur düğmesi
        fabPlayPause.setOnClickListener(v -> {
            // Önce mevcut durumu kontrol et ve tersine çevir
            isPlaying = !isPlaying;
            updatePlayPauseButton();
            
            Log.d("SoundPlayerFragment", "Play button clicked, new isPlaying state: " + isPlaying + 
                 ", sound: " + (sound != null ? sound.getTitle() : "null") + 
                 ", soundPath: " + (sound != null ? sound.getSoundPath() : "null"));
            
            try {
                if (listener != null) {
                    listener.onPlayStateChanged(isPlaying);
                }
            } catch (Exception e) {
                Log.e("SoundPlayerFragment", "Error playing sound: " + e.getMessage(), e);
                // Hata durumunda kullanıcıya bilgi ver
                Toast.makeText(requireContext(), 
                      "Ses oynatılırken bir hata oluştu: " + e.getMessage(), 
                      Toast.LENGTH_SHORT).show();
                // UI'yi geri al
                isPlaying = !isPlaying;
                updatePlayPauseButton();
            }
        });
        
        // Ses karıştırıcı düğmesi - DEVRE DIŞI
        /* 
        fabMixer.setOnClickListener(v -> {
            if (listener != null) {
                listener.onShowMixer();
            }
            
            // Fragment işlemi
            FragmentManager fragmentManager = getParentFragmentManager();
            SoundMixerFragment mixerFragment = SoundMixerFragment.newInstance(sound);
            mixerFragment.show(fragmentManager, "sound_mixer");
        });
        */
        
        // Mixer butonunu gizle
        fabMixer.setVisibility(View.GONE);
        
        // Zamanlayıcı
        textTimer.setOnClickListener(v -> showTimerDialog());
    }
    
    private void updatePlayPauseButton() {
        fabPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
    }
    
    private void showTimerDialog() {
        // Timer seçenekleri
        final int[] timerValues = {0, 15, 30, 45, 60, 120};
        
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_timer_selection);
        
        RadioGroup radioGroup = dialog.findViewById(R.id.timer_radio_group);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);
        Button btnOk = dialog.findViewById(R.id.btn_ok);
        
        // Mevcut timer değerine göre radio button seçimi
        int selectedIndex = 0;
        for (int i = 0; i < timerValues.length; i++) {
            if (timerValues[i] == timerMinutes) {
                selectedIndex = i;
                break;
            }
        }
        
        // RadioButton ID'leri
        int[] radioButtonIds = {
            R.id.radio_off, R.id.radio_15min, R.id.radio_30min, 
            R.id.radio_45min, R.id.radio_1hour, R.id.radio_2hour
        };
        
        // Mevcut zamanlayıcı değerine göre radiobutton seç
        if (selectedIndex >= 0 && selectedIndex < radioButtonIds.length) {
            radioGroup.check(radioButtonIds[selectedIndex]);
        }
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnOk.setOnClickListener(v -> {
            int selectedId = radioGroup.getCheckedRadioButtonId();
            int selectedTimerIndex = -1;
            
            for (int i = 0; i < radioButtonIds.length; i++) {
                if (radioButtonIds[i] == selectedId) {
                    selectedTimerIndex = i;
                    break;
                }
            }
            
            if (selectedTimerIndex != -1) {
                timerMinutes = timerValues[selectedTimerIndex];
                updateTimerUI();
            }
            
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    private void updateTimerUI() {
        if (timerMinutes == 0) {
            textTimer.setText("Kapalı");
        } else {
            textTimer.setText(timerMinutes + " dk.");
        }
        
        // Timer değişikliğini listener'a bildir
        if (listener != null) {
            listener.onTimerSelected(timerMinutes);
        }
    }
    
    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setLayout(width, height);
        }
    }
    
    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }
    
    /**
     * Fragment'i ekranda gösterir.
     * 
     * @param fragmentManager Fragment yöneticisi
     */
    public void show(FragmentManager fragmentManager) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.add(android.R.id.content, this).addToBackStack(null).commit();
    }
} 