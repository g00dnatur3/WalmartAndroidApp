package com.walmart.products.service;

import android.graphics.Bitmap;
import android.util.Log;
import android.util.LruCache;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.loopj.android.http.AsyncHttpClient;
import com.walmart.products.util.EventEmitter;
import com.walmart.products.util.Function;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import static com.walmart.products.service.WalmartService.CacheEntry;
import static com.walmart.products.service.WalmartServiceConfig.*;
import static com.walmart.products.service.WalmartService.CacheEntry;

@RunWith(MockitoJUnitRunner.class)
public class WalmartServiceTests {

    protected final String TAG = getClass().getCanonicalName();

    /** injected into walmartService **/
    @Spy
    Map<Integer, Boolean> mSpyPagesLoading = new HashMap<Integer, Boolean>();
    @Mock
    WalmartServiceUtils mMockUtils;
    @Mock
    EventEmitter mMockEmitter;

    /** not injected into walmartService **/
    @Mock
    JsonNode mMockPageNode;
    @Mock
    JsonNode mMockItemNode;
    @Mock
    ArrayNode mMockItemsNode;
    @Mock
    Bitmap mMockBitmap;
    @Spy
    Function mSpyOnComplete = new Function() {
        @Override
        public void call(Object... args) {}
    };

    CacheEntry mSpyCacheEntry; // spy created in setup

    /** object under test **/
    @InjectMocks
    WalmartService walmartService = new WalmartService();

    /** test values **/
    final int indexLoaded = 0;
    final int indexNotLoaded = 1;
    final String mediumKey = (indexLoaded % PAGE_SIZE) + ".medium";
    final String mediumImageUrl = "mediumImageUrl";
    final int fromIndex = 0;
    int toIndex = 0;

    @Before
    public void setup() {

        mSpyCacheEntry = spy(new CacheEntry(mMockPageNode));
        when(mMockUtils.getPage(indexLoaded)).thenReturn(mSpyCacheEntry);
        when(mMockUtils.getPage(indexNotLoaded)).thenReturn(null);
        when(mMockUtils.getItemsNode(mMockPageNode)).thenReturn(mMockItemsNode);
        when(mMockItemsNode.get(indexLoaded % PAGE_SIZE)).thenReturn(mMockItemNode);
    }

    @Test
    public void test_getProduct() {

        // verify product is returned from cache
        assertEquals(mMockItemNode, walmartService.getProduct(indexLoaded));
        verify(mMockUtils, times(1)).getPage(indexLoaded);
        verify(mSpyCacheEntry, times(1)).page();
        verify(mMockUtils, times(1)).getItemsNode(mMockPageNode);

        // verify null is returned when product not in cache
        assertEquals(null, walmartService.getProduct(indexNotLoaded));
    }

    @Test
    public void test_getThumbnail() {

        // verify thumbnail is returned from cache
        assertEquals(null, walmartService.getThumbnail(indexLoaded));
        String key = (indexLoaded % PAGE_SIZE) + ".thumb";
        mSpyCacheEntry.bitmaps().put(key, mMockBitmap);
        assertEquals(mMockBitmap, walmartService.getThumbnail(indexLoaded));
    }

