package de.dennisguse.opentracks.io.file.exporter;

import android.content.Context;

import java.io.OutputStream;

import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.io.file.exporter.TrackExporter;

public class TrackExportThread extends Thread {

    private OutputStream outputStream;
    private Context context;
    private TrackFileFormat trackFileFormat;
    private Track track;

    public TrackExportThread(OutputStream outputStream, Context context, TrackFileFormat trackFileFormat, Track track) {
        this.outputStream = outputStream;
        this.context = context;
        this.trackFileFormat = trackFileFormat;
        this.track = track;
    }

    @Override
    public void run() {
        TrackExporter trackExporter = trackFileFormat.newTrackExporter(context);
        trackExporter.writeTrack(track, outputStream);
    }
}
