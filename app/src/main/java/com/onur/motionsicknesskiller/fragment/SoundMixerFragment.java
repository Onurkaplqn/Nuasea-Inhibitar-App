package com.onur.motionsicknesskiller.fragment;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.onur.motionsicknesskiller.R;
import com.onur.motionsicknesskiller.adapter.MixerSoundAdapter;
import com.onur.motionsicknesskiller.model.Sound;
import com.onur.motionsicknesskiller.service.SoundService;

import java.util.ArrayList;
import java.util.List;

/**
 * Ses karıştırıcı ekranı için Fragment sınıfı.
 * Bu sınıf, tam ekran bir dialog fragment olarak çalışır.
 */
public class SoundMixerFragment extends DialogFragment implements MixerSoundAdapter.OnMixerSoundListener {

    private static final String ARG_MAIN_SOUND = "main_sound";
    
    private Sound mainSound;
    private List<Sound> mixedSounds = new ArrayList<>();
    
    private TextView textTitle;
    private TextView textSubtitle;
    private ImageView buttonClose;
    private Button buttonReset;
    private TabLayout tabLayout;
    private SeekBar seekbarMainSound;
    private RecyclerView recyclerSounds;
    
    private MixerSoundAdapter adapter;
    private OnSoundMixerListener listener;
    
    /**
     * Ses karıştırıcı ekranındaki olayları dinleyen arayüz.
     */
    public interface OnSoundMixerListener {
        void onMixerClosed();
        void onMainSoundVolumeChanged(int volume);
        void onAddSound(Sound sound);
        void onRemoveSound(Sound sound);
        void onSoundVolumeChanged(Sound sound, int volume);
    }
    
    /**
     * Yeni bir fragment örneği oluşturur.
     * 
     * @param mainSound Ana ses
     * @return Yeni fragment örneği
     */
    public static SoundMixerFragment newInstance(Sound mainSound) {
        SoundMixerFragment fragment = new SoundMixerFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_MAIN_SOUND, mainSound);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialogStyle);
        
        if (getArguments() != null) {
            mainSound = (Sound) getArguments().getSerializable(ARG_MAIN_SOUND);
        }
    }
    
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnSoundMixerListener) {
            listener = (OnSoundMixerListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnSoundMixerListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sound_mixer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // View'leri bul
        textTitle = view.findViewById(R.id.text_title);
        textSubtitle = view.findViewById(R.id.text_subtitle);
        buttonClose = view.findViewById(R.id.button_close);
        buttonReset = view.findViewById(R.id.button_reset);
        tabLayout = view.findViewById(R.id.tab_layout);
        TextView textMainSound = view.findViewById(R.id.text_main_sound);
        seekbarMainSound = view.findViewById(R.id.seekbar_main_sound);
        recyclerSounds = view.findViewById(R.id.recycler_sounds);
        
        // Ana ses başlığını ayarla
        if (mainSound != null) {
            textMainSound.setText(mainSound.getTitle());
        }
        
        // RecyclerView'ı ayarla
        setupRecyclerView();
        
        // Olayları ayarla
        setupListeners();
        
        // Tab layout için sekmeler ve olaylar
        setupTabLayout();
    }
    
    private void setupRecyclerView() {
        recyclerSounds.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MixerSoundAdapter(requireContext(), mixedSounds, this);
        recyclerSounds.setAdapter(adapter);
        
        // Örnek sesler ekleyelim (gerçek projede bu yükleme dinamik olacak)
        addExampleSounds();
    }
    
    private void addExampleSounds() {
        // Doğa sesleri örneği
        Sound sound1 = new Sound("1", "Ağustos böceği", "nature", R.drawable.denearkplan, "august_cricket");
        Sound sound2 = new Sound("2", "Vahşi orman", "nature", R.drawable.denearkplan, "wild_forest");
        Sound sound3 = new Sound("3", "Ormanda yürüyüş", "nature", R.drawable.denearkplan, "forest_walk");
        
        // Bu sesler kullanıcının karışıma eklediği ek sesler
        mixedSounds.add(sound1);
        mixedSounds.add(sound2);
        adapter.notifyDataSetChanged();
    }
    
    private void setupListeners() {
        // Kapatma düğmesi
        buttonClose.setOnClickListener(v -> dismiss());
        
        // Sıfırlama düğmesi
        buttonReset.setOnClickListener(v -> {
            // Tüm sesleri sıfırla
            seekbarMainSound.setProgress(75); // Ana ses için 75% varsayılan değer
            mixedSounds.clear();
            adapter.notifyDataSetChanged();
            
            if (listener != null) {
                listener.onMainSoundVolumeChanged(75);
            }
            
            // Servise bildir
            SoundService.resetMixer(requireContext());
        });
        
        // Ana ses seviyesi değişikliği
        seekbarMainSound.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && listener != null) {
                    listener.onMainSoundVolumeChanged(progress);
                }
                
                // Servise bildir
                if (mainSound != null) {
                    SoundService.setMainSoundVolume(requireContext(), mainSound, progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // İhtiyaç halinde kullanılabilir
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // İhtiyaç halinde kullanılabilir
            }
        });
    }
    
    private void setupTabLayout() {
        // Tab olayları
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // Seçilen kategoriye göre ses listesini yükle
                loadSoundsByCategory(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // İhtiyaç halinde kullanılabilir
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // İhtiyaç halinde kullanılabilir
            }
        });
    }
    
    private void loadSoundsByCategory(int categoryIndex) {
        // Burada gerçek uygulamada, veritabanından veya API'den
        // ilgili kategoriye ait sesleri yükleyeceksiniz
        String category;
        
        switch (categoryIndex) {
            case 0:
                category = "nature"; // Doğa
                break;
            case 1:
                category = "life";   // Yaşam
                break;
            case 2:
                category = "city";   // Şehirler
                break;
            case 3:
                category = "asmr";   // ASMR
                break;
            default:
                category = "nature";
        }
        
        // Kategoriye göre sesleri yükle
        // (Bu şimdilik demo amaçlı boş bırakılmıştır)
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
    
    // MixerSoundAdapter.OnMixerSoundListener arayüzü metotları
    
    @Override
    public void onVolumeChanged(Sound sound, int position, int volume) {
        if (listener != null) {
            listener.onSoundVolumeChanged(sound, volume);
        }
        
        // Servise bildir
        SoundService.setMixedSoundVolume(requireContext(), sound, volume);
    }
    
    @Override
    public void onRemoveSound(Sound sound, int position) {
        mixedSounds.remove(position);
        adapter.notifyItemRemoved(position);
        
        if (listener != null) {
            listener.onRemoveSound(sound);
        }
        
        // Servise bildir
        SoundService.removeSoundFromMix(requireContext(), sound);
    }
} 