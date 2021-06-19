package de.dennisguse.opentracks;


import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.ViewInteraction;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.runner.AndroidJUnit4;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;

import de.dennisguse.opentracks.content.data.Layout;
import de.dennisguse.opentracks.util.PreferencesUtils;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.stream.Collectors;

import static de.dennisguse.opentracks.RecyclerViewAssertions.withRowContaining;
import static org.hamcrest.Matchers.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class EspressoCustomLayoutTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    @Rule
    public ActivityTestRule<TrackListActivity> mActivityTestRule = new ActivityTestRule<>(TrackListActivity.class);

    @Rule
    public GrantPermissionRule mGrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.ACCESS_FINE_LOCATION");

    @Test
    public void customLayoutTest() {
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

        SharedPreferences sharedPreferences = PreferencesUtils.getSharedPreferences(context);
        Layout layout = PreferencesUtils.getCustomLayout(sharedPreferences, context);
        for (Layout.Field field : layout.getFields().stream().filter(Layout.Field::isVisible).collect(Collectors.toList())) {
            onView(withId(R.id.stats_recycler_view))
                    .check(withRowContaining(withText(field.getTitle().toUpperCase())));
        }
    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
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
}
