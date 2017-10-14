package com.walmart.products.http;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;

import java.io.IOException;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpStatus;

/**
 * Jackson is the fastest JSON parser for larger JSON data sets.
 * source: http://blog.takipi.com/the-ultimate-json-library-json-simple-vs-gson-vs-jackson-vs-json/
 */
public class JsonHttpResponseHandler extends AsyncHttpResponseHandler {

    protected static final ObjectMapper mMapper = new ObjectMapper();

    static {
        mMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    protected final String TAG = getClass().getCanonicalName();

    public void onSuccess(int statusCode, Header[] headers, JsonNode response) {
        Log.w(TAG, "onSuccess(int, Header[], JsonNode) was not overriden, but callback was received");
    }

    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JsonNode errorResponse) {
        Log.w(TAG, "onFailure(int, Header[], Throwable, JsonNode) was not overriden, but callback was received", throwable);
    }

    @Override
    public final void onSuccess(final int statusCode, final Header[] headers, final byte[] responseBytes) {
        if (statusCode != HttpStatus.SC_NO_CONTENT) {
            Runnable parser = new Runnable() {
                @Override
                public void run() {
                    try {
                        final JsonNode jsonResponse = parseResponse(responseBytes);
                        postRunnable(new Runnable() {
                            @Override
                            public void run() {
                                onSuccess(statusCode, headers, jsonResponse);
                            }
                        });
                    } catch (final IOException ex) {
                        postRunnable(new Runnable() {
                            @Override
                            public void run() {
                                onFailure(statusCode, headers, ex, (JsonNode) null);
                            }
                        });
                    }
                }
            };
            if (!getUseSynchronousMode() && !getUsePoolThread()) {
                new Thread(parser).start();
            } else {
                // In synchronous mode everything should be run on one thread
                parser.run();
            }
        } else {
            onSuccess(statusCode, headers, new ObjectNode(JsonNodeFactory.instance));
        }
    }

    @Override
    public final void onFailure(final int statusCode, final Header[] headers, final byte[] responseBytes, final Throwable throwable) {
        if (responseBytes != null) {
            Runnable parser = new Runnable() {
                @Override
                public void run() {
                    try {
                        final JsonNode jsonResponse = parseResponse(responseBytes);
                        postRunnable(new Runnable() {
                            @Override
                            public void run() {
                                onFailure(statusCode, headers, (Throwable) null, jsonResponse);
                            }
                        });
                    } catch (final IOException ex) {
                        postRunnable(new Runnable() {
                            @Override
                            public void run() {
                                onFailure(statusCode, headers, ex, (JsonNode) null);
                            }
                        });
                    }
                }
            };
            if (!getUseSynchronousMode() && !getUsePoolThread()) {
                new Thread(parser).start();
            } else {
                // In synchronous mode everything should be run on one thread
                parser.run();
            }
        } else {
            AsyncHttpClient.log.v(TAG, "response body is null, calling onFailure(Throwable, JsonNode)");
            onFailure(statusCode, headers, throwable, (JsonNode) null);
        }
    }

    protected JsonNode parseResponse(byte[] responseBody) throws IOException {
        String jsonString = IOUtils.toString(responseBody, Charsets.UTF_8.displayName());
        if (jsonString == null) return new ObjectNode(JsonNodeFactory.instance);
        return mMapper.readValue(jsonString, JsonNode.class);
    }

}