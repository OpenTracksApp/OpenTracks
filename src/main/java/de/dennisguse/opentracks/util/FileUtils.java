/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package de.dennisguse.opentracks.util;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.dennisguse.opentracks.BuildConfig;
import de.dennisguse.opentracks.data.models.Track;

/**
 * Utilities for dealing with files.
 *
 * @author Rodrigo Damazio
 */
public class FileUtils {

    public static final String FILEPROVIDER = BuildConfig.APPLICATION_ID + ".fileprovider";
    /**
     * The maximum FAT32 path length. See the FAT32 spec at
     * <a href="http://msdn.microsoft.com/en-us/windows/hardware/gg463080">...</a>
     */
    static final int MAX_FAT32_PATH_LENGTH = 260;
    private static final String TAG = FileUtils.class.getSimpleName();

    private static final Set<Character> FAT32_SPECIAL_CHARS = new HashSet<>(Arrays.asList('$', '%', '\'', '-', '_', '@', '~', '`', '!', '(', ')', '{', '}', '^', '#', '&', '+', ',', ';', '=', '[', ']', ' '));

    private FileUtils() {
    }

    /**
     * Returns the external pictures directory for the app.
     *
     * @param context the context of the app
     * @return the external pictures directory
     */
    public static File getPhotoDir(Context context) {
        return context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
    }

    /**
     * Returns the directory for photos associated with the given track.
     *
     * @param context the context of the app
     * @param trackId the id of the track
     * @return the directory for photos associated with the given track
     */
    public static File getPhotoDir(Context context, Track.Id trackId) {
        File photoDirectory = new File(getPhotoDir(context), String.valueOf(trackId.getId()));
        photoDirectory.mkdirs();
        return photoDirectory;
    }

    /**
     * Returns the full path of a DocumentFile.
     *
     * @param file the DocumentFile
     * @return the full path of the DocumentFile
     */
    public static String getPath(DocumentFile file) {
        if (file == null) {
            return "";
        }
        if (file.getParentFile() == null) {
            return file.getName();
        }
        return getPath(file.getParentFile()) + File.separator + file.getName();
    }

