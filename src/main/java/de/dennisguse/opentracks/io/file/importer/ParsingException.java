package de.dennisguse.opentracks.io.file.importer;

import androidx.annotation.NonNull;

public class ParsingException extends RuntimeException {

    protected ParsingException(@NonNull String message) {
        super(message);
    }

    protected ParsingException(@NonNull String message, Exception cause) {
        super(message, cause);
    }

    @NonNull
    @Override
    public String toString() {
        return "" + getMessage();
    }
}
