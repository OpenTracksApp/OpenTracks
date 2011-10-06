// Copyright 2010 Google Inc. All Rights Reserved.
package com.google.android.apps.mytracks.io.mymaps;

import com.google.android.apps.mytracks.io.AuthManager;
import com.google.android.apps.mytracks.io.AuthManager.AuthCallback;
import com.google.android.apps.mytracks.io.gdata.GDataClientFactory;
import com.google.android.common.gdata.AndroidXmlParserFactory;
import com.google.wireless.gdata.client.GDataClient;
import com.google.wireless.gdata.client.HttpException;
import com.google.wireless.gdata.parser.xml.SimplePullParser.ParseException;
import com.google.wireless.gdata2.ConflictDetectedException;
import com.google.wireless.gdata2.client.AuthenticationException;

import android.content.Context;
import android.util.Log;

import java.io.IOException;

/**
 * MyMapsGDataWrapper provides a wrapper around GData operations that maintains
 * the GData client, and provides a method to run gdata queries with proper
 * error handling. After a query is run, the wrapper can be queried about the
 * error that occurred.
 */
class MyMapsGDataWrapper {
  /**
   * A QueryFunction is passed in when executing a query. The query function
   * of the class is called with the GData client as a parameter. The
   * function should execute whatever operations it desires on the client
   * without concern for whether the client will throw an error.
   */
  interface QueryFunction {
    public abstract void query(MapsClient client)
        throws AuthenticationException, IOException, ParseException,
        ConflictDetectedException, Exception;
  }

  // The types of error that may be encountered
  /** No error occurred. */
  public static final int ERROR_NO_ERROR = 0;
  /** There was an authentication error, the auth token may be invalid. */
  public static final int ERROR_AUTH = 1;
  /** There was an internal error on the server side. */
  public static final int ERROR_INTERNAL = 2;
  /** There was an error connecting to the server. */
  public static final int ERROR_CONNECTION = 3;
  /** The item queried did not exit. */
  public static final int ERROR_NOT_FOUND = 4;
  /** There was an error parsing or serializing locally. */
  public static final int ERROR_LOCAL = 5;
  /** There was a conflict, update the entry and try again. */
  public static final int ERROR_CONFLICT = 6;
  /**
   * A query was run after cleaning up the wrapper, so the client was invalid.
   */
  public static final int ERROR_CLEANED_UP = 7;
  /** An unknown error occurred. */
  public static final int ERROR_UNKNOWN = 100;

  private final GDataClient androidGdataClient;
  private final AuthManager auth;
  private final MapsClient client;

  private String errorMessage;
  private int errorType;
  private boolean retryOnAuthFailure;
  private int retriesPending;
  private boolean cleanupCalled;

  public MyMapsGDataWrapper(Context context, AuthManager auth) {
    androidGdataClient = GDataClientFactory.getGDataClient(context);
    this.auth = auth;
    client =
        new MapsClient(androidGdataClient, new XmlMapsGDataParserFactory(
            new AndroidXmlParserFactory()));
    errorType = ERROR_NO_ERROR;
    errorMessage = null;
    retryOnAuthFailure = false;
    retriesPending = 0;
    cleanupCalled = false;
  }

  public boolean runQuery(final QueryFunction query) {
    if (client == null) {
      errorType = ERROR_CLEANED_UP;
      errorMessage = "GData Wrapper has already been cleaned up!";
      return false;
    }
    try {
      query.query(client);
      errorType = ERROR_NO_ERROR;
      errorMessage = null;
      return true;
    } catch (AuthenticationException e) {
      Log.e(MyMapsConstants.TAG, "Exception", e);
      errorType = ERROR_AUTH;
      errorMessage = e.getMessage();
    } catch (HttpException e) {
      Log.e(MyMapsConstants.TAG, "HttpException", e);
      errorMessage = e.getMessage();
      if (errorMessage != null && errorMessage.contains("401")) {
        errorType = ERROR_AUTH;
      } else {
        errorType = ERROR_CONNECTION;
      }
    } catch (IOException e) {
      Log.e(MyMapsConstants.TAG, "Exception", e);
      errorMessage = e.getMessage();
      if (errorMessage != null && errorMessage.contains("503")) {
        errorType = ERROR_INTERNAL;
      } else {
        errorType = ERROR_CONNECTION;
      }
    } catch (ParseException e) {
      Log.e(MyMapsConstants.TAG, "Exception", e);
      errorType = ERROR_LOCAL;
      errorMessage = e.getMessage();
    } catch (ConflictDetectedException e) {
      Log.e(MyMapsConstants.TAG, "Exception", e);
      errorType = ERROR_CONFLICT;
      errorMessage = e.getMessage();
    } catch (Exception e) {
      Log.e(MyMapsConstants.TAG, "Exception", e);
      errorType = ERROR_UNKNOWN;
      errorMessage = e.getMessage();
      e.printStackTrace();
    }
    Log.d(MyMapsConstants.TAG, "GData error encountered: " + errorMessage);
    if (errorType == ERROR_AUTH && auth != null) {
      AuthCallback whenFinished = null;
      if (retryOnAuthFailure) {
        retriesPending++;
        whenFinished = new AuthCallback() {
          @Override
          public void onAuthResult(boolean success) {
            retriesPending--;
            retryOnAuthFailure = false;
            runQuery(query);
            if (cleanupCalled && retriesPending == 0) {
              cleanUp();
            }
          }
        };
      }
      auth.invalidateAndRefresh(whenFinished);
    }
    return false;
  }

  public int getErrorType() {
    return errorType;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  // cleanUp must be called when done using this wrapper to close the client.
  // Note that the cleanup will be delayed if auth failure retries were
  // requested and there is a pending retry.
  public void cleanUp() {
    if (retriesPending == 0 && !cleanupCalled) {
      androidGdataClient.close();
    }
    cleanupCalled = true;
  }

  public void setRetryOnAuthFailure(boolean retry) {
    retryOnAuthFailure = retry;
  }
}
