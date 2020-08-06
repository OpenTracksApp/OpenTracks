package de.dennisguse.opentracks.io.file.post_workout_export;

import android.content.Context;

import java.io.OutputStream;

import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.io.file.exporter.TrackExporter;

public class LastTrackExportThread extends Thread {

    private OutputStream outputStream;
    private Context context;
    private TrackFileFormat trackFileFormat;

    LastTrackExportThread(OutputStream outputStream, Context context, TrackFileFormat trackFileFormat) {
        this.outputStream = outputStream;
        this.context = context;
        this.trackFileFormat = trackFileFormat;
    }

    @Override
    public void run() {
        ContentProviderUtils contentProviderUtils = new ContentProviderUtils(context);
        Track track = contentProviderUtils.getLastTrack();
        exportTrack(track);
    }

    private void exportTrack(Track track) {
        TrackExporter trackExporter = trackFileFormat.newTrackExporter(context);
        trackExporter.writeTrack(new Track[]{track}, outputStream);
    }
}
