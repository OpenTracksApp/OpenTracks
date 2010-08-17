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

import android.database.MatrixCursor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import junit.framework.TestCase;

/**
 * Tests for {@link DatabaseDumper}.
 *
 * @author Rodrigo Damazio
 */
public class DatabaseDumperTest extends TestCase {

  private static final String[] COLUMN_NAMES = {
    "intCol", "longCol", "floatCol", "doubleCol", "stringCol", "boolCol"
  };
  private static final byte[] COLUMN_TYPES = {
    ContentTypeIds.INT_TYPE_ID, ContentTypeIds.LONG_TYPE_ID,
    ContentTypeIds.FLOAT_TYPE_ID, ContentTypeIds.DOUBLE_TYPE_ID,
    ContentTypeIds.STRING_TYPE_ID, ContentTypeIds.BOOLEAN_TYPE_ID
  };
  private static final String[][] FAKE_DATA = {
      { "42", "123456789", "3.1415", "2.72", "lolcat",  "1" },
      { null, "123456789", "3.1415", "2.72", "lolcat",  "1" },
      { "42",        null, "3.1415", "2.72", "lolcat",  "1" },
      { "42", "123456789",     null, "2.72", "lolcat",  "1" },
      { "42", "123456789", "3.1415",   null, "lolcat",  "1" },
      { "42", "123456789", "3.1415", "2.72",     null,  "1" },
      { "42", "123456789", "3.1415", "2.72", "lolcat", null },
  };
  private static final long[] EXPECTED_FIELD_SETS = {
      0x3F, 0x3E, 0x3D, 0x3B, 0x37, 0x2F, 0x1F
  };

  private MatrixCursor cursor;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    // Add fake data to the cursor
    cursor = new MatrixCursor(COLUMN_NAMES);
    for (String[] row : FAKE_DATA) {
      cursor.addRow(row);
    }
  }

  public void testWriteAllRows_noNulls() throws Exception {
    testWriteAllRows(false);
  }

  public void testWriteAllRows_withNulls() throws Exception {
    testWriteAllRows(true);
  }

  private void testWriteAllRows(boolean hasNullFields) throws Exception {
    // Dump it
    DatabaseDumper dumper = new DatabaseDumper(COLUMN_NAMES, COLUMN_TYPES, hasNullFields);
    ByteArrayOutputStream outStream = new ByteArrayOutputStream(1024);
    DataOutputStream writer = new DataOutputStream(outStream );
    dumper.writeAllRows(cursor, writer);

    // Read the results
    byte[] result = outStream.toByteArray();
    ByteArrayInputStream inputStream = new ByteArrayInputStream(result);
    DataInputStream reader = new DataInputStream(inputStream);

    // Verify the header
    assertHeader(reader);

    // Verify the number of rows
    assertEquals(FAKE_DATA.length, reader.readInt());

    // Verify the row contents

    // Row 0
    assertEquals(EXPECTED_FIELD_SETS[0], reader.readLong());
    assertEquals(42, reader.readInt());
    assertEquals(123456789L, reader.readLong());
    assertEquals(3.1415f, reader.readFloat());
    assertEquals(2.72, reader.readDouble());
    assertEquals("lolcat", reader.readUTF());
    assertTrue(reader.readBoolean());

    // Row 1
    assertEquals(EXPECTED_FIELD_SETS[1], reader.readLong());
    if (hasNullFields) reader.readInt();
    assertEquals(123456789L, reader.readLong());
    assertEquals(3.1415f, reader.readFloat());
    assertEquals(2.72, reader.readDouble());
    assertEquals("lolcat", reader.readUTF());
    assertTrue(reader.readBoolean());

    // Row 2
    assertEquals(EXPECTED_FIELD_SETS[2], reader.readLong());
    assertEquals(42, reader.readInt());
    if (hasNullFields) reader.readLong();
    assertEquals(3.1415f, reader.readFloat());
    assertEquals(2.72, reader.readDouble());
    assertEquals("lolcat", reader.readUTF());
    assertTrue(reader.readBoolean());

    // Row 3
    assertEquals(EXPECTED_FIELD_SETS[3], reader.readLong());
    assertEquals(42, reader.readInt());
    assertEquals(123456789L, reader.readLong());
    if (hasNullFields) reader.readFloat();
    assertEquals(2.72, reader.readDouble());
    assertEquals("lolcat", reader.readUTF());
    assertTrue(reader.readBoolean());

    // Row 4
    assertEquals(EXPECTED_FIELD_SETS[4], reader.readLong());
    assertEquals(42, reader.readInt());
    assertEquals(123456789L, reader.readLong());
    assertEquals(3.1415f, reader.readFloat());
    if (hasNullFields) reader.readDouble();
    assertEquals("lolcat", reader.readUTF());
    assertTrue(reader.readBoolean());

    // Row 5
    assertEquals(EXPECTED_FIELD_SETS[5], reader.readLong());
    assertEquals(42, reader.readInt());
    assertEquals(123456789L, reader.readLong());
    assertEquals(3.1415f, reader.readFloat());
    assertEquals(2.72, reader.readDouble());
    if (hasNullFields) reader.readUTF();
    assertTrue(reader.readBoolean());

    // Row 6
    assertEquals(EXPECTED_FIELD_SETS[6], reader.readLong());
    assertEquals(42, reader.readInt());
    assertEquals(123456789L, reader.readLong());
    assertEquals(3.1415f, reader.readFloat());
    assertEquals(2.72, reader.readDouble());
    assertEquals("lolcat", reader.readUTF());
    if (hasNullFields) reader.readBoolean();
  }

  public void testFewerRows() throws Exception {
    // Dump only the first two rows
    DatabaseDumper dumper = new DatabaseDumper(COLUMN_NAMES, COLUMN_TYPES, false);
    ByteArrayOutputStream outStream = new ByteArrayOutputStream(1024);
    DataOutputStream writer = new DataOutputStream(outStream);

    dumper.writeHeaders(cursor, 2, writer);
    cursor.moveToFirst();
    dumper.writeOneRow(cursor, writer);
    cursor.moveToNext();
    dumper.writeOneRow(cursor, writer);

    // Read the results
    byte[] result = outStream.toByteArray();
    ByteArrayInputStream inputStream = new ByteArrayInputStream(result);
    DataInputStream reader = new DataInputStream(inputStream);

    // Verify the header
    assertHeader(reader);

    // Verify the number of rows
    assertEquals(2, reader.readInt());

    // Row 0
    assertEquals(EXPECTED_FIELD_SETS[0], reader.readLong());
    assertEquals(42, reader.readInt());
    assertEquals(123456789L, reader.readLong());
    assertEquals(3.1415f, reader.readFloat());
    assertEquals(2.72, reader.readDouble());
    assertEquals("lolcat", reader.readUTF());
    assertTrue(reader.readBoolean());

    // Row 1
    assertEquals(EXPECTED_FIELD_SETS[1], reader.readLong());
    // Null field not read
    assertEquals(123456789L, reader.readLong());
    assertEquals(3.1415f, reader.readFloat());
    assertEquals(2.72, reader.readDouble());
    assertEquals("lolcat", reader.readUTF());
    assertTrue(reader.readBoolean());
  }

  private void assertHeader(DataInputStream reader) throws IOException {
    assertEquals(6, reader.readInt());
    for (int i = 0; i < COLUMN_NAMES.length; i++) {
      assertEquals(COLUMN_NAMES[i], reader.readUTF());
      assertEquals(COLUMN_TYPES[i], reader.readByte());
    }
  }
}
