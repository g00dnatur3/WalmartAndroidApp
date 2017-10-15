package com.walmart.products.activity;

import android.support.test.rule.ActivityTestRule;

import org.junit.Rule;

public class ProductListActivityTests extends BaseActivityTests {

    @Rule
    public ActivityTestRule<ProductListActivity> mActivityRule = new ActivityTestRule<>(ProductListActivity.class);

    @Override
    protected void onBefore() {

    }

    @Override
    protected void onAfter() {

    }
}
