package de.dennisguse.opentracks.io.file.importer;

class ImportAlreadyExistsException extends RuntimeException {
    public ImportAlreadyExistsException(Exception e) {
        super(e);
    }
}
