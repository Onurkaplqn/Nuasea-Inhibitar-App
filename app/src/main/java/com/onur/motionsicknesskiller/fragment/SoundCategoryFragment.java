package com.onur.motionsicknesskiller.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.onur.motionsicknesskiller.R;
import com.onur.motionsicknesskiller.adapter.SoundAdapter;
import com.onur.motionsicknesskiller.model.Sound;
import com.onur.motionsicknesskiller.model.SoundCategory;

/**
 * Her bir kategori için ses kartlarını görüntüleyen fragment
 */
public class SoundCategoryFragment extends Fragment {
    
    private static final String ARG_CATEGORY = "category";
    
    private SoundCategory category;
    private SoundAdapter.OnSoundClickListener listener;
    
    public static SoundCategoryFragment newInstance(SoundCategory category, 
                                                   SoundAdapter.OnSoundClickListener listener) {
        SoundCategoryFragment fragment = new SoundCategoryFragment();
        fragment.category = category;
        fragment.listener = listener;
        
        Bundle args = new Bundle();
        args.putSerializable(ARG_CATEGORY, category);
        fragment.setArguments(args);
        
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (savedInstanceState != null) {
            category = (SoundCategory) savedInstanceState.getSerializable(ARG_CATEGORY);
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sound_category, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        RecyclerView recyclerView = view.findViewById(R.id.recyclerSounds);
        
        // Grid layout manager ile kartları görüntüle (2 sütun)
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        
        // Eğer listener null ise, activity'den al
        if (listener == null && getActivity() instanceof SoundAdapter.OnSoundClickListener) {
            listener = (SoundAdapter.OnSoundClickListener) getActivity();
        }
        
        // Adapter'ı ayarla
        SoundAdapter adapter = new SoundAdapter(getContext(), category.getSounds(), listener);
        recyclerView.setAdapter(adapter);
    }
    
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(ARG_CATEGORY, category);
    }
} 