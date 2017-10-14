package com.walmart.products.activity;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.test.espresso.IdlingResource;
import android.util.Log;

import com.walmart.products.service.WalmartService;

public class WalmartServiceIdlingResource implements IdlingResource {

    private WalmartService mService;

    private boolean mBound = false;

    private ResourceCallback resourceCallback;

    public WalmartServiceIdlingResource(Context context) {
        Intent intent = new Intent(context, WalmartService.class);
        context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public String getName() {
        return WalmartServiceIdlingResource.class.getName();
    }

    @Override
    public boolean isIdleNow() {
        boolean idle = mBound && !mService.isLoading();
        if (idle && resourceCallback != null) {
            resourceCallback.onTransitionToIdle();
        }
        return idle;
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
        this.resourceCallback = resourceCallback;
    }

    protected ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            WalmartService.WalmartServiceBinder binder = (WalmartService.WalmartServiceBinder) service;
            mService = binder.getService();
            mBound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
}