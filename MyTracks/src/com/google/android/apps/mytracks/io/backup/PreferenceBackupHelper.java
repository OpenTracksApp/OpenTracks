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
import com.google.android.apps.mytracks.util.ApiAdapterFactory;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;

/**
 * Helper for backing up and restoring shared preferences.
 * 
 * @author Rodrigo Damazio
 */
public class PreferenceBackupHelper {

  private static final int BUFFER_SIZE = 2048;

  private final HashSet<String> doNotBackup;

  public PreferenceBackupHelper(Context context) {
    doNotBackup = new HashSet<String>();
    doNotBackup.add(context.getString(R.string.activity_recognition_type_key));
    doNotBackup.add(context.getString(R.string.ant_bike_cadence_sensor_id_key));
    doNotBackup.add(context.getString(R.string.ant_combined_bike_sensor_id_key));
    doNotBackup.add(context.getString(R.string.ant_heart_rate_monitor_id_key));
    doNotBackup.add(context.getString(R.string.ant_speed_distance_monitor_id_key));
    doNotBackup.add(context.getString(R.string.auto_resume_track_current_retry_key));
    doNotBackup.add(context.getString(R.string.bluetooth_sensor_key));
    doNotBackup.add(context.getString(R.string.confirm_play_earth_key));
    doNotBackup.add(context.getString(R.string.drive_deleted_list_key));
    doNotBackup.add(context.getString(R.string.drive_largest_change_id_key));
    doNotBackup.add(context.getString(R.string.drive_sync_key));
    doNotBackup.add(context.getString(R.string.google_account_key));
    doNotBackup.add(context.getString(R.string.recording_track_id_key));
    doNotBackup.add(context.getString(R.string.recording_track_paused_key));
  }

  /**
   * Exports all shared preferences from the given object as a byte array.
   * 
   * @param preferences the preferences to export
   * @return the corresponding byte array
   * @throws IOException if there are any errors while writing to the byte array
   */
  public byte[] exportPreferences(SharedPreferences preferences) throws IOException {
    ByteArrayOutputStream bufStream = new ByteArrayOutputStream(BUFFER_SIZE);
    DataOutputStream outWriter = new DataOutputStream(bufStream);
    exportPreferences(preferences, outWriter);

    return bufStream.toByteArray();
  }

  /**
   * Exports all shared preferences from the given object into the given output
   * stream.
   * 
   * @param preferences the preferences to export
   * @param outWriter the stream to write them to
   * @throws IOException if there are any errors while writing the output
   */
  public void exportPreferences(SharedPreferences preferences, DataOutputStream outWriter)
      throws IOException {
    Map<String, ?> values = preferences.getAll();

    for (String name : doNotBackup) {
      values.remove(name);
    }
    outWriter.writeInt(values.size());
    for (Map.Entry<String, ?> entry : values.entrySet()) {
      writePreference(entry.getKey(), entry.getValue(), outWriter);
    }
    outWriter.flush();
  }

  /**
   * Imports all preferences from the given byte array.
   * 
   * @param data the byte array to read preferences from
   * @param preferences the shared preferences to edit
   * @throws IOException if there are any errors while reading
   */
  public void importPreferences(byte[] data, SharedPreferences preferences) throws IOException {
    ByteArrayInputStream bufStream = new ByteArrayInputStream(data);
    DataInputStream reader = new DataInputStream(bufStream);

    importPreferences(reader, preferences);
  }

  /**
   * Imports all preferences from the given stream.
   * 
   * @param reader the stream to read from
   * @param preferences the shared preferences to edit
   * @throws IOException if there are any errors while reading
   */
  public void importPreferences(DataInputStream reader, SharedPreferences preferences)
      throws IOException {
    Editor editor = preferences.edit();
    editor.clear();

    int numPreferences = reader.readInt();
    for (int i = 0; i < numPreferences; i++) {
      String name = reader.readUTF();
      byte typeId = reader.readByte();
      readAndSetPreference(name, typeId, reader, editor);
    }
    ApiAdapterFactory.getApiAdapter().applyPreferenceChanges(editor);
  }

  /**
   * Reads a single preference and sets it into the given editor.
   * 
   * @param name the name of the preference to read
   * @param typeId the type ID of the preference to read
   * @param reader the reader to read from
   * @param editor the editor to set the preference in
   * @throws IOException if there are errors while reading
   */
  private void readAndSetPreference(String name, byte typeId, DataInputStream reader, Editor editor)
      throws IOException {

    boolean save = true;
    if (doNotBackup.contains(name)) {
      save = false;
    }
    switch (typeId) {
      case ContentTypeIds.BOOLEAN_TYPE_ID:
        boolean booleanValue = reader.readBoolean();
        if (save) {
          editor.putBoolean(name, booleanValue);
        }
        return;
      case ContentTypeIds.LONG_TYPE_ID:
        long longValue = reader.readLong();
        if (save) {
          editor.putLong(name, longValue);
        }
        return;
      case ContentTypeIds.FLOAT_TYPE_ID:
        float floatValue = reader.readFloat();
        if (save) {
          editor.putFloat(name, floatValue);
        }
        return;
      case ContentTypeIds.INT_TYPE_ID:
        int intValue = reader.readInt();
        if (save) {
          editor.putInt(name, intValue);
        }
        return;
      case ContentTypeIds.STRING_TYPE_ID:
        String utfValue = reader.readUTF();
        if (save) {
          editor.putString(name, utfValue);
        }
        return;
    }
  }

  /**
   * Writes a single preference.
   * 
   * @param name the name of the preference to write
   * @param value the correctly-typed value of the preference
   * @param writer the writer to write to
   * @throws IOException if there are errors while writing
   */
  private void writePreference(String name, Object value, DataOutputStream writer)
      throws IOException {
    writer.writeUTF(name);

    if (value instanceof Boolean) {
      writer.writeByte(ContentTypeIds.BOOLEAN_TYPE_ID);
      writer.writeBoolean((Boolean) value);
    } else if (value instanceof Integer) {
      writer.writeByte(ContentTypeIds.INT_TYPE_ID);
      writer.writeInt((Integer) value);
    } else if (value instanceof Long) {
      writer.writeByte(ContentTypeIds.LONG_TYPE_ID);
      writer.writeLong((Long) value);
    } else if (value instanceof Float) {
      writer.writeByte(ContentTypeIds.FLOAT_TYPE_ID);
      writer.writeFloat((Float) value);
    } else if (value instanceof String) {
      writer.writeByte(ContentTypeIds.STRING_TYPE_ID);
      writer.writeUTF((String) value);
    } else {
      throw new IllegalArgumentException("Type " + value.getClass().getName() + " not supported");
    }
  }
}
