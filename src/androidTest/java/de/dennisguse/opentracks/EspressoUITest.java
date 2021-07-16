package de.dennisguse.opentracks;

import android.view.MotionEvent;
import android.view.View;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.action.MotionEvents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import com.google.android.material.tabs.TabLayout;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.anything;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class EspressoUITest {

    @Rule
    public ActivityScenarioRule<TrackListActivity> mActivityTestRule = new ActivityScenarioRule<>(TrackListActivity.class);

    @Rule
    public GrantPermissionRule mGrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

    @LargeTest
    @Test
    public void record_pause_resume_stop() {
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
            trackControllerRecordButton.perform(waitFor(5000));

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
        }
    }

    @LargeTest
    @Test
    public void record_move_through_tabs() {
        {
            // TrackListActivity: start recording
            ViewInteraction trackControllerRecordButton = onView(withId(R.id.controller_record));
            trackControllerRecordButton.perform(click());
        }
        {
            // TrackRecordingActivity
            ViewInteraction tabLayout = onView(withId(R.id.track_detail_activity_tablayout));
            ViewInteraction trackControllerStopButton = onView(withId(R.id.controller_stop));

            tabLayout.perform(selectTabAtIndex(1));
            tabLayout.perform(waitFor(1000));

            tabLayout.perform(selectTabAtIndex(2));
            tabLayout.perform(waitFor(1000));

            tabLayout.perform(selectTabAtIndex(3));
            tabLayout.perform(waitFor(1000));

            tabLayout.perform(selectTabAtIndex(0));
            tabLayout.perform(waitFor(1000));

            // stop
            trackControllerStopButton.perform(veryLongTouch(1600));
        }
    }

    @LargeTest
    @Test
    public void selectAndDeleteTrack() {
        onView(withId(R.id.track_list)).check(matches(isDisplayed()));
        onData(anything()).inAdapterView(withId(R.id.track_list)).atPosition(0).perform(veryLongTouch(2000));
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

    private static ViewAction selectTabAtIndex(final int index) {
        return new ViewAction() {
            @Override
            public String getDescription() {
                return "Selecting tab.";
            }

            @Override
            public Matcher<View> getConstraints() {
                return isDisplayed();
            }

            @Override
            public void perform(UiController uiController, View view) {
                TabLayout tabLayout = (TabLayout) view;
                tabLayout.getTabAt(index).select();
            }
        };
    }
}
