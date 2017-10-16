package com.walmart.products.service;

import android.graphics.Bitmap;
import android.util.Log;
import android.util.LruCache;

import com.fasterxml.jackson.databind.JsonNode;
import com.loopj.android.http.AsyncHttpClient;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;


public class WalmartServiceConfig {

    public static final int PAGE_SIZE = 100;

    public static final int MAX_PAGES = 2;

    public static final int MIN_THREADS = 2;

    public static final int MAX_THREADS = 8;

    // when the number of threads is greater than the core,
    // this is the maximum time that excess idle threads will wait for new tasks
    public static final int THREAD_TIMEOUT = 10; //seconds

    public static final String BASE_URL = "http://api.walmartlabs.com";

    public static final String FIRST_PAGE_URL = "/v1/paginated/items?format=json&apiKey=vns2unqneevgc3vweue9eqnt";

    public static final int HTTP_TIMEOUT = 20 * 1000;

}
