package de.dennisguse.opentracks.io.file;

import de.dennisguse.opentracks.content.data.Track;

public class ExportFileName {

    public static String getExportFileNameByTrackId(Track.Id trackId, String trackFileFormatExtension) {
        return trackId.getId() + "." + trackFileFormatExtension;
    }
}
