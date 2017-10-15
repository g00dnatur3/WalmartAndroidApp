package com.walmart.products.activity;

import android.app.Instrumentation;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;

import util.RecyclerViewMatcher;
import util.WalmartServiceIdlingResource;

import org.junit.After;
import org.junit.Before;

import static android.support.test.espresso.core.deps.guava.base.Preconditions.checkNotNull;

public abstract class BaseActivityTests {

    final String itemNameAt_0 = "Rose Cottage Girls' Hunter Green  Jacket Dress";

    final String itemNameAt_1 = "Wrangler Men's Relaxed Fit Jean";

    final String itemNameAt_12 = "Hanes - Men's Ankle Crew Socks, 6 Pairs";

    WalmartServiceIdlingResource mWalmartServiceIdlingResource;

    @Before
    public void before() {
        // register WalmartService IdlingResource
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        mWalmartServiceIdlingResource = new WalmartServiceIdlingResource(instrumentation.getTargetContext());
        Espresso.registerIdlingResources(mWalmartServiceIdlingResource);
        onBefore();
    }

    @After
    public void after() {
        Espresso.unregisterIdlingResources(mWalmartServiceIdlingResource);
        onAfter();
    }

    protected abstract void onBefore();

    protected abstract void onAfter();

    protected RecyclerViewMatcher withRecyclerView(final int recyclerViewId) {
        return new RecyclerViewMatcher(recyclerViewId);
    }
}
