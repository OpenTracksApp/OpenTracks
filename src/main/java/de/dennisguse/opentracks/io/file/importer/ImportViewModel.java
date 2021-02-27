package de.dennisguse.opentracks.io.file.importer;

import android.app.Application;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.util.FileUtils;

public class ImportViewModel extends AndroidViewModel implements ImportServiceResultReceiver.Receiver {

    private static final String TAG = ImportViewModel.class.getSimpleName();

    private MutableLiveData<Summary> importData;
    private final ImportServiceResultReceiver resultReceiver;
    private final Summary summary;
    private boolean cancel = false;
    private final List<DocumentFile> filesToImport = new ArrayList<>();

    public ImportViewModel(@NonNull Application application) {
        super(application);
        resultReceiver = new ImportServiceResultReceiver(new Handler(), this);
        summary = new Summary();
    }

    public LiveData<Summary> getImportData(DocumentFile documentFile) {
        if (importData == null) {
            importData = new MutableLiveData<>();
            loadData(documentFile);
        }
        return importData;
    }

    public void cancel() {
        cancel = true;
    }

    private void loadData(DocumentFile documentFile) {
        List<DocumentFile> fileList = FileUtils.getFiles(documentFile);
        summary.totalCount = fileList.size();
        filesToImport.addAll(fileList);
        importNextFile();
    }

    private void importNextFile() {
        if (cancel || filesToImport.isEmpty()) {
            return;
        }
        ImportService.enqueue(getApplication(), resultReceiver, filesToImport.get(0).getUri());
        filesToImport.remove(0);
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        if (resultData == null) {
            throw new RuntimeException(TAG + ": onReceiveResult resultData NULL");
        }

        Track.Id trackId = resultData.getParcelable(ImportServiceResultReceiver.RESULT_EXTRA_TRACK_ID);
        String fileName = resultData.getString(ImportServiceResultReceiver.RESULT_EXTRA_FILENAME);
        String message = resultData.getString(ImportServiceResultReceiver.RESULT_EXTRA_MESSAGE);

        switch (resultCode) {
            case ImportServiceResultReceiver.RESULT_CODE_ERROR:
                summary.errorCount++;
                summary.fileErrors.add(getApplication().getString(R.string.import_error_info, fileName, message));
                break;
            case ImportServiceResultReceiver.RESULT_CODE_IMPORTED:
                summary.importedTrackIds.add(trackId);
                summary.successCount++;
                break;
            case ImportServiceResultReceiver.RESULT_CODE_ALREADY_EXISTS:
                summary.existsCount++;
                break;
            default:
                throw new RuntimeException(TAG + ": import service result code invalid: " + resultCode);
        }

        importData.postValue(summary);
        importNextFile();
    }

    public static class Summary {
        private int totalCount;
        private int successCount;
        private int existsCount;
        private int errorCount;
        private final ArrayList<Track.Id> importedTrackIds = new ArrayList<>();
        private final ArrayList<String> fileErrors = new ArrayList<>();

        public int getTotalCount() {
            return totalCount;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getExistsCount() {
            return existsCount;
        }

        public int getErrorCount() {
            return errorCount;
        }

        public ArrayList<Track.Id> getImportedTrackIds() {
            return importedTrackIds;
        }

        public ArrayList<String> getFileErrors() {
            return fileErrors;
        }
    }
}
