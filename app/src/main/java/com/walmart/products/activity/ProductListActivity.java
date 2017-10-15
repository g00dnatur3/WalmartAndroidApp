package com.walmart.products.activity;

import android.content.Intent;
import android.databinding.DataBindingUtil;

import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.walmart.products.R;
import com.walmart.products.databinding.ProductListBinding;
import com.walmart.products.util.Function;
import com.walmart.products.view.EndlessRecyclerOnScrollListener;

import com.walmart.products.view.ProductListAdapter;

public class ProductListActivity extends BaseActivity {

    private ProductListBinding mBinding;

    private ProductListAdapter mAdapter;

    private LinearLayoutManager mLinearLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");

        // inject start position and item count if any
        mStartPosition = getIntent().getIntExtra("position", 0);
        Log.i(TAG, "start position: " + mStartPosition);
        setItemCount(getIntent().getIntExtra("itemCount", 0));
        Log.i(TAG, "start itemCount: " + getItemCount());

        mBinding = DataBindingUtil.setContentView(this, R.layout.product_list);
        mLinearLayoutManager = new LinearLayoutManager(ProductListActivity.this);
        mLinearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mBinding.recyclerView.setLayoutManager(mLinearLayoutManager);
        mBinding.recyclerView.addItemDecoration(
                new DividerItemDecoration(ProductListActivity.this, DividerItemDecoration.VERTICAL));
        mAdapter = new ProductListAdapter(this);
        mBinding.recyclerView.setAdapter(mAdapter);
        notifyDataSetChanged();
    }

    @Override
    protected ProgressBar getProgressBar() {
        return mBinding.listProgressBar;
    }

    @Override
    public View getMainView() {
        return mBinding.recyclerView;
    }

    @Override
    protected void onServiceBound() {
        if (getItemCount() == 0) {
            loadMore(new Function() {
                @Override
                public void call(Object... args) {
                    addScrollListener();
                }
            });
        } else {
            notifyDataSetChanged();
            addScrollListener();
            mLinearLayoutManager.scrollToPositionWithOffset(mStartPosition, 10);
        }
    }

    public void onBackPressed(){
        moveTaskToBack(true);
    }

    @Override
    public void notifyDataSetChanged() {
        mAdapter.notifyDataSetChanged();
    }

    private void addScrollListener() {
        mBinding.recyclerView.removeOnScrollListener(null); // null will clear all listeners.
        mBinding.recyclerView.addOnScrollListener(new EndlessRecyclerOnScrollListener() {
            @Override
            public void onLoadMore(Function onComplete) {
                loadMore(onComplete);
            }
            @Override
            public void onEnsureLoaded(int fromIndex, int toIndex, final Function onComplete) {
                ensureDataLoaded(fromIndex, toIndex, onComplete);
            }
        });
    }

    public void startProductDetailActivity(int position) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra("position", position);
        intent.putExtra("itemCount", getItemCount());
        startActivity(intent);
    }
}
