package com.walmart.products.activity;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.walmart.products.R;
import com.walmart.products.databinding.ProductDetailBinding;
import com.walmart.products.util.Function;
import com.walmart.products.view.ProductDetailAdapter;

public class ProductDetailActivity extends BaseActivity {

    private ProductDetailBinding mBinding;

    private ProductDetailAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        // inject start position and item count if any
        mStartPosition = getIntent().getIntExtra("position", 0);
        Log.i(TAG, "start position: " + mStartPosition);
        setItemCount(getIntent().getIntExtra("itemCount", 0));
        Log.i(TAG, "start itemCount: " + getItemCount());
        mBinding = DataBindingUtil.setContentView(this, R.layout.product_detail);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); //show back button
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // back button in top left corner was pressed.
        startProductListActivity();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    @Override
    public View getMainView() {
        return mBinding.viewPager;
    }

    @Override
    protected void onServiceBound() {
        //------------------------------------------------------------------------------------
        // Need to setAdapter after service is bound, otherwise you can receive error below
        // ProductDetailFragment: showProduct failed  - WalmartService is not bound
        mAdapter = new ProductDetailAdapter(getSupportFragmentManager(), this);
        mBinding.viewPager.setAdapter(mAdapter);
        //-------------------------------------------------------------------------------------
        if (getItemCount() == 0) {
            loadMore(new Function() {
                @Override
                public void call(Object... args) {
                    Log.i(TAG, "first page loaded");
                }
            });
        } else {
            mBinding.viewPager.setCurrentItem(mStartPosition);
        }
    }

    public void onBackPressed(){
        startProductListActivity();
    }

    @Override
    protected ProgressBar getProgressBar() {
        return mBinding.detailProgressBar;
    }

    @Override
    public void notifyDataSetChanged() {
        mAdapter.notifyDataSetChanged();
    }

    public void startProductListActivity() {
        Intent intent = new Intent(this, ProductListActivity.class);
        intent.putExtra("position", mBinding.viewPager.getCurrentItem());
        intent.putExtra("itemCount", getItemCount());
        startActivity(intent);
    }
}
