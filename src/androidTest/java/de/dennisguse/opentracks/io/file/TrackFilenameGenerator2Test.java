package de.dennisguse.opentracks.io.file;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import de.dennisguse.opentracks.data.models.Track;

//TODO Merge with TrackFilenameGeneratorTest whenever Junit5 gets available.
//https://github.com/android/android-test/issues/224
@RunWith(Parameterized.class)
public class TrackFilenameGenerator2Test {

    @Parameterized.Parameters
    public static Collection<String> data() {
        return List.of(
                "{name}_{starime}",
                "{name",
                "name}");
    }

    private final TrackFilenameGenerator subject;

    public TrackFilenameGenerator2Test(String template) {
        this.subject = new TrackFilenameGenerator(template);
    }

    @Test(expected = TrackFilenameGenerator.TemplateInvalidException.class)
    public void testFilenameTemplate() {
        // given
        Track track = new Track();
        track.setName("Best Track");
        track.setUuid(UUID.fromString("f81d4fae-7dec-11d0-a765-00a0c91e6bf6"));
        track.getTrackStatistics().setStartTime(Instant.parse("2020-02-02T02:02:02Z"));

        // when
        String filename = subject.format(track, TrackFileFormat.GPX);
    }
}