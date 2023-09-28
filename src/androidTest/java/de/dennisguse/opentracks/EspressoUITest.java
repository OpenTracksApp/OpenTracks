package de.dennisguse.opentracks;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withParentIndex;
import static org.hamcrest.Matchers.allOf;
import static de.dennisguse.opentracks.util.EspressoUtils.selectTabAtIndex;
import static de.dennisguse.opentracks.util.EspressoUtils.waitFor;

import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class EspressoUITest {

    @Rule
    public ActivityScenarioRule<TrackListActivity> mActivityTestRule = new ActivityScenarioRule<>(TrackListActivity.class);

    @Rule
    public GrantPermissionRule mGrantPermissionRule = TestUtil.createGrantPermissionRule();
    @LargeTest
    @Test
    public void record_stop_resume_stop_finish() {
        {
            // TrackListActivity: start recording
            ViewInteraction trackControllerRecordButton = onView(withId(R.id.track_list_fab_action));
            trackControllerRecordButton.perform(click());
        }
        {
            // TrackRecordingActivity: wait to record some time and then stop
            onView(withId(R.id.track_recording_fab_action))
                    .perform(waitFor(5000))
                    .perform(longClick());

            // TrackStoppedActivity: resume
            onView(allOf(withId(R.id.resume_button), isClickable()))
                    .perform(click());

            // TrackRecordingActivity: wait and then stop
            onView(withId(R.id.track_recording_fab_action))
                    .perform(waitFor(5000))
                    .perform(longClick());

            // TrackStoppedActivity
            onView(withId(R.id.finish_button))
                    .perform(click());
        }
    }

    @LargeTest
    @Test
    public void record_move_through_tabs() {
        {
            // TrackListActivity: start recording
            ViewInteraction trackControllerRecordButton = onView(withId(R.id.track_list_fab_action));
            trackControllerRecordButton.perform(click());
        }
        {
            // TrackRecordingActivity
            ViewInteraction tabLayout = onView(withId(R.id.track_detail_activity_tablayout));
            ViewInteraction trackControllerStopButton = onView(withId(R.id.track_recording_fab_action));

            tabLayout.perform(selectTabAtIndex(1));
            tabLayout.perform(waitFor(1000));

            tabLayout.perform(selectTabAtIndex(2));
            tabLayout.perform(waitFor(1000));

            tabLayout.perform(selectTabAtIndex(3));
            tabLayout.perform(waitFor(1000));

            tabLayout.perform(selectTabAtIndex(0));
            tabLayout.perform(waitFor(1000));

            // stop
            trackControllerStopButton.perform(longClick());
        }
    }

    @LargeTest
    @Test
    public void selectAndDeleteTrack() {
        onView(withId(R.id.track_list)).check(matches(isDisplayed()));
        onView(allOf(withParent(withId(R.id.track_list)), withParentIndex(0))).perform(longClick());
    }
}
