package de.dennisguse.opentracks.io.file.importer;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import de.dennisguse.opentracks.data.models.Track;

/**
 * Uses SAX2 to parse XML files.
 * <p>
 * NOTE: SAX2 always closes InputStreams after processing.
 */
public class XMLImporter {

    private static final String TAG = XMLImporter.class.getSimpleName();

    private final TrackParser parser;

    public XMLImporter(TrackParser parser) {
        this.parser = parser;
    }

    @NonNull
    public List<Track.Id> importFile(Context context, Uri uri) throws ImportParserException, ImportAlreadyExistsException, IOException {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            return importFile(inputStream);
        }
    }

    public List<Track.Id> importFile(InputStream inputStream) throws ImportParserException, ImportAlreadyExistsException, IOException {
        try {
            SAXParserFactory.newInstance().newSAXParser().parse(inputStream, parser.getHandler());
            return parser.getImportTrackIds();
        } catch (SAXException | ParserConfigurationException | ParsingException e) {
            Log.e(TAG, "Unable to import file", e);
            if (!parser.getImportTrackIds().isEmpty()) {
                parser.cleanImport();
            }
            throw new ImportParserException(e);
        } catch (SQLiteConstraintException e) {
            Log.e(TAG, "Unable to import file", e);
            throw new ImportAlreadyExistsException(e);
        }
    }

    interface TrackParser {
        @Deprecated
        DefaultHandler getHandler();

        List<Track.Id> getImportTrackIds();

        void cleanImport();
    }
}
