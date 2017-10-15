package com.walmart.products.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.LruCache;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.BinaryHttpResponseHandler;
import com.walmart.products.http.JsonHttpResponseHandler;
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
            StringBuffer err = new StringBuffer("failed to getPage at: ");
            err.append(pageNum).append(", reason: page not loaded into cache");
            Log.v(TAG,  err.toString());
        }
        return cacheEntry;
    }

    protected boolean isPageLoaded(int index) {
        return mPageCache.get(index) != null;
    }

    protected void loadPage(final Context context, final Map<Integer, Boolean> pagesLoading, final int pageNum, final Function onComplete) {
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
        synchronized (this) {
            // page already being loaded, lets not waste time and resources loading it again...
            if (pagesLoading.containsKey(pageNum)) {
                onComplete.call("loadPage failed - page already being loaded: " + pageNum);
                return;
            }
            // ok lets load the page
            pagesLoading.put(pageNum, true);
        }
        mHttpClient.get(context, BASE_URL + pageUrl, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JsonNode pageNode) {
                mPageUrls.put(pageNum+1, pageNode.get("nextPage").textValue());
                final CacheEntry cacheEntry = new CacheEntry(pageNode);
                loadThumbnails(context, cacheEntry, pageNum, new Function() {
                    @Override
                    public void call(Object... args) {
                        synchronized (WalmartServiceUtils.class) {
                            pagesLoading.remove(pageNum); // no longer loading page, remove
                            mPageCache.put(pageNum, cacheEntry);
                        }
                        Log.i(TAG, "loadPage success - page: " + pageNum);
                        onComplete.call(null, null);
                    }
                });
            }
            @Override
            public void onFailure(int status, Header[] headers, Throwable error, JsonNode errorResponse) {
                pagesLoading.remove(pageNum); // no longer loading page, remove
                onComplete.call(logAndGetHttpError("loadPage", pageUrl, status, error));
            }
        });
    }

    /**
     * Thumbnails are the only pre-loaded image, all other images will be loaded on demand
     */
    protected void loadThumbnails(Context context, final CacheEntry cacheEntry, final int pageNum, final Function onComplete) {

        // we dont return any errors here, if we fail we log it and call onComplete(null, null)
        // internally we could add retry logic to the httpRequest for getting the thumbnail,
        // but propagating such an error seems futile - try and resolve here (internally).

        ArrayNode itemsNode = getItemsNode(cacheEntry.page());
        if (itemsNode == null) {
            // if gets here there is error somewhere else.
            Log.i(TAG, "loadThumbnails failed - itemsNode is null");
            onComplete.call(null, null);
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
                    Log.i(TAG, "finished loading thumbnails for page: " + pageNum);
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
            String err = "thumbnailImage url is empty";
            Log.e(TAG, "loadThumbnail failed - " + err);
            onComplete.call(err);
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
                onComplete.call(logAndGetHttpError("loadThumbnail", url, status, error));
            }
        });
    }

    protected ArrayNode getItemsNode(JsonNode pageNode) {
        if (pageNode.get("items") instanceof ArrayNode) {
            return (ArrayNode) pageNode.get("items");
        } else {
            StringBuffer err = new StringBuffer("failed to getItemsNode");
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
