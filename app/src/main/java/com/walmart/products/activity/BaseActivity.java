package com.walmart.products.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.walmart.products.service.WalmartService;
import com.walmart.products.util.Function;

import static com.walmart.products.service.WalmartServiceConfig.PAGE_SIZE;

// principle of least privilege:
// default accessor so only the activity package can extend it
abstract class BaseActivity extends AppCompatActivity {

    private static final String WALMART_SERVICE_NOT_BOUND = "WalmartService not bound";

    protected final String TAG = getClass().getCanonicalName();

    protected WalmartService mService;

    protected int mStartPosition;

    private boolean mBound = false;

    private int mItemCount = 0;

    public int getItemCount() {
        return mItemCount;
    }

    // only the parent activity can change item count for this activity.
    protected void setItemCount(int mItemCount) {
        Log.i(TAG, "setItemCount: " + mItemCount);
        this.mItemCount = mItemCount;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
        Intent intent = new Intent(this, WalmartService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop");
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
        mItemCount = 0; //data is reset
    }

    protected ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            WalmartService.WalmartServiceBinder binder = (WalmartService.WalmartServiceBinder) service;
            mService = binder.getService();
            mBound = true;
            onServiceBound();
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    public WalmartService getService() {
        return (mBound) ? mService : null;
    }

    private void postNotifyDataSetChanged(final int newItemCount, final boolean updateLoadingIndicator) {
        getMainView().post(new Runnable() {
            @Override
            public void run() {
                if (getItemCount() < newItemCount) setItemCount(newItemCount);
                notifyDataSetChanged();
                if (updateLoadingIndicator) hideLoadingIndicator();
            }
        });
    }

    public void ensureDataLoaded(int fromIndex, final int toIndex, final Function onComplete) {
        ensureDataLoaded(fromIndex, toIndex, onComplete, true);
    }

    public void ensureDataLoaded(int fromIndex, final int toIndex, final Function onComplete, final boolean updateLoadingIndicator) {
        if (getService() == null) {
            Log.i(TAG, "ensureDataLoaded failed - " + WALMART_SERVICE_NOT_BOUND);
            onComplete.call(WALMART_SERVICE_NOT_BOUND);
            return;
        }
        if (mService.isLoaded(fromIndex, toIndex) || mService.isLoading(fromIndex, toIndex)) {
            //TODO: WalmartService needs an EventEmitter for "LOAD_COMPLETE" events:
            //TODO: The service.on("LOAD_COMPLETE", callback) -- callback will have pageNun
            onComplete.call(null, null); //notify ScrollListener loading is complete
            return;
        }
        if (updateLoadingIndicator) showLoadingIndicator();
        mService.loadProducts(fromIndex, toIndex, new Function() {
            @Override
            public void call(Object... args) {
                if (args[0] != null) {
                    Log.e(TAG, (String) args[0]);
                } else {
                    postNotifyDataSetChanged(toIndex, updateLoadingIndicator);
                    Log.i(TAG, "ensureDataLoaded_After - itemCount: " + getItemCount());
                }
                onComplete.call(null, null);
            }
        });
    }

    public void loadMore(final Function onComplete) {
        loadMore(onComplete, true);
    }

    public void loadMore(final Function onComplete, final boolean updateLoadingIndicator) {
        if (getService() == null) {
            Log.i(TAG, "loadMore failed - " + WALMART_SERVICE_NOT_BOUND);
            onComplete.call(WALMART_SERVICE_NOT_BOUND);
            return;
        }
        if (updateLoadingIndicator) showLoadingIndicator();
        int fromIndex = getItemCount();
        final int toIndex = getItemCount() + PAGE_SIZE-1;
        mService.loadProducts(fromIndex, toIndex, new Function() {
            @Override
            public void call(Object... args) {
                if (args[0] != null) {
                    Log.e(TAG, (String) args[0]);
                } else {
                    postNotifyDataSetChanged(toIndex, updateLoadingIndicator);
                    Log.i(TAG, "loadMore_After - itemCount: " + getItemCount());
                }
                onComplete.call(null, null);
            }
        });
    }

    public void showLoadingIndicator() {
        getProgressBar().setVisibility(View.VISIBLE);
    }

    public void hideLoadingIndicator() {
        getProgressBar().setVisibility(View.GONE);
    }

    protected abstract ProgressBar getProgressBar();

    public abstract View getMainView();

    protected abstract void onServiceBound();

    public abstract void notifyDataSetChanged();

}
