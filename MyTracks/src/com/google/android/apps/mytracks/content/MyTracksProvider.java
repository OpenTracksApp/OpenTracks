/*
 * Copyright 2011 Google Inc.
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
package com.google.android.apps.mytracks.content;

import com.google.android.apps.mytracks.Constants;
import com.google.android.maps.mytracks.R;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Process;

/**
 * A content provider that accesses MyTracks tracks, track points, and
 * waypoints. After checking access, calls the corresponding methods in
 * {@link DatabaseProvider}.
 *
 * @author Jimmy Shih
 */
public class MyTracksProvider extends DatabaseProvider {
 
  private boolean canAccess() {
    if (Binder.getCallingPid() == Process.myPid()) {
      return true;
    } else {
      Context context = getContext();
      SharedPreferences sharedPreferences = context.getSharedPreferences(
          Constants.SETTINGS_NAME, 0);
      return sharedPreferences.getBoolean(context.getString(R.string.share_data_key), false);
    }
  }
  
  /**
   * Gets a URI based on the input URI, but replaces the
   * {@link MyTracksProvider} authority with the {@link DatabaseProvider}
   * authority. Returns null if the input URI doesn't contain the
   * {@link MyTracksProvider} authority.
   */
  private Uri getDatabaseProviderUri(Uri uri) {
    String authority = uri.getAuthority();
    if (authority != null && authority.equals(MyTracksProviderUtils.AUTHORITY)) {
      String databaseUri = "content://" + MyTracksProviderUtils.DATABASE_AUTHORITY;
      String path = uri.getPath();
      if (path != null && !path.isEmpty()) {
        databaseUri += path;
      }
      return Uri.parse(databaseUri);
    }
    return null;
  }

  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs) {
    if (!canAccess()) {
      return 0;
      }
    return super.delete(getDatabaseProviderUri(uri), selection, selectionArgs);
  }

  @Override
  public String getType(Uri uri) {
    if (!canAccess()) {
      return null;
    }
    return super.getType(getDatabaseProviderUri(uri));
  }

  @Override
  public Uri insert(Uri uri, ContentValues values) {
    if (!canAccess()) {
      return null;
    }
    return super.insert(getDatabaseProviderUri(uri), values);
  }
  
  @Override
  public int bulkInsert(Uri uri, ContentValues[] values) {
    if (!canAccess()) {
      return 0;
    }
    return super.bulkInsert(getDatabaseProviderUri(uri), values);
  }

  @Override
  public Cursor query(
      Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    if (!canAccess()) {
      return null;
    }
    return super.query(getDatabaseProviderUri(uri), projection, selection, selectionArgs, sortOrder);
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
    if (!canAccess()) {
      return 0;
    }
    return super.update(getDatabaseProviderUri(uri), values, selection, selectionArgs);
  }
}
