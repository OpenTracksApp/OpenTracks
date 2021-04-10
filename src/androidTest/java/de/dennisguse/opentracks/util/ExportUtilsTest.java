package de.dennisguse.opentracks.util;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RunWith(JUnit4.class)
public class ExportUtilsTest extends TestCase {

    @Test
    public void testIsExportFileExists_exists() {
        // given
        UUID uuid = UUID.randomUUID();
        String format = "kmz";
        List<String> fileNames = new ArrayList<>();
        fileNames.add(uuid.toString().substring(0, 8) + "_name.gpx");
        fileNames.add(uuid.toString().substring(0, 8) + "_other name name.kmz");

        // when
        boolean exists = ExportUtils.isExportFileExists(uuid, format, fileNames);

        // then
        assertTrue(exists);
    }

    @Test
    public void testIsExportFileExists_not_exists1() {
        // given
        UUID uuid = UUID.randomUUID();
        String format = "gpx";
        List<String> fileNames = new ArrayList<>();
        fileNames.add(UUID.randomUUID().toString().substring(0, 8) + "_name.gpx");
        fileNames.add(UUID.randomUUID().toString().substring(0, 8) + "_other name name.gpx");

        // when
        boolean exists = ExportUtils.isExportFileExists(uuid, format, fileNames);

        // then
        assertFalse(exists);
    }

    @Test
    public void testIsExportFileExists_not_exists2() {
        // given
        UUID uuid = UUID.randomUUID();
        String format = "kmz";
        List<String> fileNames = new ArrayList<>();
        fileNames.add(uuid.toString().substring(0, 8) + "_name.gpx");
        fileNames.add(UUID.randomUUID().toString().substring(0, 8) + "_other name name.gpx");

        // when
        boolean exists = ExportUtils.isExportFileExists(uuid, format, fileNames);

        // then
        assertFalse(exists);
    }
}
