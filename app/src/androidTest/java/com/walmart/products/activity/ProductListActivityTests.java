package com.walmart.products.activity;

import android.support.test.rule.ActivityTestRule;
import android.view.View;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;

import com.walmart.products.Application;
import com.walmart.products.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static android.support.test.espresso.contrib.RecyclerViewActions.scrollToPosition;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

public class ProductListActivityTests extends BaseActivityTests {

    @Rule
    public ActivityTestRule<ProductListActivity> mActivityRule = new ActivityTestRule<>(ProductListActivity.class);

    @Test
    public void test_clickItem() {

        onView(withText("Walmart Products")).check(matches(isDisplayed()));

        // match first item has correct name
        onView(withRecyclerView(R.id.recycler_view).atPosition(0))
                .check(matches(hasDescendant(withText(itemNameAt_0))));

        // scroll to item 12
        onView(withId(R.id.recycler_view)).perform(scrollToPosition(12));

        // verify item name at 12
        onView(withRecyclerView(R.id.recycler_view).atPosition(12))
                .check(matches(hasDescendant(withText(itemNameAt_12))));

        // click on 12
        onView(withId(R.id.recycler_view)).perform(actionOnItemAtPosition(12, click()));

        // verify you are now at product detaills
        onView(withText("Product Details")).check(matches(isDisplayed()));

        // verify you are on correct item
        onView( allOf(isDisplayed(), withId(R.id.detail_name))).check(matches(withText(itemNameAt_12)));
    }

    @Override
    protected void onBefore() {
    }

    @Override
    protected void onAfter() {
    }

}
