package com.onur.motionsicknesskiller.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.onur.motionsicknesskiller.R;
import com.onur.motionsicknesskiller.model.Sound;

import java.util.List;

/**
 * Ses kartlarını RecyclerView'da göstermek için adapter
 */
public class SoundAdapter extends RecyclerView.Adapter<SoundAdapter.SoundViewHolder> {

    private final Context context;
    private final List<Sound> sounds;
    private final OnSoundClickListener listener;

    /**
     * Ses kartlarıyla etkileşim için listener arayüzü
     */
    public interface OnSoundClickListener {
        void onSoundClick(Sound sound);
        void onDownloadClick(Sound sound);
    }

    public SoundAdapter(Context context, List<Sound> sounds, OnSoundClickListener listener) {
        this.context = context;
        this.sounds = sounds;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SoundViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_sound_card, parent, false);
        return new SoundViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SoundViewHolder holder, int position) {
        Sound sound = sounds.get(position);
        
        // Arkaplan resmini ayarla
        holder.ivSoundImage.setImageResource(sound.getImageResId());
        
        // Ses başlığını ayarla
        holder.tvSoundTitle.setText(sound.getTitle());
        
        // İndirme butonunu ayarla (sadece indirilebilir sesler için göster)
        if (!sound.isLocal()) {
            holder.btnDownload.setVisibility(View.VISIBLE);
        } else {
            holder.btnDownload.setVisibility(View.GONE);
        }
        
        // Tıklama olaylarını ayarla
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSoundClick(sound);
            }
        });
        
        holder.btnDownload.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDownloadClick(sound);
            }
        });
    }

    @Override
    public int getItemCount() {
        return sounds.size();
    }

    /**
     * Yeni sesler eklemek için listeyi günceller.
     * 
     * @param newSounds Eklenecek yeni sesler listesi
     */
    public void addSounds(List<Sound> newSounds) {
        int startPosition = sounds.size();
        sounds.addAll(newSounds);
        notifyItemRangeInserted(startPosition, newSounds.size());
    }

    /**
     * Listeyi tamamen yeni seslerle değiştirir.
     * 
     * @param newSounds Yeni sesler listesi
     */
    public void setSounds(List<Sound> newSounds) {
        sounds.clear();
        sounds.addAll(newSounds);
        notifyDataSetChanged();
    }

    /**
     * Ses kartı ViewHolder sınıfı
     */
    static class SoundViewHolder extends RecyclerView.ViewHolder {
        
        ImageView ivSoundImage;
        TextView tvSoundTitle;
        ImageButton btnDownload;
        
        SoundViewHolder(@NonNull View itemView) {
            super(itemView);
            ivSoundImage = itemView.findViewById(R.id.ivSoundImage);
            tvSoundTitle = itemView.findViewById(R.id.tvSoundTitle);
            btnDownload = itemView.findViewById(R.id.btnDownload);
        }
    }
} 