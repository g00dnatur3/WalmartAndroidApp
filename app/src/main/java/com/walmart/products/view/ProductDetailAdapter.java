package com.walmart.products.view;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;

import com.walmart.products.activity.ProductDetailActivity;

import java.util.HashMap;

public class ProductDetailAdapter extends FragmentPagerAdapter {

    private final HashMap<Integer, Fragment> fragments = new HashMap<Integer, Fragment>();

    private final ProductDetailActivity mActivity;

    public ProductDetailAdapter(FragmentManager fm, ProductDetailActivity mActivity) {
        super(fm);
        this.mActivity = mActivity;
    }

    @Override
    public Fragment getItem(int position) {
        if (fragments.containsKey(position)) {
            return fragments.get(position);
        }
        Fragment fragment = new ProductDetailFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("position", position);
        fragment.setArguments(bundle);
        fragments.put(position, fragment);
        return fragment;
    }

    @Override
    public int getCount() {
        return mActivity.getItemCount();
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        super.destroyItem(container, position, object);
        fragments.remove(position);
    }

}
