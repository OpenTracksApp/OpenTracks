package de.dennisguse.opentracks.io.file;

import androidx.documentfile.provider.DocumentFile;

import de.dennisguse.opentracks.content.data.Track;

public class ExportFile {

    public static DocumentFile getExportDocumentFile(Track.Id trackId, String trackFileFormatExtension, DocumentFile directory, String mimeType) {
        String fileName = getExportFileNameByTrackId(trackId, trackFileFormatExtension);
        DocumentFile file = directory.findFile(fileName);
        if (file == null) {
            file = directory.createFile(mimeType, fileName);
        }
        return file;
    }

    private static String getExportFileNameByTrackId(Track.Id trackId, String trackFileFormatExtension) {
        return trackId.getId() + "." + trackFileFormatExtension;
    }
}
