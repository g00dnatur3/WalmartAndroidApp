package com.walmart.products;

import android.util.Log;
import android.util.LruCache;

import com.loopj.android.http.AsyncHttpClient;
import com.walmart.products.service.WalmartService;
import com.walmart.products.service.WalmartServiceUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import javax.inject.Singleton;

import dagger.Provides;

import static com.walmart.products.service.WalmartServiceConfig.*;

@dagger.Module
public class AppModule {

    // if you need to log, uncomment...
    //private final String TAG = getClass().getCanonicalName();

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
        httpClient.setThreadPool(Executors.newFixedThreadPool(MAX_THREADS));
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
        Map<Integer, String> pageUrls = Collections.synchronizedMap(new HashMap<Integer, String>());
        pageUrls.put(0, FIRST_PAGE_URL);
        return pageUrls;
    }

    @Provides @Singleton
    LruCache<Integer, WalmartService.CacheEntry> providePageCache() {
        // LruCache is already thread safe
        return new LruCache<Integer, WalmartService.CacheEntry>(MAX_PAGES);
    }
}