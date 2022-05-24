package de.dennisguse.opentracks;


import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static de.dennisguse.opentracks.util.EspressoUtils.childAtPosition;
import static de.dennisguse.opentracks.util.EspressoUtils.waitFor;
import static de.dennisguse.opentracks.util.EspressoUtils.withListSize;

import android.app.ActivityManager;
import android.app.Instrumentation;
import android.content.Context;
import android.view.View;
import android.widget.ListView;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.dennisguse.opentracks.services.TrackDeleteService;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class EspressoDeleteTrackTest {

    @Rule
    public ActivityScenarioRule<TrackListActivity> mActivityTestRule = new ActivityScenarioRule<>(TrackListActivity.class);

    @Rule
    public GrantPermissionRule mGrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

    private MyIdlingResource idlingResource;

    @Before
    public void registerIntentServiceIdlingResource() {
        Instrumentation instrumentation
                = InstrumentationRegistry.getInstrumentation();
        idlingResource = new MyIdlingResource(instrumentation.getTargetContext());
        IdlingRegistry.getInstance().register(idlingResource);
    }

    @After
    public void unregisterIntentServiceIdlingResource() {
        IdlingRegistry.getInstance().unregister(idlingResource);
    }

    @Ignore("Test fails permanently")
    @Deprecated
    @Test
    public void espressoDeleteTrackTest() {
        int countBefore;

        {
            // TrackListActivity: start recording
            ViewInteraction trackControllerRecordButton = onView(withId(R.id.track_list_fab_action));
            trackControllerRecordButton.perform(click());
        }

        {
            // TrackRecordingActivity
            ViewInteraction trackControllerStopButton = onView(withId(R.id.track_recording_fab_action));

            // wait; stay recording
            trackControllerStopButton.perform(waitFor(5000));

            // stop;
            trackControllerStopButton.perform(longClick());
        }

        // back
        pressBack();

        // get number of items before deleting a track
        countBefore = numberOfItemsListView();

        // select track
        onData(anything()).inAdapterView(withId(R.id.track_list)).atPosition(0).perform(longClick());

        // open menu and delete selected track
        ViewInteraction overflowMenuButton = onView(
                allOf(withContentDescription("More options"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.bottom_app_bar),
                                        2),
                                2),
                        isDisplayed()));
        overflowMenuButton.perform(click());

        ViewInteraction appCompatTextView = onView(
                allOf(withId(R.id.title), withText("Delete"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.content),
                                        0),
                                0),
                        isDisplayed()));
        appCompatTextView.perform(click());

        ViewInteraction appCompatButton = onView(
                allOf(withId(android.R.id.button1), withText("Yes"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.buttonPanel),
                                        0),
                                3)));
        appCompatButton.perform(scrollTo(), click());

        // check number of items after delete
        onView(withId(R.id.track_list)).check(ViewAssertions.matches(withListSize(countBefore - 1)));
    }


    private int numberOfItemsListView() {
        final int[] counts = new int[1];
        onView(withId(R.id.track_list)).check(matches(new TypeSafeMatcher<View>() {
            @Override
            public boolean matchesSafely(View view) {
                ListView listView = (ListView) view;

                counts[0] = listView.getCount();

                return true;
            }

            @Override
            public void describeTo(Description description) {
            }
        }));

        return counts[0];
    }


    private static class MyIdlingResource implements IdlingResource {

        private final Context context;
        private ResourceCallback resourceCallback;

        public MyIdlingResource(Context context) {
            this.context = context;
        }

        @Override
        public String getName() {
            return MyIdlingResource.class.getName();
        }

        @Override
        public void registerIdleTransitionCallback(IdlingResource.ResourceCallback resourceCallback) {
            this.resourceCallback = resourceCallback;
        }

        @Override
        public boolean isIdleNow() {
            boolean idle = !isDeleteServiceRunning();
            if (idle && resourceCallback != null) {
                resourceCallback.onTransitionToIdle();
            }
            return idle;
        }

        private boolean isDeleteServiceRunning() {
            ActivityManager manager =
                    (ActivityManager) context.getSystemService(
                            Context.ACTIVITY_SERVICE);
            for (ActivityManager.RunningServiceInfo info :
                    manager.getRunningServices(Integer.MAX_VALUE)) {
                if (TrackDeleteService.class.getName().equals(
                        info.service.getClassName())) {
                    return true;
                }
            }
            return false;
        }
    }
}
