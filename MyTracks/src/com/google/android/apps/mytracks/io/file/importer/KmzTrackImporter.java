/*
 * Copyright 2013 Google Inc.
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

package com.google.android.apps.mytracks.io.file.importer;

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.io.file.exporter.KmzTrackExporter;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Imports a KMZ file.
 * 
 * @author Jimmy Shih
 */
public class KmzTrackImporter implements TrackImporter {

  private static final String TAG = KmzTrackImporter.class.getSimpleName();
  private static final int BUFFER_SIZE = 4096;

  private final Context context;
  private final String photoPath;

  public KmzTrackImporter(Context context, String photoPath) {
    this.context = context;
    this.photoPath = photoPath;
  }

  @Override
  public long importFile(InputStream inputStream) {
    ZipInputStream zipInputStream = null;
    long importedTrackId = -1L;
    try {
      ZipEntry zipEntry;

      zipInputStream = new ZipInputStream(inputStream);
      while ((zipEntry = zipInputStream.getNextEntry()) != null) {
        if (Thread.interrupted()) {
          Log.d(TAG, "Thread interrupted");
          cleanImport(importedTrackId);
          return -1L;
        }
        String fileName = zipEntry.getName();
        if (fileName.equals(KmzTrackExporter.KMZ_KML_FILE)) {
          importedTrackId = parseKml(zipInputStream);
          if (importedTrackId == -1L) {
            Log.d(TAG, "Unable to parse kml in kmz");
            cleanImport(importedTrackId);
            return -1L;
          }
        } else {
          String prefix = KmzTrackExporter.KMZ_IMAGES_DIR + File.separatorChar;
          if (fileName.startsWith(prefix)) {
            readImageFile(zipInputStream, fileName.substring(prefix.length()));
          }
        }
        zipInputStream.closeEntry();
      }
      return importedTrackId;
    } catch (IOException e) {
      Log.e(TAG, "Unable to import file", e);
      cleanImport(importedTrackId);
      return -1L;
    } finally {
      if (zipInputStream != null) {
        try {
          zipInputStream.close();
        } catch (IOException e) {
          Log.e(TAG, "Unable to close zip input stream", e);
        }
      }
    }
  }

  /**
   * Cleans up import.
   * 
   * @param importedTrackId the imported track id
   */
  private void cleanImport(long importedTrackId) {
    if (importedTrackId != -1L) {
      MyTracksProviderUtils myTracksProviderUtils = MyTracksProviderUtils.Factory.get(context);
      myTracksProviderUtils.deleteTrack(importedTrackId);
    }
    if (photoPath != null) {
      File dir = new File(photoPath);
      if (dir.exists() && dir.isDirectory()) {
        for (File file : dir.listFiles()) {
          file.delete();
        }
        dir.delete();
      }
    }
  }

  /**
   * Parses kml
   * 
   * @param zipInputStream the zip input stream
   * @return the imported track id or -1L
   */
  private long parseKml(ZipInputStream zipInputStream) throws IOException {
    ByteArrayInputStream byteArrayInputStream = null;
    try {
      KmlFileTrackImporter kmlFileTrackImporter = new KmlFileTrackImporter(context, -1L, photoPath);
      byteArrayInputStream = new ByteArrayInputStream(getKml(zipInputStream));
      return kmlFileTrackImporter.importFile(byteArrayInputStream);
    } finally {
      if (byteArrayInputStream != null) {
        byteArrayInputStream.close();
      }
    }
  }

  /**
   * Gets the kml as byte array.
   * 
   * @param zipInputStream the zip input stream
   */
  private byte[] getKml(ZipInputStream zipInputStream) throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = null;
    try {
      byteArrayOutputStream = new ByteArrayOutputStream();
      byte[] buffer = new byte[BUFFER_SIZE];
      int count;
      while ((count = zipInputStream.read(buffer)) != -1) {
        byteArrayOutputStream.write(buffer, 0, count);
      }
      return byteArrayOutputStream.toByteArray();
    } finally {
      if (byteArrayOutputStream != null) {
        byteArrayOutputStream.close();
      }
    }
  }

  /**
   * Reads an image file.
   * 
   * @param zipInputStream the zip input stream
   * @param fileName the file name
   */
  private void readImageFile(ZipInputStream zipInputStream, String fileName) throws IOException {
    FileOutputStream fileOutputStream = null;
    try {
      fileOutputStream = new FileOutputStream(photoPath + File.separatorChar + fileName);
      byte[] buffer = new byte[BUFFER_SIZE];
      int count;
      while ((count = zipInputStream.read(buffer)) != -1) {
        fileOutputStream.write(buffer, 0, count);
      }
    } finally {
      if (fileOutputStream != null) {
        fileOutputStream.close();
      }
    }
  }
}
