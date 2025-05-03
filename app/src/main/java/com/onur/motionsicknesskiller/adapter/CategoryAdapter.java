package com.onur.motionsicknesskiller.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.onur.motionsicknesskiller.fragment.SoundCategoryFragment;
import com.onur.motionsicknesskiller.model.SoundCategory;

import java.util.List;

/**
 * ViewPager2 için kategori adaptörü
 * Her kategori için bir SoundCategoryFragment oluşturur
 */
public class CategoryAdapter extends FragmentStateAdapter {

    private final List<SoundCategory> categories;
    private final SoundAdapter.OnSoundClickListener listener;

    public CategoryAdapter(@NonNull FragmentActivity fragmentActivity,
                          List<SoundCategory> categories,
                          SoundAdapter.OnSoundClickListener listener) {
        super(fragmentActivity);
        this.categories = categories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return SoundCategoryFragment.newInstance(categories.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }
} 