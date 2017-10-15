package com.walmart.products.activity;

import android.support.test.espresso.Espresso;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.view.ViewPager;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import com.walmart.products.R;
import util.ViewPagerIdlingResource;

@RunWith(AndroidJUnit4.class)
public class ProductDetailActivityTests extends BaseActivityTests {

    ViewPagerIdlingResource mViewPagerIdlingResource;

    @Rule
    public ActivityTestRule<ProductDetailActivity> mActivityRule = new ActivityTestRule<>(ProductDetailActivity.class);

    @Test
    public void test_swipeBetweenFirstAndSecondPage() {
        onView(withText("Product Details")).check(matches(isDisplayed()));
        onView( allOf(isDisplayed(), withId(R.id.detail_name))).check(matches(withText(itemNameAt_0)));
        onView(ViewMatchers.withId(R.id.view_pager)).perform(swipeLeft());
        onView( allOf(isDisplayed(), withId(R.id.detail_name))).check(matches(withText(itemNameAt_1)));
    }

    @Override
    protected void onBefore() {
        mViewPagerIdlingResource = new ViewPagerIdlingResource(
                (ViewPager) mActivityRule.getActivity().findViewById(R.id.view_pager));
        Espresso.registerIdlingResources(mViewPagerIdlingResource);
    }

    @Override
    protected void onAfter() {
        Espresso.unregisterIdlingResources(mViewPagerIdlingResource);
    }
}