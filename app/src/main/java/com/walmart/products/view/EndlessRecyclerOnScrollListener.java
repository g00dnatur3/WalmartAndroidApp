package com.walmart.products.view;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.walmart.products.service.WalmartServiceConfig;
import com.walmart.products.util.Function;

public abstract class EndlessRecyclerOnScrollListener extends RecyclerView.OnScrollListener {

    private static final int VISIBLE_THRESHOLD = 20; //when we are 20 items until end, load more

    // how much data behind the visible and in front of the visible will be loaded
    private static final int FRONT_BACK_LOAD_SIZE = Math.max((WalmartServiceConfig.PAGE_SIZE/4), 25);

    // because this is an abstract class, the TAG is from the getSuperclass()
    private final String TAG = getClass().getSuperclass().getCanonicalName();

    // true if we are still waiting for the last set of data to load
    private boolean mLoading = false;

    private Function onLoadingComplete = new Function() {
        @Override
        public void call(Object... args) {
            mLoading = false;
        }
    };

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
        if (mLoading) return; //!!Important, we do not want to spawn multiple loading events
        int visibleItemCount = recyclerView.getChildCount();
        int totalItemCount = recyclerView.getLayoutManager().getItemCount();
        int firstVisibleItem = ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
        if (!mLoading && (totalItemCount - visibleItemCount)
                <= (firstVisibleItem + VISIBLE_THRESHOLD)) {
            // End has been reached
            mLoading = true;
            onLoadMore(onLoadingComplete);
        }
    }

    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        super.onScrollStateChanged(recyclerView, newState);
        if (mLoading) return; //!!Important, we do not want to spawn multiple loading events
        if (newState ==  RecyclerView.SCROLL_STATE_IDLE) {
            int firstVisibleItem = ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
            int lastVisibleItem = ((LinearLayoutManager) recyclerView.getLayoutManager()).findLastVisibleItemPosition();
            int fromIndex = firstVisibleItem - FRONT_BACK_LOAD_SIZE;
            if (fromIndex < 0) fromIndex = 0;
            int toIndex = lastVisibleItem + FRONT_BACK_LOAD_SIZE;
            mLoading = true;
            onEnsureLoaded(fromIndex, toIndex, onLoadingComplete);
        }
        //Log.i(TAG, "onScrollStateChanged: " + newState);
    }

    public abstract void onLoadMore(Function onComplete);

    public abstract void onEnsureLoaded(int fromIndex, int toIndex, Function onComplete);

}