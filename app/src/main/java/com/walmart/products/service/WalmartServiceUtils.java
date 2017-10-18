package com.walmart.products.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.BinaryHttpResponseHandler;
import com.walmart.products.http.JsonHttpResponseHandler;
import com.walmart.products.util.EventEmitter;
import com.walmart.products.util.Function;

import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import cz.msebera.android.httpclient.Header;

import static com.walmart.products.service.WalmartServiceConfig.*;
import static com.walmart.products.service.WalmartService.CacheEntry;

/**
 * WalmartService Auxiliary methods.
 *
 * This is not an object, but a singleton container of functions.
 *
 * Only singletons should be injected, if you are not injecting a singleton
 * then you MUST pass the dependency as a parameter to the function.
 *
 * Also this class should have ZERO member variables.
 *
 *
 *  INFO: About the service callbacks and threads
 *
 *  - HttpCalls are executed within a worker thread from a threadpool inside AsyncHttpClient
 *  - When an http call is complete, a new thread is created for parsing JSON
 *  - After parsing, the onComplete callback is called on the original callers thread.
 *  - If your app called from the UI thread, it will get called back on the UI thread.
 *
 */
@Singleton
public class WalmartServiceUtils {

    @Inject
    Map<Integer, String> mPageUrls;

    @Inject
    LruCache<Integer, CacheEntry> mPageCache;

    @Inject
    AsyncHttpClient mHttpClient;

    protected final String TAG = getClass().getCanonicalName();

    protected CacheEntry getPage(int index) {
        final int pageNum = index / PAGE_SIZE;
        CacheEntry cacheEntry = mPageCache.get(pageNum);
        if (cacheEntry == null) {
            StringBuffer err = new StringBuffer("getPage failed - at: ");
            err.append(pageNum).append(", reason: page not loaded into cache");
            Log.v(TAG,  err.toString());
        }
        return cacheEntry;
    }

    protected boolean isPageLoaded(int index) {
        return mPageCache.get(index) != null;
    }

