package de.dennisguse.opentracks;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.test.espresso.PerformException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.action.MotionEvents;
import androidx.test.espresso.util.HumanReadables;
import androidx.test.espresso.util.TreeIterables;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeoutException;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class EspressoEditTrackRecordingTest {

    @Rule
    public ActivityTestRule<TrackListActivity> mActivityTestRule = new ActivityTestRule<>(TrackListActivity.class);

    @Rule
    public GrantPermissionRule mGrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.ACCESS_FINE_LOCATION",
                    "android.permission.ACCESS_COARSE_LOCATION");

    @LargeTest
    @Test
    public void espressoEditTrackRecordingTest() {
        {
            // TrackListActivity: start recording
            ViewInteraction trackControllerRecordButton = onView(withId(R.id.controller_record));
            trackControllerRecordButton.perform(click());
        }
        {
            // TrackRecordingActivity
            ViewInteraction trackControllerRecordButton = onView(withId(R.id.controller_record));
            ViewInteraction trackControllerStopButton = onView(withId(R.id.controller_stop));

            // wait; stay recording
            trackControllerRecordButton.perform(waitFor(15000));

            // open menu
            ViewInteraction overflowMenuButton = onView(
                    allOf(withContentDescription("More options"),
                            childAtPosition(
                                    childAtPosition(
                                            withId(R.id.toolbar),
                                            2),
                                    2),
                            isDisplayed()));
            overflowMenuButton.perform(click());

            // click on edit
            ViewInteraction appCompatTextView = onView(
                    allOf(withId(R.id.title), withText("Edit"),
                            childAtPosition(
                                    childAtPosition(
                                            withId(R.id.content),
                                            0),
                                    0),
                            isDisplayed()));
            appCompatTextView.perform(click());

            // change name for "New Name"
            ViewInteraction textInputEditText = onView(
                    allOf(withId(R.id.track_edit_name),
                            childAtPosition(
                                    childAtPosition(
                                            withClassName(is("com.google.android.material.textfield.TextInputLayout")),
                                            0),
                                    1)));
            textInputEditText.perform(scrollTo(), replaceText("New Name"));

            ViewInteraction textInputEditText2 = onView(
                    allOf(withId(R.id.track_edit_name), withText("New Name"),
                            childAtPosition(
                                    childAtPosition(
                                            withClassName(is("com.google.android.material.textfield.TextInputLayout")),
                                            0),
                                    1),
                            isDisplayed()));
            textInputEditText2.perform(closeSoftKeyboard());

            // save edition
            ViewInteraction appCompatButton = onView(
                    allOf(withId(R.id.track_edit_save),
                            childAtPosition(
                                    childAtPosition(
                                            withClassName(is("android.widget.LinearLayout")),
                                            3),
                                    1),
                            isDisplayed()));
            appCompatButton.perform(click());

            //pause
            trackControllerRecordButton.perform(veryLongTouch(1600));

            // wait; stay paused
            trackControllerRecordButton.perform(waitFor(1000));

            // resume
            trackControllerRecordButton.perform(click());

            // wait; stay recording
            trackControllerRecordButton.perform(waitFor(1000));

            // stop;
            trackControllerStopButton.perform(veryLongTouch(1600));

            // check track's name is "New Name"
            ViewInteraction textView = onView(
                    allOf(withId(R.id.stats_name_value),
                            withParent(withParent(IsInstanceOf.instanceOf(android.widget.ScrollView.class))),
                            isDisplayed()));
            textView.check(matches(withText("New Name")));
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

    public static ViewAction waitId(final int viewId, final long millis) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isRoot();
            }

            @Override
            public String getDescription() {
                return "wait for a specific view with id <" + viewId + "> during " + millis + " millis.";
            }

            @Override
            public void perform(final UiController uiController, final View view) {
                uiController.loopMainThreadUntilIdle();
                final long startTime = System.currentTimeMillis();
                final long endTime = startTime + millis;
                final Matcher<View> viewMatcher = withId(viewId);

                do {
                    for (View child : TreeIterables.breadthFirstViewTraversal(view)) {
                        // found view with required ID
                        if (viewMatcher.matches(child)) {
                            return;
                        }
                    }

                    uiController.loopMainThreadForAtLeast(50);
                }
                while (System.currentTimeMillis() < endTime);

                // timeout happens
                throw new PerformException.Builder()
                        .withActionDescription(this.getDescription())
                        .withViewDescription(HumanReadables.describe(view))
                        .withCause(new TimeoutException())
                        .build();
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

    private static ViewAction veryLongTouch(final int duration_ms) {
        return new ViewAction() {
            @Override
            public String getDescription() {
                return "Perform long touch.";
            }

            @Override
            public Matcher<View> getConstraints() {
                return isDisplayed();
            }

            @Override
            public void perform(UiController uiController, final View view) {
                // Get view absolute position
                int[] location = new int[2];
                view.getLocationOnScreen(location);

                // Offset coordinates by view position
                float[] coordinates = new float[]{location[0] + 1, location[1] + 1};

                // Send down event, pause, and send up
                MotionEvent down = MotionEvents.sendDown(uiController, coordinates, new float[]{1f, 1f}).down;
                uiController.loopMainThreadForAtLeast(duration_ms);
                MotionEvents.sendUp(uiController, down, coordinates);
            }
        };
    }
}
