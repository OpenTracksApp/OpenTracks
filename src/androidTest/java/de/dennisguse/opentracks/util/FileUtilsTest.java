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

import static org.junit.Assert.assertEquals;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Tests for {@link FileUtils}.
 *
 * @author Rodrigo Damazio
 */
@RunWith(JUnit4.class)
public class FileUtilsTest {

    /**
     * Tests {@link FileUtils#buildUniqueFileName(File, String, String)} when the file is new.
     */
    @Test
    public void testBuildUniqueFileName_new() {
        String filename = FileUtils.buildUniqueFileName(FileUtils.getPhotoDir(ApplicationProvider.getApplicationContext()), "Filename", "ext");
        assertEquals("Filename.ext", filename);
    }

    /**
     * Tests {@link FileUtils#buildUniqueFileName(File, String, String)} when the file exists already.
     */
    @Test
    public void testBuildUniqueFileName_exist() throws IOException {
        // given
        new File(FileUtils.getPhotoDir(ApplicationProvider.getApplicationContext()), "default.prop").createNewFile();

        //when
        String filename = FileUtils.buildUniqueFileName(FileUtils.getPhotoDir(ApplicationProvider.getApplicationContext()), "default", "prop");

        // then
        assertEquals("default(1).prop", filename);
    }

    /**
     * Tests {@link FileUtils#sanitizeFileName(String)} with special characters.
     * Verifies that they are sanitized.
     */
    @Test
    public void testSanitizeFileName() {
        String name = "Swim\10ming-^across:/the/ pacific (ocean).";
        String expected = "Swim_ming-^across_the_ pacific (ocean).";
        assertEquals(expected, FileUtils.sanitizeFileName(name));
    }

    @Test
    public void testSanitizeFileNameWithSuffix() {
        String name = "FileName.jpeg";
        assertEquals(name, FileUtils.sanitizeFileName(name));
    }

    /**
     * Tests {@link FileUtils#sanitizeFileName(String)} with i18n characters (in Chinese and Russian).
     * Verifies that they are allowed.
     */
    @Test
    public void testSanitizeFileName_i18n() {
        String name = "您好-привет";
        String expected = "您好-привет";
        assertEquals(expected, FileUtils.sanitizeFileName(name));
    }

    /**
     * Tests {@link FileUtils#sanitizeFileName(String)} with special FAT32 characters.
     * Verifies that they are allowed.
     */
    @Test
    public void testSanitizeFileName_special_characters() {
        String name = "$%'-_@~`!(){}^#&+,;=[] ";
        String expected = "$%'-_@~`!(){}^#&+,;=[] ";
        assertEquals(expected, FileUtils.sanitizeFileName(name));
    }

    /**
     * Tests {@link FileUtils#sanitizeFileName(String)} with multiple escaped characters in a row.
     * Verifies that they are collapsed into one underscore.
     */
    @Test
    public void testSanitizeFileName_collapse() {
        String name = "hello//there";
        String expected = "hello_there";
        assertEquals(expected, FileUtils.sanitizeFileName(name));
    }

    @Test
    public void testSanitizeFileName_emoji() {
        String name = "\uD83C\uDF5B-Food";
        String expected = "_-Food";
        assertEquals(expected, FileUtils.sanitizeFileName(name));
    }

    /**
     * Tests {@link FileUtils#truncateFileName(File, String, String)}.
     * Verifies the a long file name is truncated.
     */
    @Test
    public void testTruncateFileName() {
        File directory = new File("/dir1/dir2/");
        String suffix = ".gpx";
        char[] name = new char[FileUtils.MAX_FAT32_PATH_LENGTH];
        Arrays.fill(name, 'a');
        String nameString = new String(name);
        String truncated = FileUtils.truncateFileName(directory, nameString, suffix);

        for (int i = 0; i < truncated.length(); i++) {
            assertEquals('a', truncated.charAt(i));
        }
        assertEquals(FileUtils.MAX_FAT32_PATH_LENGTH, new File(directory, truncated + suffix).getPath().length());
    }
}
