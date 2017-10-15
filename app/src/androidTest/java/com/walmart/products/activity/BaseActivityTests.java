package com.walmart.products.activity;

import android.app.Instrumentation;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;

import org.junit.After;
import org.junit.Before;

public abstract class BaseActivityTests {

    protected final String firstProductName = "Rose Cottage Girls' Hunter Green  Jacket Dress";

    protected final String secondProductName = "Wrangler Men's Relaxed Fit Jean";

    protected WalmartServiceIdlingResource walmartServiceIdlingResource;

    protected ViewPagerIdlingResource viewPagerIdlingResource;

    @Before
    public void before() {
        // register WalmartService IdlingResource
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        walmartServiceIdlingResource = new WalmartServiceIdlingResource(instrumentation.getTargetContext());
        Espresso.registerIdlingResources(walmartServiceIdlingResource);
        onBefore();
    }

    @After
    public void after() {
        Espresso.unregisterIdlingResources(walmartServiceIdlingResource);
        onAfter();
    }

    protected abstract void onBefore();

    protected abstract void onAfter();
}
