package de.dennisguse.opentracks.io.file.post_workout_export;

import java.io.OutputStream;

import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.io.file.exporter.GpxTrackWriter;
import de.dennisguse.opentracks.io.file.exporter.SingleTrackExporter;
import de.dennisguse.opentracks.io.file.exporter.TrackWriter;

public class SingleTrackExportThread extends Thread {

    private Track track;
    private OutputStream outputStream;
    private ContentProviderUtils contentProviderUtils;

    SingleTrackExportThread(OutputStream outputStream, ContentProviderUtils contentProviderUtils) {
        this.outputStream = outputStream;
        this.contentProviderUtils = contentProviderUtils;
        this.track = contentProviderUtils.getLastTrack();
    }

    @Override
    public void run() {
        exportTrack(track);
    }


    private void exportTrack(Track track) {
        TrackWriter trackWriter = new GpxTrackWriter("OpenTracks");
        SingleTrackExporter trackExporter = new SingleTrackExporter(contentProviderUtils, trackWriter, track);

        trackExporter.writeTrack(outputStream);
    }
}
