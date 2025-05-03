package com.onur.motionsicknesskiller.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.onur.motionsicknesskiller.R;
import com.onur.motionsicknesskiller.model.Sound;

import java.util.List;

/**
 * Ses mikserindeki sesleri RecyclerView içinde göstermek için adapter sınıfı.
 */
public class MixerSoundAdapter extends RecyclerView.Adapter<MixerSoundAdapter.MixerSoundViewHolder> {

    private final List<Sound> soundList;
    private final Context context;
    private final OnMixerSoundListener listener;

    /**
     * Ses mikserindeki ses öğelerine yapılan işlemleri dinleyen arayüz.
     */
    public interface OnMixerSoundListener {
        void onVolumeChanged(Sound sound, int position, int volume);
        void onRemoveSound(Sound sound, int position);
    }

    public MixerSoundAdapter(Context context, List<Sound> soundList, OnMixerSoundListener listener) {
        this.context = context;
        this.soundList = soundList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MixerSoundViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_mixer_sound, parent, false);
        return new MixerSoundViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MixerSoundViewHolder holder, int position) {
        Sound sound = soundList.get(position);
        
        // Ses adını ayarla
        holder.textSoundName.setText(sound.getTitle());
        
        // Ses seviyesi ayarı
        holder.seekbarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && listener != null) {
                    listener.onVolumeChanged(sound, holder.getAdapterPosition(), progress);
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
        
        // Kaldırma butonu olayı
        holder.buttonRemove.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemoveSound(sound, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return soundList.size();
    }

    /**
     * Yeni bir ses ekler.
     * 
     * @param sound Eklenecek ses
     */
    public void addSound(Sound sound) {
        soundList.add(sound);
        notifyItemInserted(soundList.size() - 1);
    }

    /**
     * Belirli bir sesi kaldırır.
     * 
     * @param position Kaldırılacak sesin pozisyonu
     */
    public void removeSound(int position) {
        if (position >= 0 && position < soundList.size()) {
            soundList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, soundList.size() - position);
        }
    }

    /**
     * ViewHolder sınıfı.
     */
    static class MixerSoundViewHolder extends RecyclerView.ViewHolder {
        
        TextView textSoundName;
        ImageView buttonRemove;
        SeekBar seekbarVolume;
        ImageView imageWaveform;

        public MixerSoundViewHolder(@NonNull View itemView) {
            super(itemView);
            textSoundName = itemView.findViewById(R.id.text_sound_name);
            buttonRemove = itemView.findViewById(R.id.button_remove);
            seekbarVolume = itemView.findViewById(R.id.seekbar_sound_volume);
            imageWaveform = itemView.findViewById(R.id.image_waveform);
        }
    }
} 