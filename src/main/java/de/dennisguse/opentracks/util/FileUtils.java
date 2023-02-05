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
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;

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
     * http://msdn.microsoft.com/en-us/windows/hardware/gg463080
     */
    static final int MAX_FAT32_PATH_LENGTH = 260;
    private static final Logger LOGGER = Logger.getLogger(FileUtils.class.getName());
    private static final String TAG = FileUtils.class.getSimpleName();
    /**
     * Returns true if it is a special FAT32 character.
     *
     * @param character the character
     */
    private static final Set<Character> specialFat32 = new HashSet<>(Arrays.asList(
            '$', '%', '\'', '-', '_', '@', '~', '`', '!', '(', ')', '{', '}', '^', '#', '&', '+', ',', ';', '=', '[', ']', ' '));

    private FileUtils() {
    }

    public static File getPhotoDir(Context context) {
        return context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
    }

    public static File getPhotoDir(Context context, Track.Id trackId) {
        File photoDirectory = new File(getPhotoDir(context), String.valueOf(trackId.getId()));
        if (!photoDirectory.exists()) {
            photoDirectory.mkdirs();
        }
        return photoDirectory;
    }

    public static String getPath(DocumentFile file) {
        if (file == null) {
            return "";
        }
        if (file.getParentFile() == null) {
            return file.getName();
        }
        StringBuilder builder = new StringBuilder();
        builder.append(getPath(file.getParentFile()));
        builder.append(File.pathSeparatorChar);
        builder.append(file.getName());
        return builder.toString();
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
        if (fileName == null || fileName.lastIndexOf('.') == -1) {
            return null;
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    public static String getExtension(DocumentFile file) {
        return getExtension(file.getName());
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
        String suffixName = suffix > 0 ? "(" + suffix + ")." + extension : "." + extension;
        String baseName = truncateFileName(directory, sanitizeFileName(base), suffixName);
        String fullName = baseName + suffixName;
        return new File(directory, fullName).exists() ? buildUniqueFileName(directory, base, extension, suffix + 1) : fullName;
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
        return builder.toString().replaceAll("_+", "_");
    }

    private static boolean isSpecialFat32(char character) {
        return specialFat32.contains(character);
    }


    /**
     * Truncates the name if necessary so the filename path length (directory + name + suffix) meets the Fat32 path limit.
     *
     * @param directory directory
     * @param name      name
     * @param suffix    suffix
     */
    static String truncateFileName(File directory, String name, String suffix) {
        int requiredLength = directory.getPath().length() + suffix.length() + 1;
        int limit = MAX_FAT32_PATH_LENGTH - requiredLength;
        return name.length() + requiredLength > MAX_FAT32_PATH_LENGTH ? name.substring(0, limit) : name;
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
            long size = in.size();
            long position = 0;
            while (position < size) {
                position += in.transferTo(position, size - position, out);
            }
        } catch (Exception e) {
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
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void deleteDirectoryRecurse(File file) {
        Path path = file.toPath();
        if (!Files.exists(path)) {
            return;
        }

        if (Files.isDirectory(path)) {
            Stream<Path> stream = null;
            try {
                stream = Files.list(path);
                stream.forEach(child -> deleteDirectoryRecurse(child.toFile()));
            } catch (IOException e) {
                LOGGER.severe("Error reading directory: " + path);
            } finally {
                if (stream != null) {
                    stream.close();
                }
            }
        }


        try {
            Files.delete(path);
        } catch (NoSuchFileException e) {
            LOGGER.severe("File already deleted: " + path);
        } catch (IOException e) {
            LOGGER.severe("Error deleting file: " + path);
        }
    }


    public static List<DocumentFile> getFiles(DocumentFile file) {
        LinkedList<DocumentFile> files = new LinkedList<>();

        if (!file.isDirectory()) {
            files.add(file);
            return files;
        }

        for (DocumentFile candidate : file.listFiles()) {
            if (!candidate.isDirectory()) {
                files.add(candidate);
            } else if (candidate.listFiles().length > 0) {
                files.addAll(getFiles(candidate));
            }
        }

        return files;
    }

}