    protected void loadPage(final EventEmitter emitter,
                            final Context context,
                            final Map<Integer, Boolean> pagesLoading,
                            final int pageNum, final Function onComplete) {
        // page already loaded
        if (isPageLoaded(pageNum)) {
            onComplete.call(null, null);
            return;
        }
        // page url not found
        final String pageUrl = mPageUrls.get(pageNum);
        if (pageUrl == null) {
            onComplete.call("loadPage failed - next page not found in mPageUrls");
            return;
        }
        final String PAGE_LOADED_EVENT = "PAGE_" + pageNum + "_LOADED";
        synchronized (this) {
            // page already being loaded, lets not waste time and resources loading it again...
            if (pagesLoading.containsKey(pageNum)) {
                StringBuffer sb = new StringBuffer();
                sb.append("loadPage failed - page already being loaded: ").append(pageNum);
                sb.append(", onComplete will be called once it is loaded");
                Log.i(TAG, sb.toString());
                // EventEmitter is needed in the situation that client A is in the process of loading page X,
                // And client B comes in and asks for page X, we need to now make two callbacks for when page X
                // has completed loading, one to A and one to B. An EventEmitter simplifies this task.
                final Handler handler = new Handler(Looper.myLooper());
                // make sure to make the callback on the callers thread.
                // the original caller thread that asked to to load page X,
                // will call this Function which will post callback to original callers thread...
                emitter.once(PAGE_LOADED_EVENT, new Function() {
                    @Override
                    public void call(final Object... args) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                onComplete.call(args);
                            }
                        });
                    }
                });
                return;
            }
            pagesLoading.put(pageNum, true);
        }
        // ok lets load the page
        mHttpClient.get(context, BASE_URL + pageUrl, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JsonNode pageNode) {
                mPageUrls.put(pageNum+1, pageNode.get("nextPage").textValue());
                final CacheEntry cacheEntry = new CacheEntry(pageNode);
                loadThumbnails(context, cacheEntry, pageNum, new Function() {
                    @Override
                    public void call(Object... args) {
                        pagesLoading.remove(pageNum); // no longer loading page, remove
                        mPageCache.put(pageNum, cacheEntry);
                        Log.i(TAG, "loadPage complete - page: " + pageNum);
                        onComplete.call(null, null);
                        if (emitter.hasListeners(PAGE_LOADED_EVENT)) emitter.emit(PAGE_LOADED_EVENT);

                    }
                });
            }
            @Override
            public void onFailure(int status, Header[] headers, Throwable error, JsonNode errorResponse) {
                pagesLoading.remove(pageNum); // no longer loading page, remove
                onComplete.call(logAndGetHttpError("loadPage", pageUrl, status, error));
                if (emitter.hasListeners(PAGE_LOADED_EVENT)) emitter.emit(PAGE_LOADED_EVENT);
            }
        });
    }

    /**
     * Thumbnails are the only pre-loaded image, all other images will be loaded on demand
     */
    protected void loadThumbnails(Context context, final CacheEntry cacheEntry, final int pageNum, final Function onComplete) {

        ArrayNode itemsNode = getItemsNode(cacheEntry.page());
        if (itemsNode == null) {
            // if gets here there is error somewhere else.
            Log.i(TAG, "loadThumbnails failed - itemsNode is null");
            onComplete.call("itemsNode is null");
            return;
        };

        // simple but effective strategy to concurrently download all the thumbnails
        final int size = itemsNode.size();
        Function _onComplete = new Function() {
            int _size = size;
            @Override
            public void call(Object... args) {
                _size--;
                if (args[0] == null) {
                    String key = (String) args[1];
                    Bitmap bmp = (Bitmap) args[2];
                    cacheEntry.bitmaps().put(key, bmp);
                    //Log.i(TAG, "Successfully loaded thumbnail, _size: " + _size);
                }
                if (_size == 0) {
                    Log.i(TAG, "loadThumbnails complete - for page: " + pageNum);
                    onComplete.call(null, null);
                }
            }
        };

        int itemIndex = 0;
        Iterator<JsonNode> iter = itemsNode.iterator();
        while (iter.hasNext()) {
            String key = itemIndex + ".thumb";
            if (!cacheEntry.bitmaps().containsKey(key)) {
                loadThumbnail(context, iter.next(), key, _onComplete);
            }
            itemIndex++;
        }
    }

    protected void loadThumbnail(Context context, final JsonNode itemNode, final String key, final Function onComplete) {
        if (itemNode.get("thumbnailImage") == null) {
            Log.e(TAG, "loadThumbnail failed - thumbnailImage url is empty");
            onComplete.call("thumbnailImage url is empty");
            return;
        }
        loadBitmap(context, itemNode.get("thumbnailImage").textValue(), key, onComplete);
    }

    protected void loadBitmap(Context context, final String url, final String key, final Function onComplete) {
        mHttpClient.get(context, url, new BinaryHttpResponseHandler(new String[]{".*"}) { //allow all content-types
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] binaryData) {
                Bitmap bmp = BitmapFactory.decodeByteArray(binaryData, 0, binaryData.length);
                onComplete.call(null, key, bmp);
            }
            @Override
            public void onFailure(int status, Header[] headers, byte[] binaryData, Throwable error) {
                onComplete.call(logAndGetHttpError("loadBitmap", url, status, error));
            }
        });
    }

    protected ArrayNode getItemsNode(JsonNode pageNode) {
        if (pageNode.get("items") instanceof ArrayNode) {
            return (ArrayNode) pageNode.get("items");
        } else {
            StringBuffer err = new StringBuffer("getItemsNode failed");
            err.append(" - JsonNode.items is not an arrayNode");
            Log.e(TAG,  err.toString());
            return null;
        }
    }

    protected String logAndGetHttpError(String methodName, String url, int status, Throwable error) {
        StringBuffer err = new StringBuffer(methodName + " - ");
        err.append("status: ").append(status).append(", ").append("url: ").append(url);
        Log.e(TAG, err.toString(), error);
        return err.toString();
    }

}