    /**
     * Copies a directory and its contents recursively to a new location.
     *
     * @param srcDir the source directory to copy
     * @param dstDir the destination directory to copy to
     * @throws IOException if an I/O error occurs during the copy operation
     */
    public static void copyDirectory(File srcDir, File dstDir) throws IOException {
        if (!srcDir.isDirectory()) {
            throw new IllegalArgumentException("Source is not a directory.");
        }

        if (!dstDir.exists()) {
            dstDir.mkdirs();
        }

        File[] files = srcDir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                File newDir = new File(dstDir, file.getName());
                copyDirectory(file, newDir);
            } else {
                String fileName = file.getName();
                String newFileName = FileUtils.buildUniqueFileName(dstDir, fileName.substring(0, fileName.lastIndexOf(".")), fileName.substring(fileName.lastIndexOf(".") + 1));
                File newFile = new File(dstDir, newFileName);
            }
        }
    }

    /**
     * Builds a filename with the given base name (prefix) and the given extension, possibly adding a suffix to ensure the file doesn't exist.
     *
     * @param directory    the directory the file will live in
     * @param fileBaseName the prefix for the file name
     * @param extension    the file's extension
     * @return the complete file name, without the directory
     */
    public static synchronized String buildUniqueFileName(File directory, String fileBaseName, String extension) {
        return buildUniqueFileName(directory, fileBaseName, extension, 0);
    }

    /**
     * Generates a unique filename by appending a numeric suffix to the base filename.
     *
     * @param directory the directory in which to create the file
     * @param baseName  the base filename to use
     * @param extension the file extension to use
     * @return a File object representing a unique filename
     */
    public static File generateUniqueFileName(File directory, String baseName, String extension) {
        File file = new File(directory, baseName + "." + extension);
        int count = 1;
        while (file.exists()) {
            file = new File(directory, baseName + "-" + count + "." + extension);
            count++;
        }
        return file;
    }


    /**
     * Gets the extension from a file name.
     *
     * @param fileName the file name
     * @return the extension of the file, or null if there is no extension or fileName is null
     */
    public static String getExtension(String fileName) {
        if (fileName == null) {
            return null;
        }
        int index = fileName.lastIndexOf('.');
        return (index == -1) ? null : fileName.substring(index + 1);
    }

    /**
     * Gets the extension of a {@link DocumentFile} object.
     *
     * @param file the DocumentFile object
     * @return the extension of the file, or null if there is no extension or file is null
     */
    public static String getExtension(DocumentFile file) {
        return (file == null) ? null : getExtension(file.getName());
    }

    /**
     * Builds a filename with the given base and the given extension, possibly adding a suffix to ensure the file doesn't exist.
     *
     * @param directory the directory the filename will be located in
     * @param base      the base for the filename
     * @param extension the extension for the filename
     * @param suffix    the first numeric suffix to try to use, or 0 for none
     * @return the complete filename, without the directory
     */
    private static String buildUniqueFileName(File directory, String base, String extension, int suffix) {
        String suffixName = (suffix > 0) ? "(" + suffix + ")" + "." + extension : "." + extension;
        String baseName = truncateFileName(directory, sanitizeFileName(base), suffixName);
        String fullName = baseName + suffixName;
        return new File(directory, fullName).exists() ? buildUniqueFileName(directory, base, extension, suffix + 1) : fullName;
    }

    /**
     * Sanitizes the name as a valid FAT32 filename.
     * For simplicity, FAT32 filename characters may be any combination of letters, digits, or characters with code point values greater than 127.
     * Replaces the invalid characters with "_" and collapses multiple "_" together.
     *
     * @param name the filename to sanitize
     * @return the sanitized filename
     */
    public static String sanitizeFileName(String name) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            builder.append((Character.isLetterOrDigit(c) || isSpecialFat32(c) || c == '.') ? c : '_');
        }
        return builder.toString().replaceAll("_+", "_");
    }

    /**
     * Determines if the character is a special FAT32 character.
     *
     * @param character the character to check
     * @return true if the character is a special FAT32 character, false otherwise
     */
    private static boolean isSpecialFat32(char character) {
        return FAT32_SPECIAL_CHARS.contains(character);
    }


    /**
     * Truncates the name if necessary so the filename path length (directory + name + suffix) meets the FAT32 path limit.
     *
     * @param directory the directory
     * @param name      the name
     * @param suffix    the suffix
     * @return the truncated filename
     */
    public static String truncateFileName(File directory, String name, String suffix) {
        int requiredLength = directory.getPath().length() + suffix.length() + 1;
        if (name.length() + requiredLength > MAX_FAT32_PATH_LENGTH) {
            int limit = MAX_FAT32_PATH_LENGTH - requiredLength;
            return name.substring(0, limit);
        }

        return name;
    }

    /**
     * Copies a file from the source to the destination.
     *
     * @param src the source file descriptor
     * @param dst the destination file
     * @throws IOException if an I/O error occurs during the copy operation
     */
    public static void copy(FileDescriptor src, File dst) throws IOException {
        try (FileChannel in = new FileInputStream(src).getChannel();
             FileChannel out = new FileOutputStream(dst).getChannel()) {
            in.transferTo(0, in.size(), out);
        } catch (IOException e) {
            // post to log
            Log.e(TAG, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Returns a content URI for a file.
     *
     * @param context the context.
     * @param file    the file.
     * @return a content URI for the file.
     */
    public static Uri getUriForFile(Context context, File file) {
        return FileProvider.getUriForFile(context, FileUtils.FILEPROVIDER, file);
    }

    /**
     * Deletes all files in a directory except for a specified list of files.
     *
     * @param directory  the directory to delete files from.
     * @param exclusions a list of files to exclude from deletion.
     */
    public static void deleteFilesExcept(File directory, List<File> exclusions) {
        if (directory == null || !directory.isDirectory()) {
            return;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (exclusions.contains(file)) {
                continue;
            }
            if (file.isDirectory()) {
                deleteDirectoryRecurse(file);
            } else {
                if (!file.delete()) {
                    Log.println(Log.ERROR, "Failure 1", "File failure of deletion " + file.getName());
                }
            }
        }
    }

    /**
     * Recursively deletes a directory and its contents.
     *
     * @param directory the directory to delete.
     */
    public static void deleteDirectoryRecurse(File directory) {
        if (directory == null || !directory.exists()) {
            return;
        }
        if (!directory.isDirectory()) {
            if (!directory.delete()) {
                Log.println(Log.ERROR, "Failure 2", "Directory failure of deletion " + directory.getName());
            }
            return;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            deleteDirectoryRecurse(file);
        }
        if (!directory.delete()) {
            Log.println(Log.ERROR, "Failure 3", "Directory failure of deletion " + directory.getName());
        }
    }

    /**
     * Recursively gets a list of all files in a directory, including files in subdirectories.
     *
     * @param file the file to search.
     * @return a list of all files in the directory.
     */
    public static ArrayList<DocumentFile> getFiles(DocumentFile file) {
        ArrayList<DocumentFile> files = new ArrayList<>();

        if (!file.isDirectory()) {
            files.add(file);
            return files;
        }

        for (DocumentFile candidate : file.listFiles()) {
            if (!candidate.isDirectory()) {
                files.add(candidate);
            } else {
                files.addAll(getFiles(candidate));
            }
        }

        return files;
    }
}
