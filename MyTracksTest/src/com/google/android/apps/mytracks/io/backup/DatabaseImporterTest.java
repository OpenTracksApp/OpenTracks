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
package com.google.android.apps.mytracks.io.backup;

import com.google.android.apps.mytracks.content.ContentTypeIds;

import android.content.ContentValues;
import android.net.Uri;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import junit.framework.TestCase;

/**
 * Tests for {@link DatabaseImporter}.
 *
 * @author Rodrigo Damazio
 */
public class DatabaseImporterTest extends TestCase {

  private static final Uri DESTINATION_URI = Uri.parse("http://www.google.com/");
  private static final int TEST_BULK_SIZE = 10;
  private ArrayList<ContentValues> insertedValues;

  private class TestableDatabaseImporter extends DatabaseImporter {
    public TestableDatabaseImporter(boolean readNullFields) {
      super(DESTINATION_URI, null, readNullFields, TEST_BULK_SIZE);
    }

    @Override
    protected void doBulkInsert(ContentValues[] values) {
      insertedValues.ensureCapacity(insertedValues.size() + values.length);

      // We need to make a copy of the values since the objects are re-used
      for (ContentValues contentValues : values) {
        insertedValues.add(new ContentValues(contentValues));
      }
    }
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    insertedValues = new ArrayList<ContentValues>();
  }

  public void testImportAllRows() throws Exception {
    testImportAllRows(false);
  }

  public void testImportAllRows_readNullFields() throws Exception {
    testImportAllRows(true);
  }
  
  private void testImportAllRows(boolean readNullFields) throws Exception {
    // Create a fake data stream to be read
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1024);
    DataOutputStream writer = new DataOutputStream(outputStream);

    writeFullHeader(writer);

    // Add the number of rows
    writer.writeInt(2);

    // Add a row with all fields present 
    writer.writeLong(0x7F);
    writer.writeInt(42);
    writer.writeBoolean(true);
    writer.writeUTF("lolcat");
    writer.writeFloat(3.1415f);
    writer.writeDouble(2.72);
    writer.writeLong(123456789L);
    writer.writeInt(4);
    writer.writeBytes("blob");

    // Add a row with some missing fields
    writer.writeLong(0x15);
    writer.writeInt(42);
    if (readNullFields) writer.writeBoolean(false);
    writer.writeUTF("lolcat");
    if (readNullFields) writer.writeFloat(0.0f);
    writer.writeDouble(2.72);
    if (readNullFields) writer.writeLong(0L);
    if (readNullFields) writer.writeInt(0);  // empty blob

    writer.flush();

    // Do the importing
    DatabaseImporter importer = new TestableDatabaseImporter(readNullFields);
    byte[] dataBytes = outputStream.toByteArray();
    importer.importAllRows(new DataInputStream(new ByteArrayInputStream(dataBytes)));

    assertEquals(2, insertedValues.size());

    // Verify the first row
    ContentValues value = insertedValues.get(0);
    assertEquals(value.toString(), 7, value.size());

    assertValue(42, "col1", value);
    assertValue(true, "col2", value);
    assertValue("lolcat", "col3", value);
    assertValue(3.1415f, "col4", value);
    assertValue(2.72, "col5", value);
    assertValue(123456789L, "col6", value);
    assertBlobValue("blob", "col7", value);

    // Verify the second row
    value = insertedValues.get(1);
    assertEquals(value.toString(), 3, value.size());

