package de.dennisguse.opentracks.io.file.importer;

import android.database.sqlite.SQLiteConstraintException;
import android.util.Log;

import androidx.annotation.NonNull;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import de.dennisguse.opentracks.content.data.Track;

public class XMLImporter implements TrackImporter {

    private static final String TAG = XMLImporter.class.getSimpleName();

    private final TrackParser parser;

    public XMLImporter(TrackParser parser) {
        this.parser = parser;
    }

    @Override
    @NonNull
    public List<Track.Id> importFile(InputStream inputStream) throws ImportParserException, ImportAlreadyExistsException {
        try {
            SAXParserFactory.newInstance().newSAXParser().parse(inputStream, parser.getHandler());
            List<Track.Id> trackIds = parser.getImportTrackIds();
            return trackIds;
        } catch (IOException | SAXException | ParserConfigurationException | AbstractFileTrackImporter.ParsingException e) {
            Log.e(TAG, "Unable to import file", e);
            if (parser.getImportTrackIds().size() > 0) {
                parser.cleanImport();
            }
            throw new ImportParserException(e);
        } catch (SQLiteConstraintException e) {
            Log.e(TAG, "Unable to import file", e);
            throw new ImportAlreadyExistsException(e);
        }
    }

    interface TrackParser {
        DefaultHandler getHandler();

        List<Track.Id> getImportTrackIds();

        void cleanImport();
    }
}
