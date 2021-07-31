package de.dennisguse.opentracks;


import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.is;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.test.espresso.DataInteraction;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

/**
 * Espresso test that checks the unit for activity category selected.
 * Also it checks that unit is properly when user change from a pace activity to an speed one.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class EspressoCategoryUnitTest {

    @Rule
    public ActivityScenarioRule<TrackListActivity> mActivityTestRule = new ActivityScenarioRule<>(TrackListActivity.class);

    @Rule
    public TestRule mRuntimePermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Rule
    public TestRule mLocaleRule = new PreferenceMetricUnitRule(context, true);

    @Test
    public void changeBetweenCategoryUpdateProperlyUnitTest() {
        // Start record.
        ViewInteraction appCompatImageButton = onView(
                allOf(withId(R.id.controller_record), withContentDescription("Record"),
                        childAtPosition(
                                allOf(withId(R.id.controller_container),
                                        childAtPosition(
                                                withId(R.id.controller_fragment),
                                                0)),
                                0),
                        isDisplayed()));
        appCompatImageButton.perform(click());

        {
            // Change to running activity (pace activity).
            ViewInteraction appCompatImageView = onView(
                    allOf(withId(R.id.stats_activity_type_icon),
                            childAtPosition(
                                    childAtPosition(
                                            withClassName(is("android.widget.ScrollView")),
                                            0),
                                    7)));
            appCompatImageView.perform(scrollTo(), click());

            DataInteraction imageView = onData(anything())
                    .inAdapterView(allOf(withId(R.id.choose_activity_type_grid_view),
                            childAtPosition(
                                    withClassName(is("android.widget.LinearLayout")),
                                    0)))
                    .atPosition(1);
            imageView.perform(click());
        }

        // Pace (min/km) in all fields.
        {
            ViewInteraction textView = onView(allOf(withId(R.id.stats_speed_unit)));
            textView.perform(waitFor(2000));
            textView.check(matches(withText("min/km")));
        }
        {
            ViewInteraction textView = onView(allOf(withId(R.id.stats_average_speed_unit)));
            textView.check(matches(withText("min/km")));
        }
        {
            ViewInteraction textView = onView(allOf(withId(R.id.stats_max_speed_unit)));
            textView.check(matches(withText("min/km")));
        }
        {
            ViewInteraction textView = onView(allOf(withId(R.id.stats_moving_speed_unit)));
            textView.check(matches(withText("min/km")));
        }

        {
            // Change to biking activity (speed activity).
            ViewInteraction appCompatImageView = onView(
                    allOf(withId(R.id.stats_activity_type_icon),
                            childAtPosition(
                                    childAtPosition(
                                            withClassName(is("android.widget.ScrollView")),
                                            0),
                                    7)));
            appCompatImageView.perform(scrollTo(), click());

            DataInteraction imageView = onData(anything())
                    .inAdapterView(allOf(withId(R.id.choose_activity_type_grid_view),
                            childAtPosition(
                                    withClassName(is("android.widget.LinearLayout")),
                                    0)))
                    .atPosition(9);
            imageView.perform(click());
        }

        // Speed (km/h) in all fields.
        {
            ViewInteraction textView = onView(allOf(withId(R.id.stats_speed_unit)));
            textView.perform(waitFor(2000));
            textView.check(matches(withText("km/h")));
        }
        {
            ViewInteraction textView = onView(allOf(withId(R.id.stats_average_speed_unit)));
            textView.check(matches(withText("km/h")));
        }
        {
            ViewInteraction textView = onView(allOf(withId(R.id.stats_max_speed_unit)));
            textView.check(matches(withText("km/h")));
        }
        {
            ViewInteraction textView = onView(allOf(withId(R.id.stats_moving_speed_unit)));
            textView.check(matches(withText("km/h")));
        }
    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }

    private static ViewAction waitFor(final long duration_ms) {
        return new ViewAction() {

            @Override
            public String getDescription() {
                return "Wait for milliseconds.";
            }

            @Override
            public Matcher<View> getConstraints() {
                return isDisplayed();
            }

            @Override
            public void perform(UiController uiController, final View view) {
                uiController.loopMainThreadForAtLeast(duration_ms);
            }
        };
    }
}
