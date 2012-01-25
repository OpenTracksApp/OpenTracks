// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.android.apps.mytracks.io.maps;

import com.google.android.apps.mytracks.io.gdata.GDataClientFactory;
import com.google.android.common.gdata.AndroidXmlParserFactory;
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
 * @author jshih@google.com (Jimmy Shih)
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
    this.activity = activity;
    this.account = account;

    context = activity.getApplicationContext();
    gDataClient = GDataClientFactory.getGDataClient(context);
    mapsClient = new MapsClient(
        gDataClient, new XmlMapsGDataParserFactory(new AndroidXmlParserFactory()));

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
    return getMaps();
  }

  @Override
  protected void onCancelled() {
    closeClient();
  }

  @Override
  protected void onPostExecute(Boolean result) {
    closeClient();
    success = result;
    completed = true;
    if (activity != null) {
      activity.onAsyncTaskCompleted(success, mapIds, mapData);
    }
  }

  /**
   * Closes the gdata client.
   */
  private void closeClient() {
    if (gDataClient != null) {
      gDataClient.close();
    }
  }

  /**
   * Gets all the maps from Google Maps.
   *
   * @return true if success.
   */
  private boolean getMaps() {
    // Reset the per request states
    authToken = null;
    mapIds = new ArrayList<String>();
    mapData = new ArrayList<MapsMapMetadata>();

    try {
      authToken = AccountManager.get(context).blockingGetAuthToken(
          account, MapsConstants.SERVICE_NAME, false);
    } catch (OperationCanceledException e) {
      Log.d(TAG, e.getMessage());
      return retryUpload();
    } catch (AuthenticatorException e) {
      Log.d(TAG, e.getMessage());
      return retryUpload();
    } catch (IOException e) {
      Log.d(TAG, e.getMessage());
      return retryUpload();
    }

    if (isCancelled()) {
      return false;
    }
    
    try {
      GDataParser gDataParser = mapsClient.getParserForFeed(
          MapFeatureEntry.class, MapsClient.getMapsFeed(), authToken);
      gDataParser.init();
      while (gDataParser.hasMoreData()) {
        MapFeatureEntry entry = (MapFeatureEntry) gDataParser.readNextEntry(null);
        mapIds.add(MapsGDataConverter.getMapidForEntry(entry));
        mapData.add(MapsGDataConverter.getMapMetadataForEntry(entry));
      }
      gDataParser.close();
    } catch (ParseException e) {
      Log.d(TAG, e.getMessage());
      return retryUpload();
    } catch (IOException e) {
      Log.d(TAG, e.getMessage());
      return retryUpload();
    } catch (HttpException e) {
      Log.d(TAG, e.getMessage());
      return retryUpload();
    }

    return true;
  }

  /**
   * Retries upload. Invalidates the authToken. If can retry, invokes
   * {@link ChooseMapAsyncTask#getMaps()}. Returns false if cannot retry.
   */
  private boolean retryUpload() {
    if (isCancelled()) {
      return false;
    }

    AccountManager.get(context).invalidateAuthToken(MapsConstants.SERVICE_NAME, authToken);
    if (canRetry) {
      canRetry = false;
      return getMaps();
    }
    return false;
  }
}
