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

package com.google.android.apps.mytracks.io.file.export;

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint;

import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * KMZ track exporter.
 * 
 * @author Jimmy Shih
 */
public class KmzTrackExporter implements TrackExporter {
  
  public static final String KMZ_EXTENSION = "kmz";
  public static final String KMZ_IMAGES_DIR = "images";
  public static final String KMZ_KML_FILE = "doc.kml";

  private static final String TAG = KmzTrackExporter.class.getSimpleName();
  private static final int BUFFER_SIZE = 4096;

  private final MyTracksProviderUtils myTracksProviderUtils;
  private final FileTrackExporter fileTrackExporter;
  private final Track[] tracks;

  /**
   * Constructor.
   * 
   * @param myTracksProviderUtils the my tracks provider utils
   * @param fileTrackExporter the file track exporter
   * @param tracks the tracks to export
   */
  public KmzTrackExporter(MyTracksProviderUtils myTracksProviderUtils,
      FileTrackExporter fileTrackExporter, Track[] tracks) {
    this.myTracksProviderUtils = myTracksProviderUtils;
    this.fileTrackExporter = fileTrackExporter;
    this.tracks = tracks;
  }

  @Override
  public boolean writeTrack(OutputStream outputStream) {
    ZipOutputStream zipOutputStream = null;
    try {
      zipOutputStream = new ZipOutputStream(outputStream);

      // Add kml file
      ZipEntry zipEntry = new ZipEntry(KMZ_KML_FILE);
      zipOutputStream.putNextEntry(zipEntry);

      boolean success = fileTrackExporter.writeTrack(zipOutputStream);
      zipOutputStream.closeEntry();
      if (!success) {
        Log.e(TAG, "Unable to write kml in kmz");
        return false;        
      }

      // Add photos
      addImages(zipOutputStream);
      return true;
    } catch (InterruptedException e) {
      Log.e(TAG, "Unable to write track", e);
      return false;
    } catch (IOException e) {
      Log.e(TAG, "Unable to write track", e);
      return false;
    } finally {
      if (zipOutputStream != null) {
        try {
          zipOutputStream.close();
        } catch (IOException e) {
          Log.e(TAG, "Unable to close zip input stream", e);;
        }
      }
    }
  }

  private void addImages(ZipOutputStream zipOutputStream) throws InterruptedException, IOException {
    for (Track track : tracks) {
      Cursor cursor = null;
      try {
        cursor = myTracksProviderUtils.getWaypointCursor(track.getId(), -1L, -1);
        if (cursor != null && cursor.moveToFirst()) {
          /*
           * Yes, this will skip the first waypoint and that is intentional as
           * the first waypoint holds the stats for the track.
           */
          while (cursor.moveToNext()) {
            if (Thread.interrupted()) {
              throw new InterruptedException();
            }
            Waypoint waypoint = myTracksProviderUtils.createWaypoint(cursor);
            String photoUrl = waypoint.getPhotoUrl();
            if (photoUrl != null && !photoUrl.equals("")) {
              addImage(zipOutputStream, photoUrl);
            }
          }
        }
      } finally {
        if (cursor != null) {
          cursor.close();
        }
      }
    }
  }

  private void addImage(ZipOutputStream zipOutputStream, String photoUrl) throws IOException {
    Uri uri = Uri.parse(photoUrl);
    ZipEntry zipEntry = new ZipEntry(
        KMZ_IMAGES_DIR + File.separatorChar + uri.getLastPathSegment());
    zipOutputStream.putNextEntry(zipEntry);

    FileInputStream fileInputStream = null;
    try {
      fileInputStream = new FileInputStream(new File(uri.getPath()));
      byte[] buffer = new byte[BUFFER_SIZE];
      int byteCount = 0;
      while ((byteCount = fileInputStream.read(buffer)) != -1) {
        zipOutputStream.write(buffer, 0, byteCount);
      }
    } finally {
      if (fileInputStream != null) {
        fileInputStream.close();
      }
    }
    zipOutputStream.closeEntry();
  }
}