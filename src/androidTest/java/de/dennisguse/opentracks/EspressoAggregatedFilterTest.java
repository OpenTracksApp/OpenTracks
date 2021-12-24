package de.dennisguse.opentracks;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import android.util.Pair;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.runner.AndroidJUnit4;

import org.hamcrest.core.IsInstanceOf;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import de.dennisguse.opentracks.content.data.TestDataUtil;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class EspressoAggregatedFilterTest {

    private final String CATEGORY = "category";

    @Rule
    public ActivityScenarioRule<TrackListActivity> mActivityTestRule = new ActivityScenarioRule<>(TrackListActivity.class);

    @Rule
    public GrantPermissionRule mGrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

    @Before
    public void setUp() {
        Pair<Track, List<TrackPoint>> pair = TestDataUtil.createTrack(new Track.Id(System.currentTimeMillis()), 20);
        pair.first.setCategory(CATEGORY);
        TestDataUtil.insertTrackWithLocations(new ContentProviderUtils(ApplicationProvider.getApplicationContext()), pair.first, pair.second);
    }

    @Test
    public void espressoAggregatedFilterTest() {
        // open AggregatedStatisticsActivity through toolbar's menu item
        onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
        onView(withId(R.id.toolbar)).check(matches(hasDescendant(withId(R.id.track_list_aggregated_stats))));
        onView(withId(R.id.track_list_aggregated_stats)).perform(click());

        // open FilterDialogFragment through toolbar's menu item
        onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
        onView(withId(R.id.toolbar)).check(matches(hasDescendant(withId(R.id.aggregated_statistics_filter))));
        onView(withId(R.id.aggregated_statistics_filter)).perform(click());

        // check there's a checkbox with CATEGORY text
        ViewInteraction checkBox = onView(
                allOf(withId(R.id.filter_dialog_check_button), withText(CATEGORY),
                        withParent(allOf(withId(R.id.filter_items),
                                withParent(IsInstanceOf.<View>instanceOf(android.view.ViewGroup.class)))),
                        isDisplayed()));
        checkBox.check(matches(isDisplayed()));

        // check there's an edit text for "from date"
        onView(withId(R.id.filter_date_edit_text_from)).check(matches(isDisplayed()));

        // check there's an edit text for "to date"
        onView(withId(R.id.filter_date_edit_text_to)).check(matches(isDisplayed()));
    }
}
