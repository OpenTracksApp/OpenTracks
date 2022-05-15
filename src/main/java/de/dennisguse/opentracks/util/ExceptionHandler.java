package de.dennisguse.opentracks.util;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import de.dennisguse.opentracks.BuildConfig;
import de.dennisguse.opentracks.ShowErrorActivity;

public class ExceptionHandler implements Thread.UncaughtExceptionHandler {

    private final Context context;
    private final Thread.UncaughtExceptionHandler defaultExceptionHandler;

    public ExceptionHandler(final Context context, final Thread.UncaughtExceptionHandler defaultExceptionHandler) {
        this.context = context;
        this.defaultExceptionHandler = defaultExceptionHandler;
    }

    @Override
    public void uncaughtException(@NonNull final Thread thread, @NonNull final Throwable exception) {
        try {
            String errorReport = generateErrorReport(formatException(thread, exception));
            Intent intent = new Intent(context, ShowErrorActivity.class);
            intent.putExtra(ShowErrorActivity.EXTRA_ERROR_TEXT, errorReport);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            // Pass exception to OS for graceful handling - OS will report it via ADB
            // and close all activities and services.
            defaultExceptionHandler.uncaughtException(thread, exception);
        } catch (final Exception fatalException) {
            // do not recurse into custom handler if exception is thrown during
            // exception handling. Pass this ultimate fatal exception to OS
            defaultExceptionHandler.uncaughtException(thread, fatalException);
        }
    }

    private String formatException(final Thread thread, final Throwable exception) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(String.format("Exception in thread \"%s\": ", thread.getName()));

        // print available stacktrace
        Writer writer = new StringWriter();
        exception.printStackTrace(new PrintWriter(writer));
        stringBuilder.append(writer);

        return stringBuilder.toString();
    }

    private String generateErrorReport(final String stackTrace) {
        return
            "### App information\n" +
            "* ID: " + BuildConfig.APPLICATION_ID + "\n" +
            "* Version: " + BuildConfig.VERSION_CODE + " " + BuildConfig.VERSION_NAME + "\n" +
            "\n" +
            "### Device information\n" +
            "* Brand: " + Build.BRAND + "\n" +
            "* Device: " + Build.DEVICE + "\n" +
            "* Model: " + Build.MODEL + "\n" +
            "* Id: " + Build.ID + "\n" +
            "* Product: " + Build.PRODUCT + "\n" +
            "\n" +
            "### Firmware\n" +
            "* SDK: " + Build.VERSION.SDK_INT + "\n" +
            "* Release: " + Build.VERSION.RELEASE + "\n" +
            "* Incremental: " + Build.VERSION.INCREMENTAL + "\n" +
            "\n" +
            "### Cause of error\n" +
            "```java\n" +
            stackTrace +
            "```\n";
    }

}