    assertValue(42, "col1", value);
    assertValue("lolcat", "col3", value);
    assertValue(2.72, "col5", value);
  }
  
  public void testImportAllRows_noRows() throws Exception {
    // Create a fake data stream to be read
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1024);
    DataOutputStream writer = new DataOutputStream(outputStream);

    writeFullHeader(writer);

    // Add the number of rows
    writer.writeInt(0);

    writer.flush();

    // Do the importing
    DatabaseImporter importer = new TestableDatabaseImporter(false);
    byte[] dataBytes = outputStream.toByteArray();
    importer.importAllRows(new DataInputStream(new ByteArrayInputStream(dataBytes)));

    assertTrue(insertedValues.isEmpty());
  }

  public void testImportAllRows_emptyRows() throws Exception {
    testImportAllRowsWithEmptyRows(false);
  }

  public void testImportAllRows_emptyRowsWithNulls() throws Exception {
    testImportAllRowsWithEmptyRows(true);
  }

  private void testImportAllRowsWithEmptyRows(boolean readNullFields) throws Exception {
    // Create a fake data stream to be read
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1024);
    DataOutputStream writer = new DataOutputStream(outputStream);

    writeFullHeader(writer);

    // Add the number of rows
    writer.writeInt(3);

    // Add 2 rows with no fields
    for (int i = 0; i < 2; i++) {
      writer.writeLong(0);
      if (readNullFields) {
        writer.writeInt(0);
        writer.writeBoolean(false);
        writer.writeUTF("");
        writer.writeFloat(0.0f);
        writer.writeDouble(0.0);
        writer.writeLong(0L);
        writer.writeInt(0);  // empty blob
      }
    }

    // Add a row with some missing fields
    writer.writeLong(0x15);
    writer.writeInt(42);
    if (readNullFields) writer.writeBoolean(false);
    writer.writeUTF("lolcat");
    if (readNullFields) writer.writeFloat(0.0f);
    writer.writeDouble(2.72);
    if (readNullFields) writer.writeLong(0L);
    if (readNullFields) writer.writeInt(0);  // empty blob

    writer.flush();

    // Do the importing
    DatabaseImporter importer = new TestableDatabaseImporter(readNullFields);
    byte[] dataBytes = outputStream.toByteArray();
    importer.importAllRows(new DataInputStream(new ByteArrayInputStream(dataBytes)));

    assertEquals(insertedValues.toString(), 3, insertedValues.size());

    ContentValues value = insertedValues.get(0);
    assertEquals(value.toString(), 0, value.size());

    value = insertedValues.get(1);
    assertEquals(value.toString(), 0, value.size());

    // Verify the third row (only one with values)
    value = insertedValues.get(2);
    assertEquals(value.toString(), 3, value.size());

    assertFalse(value.containsKey("col2"));
    assertFalse(value.containsKey("col4"));
    assertFalse(value.containsKey("col6"));

    assertValue(42, "col1", value);
    assertValue("lolcat", "col3", value);
    assertValue(2.72, "col5", value);
  }

  public void testImportAllRows_bulks() throws Exception {
    // Create a fake data stream to be read
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1024);
    DataOutputStream writer = new DataOutputStream(outputStream);

    // Add the header
    writer.writeInt(2);
    writer.writeUTF("col1");
    writer.writeByte(ContentTypeIds.INT_TYPE_ID);
    writer.writeUTF("col2");
    writer.writeByte(ContentTypeIds.STRING_TYPE_ID);

    // Add lots of rows (so the insertions are split in multiple bulks)
    int numRows = TEST_BULK_SIZE * 5 / 2;
    writer.writeInt(numRows);
    for (int i = 0; i < numRows; i++) {
      writer.writeLong(3);
      writer.writeInt(i);
      writer.writeUTF(Integer.toString(i * 2));
    }

    writer.flush();

    // Do the importing
    DatabaseImporter importer = new TestableDatabaseImporter(false);
    byte[] dataBytes = outputStream.toByteArray();
    importer.importAllRows(new DataInputStream(new ByteArrayInputStream(dataBytes)));

    // Verify the rows
    assertEquals(numRows, insertedValues.size());
    for (int i = 0; i < numRows; i++) {
      ContentValues value = insertedValues.get(i);
      assertEquals(value.toString(), 2, value.size());
      assertValue(i, "col1", value);
      assertValue(Integer.toString(i * 2), "col2", value);
    }
  }

  private void writeFullHeader(DataOutputStream writer) throws IOException {
    // Add the header
    writer.writeInt(7);
    writer.writeUTF("col1");
    writer.writeByte(ContentTypeIds.INT_TYPE_ID);
    writer.writeUTF("col2");
    writer.writeByte(ContentTypeIds.BOOLEAN_TYPE_ID);
    writer.writeUTF("col3");
    writer.writeByte(ContentTypeIds.STRING_TYPE_ID);
    writer.writeUTF("col4");
    writer.writeByte(ContentTypeIds.FLOAT_TYPE_ID);
    writer.writeUTF("col5");
    writer.writeByte(ContentTypeIds.DOUBLE_TYPE_ID);
    writer.writeUTF("col6");
    writer.writeByte(ContentTypeIds.LONG_TYPE_ID);
    writer.writeUTF("col7");
    writer.writeByte(ContentTypeIds.BLOB_TYPE_ID);
  }

  private <T> void assertValue(T expectedValue, String name, ContentValues values) {
    @SuppressWarnings("unchecked")
    T value = (T) values.get(name);
    assertNotNull(value);
    assertEquals(expectedValue, value);
  }
  
  private void assertBlobValue(String expectedValue, String name, ContentValues values ){
    byte[] blob = values.getAsByteArray(name);
    assertEquals(expectedValue, new String(blob));
  }
}
