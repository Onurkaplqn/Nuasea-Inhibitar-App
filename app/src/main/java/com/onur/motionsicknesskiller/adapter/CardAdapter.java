package com.onur.motionsicknesskiller.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.onur.motionsicknesskiller.MainActivity;
import com.onur.motionsicknesskiller.R;
import com.onur.motionsicknesskiller.RelaxationActivity;
import com.onur.motionsicknesskiller.model.Card;

import java.util.List;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.CardViewHolder> {

    private List<Card> cards;
    private Context context;
    private int currentPlayingPosition = -1;

    public CardAdapter(Context context, List<Card> cards) {
        this.context = context;
        this.cards = cards;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_card, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        Card card = cards.get(position);
        
        // Kart verilerini ayarla
        holder.titleTextView.setText(card.getTitle());
        
        // Kart arka plan rengini ayarla
        holder.cardBackground.setBackgroundColor(card.getColorResourceId());
        
        // Lottie animasyonunu ayarla - Gelişmiş hata yönetimi ve yedekleme sistemi
        try {
            if (card.getLottieFileName() != null && !card.getLottieFileName().isEmpty()) {
                // Raw klasöründeki JSON dosyasını doğru şekilde yükle
                final String fileName = card.getLottieFileName().endsWith(".json") ?
                    card.getLottieFileName().substring(0, card.getLottieFileName().lastIndexOf(".json")) :
                    card.getLottieFileName();
                
                final int resourceId = context.getResources().getIdentifier(
                        fileName, "raw", context.getPackageName());
                        
                if (resourceId != 0) {
                    try {
                        // Ana animasyon yükleme denemesi
                        holder.animationView.setAnimation(resourceId);
                        
                        // Animasyon durumunu izleme ve hata dinleyicisi
                        holder.animationView.addAnimatorUpdateListener(animation -> {
                            // Animasyon güncelleme işlemleri buraya eklenebilir
                        });
                        
                        // Hata durumunda yedek animasyon için dinleyici
                        holder.animationView.setFailureListener(exception -> {
                            // Ayrıntılı hata bilgisi
                            System.err.println("Animasyon yükleme hatası: " + exception.getMessage());
                            exception.printStackTrace();
                            
                            // Yedek animasyon kullanma girişimi
                            try {
                                // Dosya adına göre güvenli yedek animasyon seç
                                String backupAnimFile;
                                if (fileName.equals("main") || fileName.equals("mainsplashscreen")) {
                                    backupAnimFile = "splash";
                                } else if (fileName.equals("lefttag")) {
                                    backupAnimFile = "relaxanimsplash";
                                } else {
                                    backupAnimFile = "loading";
                                }
                                
                                int backupResourceId = context.getResources().getIdentifier(
                                        backupAnimFile, "raw", context.getPackageName());
                                        
                                if (backupResourceId != 0) {
                                    holder.animationView.setAnimation(backupResourceId);
                                    holder.animationView.setVisibility(View.VISIBLE);
                                } else {
                                    // Yedek animasyon da bulunamadı
                                    holder.animationView.setVisibility(View.INVISIBLE);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                holder.animationView.setVisibility(View.INVISIBLE);
                            }
                        });
                        
                        // Sadece mevcut pozisyon için animasyonu başlat
                        if (position == currentPlayingPosition) {
                            holder.animationView.playAnimation();
                        } else {
                            holder.animationView.pauseAnimation();
                        }
                    } catch (Exception e) {
                        System.err.println("İlk animasyon yükleme hatası: " + e.getMessage());
                        e.printStackTrace();
                        
                        // İlk denemede hata - yedek animasyon kullan
                        tryLoadBackupAnimation(holder, card, position);
                    }
                } else {
                    System.err.println("Dosya bulunamadı: " + fileName);
                    
                    // Dosya bulunamadı - yedek animasyon kullan
                    tryLoadBackupAnimation(holder, card, position);
                }
            }
        } catch (Exception e) {
            System.err.println("Genel animasyon yükleme hatası: " + e.getMessage());
            e.printStackTrace();
            
            // Genel hata - görünümü gizle
            holder.animationView.setVisibility(View.INVISIBLE);
        }
        
        // Başla butonuna tıklama işleyicisi
        final int cardType = card.getType();
        holder.startButton.setOnClickListener(v -> navigateToActivity(cardType));
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }
    
    public void playAnimationForPosition(int position) {
        if (currentPlayingPosition == position) {
            return; // Zaten oynatılıyor
        }
        
        // Önceki animasyonu durdur
        notifyItemChanged(currentPlayingPosition);
        
        // Yeni pozisyonu ayarla ve animasyonu başlat
        currentPlayingPosition = position;
        notifyItemChanged(position);
    }

    private void navigateToActivity(int cardType) {
        Intent intent;
        
        switch (cardType) {
            case Card.TYPE_RELAXATION:
                intent = new Intent(context, RelaxationActivity.class);
                break;
            case Card.TYPE_NIGHT_MODE:
                intent = new Intent(context, MainActivity.class);
                intent.putExtra("card_type", cardType);
                break;
            default:
                return;
        }
        
        context.startActivity(intent);
    }

    // Yedek animasyon yükleme yardımcı metodu
    private void tryLoadBackupAnimation(@NonNull CardViewHolder holder, Card card, int position) {
        try {
            // Kart tipine göre güvenli bir yedek animasyon seç
            String backupFile;
            if (card.getType() == Card.TYPE_RELAXATION) {
                backupFile = "loading"; // Rahatlama kartı için
            } else {
                backupFile = "splash"; // Ekran-Hareket kartı için
            }
            
            int backupId = context.getResources().getIdentifier(
                    backupFile, "raw", context.getPackageName());
                    
            if (backupId != 0) {
                holder.animationView.setAnimation(backupId);
                
                // Sadece gerekiyorsa oynat
                if (position == currentPlayingPosition) {
                    holder.animationView.playAnimation();
                }
            } else {
                // Son çare: eyefiltersplash dosyasını dene
                int lastResortId = context.getResources().getIdentifier(
                        "eyefiltersplash", "raw", context.getPackageName());
                        
                if (lastResortId != 0) {
                    holder.animationView.setAnimation(lastResortId);
                    
                    if (position == currentPlayingPosition) {
                        holder.animationView.playAnimation();
                    }
                } else {
                    // Hiçbir animasyon yüklenemedi
                    holder.animationView.setVisibility(View.INVISIBLE);
                }
            }
        } catch (Exception e) {
            System.err.println("Yedek animasyon yükleme hatası: " + e.getMessage());
            e.printStackTrace();
            holder.animationView.setVisibility(View.INVISIBLE);
        }
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        Button startButton;
        View cardBackground;
        View divider;
        LottieAnimationView animationView;

        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.card_title);
            startButton = itemView.findViewById(R.id.button_start);
            cardBackground = itemView.findViewById(R.id.card_background);
            divider = itemView.findViewById(R.id.divider);
            animationView = itemView.findViewById(R.id.animation_view);
        }
    }
} 