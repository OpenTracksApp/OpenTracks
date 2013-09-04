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

package com.google.android.apps.mytracks.io.file;

import com.google.android.apps.mytracks.io.file.export.KmzTrackExporter;

import android.content.Context;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * Imports a KMZ file.
 * 
 * @author Jimmy Shih
 */
public class KmzTrackImporter implements TrackImporter {

  private final Context context;
  private final String photoPath;

  public KmzTrackImporter(Context context, String photoPath) {
    this.context = context;
    this.photoPath = photoPath;
  }

  @Override
  public long[] importFile(InputStream inputStream)
      throws IOException, ParserConfigurationException, SAXException {
    long[] result = null;
    ZipInputStream zipInputStream = null;
    try {
      zipInputStream = new ZipInputStream(inputStream);
      ZipEntry zipEntry;
      while ((zipEntry = zipInputStream.getNextEntry()) != null) {
        String fileName = zipEntry.getName();
        if (fileName.equals(KmzTrackExporter.KML_FILE_NAME)) {
          KmlFileTrackImporter kmlFileTrackImporter = new KmlFileTrackImporter(
              context, -1L, photoPath);
          byte[] buffer = readKml(zipInputStream);
          result = kmlFileTrackImporter.importFile(new ByteArrayInputStream(buffer));
        } else {
          String prefix = KmzTrackExporter.KMZ_IMAGES_DIR + File.separatorChar;
          if (fileName.startsWith(prefix)) {
            readFile(zipInputStream, fileName.substring(prefix.length()));
          }
        }
        zipInputStream.closeEntry();
      }
    } finally {
      if (zipInputStream != null) {
        zipInputStream.close();
      }
    }
    return result == null ? new long[0] : result;
  }

  private byte[] readKml(ZipInputStream zipInputStream) throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = null;
    try {
      byteArrayOutputStream = new ByteArrayOutputStream();
      byte[] buffer = new byte[KmzTrackExporter.BUFFER_SIZE];
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

  private void readFile(ZipInputStream zipInputStream, String fileName) throws IOException {
    FileOutputStream fileOutputStream = null;
    try {
      fileOutputStream = new FileOutputStream(photoPath + File.separatorChar + fileName);
      byte[] buffer = new byte[KmzTrackExporter.BUFFER_SIZE];
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
