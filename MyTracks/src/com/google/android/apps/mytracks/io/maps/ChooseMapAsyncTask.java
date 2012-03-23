/*
 * Copyright 2012 Google Inc.
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
package com.google.android.apps.mytracks.io.maps;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.io.gdata.GDataClientFactory;
import com.google.android.apps.mytracks.io.gdata.maps.MapFeatureEntry;
import com.google.android.apps.mytracks.io.gdata.maps.MapsClient;
import com.google.android.apps.mytracks.io.gdata.maps.MapsConstants;
import com.google.android.apps.mytracks.io.gdata.maps.MapsGDataConverter;
import com.google.android.apps.mytracks.io.gdata.maps.MapsMapMetadata;
import com.google.android.apps.mytracks.io.gdata.maps.XmlMapsGDataParserFactory;
import com.google.android.common.gdata.AndroidXmlParserFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.wireless.gdata.client.GDataClient;
import com.google.wireless.gdata.client.HttpException;
import com.google.wireless.gdata.parser.GDataParser;
import com.google.wireless.gdata.parser.ParseException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

/**
 * AsyncTask for {@link ChooseMapActivity} to get all the maps from Google Maps.
 *
 * @author Jimmy Shih
 */
public class ChooseMapAsyncTask extends AsyncTask<Void, Integer, Boolean> {
  private static final String TAG = ChooseMapAsyncTask.class.getSimpleName();

  private ChooseMapActivity activity;
  private final Account account;
  private final Context context;
  private final GDataClient gDataClient;
  private final MapsClient mapsClient;

  /**
   * True if can retry sending to Google Fusion Tables.
   */
  private boolean canRetry;

  /**
   * True if the AsyncTask has completed.
   */
  private boolean completed;

  /**
   * True if the result is success.
   */
  private boolean success;

  // The following variables are for per request states
  private String authToken;
  private ArrayList<String> mapIds;
  private ArrayList<MapsMapMetadata> mapData;

  public ChooseMapAsyncTask(ChooseMapActivity activity, Account account) {
    this(activity, account, activity.getApplicationContext(), GDataClientFactory
        .getGDataClient(activity.getApplicationContext()), new MapsClient(
        GDataClientFactory.getGDataClient(activity.getApplicationContext()),
        new XmlMapsGDataParserFactory(new AndroidXmlParserFactory())));
  }

	/**
	 * Creates this constructor for test.
	 */
	public ChooseMapAsyncTask(ChooseMapActivity activity, Account account,
			Context context, GDataClient gDataClient, MapsClient mapsClient) {
		this.activity = activity;
		this.account = account;

		this.context = context;
		this.gDataClient = gDataClient;
		this.mapsClient = mapsClient;

		canRetry = true;
		completed = false;
		success = false;
	}

  /**
   * Sets the activity associated with this AyncTask.
   *
   * @param activity the activity.
   */
  public void setActivity(ChooseMapActivity activity) {
    this.activity = activity;
    if (completed && activity != null) {
      activity.onAsyncTaskCompleted(success, mapIds, mapData);
    }
  }

  @Override
  protected void onPreExecute() {
    activity.showProgressDialog();
  }

  @Override
  protected Boolean doInBackground(Void... params) {
    try {
      return getMaps();
    } finally {
      if (gDataClient != null) {
        gDataClient.close();
      } 
    }
  }

  @Override
  protected void onPostExecute(Boolean result) {
    success = result;
    completed = true;
    if (activity != null) {
      activity.onAsyncTaskCompleted(success, mapIds, mapData);
    }
  }

  /**
   * Gets all the maps from Google Maps.
   *
   * @return true if success.
   */
  @VisibleForTesting
  boolean getMaps() {
    // Reset the per request states
    authToken = null;
    mapIds = new ArrayList<String>();
    mapData = new ArrayList<MapsMapMetadata>();

    try {
      authToken = AccountManager.get(context).blockingGetAuthToken(
          account, MapsConstants.SERVICE_NAME, false);
    } catch (OperationCanceledException e) {
      Log.d(TAG, "Unable to get auth token", e);
      return retryUpload();
    } catch (AuthenticatorException e) {
      Log.d(TAG, "Unable to get auth token", e);
      return retryUpload();
    } catch (IOException e) {
      Log.d(TAG, "Unable to get auth token", e);
      return retryUpload();
    }

    if (isCancelled()) {
      return false;
    }
    GDataParser gDataParser = null;
    try {
      gDataParser = mapsClient.getParserForFeed(
          MapFeatureEntry.class, MapsClient.getMapsFeed(), authToken);
      gDataParser.init();
      while (gDataParser.hasMoreData()) {
        MapFeatureEntry entry = (MapFeatureEntry) gDataParser.readNextEntry(null);
        mapIds.add(MapsGDataConverter.getMapidForEntry(entry));
        mapData.add(MapsGDataConverter.getMapMetadataForEntry(entry));
      }
    } catch (ParseException e) {
      Log.d(TAG, "Unable to get maps", e);
      return retryUpload();
    } catch (IOException e) {
      Log.d(TAG, "Unable to get maps", e);
      return retryUpload();
    } catch (HttpException e) {
      Log.d(TAG, "Unable to get maps", e);
      return retryUpload();
    } finally {
      if (gDataParser != null) {
        gDataParser.close();
      }
    }

    return true;
  }

  /**
   * Retries upload. Invalidates the authToken. If can retry, invokes
   * {@link ChooseMapAsyncTask#getMaps()}. Returns false if cannot retry.
   */
  @VisibleForTesting
  boolean retryUpload() {
    if (isCancelled()) {
      return false;
    }

    AccountManager.get(context).invalidateAuthToken(Constants.ACCOUNT_TYPE, authToken);
    if (canRetry) {
      canRetry = false;
      return getMaps();
    }
    return false;
  }
  
  /**
   * Gets the complete status of task.
   */
  @VisibleForTesting
  boolean getCompleted() {
    return completed;
  }
  
  /**
   * Sets the complete status of task.
   * @param completed
   */
  @VisibleForTesting
  void setCompleted(boolean completed) {
    this.completed = completed;
  }
  
  /**
   * Sets the status of canRetry.
   * @param canRetry status of canRetry
   */
  @VisibleForTesting
  void setCanRetry(boolean canRetry) {
    this.canRetry = canRetry;
  }
  
  /**
   * Gets the status of canRetry.
   */
  @VisibleForTesting
  boolean getCanRetry() {
    return canRetry;
  }
}