package de.dennisguse.opentracks.ui.customRecordingLayout;

/**
 * Use this class to throw custom Run Time Exception, use this class as a template to enhance
 * the error message
 */
public class CustomRunTimeExceptionHandler extends RuntimeException {
    public CustomRunTimeExceptionHandler(String errorMessage) {
        super(errorMessage);
    }
}
