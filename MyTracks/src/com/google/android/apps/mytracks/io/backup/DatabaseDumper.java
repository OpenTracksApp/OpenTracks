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

import android.database.Cursor;
import android.database.MergeCursor;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Database dumper which is able to write only part of the database
 * according to some query.
 *
 * This dumper is symmetrical to {@link DatabaseImporter}.
 *
 * @author Rodrigo Damazio
 */
class DatabaseDumper {

  /** The names of the columns being dumped. */
  private final String[] columnNames;
  /** The types of the columns being dumped. */
  private final byte[] columnTypes;
  /** Whether to output null fields. */
  private final boolean outputNullFields;

  // Temporary state
  private int[] columnIndices;
  private boolean[] hasFields;

  public DatabaseDumper(String[] columnNames, byte[] columnTypes,
      boolean outputNullFields) {
    if (columnNames.length != columnTypes.length) {
      throw new IllegalArgumentException("Names don't match types");
    }

    this.columnNames = columnNames;
    this.columnTypes = columnTypes;
    this.outputNullFields = outputNullFields;
  }

  /**
   * Writes the header plus all rows that can be read from the given cursor.
   * This assumes the cursor will have the same column and column indices on
   * every row (and thus may not work with a {@link MergeCursor}).
   */
  public void writeAllRows(Cursor cursor, DataOutputStream writer)
      throws IOException {
    writeHeaders(cursor, cursor.getCount(), writer);

    if (!cursor.moveToFirst()) {
      return;
    }

    do {
      writeOneRow(cursor, writer);
    } while (cursor.moveToNext());
  }

  /**
   * Writes just the headers for the data that will come from the given cursor.
   * The headers include column information and the number of rows that will be
   * written.
   *
   * @param cursor the cursor to get columns from
   * @param numRows the number of rows that will be later written
   * @param writer the output to write to
   * @throws IOException if there are errors while writing
   */
  public void writeHeaders(Cursor cursor, int numRows, DataOutputStream writer)
      throws IOException {
    initializeCachedValues(cursor);
    writeQueryMetadata(numRows, writer);
  }

  /**
   * Writes the current row from the cursor. The cursor is not advanced.
   * This must be called after {@link #writeHeaders}.
   *
   * @param cursor the cursor to write data from
   * @param writer the output to write to
   * @throws IOException if there are any errors while writing
   */
  public void writeOneRow(Cursor cursor, DataOutputStream writer)
      throws IOException {
    if (columnIndices == null) {
      throw new IllegalStateException(
          "Cannot write rows before writing the header");
    }

    if (columnIndices.length > Long.SIZE) {
      throw new IllegalArgumentException("Too many fields");
    }

    // Build a bitmap of which fields are present
    long fields = 0;
    for (int i = 0; i < columnIndices.length; i++) {
      hasFields[i] = !cursor.isNull(columnIndices[i]);
      fields |= (hasFields[i] ? 1 : 0) << i;
    }
    writer.writeLong(fields);

    // Actually write the present fields
    for (int i = 0; i < columnIndices.length; i++) {
      if (hasFields[i]) {
        writeCell(columnIndices[i], columnTypes[i], cursor, writer);
      } else if (outputNullFields) {
        writeDummyCell(columnTypes[i], writer);
      }
    }
  }

  /**
   * Initializes the column indices and other temporary state for reading from
   * the given cursor.
   */
  private void initializeCachedValues(Cursor cursor) {
    // These indices are constant for every row (unless we're fed a MergeCursor)
    if (cursor instanceof MergeCursor) {
      throw new IllegalArgumentException("Cannot use a MergeCursor");
    }

    columnIndices = new int[columnNames.length];
    for (int i = 0; i < columnNames.length; i++) {
      String columnName = columnNames[i];
      columnIndices[i] = cursor.getColumnIndexOrThrow(columnName);
    }

    hasFields = new boolean[columnIndices.length];
  }

  /**
   * Writes metadata about the query to be dumped.
   *
   * @param numRows the number of rows that will be dumped
   * @param writer the output to write to
   * @throws IOException if there are any errors while writing
   */
  private void writeQueryMetadata(
      int numRows, DataOutputStream writer) throws IOException {
    // Write column data
    writer.writeInt(columnNames.length);
    for (int i = 0; i < columnNames.length; i++) {
      String columnName = columnNames[i];
      byte columnType = columnTypes[i];
      writer.writeUTF(columnName);
      writer.writeByte(columnType);
    }

    // Write the number of rows
    writer.writeInt(numRows);
  }

  /**
   * Writes a single cell of the database to the output.
   *
   * @param columnIdx the column index to read from
   * @param columnTypeId the type of the column to be read
   * @param cursor the cursor to read from
   * @param writer the output to write to
   * @throws IOException if there are any errors while writing
   */
  private void writeCell(
      int columnIdx, byte columnTypeId, Cursor cursor, DataOutputStream writer)
      throws IOException {
    switch (columnTypeId) {
      case ContentTypeIds.LONG_TYPE_ID:
        writer.writeLong(cursor.getLong(columnIdx));
        return;
      case ContentTypeIds.DOUBLE_TYPE_ID:
        writer.writeDouble(cursor.getDouble(columnIdx));
        return;
      case ContentTypeIds.FLOAT_TYPE_ID:
        writer.writeFloat(cursor.getFloat(columnIdx));
        return;
      case ContentTypeIds.BOOLEAN_TYPE_ID:
        writer.writeBoolean(cursor.getInt(columnIdx) != 0);
        return;
      case ContentTypeIds.INT_TYPE_ID:
        writer.writeInt(cursor.getInt(columnIdx));
        return;
      case ContentTypeIds.STRING_TYPE_ID:
        writer.writeUTF(cursor.getString(columnIdx));
        return;
      case ContentTypeIds.BLOB_TYPE_ID: {
        byte[] blob = cursor.getBlob(columnIdx);
        writer.writeInt(blob.length);
        writer.write(blob);
        return;
      }
      default:
        throw new IllegalArgumentException(
            "Type " + columnTypeId + " not supported");
    }
  }

  /**
   * Writes a dummy cell value to the output.
   *
   * @param columnTypeId the type of the value to write
   * @throws IOException if there are any errors while writing
   */
  private void writeDummyCell(byte columnTypeId, DataOutputStream writer)
      throws IOException {
    switch (columnTypeId) {
      case ContentTypeIds.LONG_TYPE_ID:
        writer.writeLong(0L);
        return;
      case ContentTypeIds.DOUBLE_TYPE_ID:
        writer.writeDouble(0.0);
        return;
      case ContentTypeIds.FLOAT_TYPE_ID:
        writer.writeFloat(0.0f);
        return;
      case ContentTypeIds.BOOLEAN_TYPE_ID:
        writer.writeBoolean(false);
        return;
      case ContentTypeIds.INT_TYPE_ID:
        writer.writeInt(0);
        return;
      case ContentTypeIds.STRING_TYPE_ID:
        writer.writeUTF("");
        return;
      case ContentTypeIds.BLOB_TYPE_ID:
        writer.writeInt(0);
        return;
      default:
        throw new IllegalArgumentException(
            "Type " + columnTypeId + " not supported");
    }
  }
}
