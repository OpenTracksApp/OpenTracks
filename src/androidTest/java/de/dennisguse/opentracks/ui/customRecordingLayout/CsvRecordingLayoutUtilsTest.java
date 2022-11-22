package de.dennisguse.opentracks.ui.customRecordingLayout;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class CsvRecordingLayoutUtilsTest extends TestCase {

    @Test
    public void testGetCsvLineParts() {
        // given
        String csvLineOk = "Layout Name;2;distance,1,1;speed,1,1;";
        String csvLineWrong1 = "Layout Name;distance,1,1;speed,1,1;";
        String csvLineWrong2 = "Layout Name;1;";

        // when
        List<String> partsOk = CsvLayoutUtils.getCsvLineParts(csvLineOk);
        List<String> partsWrong1 = CsvLayoutUtils.getCsvLineParts(csvLineWrong1);
        List<String> partsWrong2 = CsvLayoutUtils.getCsvLineParts(csvLineWrong2);

        // then
        assertNotNull(partsOk);
        assertEquals(partsOk.size(), 4);
        assertEquals(partsOk.get(0), "Layout Name");
        assertEquals(partsOk.get(1), "2");
        assertEquals(partsOk.get(2), "distance,1,1");
        assertEquals(partsOk.get(3), "speed,1,1");
        assertNull(partsWrong1);
        assertNull(partsWrong2);
    }

    @Test
    public void testGetCsvFieldParts() {
        // given
        String csvFieldOk1 = "distance,0,0";
        String csvFieldOk2 = "speed,0,1";
        String csvFieldOk3 = "time,1,0";
        String csvFieldOk4 = "moving time,1,1";
        String csvFieldOk5 = ";moving time,1,1";
        String csvLineWrong1 = "";
        String csvLineWrong2 = "distance,speed,time";
        String csvLineWrong3 = "distance,0,1o";
        String csvLineWrong4 = "distance,0z,1";
        String csvLineWrong5 = "distance";
        String csvLineWrong6 = "distance,0";
        String csvLineWrong7 = "distance,1,1;";

        // when
        String[] ok1 = CsvLayoutUtils.getCsvFieldParts(csvFieldOk1);
        String[] ok2 = CsvLayoutUtils.getCsvFieldParts(csvFieldOk2);
        String[] ok3 = CsvLayoutUtils.getCsvFieldParts(csvFieldOk3);
        String[] ok4 = CsvLayoutUtils.getCsvFieldParts(csvFieldOk4);
        String[] ok5 = CsvLayoutUtils.getCsvFieldParts(csvFieldOk5);
        String[] wrong1 = CsvLayoutUtils.getCsvFieldParts(csvLineWrong1);
        String[] wrong2 = CsvLayoutUtils.getCsvFieldParts(csvLineWrong2);
        String[] wrong3 = CsvLayoutUtils.getCsvFieldParts(csvLineWrong3);
        String[] wrong4 = CsvLayoutUtils.getCsvFieldParts(csvLineWrong4);
        String[] wrong5 = CsvLayoutUtils.getCsvFieldParts(csvLineWrong5);
        String[] wrong6 = CsvLayoutUtils.getCsvFieldParts(csvLineWrong6);
        String[] wrong7 = CsvLayoutUtils.getCsvFieldParts(csvLineWrong7);

        // then
        assertNotNull(ok1);
        assertNotNull(ok2);
        assertNotNull(ok3);
        assertNotNull(ok4);
        assertNotNull(ok5);
        assertFieldOk(ok1, "distance", "0", "0");
        assertFieldOk(ok2, "speed", "0", "1");
        assertFieldOk(ok3, "time", "1", "0");
        assertFieldOk(ok4, "moving time", "1", "1");
        assertFieldOk(ok5, ";moving time", "1", "1");
        assertNull(wrong1);
        assertNull(wrong2);
        assertNull(wrong3);
        assertNull(wrong4);
        assertNull(wrong5);
        assertNull(wrong6);
        assertNull(wrong7);
    }

    private void assertFieldOk(String[] ok, String name, String val1, String val2) {
        assertEquals(ok[0], name);
        assertEquals(ok[1], val1);
        assertEquals(ok[2], val2);
    }
}