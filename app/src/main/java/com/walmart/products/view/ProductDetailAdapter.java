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

    private boolean mFirstFragmentLoaded = false;

    public ProductDetailAdapter(FragmentManager fm, ProductDetailActivity mActivity) {
        super(fm);
        this.mActivity = mActivity;
        ProductDetailFragment.clearFragmentsLoading();
    }

    @Override
    public Fragment getItem(int position) {
        boolean addToCache = true;
        if (!mFirstFragmentLoaded) {
            // the viewPager will try to get at index 0, even if we scroll to mStartPosition
            // immediately after calling setAdapter... this cause the service to start loading
            // at index 0 when we are at start position 400 (for example).
            if (position == 0 && mActivity.getStartPosition() > 0) {
                position = mActivity.getStartPosition()-1;
                addToCache = false;
                mFirstFragmentLoaded = true;
            }
        }
        if (fragments.containsKey(position)) {
            return fragments.get(position);
        }
        Fragment fragment = new ProductDetailFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("position", position);
        fragment.setArguments(bundle);
        if (addToCache) fragments.put(position, fragment);
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
