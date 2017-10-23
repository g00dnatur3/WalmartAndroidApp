package com.walmart.products;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.LruCache;

import com.loopj.android.http.AsyncHttpClient;
import com.walmart.products.service.WalmartService;
import com.walmart.products.service.WalmartServiceUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Provides;

import static com.walmart.products.service.WalmartServiceConfig.*;

@dagger.Module
public class AppModule {

    private final String TAG = getClass().getCanonicalName();

    private final Application mApplication;

    public AppModule(Application mApplication) {
        this.mApplication = mApplication;
    }

    @Provides @Singleton
    AsyncHttpClient provideAsyncHttpClient() {
        AsyncHttpClient httpClient = new AsyncHttpClient();
        httpClient.setLoggingLevel(Log.ERROR);
        httpClient.setConnectTimeout(HTTP_TIMEOUT);
        httpClient.setResponseTimeout(HTTP_TIMEOUT);
        RejectedExecutionHandler rejectHandler = new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                Log.e(TAG + ".asyncHttpClient",
                        "rejectedExecution - thread bounds and queue capacities are reached");
            }
        };
        httpClient.setThreadPool(new ThreadPoolExecutor(
                MIN_THREADS, MAX_THREADS,
                THREAD_TIMEOUT,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(PAGE_SIZE*MAX_PAGES),
                rejectHandler));
        return httpClient;
    }

    @Provides @Singleton
    WalmartServiceUtils provideWalmartServiceUtils() {
        WalmartServiceUtils utils = new WalmartServiceUtils();
        mApplication.component().inject(utils);
        return utils;
    };

    /**
     * DATA STRUCTURES
     *
     * PAGE_URLS - all the page urls we have visited,
     *          if a page was purged from cache we can use the url from here to get it.
     *
     * LRU_PAGE_CACHE - the page cache - includes a Bitmap cache for images.
     *
     */

    @Provides @Singleton
    Map<Integer, String> providePageUrls() {
        Map<Integer, String> pageUrls = new ConcurrentHashMap<>();
        pageUrls.put(0, FIRST_PAGE_URL);
        return pageUrls;
    }

    @Provides @Singleton
    LruCache<Integer, WalmartService.CacheEntry> providePageCache() {
        // LruCache is already thread safe
        return new LruCache<>(MAX_PAGES);
    }
}