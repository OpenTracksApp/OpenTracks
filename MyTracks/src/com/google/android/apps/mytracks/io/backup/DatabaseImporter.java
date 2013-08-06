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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * Database importer which reads values written by {@link DatabaseDumper}.
 *
 * @author Rodrigo Damazio
 */
public class DatabaseImporter {

  /** Maximum number of entries in a bulk insertion */
  private static final int DEFAULT_BULK_SIZE = 1024;

  private final Uri destinationUri;
  private final ContentResolver resolver;
  private final boolean readNullFields;
  private final int bulkSize;

  // Metadata read from the reader
  private String[] columnNames;
  private byte[] columnTypes;

  public DatabaseImporter(Uri destinationUri, ContentResolver resolver,
      boolean readNullFields) {
    this(destinationUri, resolver, readNullFields, DEFAULT_BULK_SIZE);
  }

  protected DatabaseImporter(Uri destinationUri, ContentResolver resolver,
      boolean readNullFields, int bulkSize) {
    this.destinationUri = destinationUri;
    this.resolver = resolver;
    this.readNullFields = readNullFields;
    this.bulkSize = bulkSize;
  }

  /**
   * Reads the header which includes metadata about the table being imported.
   *
   * @throws IOException if there are any problems while reading
   */
  private void readHeaders(DataInputStream reader) throws IOException {
    int numColumns = reader.readInt();
    columnNames = new String[numColumns];
    columnTypes = new byte[numColumns];
    for (int i = 0; i < numColumns; i++) {
      columnNames[i] = reader.readUTF();
      columnTypes[i] = reader.readByte();
    }
  }

  /**
   * Imports all rows from the reader into the database.
   * Insertion is done in bulks for efficiency.
   *
   * @throws IOException if there are any errors while reading
   */
  public void importAllRows(DataInputStream reader) throws IOException {
    readHeaders(reader);

    ContentValues[] valueBulk = new ContentValues[bulkSize];
    int numValues = 0;

    int numRows = reader.readInt();
    int numColumns = columnNames.length;

    // For each row
    for (int r = 0; r < numRows; r++) {
      if (valueBulk[numValues] == null) {
        valueBulk[numValues] = new ContentValues(numColumns);
      } else {
        // Reuse values objects
        valueBulk[numValues].clear();
      }

      // Read the fields bitmap
      long fields = reader.readLong();
      for (int c = 0; c < numColumns; c++) {
        if ((fields & 1) == 1) {
          // Field is present, read into values
          readOneCell(columnNames[c], columnTypes[c], valueBulk[numValues],
              reader);
        } else if (readNullFields) {
          // Field not present but still written, read and discard
          readOneCell(columnNames[c], columnTypes[c], null, reader);
        }

        fields >>= 1;
      }

      numValues++;

      // If we have enough values, flush them as a bulk insertion
      if (numValues >= bulkSize) {
        doBulkInsert(valueBulk);
        numValues = 0;
      }
    }

    // Do a final bulk insert with the leftovers
    if (numValues > 0) {
      ContentValues[] leftovers = new ContentValues[numValues];
      System.arraycopy(valueBulk, 0, leftovers, 0, numValues);
      doBulkInsert(leftovers);
    }
  }

  protected void doBulkInsert(ContentValues[] values) {
    resolver.bulkInsert(destinationUri, values);
  }

  /**
   * Reads a single cell from the reader.
   *
   * @param name the name of the column to be read
   * @param typeId the type ID of the column to be read
   * @param values the {@link ContentValues} object to put the read cell value
   *        in - if null, the value is just discarded
   * @throws IOException if there are any problems while reading
   */
  private void readOneCell(String name, byte typeId, ContentValues values,
      DataInputStream reader) throws IOException {
    switch (typeId) {
      case ContentTypeIds.BOOLEAN_TYPE_ID: {
        boolean value = reader.readBoolean();
        if (values != null) { values.put(name, value); }
        return;
      }
      case ContentTypeIds.LONG_TYPE_ID: {
        long value = reader.readLong();
        if (values != null) { values.put(name, value); }
        return;
      }
      case ContentTypeIds.DOUBLE_TYPE_ID: {
        double value = reader.readDouble();
        if (values != null) { values.put(name, value); }
        return;
      }
      case ContentTypeIds.FLOAT_TYPE_ID: {
        Float value = reader.readFloat();
        if (values != null) { values.put(name, value); }
        return;
      }
      case ContentTypeIds.INT_TYPE_ID: {
        int value = reader.readInt();
        if (values != null) { values.put(name, value); }
        return;
      }
      case ContentTypeIds.STRING_TYPE_ID: {
        String value = reader.readUTF();
        if (values != null) { values.put(name, value); }
        return;
      }
      case ContentTypeIds.BLOB_TYPE_ID: {
        int blobLength = reader.readInt();
        if (blobLength != 0) {
          byte[] blob = new byte[blobLength];
          int readBytes = reader.read(blob, 0, blobLength);
          if (readBytes != blobLength) {
            throw new IOException(String.format(Locale.US,
                "Short read on column %s; expected %d bytes, read %d",
                name, blobLength, readBytes));
          }
          
          if (values != null) {
            values.put(name, blob);
          }
        }
        return;
      }
      default:
        throw new IOException("Read unknown type " + typeId);
    }
  }
}
