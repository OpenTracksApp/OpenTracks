package de.dennisguse.opentracks.io.file.importer;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.util.FileUtils;

public class ImportViewModel extends AndroidViewModel {

    private MutableLiveData<Summary> importData;
    private final Summary summary;
    private boolean cancel = false;
    private final List<DocumentFile> filesToImport = new ArrayList<>();

    public ImportViewModel(@NonNull Application application) {
        super(application);
        summary = new Summary();
    }

    LiveData<Summary> getImportData(List<DocumentFile> documentFiles) {
        if (importData == null) {
            importData = new MutableLiveData<>();
            loadData(documentFiles);
        }
        return importData;
    }

    void cancel() {
        //TODO Cancel for import is currently not implemented.
        cancel = true;
    }

    private void loadData(List<DocumentFile> documentFiles) {
        List<ArrayList<DocumentFile>> nestedFileList = documentFiles.stream()
                .map(FileUtils::getFiles)
                .collect(Collectors.toList());

        List<DocumentFile> fileList = new ArrayList<>();
        nestedFileList.forEach(fileList::addAll);

        summary.totalCount = fileList.size();
        filesToImport.addAll(fileList);
        importNextFile();
    }

    //TODO This should be happen in ImportActivity, right? (all of this is business logic and not just data)
    private void importNextFile() {
        if (cancel || filesToImport.isEmpty()) {
            return;
        }

        final DocumentFile documentFile = filesToImport.get(0);

        WorkManager workManager = WorkManager.getInstance(getApplication());
        WorkRequest importRequest = new OneTimeWorkRequest.Builder(ImportWorker.class)
                .setInputData(new Data.Builder()
                        .putString(ImportWorker.URI_KEY, documentFile.getUri().toString())
                        .build())
                .build();

        workManager
                .getWorkInfoByIdLiveData(importRequest.getId())
                .observe(this, workInfo -> {
                    if (workInfo != null) {
                        WorkInfo.State state = workInfo.getState();
                        if (state.isFinished()) {
                            switch (state) {
                                case SUCCEEDED -> {
                                    summary.importedTrackIds.addAll(
                                            Arrays.stream(workInfo.getOutputData().getLongArray(ImportWorker.RESULT_SUCCESS_LIST_TRACKIDS_KEY))
                                                    .mapToObj(Track.Id::new)
                                                    .toList());

                                    summary.successCount++;
                                }
                                case FAILED -> {
                                    if (workInfo.getOutputData().getBoolean(ImportWorker.RESULT_FAILURE_IS_DUPLICATE, false)) {
                                        summary.existsCount++;
                                    } else {
                                        // Some error happened
                                        String errorMessage = workInfo.getOutputData().getString(ImportWorker.RESULT_MESSAGE_KEY);
                                        summary.fileErrors.add(getApplication().getString(R.string.import_error_info, documentFile.getName(), errorMessage));
                                    }
                                }
                            }

                            importData.postValue(summary);
                            importNextFile();
                        }
                    }
                });

        workManager.enqueue(importRequest);
        filesToImport.remove(0);
    }

    static class Summary {
        private int totalCount;
        private int successCount;
        private int existsCount;
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
            return fileErrors.size();
        }

        public ArrayList<Track.Id> getImportedTrackIds() {
            return importedTrackIds;
        }

        public ArrayList<String> getFileErrors() {
            return fileErrors;
        }
    }
}
