package com.walmart.products.activity;

import android.app.Instrumentation;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.view.ViewPager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.action.ViewActions.swipeRight;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import com.walmart.products.R;

@RunWith(AndroidJUnit4.class)
public class ProductDetailActivityTests {

    final String firstProductName = "Rose Cottage Girls' Hunter Green  Jacket Dress";

    final String secondProductName = "Wrangler Men's Relaxed Fit Jean";

    @Rule
    public ActivityTestRule<ProductDetailActivity> mActivityRule = new ActivityTestRule<>(ProductDetailActivity.class);

    private WalmartServiceIdlingResource walmartServiceIdlingResource;
    private ViewPagerIdlingResource viewPagerIdlingResource;

    @Before
    public void registerIntentServiceIdlingResource() {
        // create & register IdlingResources
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        walmartServiceIdlingResource = new WalmartServiceIdlingResource(instrumentation.getTargetContext());
        Espresso.registerIdlingResources(walmartServiceIdlingResource);
        viewPagerIdlingResource = new ViewPagerIdlingResource(
                (ViewPager) mActivityRule.getActivity().findViewById(R.id.view_pager));
        Espresso.registerIdlingResources(viewPagerIdlingResource);
    }

    @After
    public void unregisterIntentServiceIdlingResource() {
        Espresso.unregisterIdlingResources(walmartServiceIdlingResource);
        Espresso.unregisterIdlingResources(viewPagerIdlingResource);
    }

    @Test
    public void testSwipeBetweenFirstAndSecondPage() {
        onView(withText("Product Details")).check(matches(isDisplayed()));
        onView( allOf(isDisplayed(), withId(R.id.detail_name))).check(matches(withText(firstProductName)));
        onView(ViewMatchers.withId(R.id.view_pager)).perform(swipeLeft());
        onView( allOf(isDisplayed(), withId(R.id.detail_name))).check(matches(withText(secondProductName)));
    }

}