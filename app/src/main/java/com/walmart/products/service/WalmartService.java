package com.walmart.products.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.walmart.products.Application;
import com.walmart.products.util.EventEmitter;
import com.walmart.products.util.Function;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import static com.walmart.products.service.WalmartServiceConfig.*;

/**
 * Serviced used to load Walmart products into memory and provide access on demand.
 *
 * No more than (PAGE_SIZE * MAX_PAGES) items (plus thumbnail bits) are in memory at a given time.
 */
public class WalmartService extends Service {

    protected final String TAG = getClass().getCanonicalName();

    @Inject
    WalmartServiceUtils mUtils;

    // keep track of current loading pages to avoid loading the same page concurrently
    private Map<Integer, Boolean> mPagesLoading;

    private final IBinder mBinder;

    private final EventEmitter mEmitter;

    public WalmartService() {
        mPagesLoading = Collections.synchronizedMap(new HashMap<Integer, Boolean>());
        mBinder = new WalmartServiceBinder();
        mEmitter = new EventEmitter();
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");
        Application.get(this).component().inject(this);
    }

    @Override
    public void onDestroy () {
        Log.i(TAG, "onDestroy");
        mUtils.mHttpClient.cancelRequests(this, true);
    }

    public class WalmartServiceBinder extends Binder {
        public WalmartService getService() {
            return WalmartService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Used by expresso to know if service app is idle
     * @return
     */
    public boolean isLoading() {
        return mPagesLoading.size() > 0;
    }

    /**
     * Get a product at a given index from the cache.
     * @param index
     * @return productNode or null is not in cache.
     */
    public JsonNode getProduct(int index) {
        CacheEntry cacheEntry = mUtils.getPage(index);
        if (cacheEntry != null) {
            JsonNode pageNode = cacheEntry.page();
            ArrayNode itemsNode = mUtils.getItemsNode(pageNode);
            return (itemsNode == null) ? null : itemsNode.get(index % PAGE_SIZE);
        }
        return null;
    }

    /**
     * Get a Thumbnail at a given index from the cache.
     * Thumbnails are preloaded with each page, so if its not there- then the page is not loaded.
     * @param index
     * @return thumbnail or null is not in cache.
     */
    public Bitmap getThumbnail(int index) {
        CacheEntry cacheEntry = mUtils.getPage(index);
        if (cacheEntry != null) {
            String key = (index % PAGE_SIZE) + ".thumb";
            return cacheEntry.bitmaps().get(key);
        }
        return null;
    }

    /**
     * Medium images are loaded on demand,
     * Therefore the onComplete will be called with the loaded Bitmap.
     * @param index
     * @param onComplete - args[0] is error if not null, else args[1] is the bitmap
     */
    public void getMediumImage(int index, final Function onComplete) {
        final CacheEntry cacheEntry = mUtils.getPage(index);
        if (cacheEntry != null) {
            int itemIndex = (index % PAGE_SIZE);
            String key =  itemIndex + ".medium";
            Bitmap bmp = cacheEntry.bitmaps().get(key);
            if (bmp == null) {
                JsonNode pageNode = cacheEntry.page();
                ArrayNode itemsNode = mUtils.getItemsNode(pageNode);
                if (itemsNode == null) {
                    onComplete.call("getMediumImage failed - itemsNode is null");
                    return;
                }
                JsonNode itemNode = itemsNode.get(itemIndex);
                if (itemNode == null) {
                    onComplete.call("getMediumImage failed - item not found");
                    return;
                }
                if (itemNode.get("mediumImage") == null) {
                    onComplete.call("getMediumImage failed - mediumImage url is empty");
                    return;
                }
                mUtils.loadBitmap(this, itemNode.get("mediumImage").textValue(), key, new Function() {
                    @Override
                    public void call(Object... args) {
                        if (args[0] == null) {
                            String key = (String) args[1];
                            Bitmap bmp = (Bitmap) args[2];
                            cacheEntry.bitmaps().put(key, bmp);
                            onComplete.call(null, bmp);
                        } else {
                            onComplete.call(args[0]);
                        }
                    }
                });
            } else {
                onComplete.call(null, bmp);
            }
        } else {
            onComplete.call("getMediumImage failed - page not loaded");
        }
    }

    /**
     * Check if the cache is loaded for the given index range.
     * @param fromIndex
     * @param toIndex
     * @return true if loaded else false
     */
    public boolean isLoaded(int fromIndex, int toIndex) {
        int beginPage = fromIndex / PAGE_SIZE;
        int endPage = toIndex / PAGE_SIZE;
        if (beginPage != endPage) {
            return mUtils.isPageLoaded(beginPage) && mUtils.isPageLoaded(endPage);
        }
        return mUtils.isPageLoaded(beginPage);
    }

    /**
     * Check if the cache is being loaded for the given index range.
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public boolean isLoading(int fromIndex, int toIndex) {
        int beginPage = fromIndex / PAGE_SIZE;
        int endPage = toIndex / PAGE_SIZE;
        if (beginPage != endPage) {
            return mPagesLoading.get(beginPage) != null && mPagesLoading.get(endPage) != null;
        }
        return mPagesLoading.get(beginPage) != null;
    }

    /**
     * Load products for a given index range.
     * @param fromIndex
     * @param toIndex
     * @param onComplete
     */
    public void loadProducts(int fromIndex, int toIndex, final Function onComplete) {
        final int beginPage = fromIndex / PAGE_SIZE;
        final int endPage = toIndex / PAGE_SIZE;
        final Context context = this;
        Function _onComplete;
        // we might need to load two pages, the from & to index could span two pages (AT MOST)
        if (beginPage != endPage) {
            if (endPage-beginPage > 1) {
                onComplete.call("invalid fromIndex and toIndex, range too big");
                return;
            }
            _onComplete = new Function() {
                @Override
                public void call(Object... args) {
                    if (args[0] != null) {
                        Log.e(TAG, args[0].toString());
                        onComplete.call(args[0]);
                    }
                    else {
                        mUtils.loadPage(mEmitter, context, mPagesLoading, endPage, onComplete);
                    }
                }
            };
        } else {
            _onComplete = onComplete;
        }
        mUtils.loadPage(mEmitter, context, mPagesLoading, beginPage, _onComplete);
    }

    /** Needed for efficient Bitmap Caching **/
    public static class CacheEntry {
        private final JsonNode mPage;
        private final Map<String, Bitmap> mBitmaps;
        public CacheEntry(JsonNode page) {
            this.mPage = page;
            mBitmaps = Collections.synchronizedMap(new HashMap<String, Bitmap>());
        }
        public JsonNode page() {
            return mPage;
        }
        public Map<String, Bitmap> bitmaps() {
            return mBitmaps;
        }
    }

}
