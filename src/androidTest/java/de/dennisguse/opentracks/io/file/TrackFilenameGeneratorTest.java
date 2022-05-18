package de.dennisguse.opentracks.io.file;

import static org.junit.Assert.assertEquals;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.TimeZone;
import java.util.UUID;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TimezoneRule;
import de.dennisguse.opentracks.data.models.Track;

@RunWith(Parameterized.class)
public class TrackFilenameGeneratorTest {

    @Rule
    public TimezoneRule timezoneRule = new TimezoneRule(TimeZone.getTimeZone("Europe/Berlin"));

    @Parameterized.Parameters
    public static Collection<String[]> data() {
        return Arrays.asList(new String[][]{
                {"{uuid}_{name}", "0000fee0_Best Track.gpx"},
                {"{name}_{uuid}", "Best Track_0000fee0.gpx"},
                {"{time}_{name}", "02_02_02_Best Track.gpx"},
                {"{date}_{name}", "2020-02-02_Best Track.gpx"},
                {ApplicationProvider.getApplicationContext().getString(R.string.export_filename_format_default), "2020-02-02_02_02_02_Best Track.gpx"},
        });
    }

    private final TrackFilenameGenerator subject;
    private final String expected;

    public TrackFilenameGeneratorTest(String template, String expected) {
        this.subject = new TrackFilenameGenerator(template);
        this.expected = expected;
    }

    @Test
    public void testFilenameTemplate() {
        // given
        Track track = new Track();
        track.setName("Best Track");
        track.setUuid(UUID.fromString("0000fee0-0000-1000-8000-00805f9b34fb"));
        track.getTrackStatistics().setStartTime(Instant.parse("2020-02-02T02:02:02Z"));

        // when
        String filename = subject.format(track, TrackFileFormat.GPX);

        // then
        assertEquals(expected, filename);
    }
}