    @Test
    public void test_getMediumImage_part1() {

        // verify error when no mediumImage field on itemNode
        walmartService.getMediumImage(indexLoaded, mSpyOnComplete);
        verify(mSpyOnComplete).call("getMediumImage failed - mediumImage url is empty");
        reset(mSpyOnComplete);

        // verify image is loaded from utils.loadBitmap()
        final JsonNode mockImageNode = mock(JsonNode.class);
        when(mMockItemNode.get("mediumImage")).thenReturn(mockImageNode);
        when(mockImageNode.textValue()).thenReturn(mediumImageUrl);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                ((Function) args[3]).call(null, mediumKey, mMockBitmap);
                return null;
            }
        }).when(mMockUtils).loadBitmap(eq(walmartService), eq(mediumImageUrl), eq(mediumKey), any(Function.class));
        walmartService.getMediumImage(indexLoaded, mSpyOnComplete);
        verify(mSpyOnComplete).call(null, mMockBitmap);
        reset(mSpyOnComplete);

        // verify image is loaded from page cache
        mSpyCacheEntry.bitmaps().put(mediumKey, mMockBitmap);
        walmartService.getMediumImage(indexLoaded, mSpyOnComplete);
        verify(mSpyOnComplete).call(null, mMockBitmap);
        reset(mSpyOnComplete);
    }

    @Test
    public void test_getMediumImage_part2() {
        // verify error when there is no itemNode
        when(mMockItemsNode.get(indexLoaded % PAGE_SIZE)).thenReturn(null);
        walmartService.getMediumImage(indexLoaded, mSpyOnComplete);
        verify(mSpyOnComplete).call("getMediumImage failed - item not found");
        reset(mSpyOnComplete);

        // verify error when there is no itemsNode
        when(mMockUtils.getItemsNode(mMockPageNode)).thenReturn(null);
        walmartService.getMediumImage(indexLoaded, mSpyOnComplete);
        verify(mSpyOnComplete).call("getMediumImage failed - itemsNode is null");
        reset(mSpyOnComplete);

        // verify error when there is no pageNode
        when(mMockUtils.getPage(indexLoaded)).thenReturn(null);
        walmartService.getMediumImage(indexLoaded, mSpyOnComplete);
        verify(mSpyOnComplete).call("getMediumImage failed - page not loaded");
    }

    @Test
    public void test_loadProducts_OnePage() {

        toIndex = PAGE_SIZE-1; // range is one page

        // verify load one page success
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                ((Function) args[4]).call(null, null);
                return null;
            }
        }).when(mMockUtils).loadPage(eq(mMockEmitter),
                eq(walmartService), eq(mSpyPagesLoading), eq(toIndex / PAGE_SIZE), any(Function.class));

        walmartService.loadProducts(fromIndex, toIndex, mSpyOnComplete);
        verify(mSpyOnComplete).call(null, null);
        verify(mMockUtils, times(1)).loadPage(eq(mMockEmitter),
                eq(walmartService), eq(mSpyPagesLoading), eq(toIndex / PAGE_SIZE), any(Function.class));
        reset(mSpyOnComplete);
        reset(mMockUtils);

        // verify load one page error
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                ((Function) args[4]).call("some error");
                return null;
            }
        }).when(mMockUtils).loadPage(
                eq(mMockEmitter),
                eq(walmartService),
                eq(mSpyPagesLoading),
                eq(fromIndex / PAGE_SIZE),
                any(Function.class));
        walmartService.loadProducts(fromIndex, toIndex, mSpyOnComplete);

        verify(mMockUtils, times(1)).loadPage(eq(mMockEmitter),
                eq(walmartService), eq(mSpyPagesLoading), eq(toIndex / PAGE_SIZE), any(Function.class));
        verify(mSpyOnComplete).call("some error");
    }

    @Test
    public void test_loadProducts_TwoPages() {

        toIndex = (PAGE_SIZE*2)-1; // range is two pages

        // needed to ensure loadPage is being called with correct pageNums
        final List<Integer> pageNums = new ArrayList<Integer>();
        pageNums.add(fromIndex / PAGE_SIZE);
        pageNums.add(toIndex / PAGE_SIZE);

        // verify load two page success
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                int pageNum = (Integer) args[3];

                // check page is valid for given inputs - 0 or 1
                assertTrue(pageNum <= (toIndex / PAGE_SIZE));

                // check being called with correct pageNums
                int size = pageNums.size();
                pageNums.remove((Integer) pageNum);
                assertTrue(size == (pageNums.size()+1)); // assert that an item was removed

                ((Function) args[4]).call(null, null);
                return null;
            }
        }).when(mMockUtils).loadPage(
                eq(mMockEmitter),
                eq(walmartService),
                eq(mSpyPagesLoading),
                anyInt(),
                any(Function.class));

        walmartService.loadProducts(fromIndex, toIndex, mSpyOnComplete);
        verify(mSpyOnComplete).call(null, null);

        verify(mMockUtils, times(2)).loadPage(eq(mMockEmitter),
                eq(walmartService), eq(mSpyPagesLoading), anyInt(), any(Function.class));
        reset(mSpyOnComplete);
    }
}
