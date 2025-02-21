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
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.BuildConfig;
import de.dennisguse.opentracks.data.models.Track;

/**
 * Utilities for dealing with files.
 *
 * @author Rodrigo Damazio
 */
public class FileUtils {

    private static final String TAG = FileUtils.class.getSimpleName();

    public static final String FILEPROVIDER = BuildConfig.APPLICATION_ID + ".fileprovider";

    /**
     * The maximum FAT32 path length. See the FAT32 spec at
     * <a href="http://msdn.microsoft.com/en-us/windows/hardware/gg463080">...</a>
     */
    static final int MAX_FAT32_PATH_LENGTH = 260;

    private FileUtils() {
    }

    public static File getPhotoDir(Context context) {
        return context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
    }

    public static File getPhotoDir(Context context, Track.Id trackId) {
        File photoDirectory = new File(getPhotoDir(context), "" + trackId.id());
        photoDirectory.mkdirs();
        return photoDirectory;
    }


    public static String getPath(DocumentFile file) {
        if (file == null) {
            return "";
        }
        if (file.getParentFile() == null) {
            return file.getName();
        }
        return getPath(file.getParentFile()) + File.pathSeparatorChar + file.getName();
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
     * Gets the extension from a file name.
     *
     * @param fileName the file name
     * @return null if there is no extension or fileName is null.
     */
    public static String getExtension(String fileName) {
        if (fileName == null) {
            return null;
        }

        int index = fileName.lastIndexOf('.');
        if (index == -1) {
            return null;
        }
        return fileName.substring(index + 1);
    }

    public static String getExtension(DocumentFile file) {
        return getExtension(file.getName());
    }

    public static String getExtension(Uri uri) {
        return getExtension(uri.toString());
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
        String suffixName = "";
        if (suffix > 0) {
            suffixName += "(" + suffix + ")";
        }
        suffixName += "." + extension;

        String baseName = sanitizeFileName(base);
        baseName = truncateFileName(directory, baseName, suffixName);
        String fullName = baseName + suffixName;

        if (!new File(directory, fullName).exists()) {
            return fullName;
        }
        return buildUniqueFileName(directory, base, extension, suffix + 1);
    }

    /**
     * Sanitizes the name as a valid fat32 filename.
     * For simplicity, fat32 filename characters may be any combination of letters, digits, or characters with code point values greater than 127.
     * Replaces the invalid characters with "_" and collapses multiple "_" together.
     *
     * @param name name
     */
    public static String sanitizeFileName(String name) {
        StringBuilder builder = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            int codePoint = name.codePointAt(i);
            char character = name.charAt(i);
            if (Character.isLetterOrDigit(character) || isSpecialFat32(character) || character == '.') {
                builder.appendCodePoint(codePoint);
            } else {
                builder.append("_");
            }
        }
        String result = builder.toString();
        return result.replaceAll("_+", "_");
    }

    /**
     * Returns true if it is a special FAT32 character.
     *
     * @param character the character
     */
    private static boolean isSpecialFat32(char character) {
        return List.of('$', '%', '\'', '-', '_', '@', '~', '`', '!', '(', ')', '{', '}', '^', '#', '&', '+', ',', ';', '=', '[', ']', ' ')
                .contains(character);
    }

    /**
     * Truncates the name if necessary so the filename path length (directory + name + suffix) meets the Fat32 path limit.
     *
     * @param directory directory
     * @param name      name
     * @param suffix    suffix
     */
    static String truncateFileName(File directory, String name, String suffix) {
        // 1 at the end accounts for the FAT32 filename trailing NUL character
        int requiredLength = directory.getPath().length() + suffix.length() + 1;
        if (name.length() + requiredLength > MAX_FAT32_PATH_LENGTH) {
            int limit = MAX_FAT32_PATH_LENGTH - requiredLength;
            return name.substring(0, limit);
        }

        return name;
    }

    /**
     * Copy a File (src) to a File (dst).
     *
     * @param src source file.
     * @param dst destination file.
     */
    public static void copy(FileDescriptor src, File dst) {
        try (FileChannel in = new FileInputStream(src).getChannel();
             FileChannel out = new FileOutputStream(dst).getChannel()) {
            in.transferTo(0, in.size(), out);
        } catch (Exception e) {
            // post to log
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * Returns a Uri for the file.
     *
     * @param context the context.
     * @param file    the file.
     */
    public static Uri getUriForFile(Context context, File file) {
        return FileProvider.getUriForFile(context, FileUtils.FILEPROVIDER, file);
    }

    /**
     * Delete the directory recursively.
     *
     * @param file the directory
     */
    public static void deleteDirectoryRecurse(File file) {
        if (file != null && file.exists() && file.isDirectory()) {
            for (File child : file.listFiles()) {
                deleteDirectoryRecurse(child);
            }
            file.delete();
        } else if (file != null && file.isFile()) {
            file.delete();
        }
    }

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
