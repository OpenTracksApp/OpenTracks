package de.dennisguse.opentracks.io.file;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import de.dennisguse.opentracks.data.models.Track;

//TODO Merge with TrackFilenameGeneratorTest whenever Junit5 gets available.
//https://github.com/android/android-test/issues/224
@RunWith(Parameterized.class)
public class TrackFilenameGeneratorTest2 {

    @Parameterized.Parameters
    public static Collection<String> data() {
        return Arrays.asList(
                "{name}_{starime}",
                "{name",
                "name}");
    }

    private final TrackFilenameGenerator subject;

    public TrackFilenameGeneratorTest2(String template) {
        this.subject = new TrackFilenameGenerator(template);
    }

    @Test(expected = TrackFilenameGenerator.TemplateInvalidException.class)
    public void testFilenameTemplate() {
        // given
        Track track = new Track();
        track.setName("Best Track");
        track.setUuid(UUID.fromString("0000fee0-0000-1000-8000-00805f9b34fb"));
        track.getTrackStatistics().setStartTime(Instant.parse("2020-02-02T02:02:02Z"));

        // when
        String filename = subject.format(track, TrackFileFormat.GPX);
    }
}