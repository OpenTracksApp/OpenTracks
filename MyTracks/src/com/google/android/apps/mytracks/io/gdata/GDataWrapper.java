/*
 * Copyright 2008 Google Inc.
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
package com.google.android.apps.mytracks.io.gdata;

import com.google.android.apps.mytracks.MyTracksConstants;
import com.google.android.apps.mytracks.io.AuthManager;
import com.google.wireless.gdata.client.GDataClient;
import com.google.wireless.gdata.client.GDataServiceClient;
import com.google.wireless.gdata.client.HttpException;
import com.google.wireless.gdata.parser.ParseException;
import com.google.wireless.gdata2.ConflictDetectedException;
import com.google.wireless.gdata2.client.AuthenticationException;

import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;


/**
 * GDataWrapper provides a wrapper around GData operations that maintains the
 * GData client, and provides a method to run GData queries with proper error
 * handling. After a query is run, the wrapper can be queried about the error
 * that occurred.
 *
 * @author Sandor Dornbush
 */
public class GDataWrapper {

  /**
   * A QueryFunction is passed in when executing a query. The query function of
   * the class is called with the GData client as a parameter. The function
   * should execute whatever operations it desires on the client without concern
   * for whether the client will throw an error.
   */
  public interface QueryFunction {
    public abstract void query(GDataServiceClient client)
        throws AuthenticationException, IOException, ParseException,
        ConflictDetectedException, HttpException;
  }

  /**
   * A AuthenticatedFunction is passed in when executing the google
   * authenticated service. The authenticated function of the class is called
   * with the current authentication token for the service. The function should
   * execute whatever operations with the google service without concern for
   * whether the client will throw an error.
   */
  public interface AuthenticatedFunction {
    public abstract void run(String authenticationToken)
        throws AuthenticationException, IOException;
  }

  // The types of error that may be encountered
  // No error occurred.
  public static final int ERROR_NO_ERROR = 0;
  // There was an authentication error, the auth token may be invalid.
  public static final int ERROR_AUTH = 1;
  // There was an internal error on the server side.
  public static final int ERROR_INTERNAL = 2;
  // There was an error connecting to the server.
  public static final int ERROR_CONNECTION = 3;
  // The item queried did not exit.
  public static final int ERROR_NOT_FOUND = 4;
  // There was an error parsing or serializing locally.
  public static final int ERROR_LOCAL = 5;
  // There was a conflict, update the entry and try again.
  public static final int ERROR_CONFLICT = 6;
  // A query was run after cleaning up the wrapper, so the client was invalid.
  public static final int ERROR_CLEANED_UP = 7;
  // An unknown error occurred.
  public static final int ERROR_UNKNOWN = 100;

  private String errorMessage;
  private int errorType;
  private GDataClient androidGDataClient;
  private GDataServiceClient gdataServiceClient;
  private AuthManager auth;
  private boolean retryOnAuthFailure;
  private int retriesPending;
  private boolean cleanupCalled;

  public GDataWrapper() {
    errorType = ERROR_NO_ERROR;
    errorMessage = null;
    auth = null;
    retryOnAuthFailure = false;
    retriesPending = 0;
    cleanupCalled = false;
  }

  public void setClient(GDataClient androidGDataClient,
      GDataServiceClient gdataServiceClient) {
    this.androidGDataClient = androidGDataClient;
    this.gdataServiceClient = gdataServiceClient;
  }

  public boolean runAuthenticatedFunction(
      final AuthenticatedFunction function) {
    return runOne(function, null);
  }

  public boolean runQuery(final QueryFunction query) {
    return runOne(null, query);
  }

  /**
   * Runs an arbitrary piece of code.
   */
  private boolean runOne(final AuthenticatedFunction function,
      final QueryFunction query) {
    try {
      if (function != null) {
        function.run(this.auth.getAuthToken());
      } else if (query != null) {
        query.query(gdataServiceClient);
      } else {
        return false;
      }
      errorType = ERROR_NO_ERROR;
      errorMessage = null;
      return true;
    } catch (AuthenticationException e) {
      Log.e(MyTracksConstants.TAG, "Exception", e);
      errorType = ERROR_AUTH;
      errorMessage = e.getMessage();
    } catch (HttpException e) {
      Log.e(MyTracksConstants.TAG, "HttpException", e);
      errorMessage = e.getMessage();
      if (errorMessage.contains("401")) {
        errorType = ERROR_AUTH;
      } else {
        errorType = ERROR_CONNECTION;
      }
    } catch (FileNotFoundException e) {
      Log.e(MyTracksConstants.TAG, "Exception", e);
      errorType = ERROR_AUTH;
      errorMessage = e.getMessage();
    } catch (IOException e) {
      Log.e(MyTracksConstants.TAG, "Exception", e);
      errorMessage = e.getMessage();
      if (errorMessage.contains("503")) {
        errorType = ERROR_INTERNAL;
      } else {
        errorType = ERROR_CONNECTION;
      }
    } catch (ParseException e) {
      Log.e(MyTracksConstants.TAG, "Exception", e);
      errorType = ERROR_LOCAL;
      errorMessage = e.getMessage();
    } catch (ConflictDetectedException e) {
      Log.e(MyTracksConstants.TAG, "Exception", e);
      errorType = ERROR_CONFLICT;
      errorMessage = e.getMessage();
    }

    Log.d(MyTracksConstants.TAG, "GData error encountered: " + errorMessage);
    if (errorType == ERROR_AUTH && auth != null) {
      Runnable whenFinished = null;
      if (retryOnAuthFailure) {
        retriesPending++;
        // This is a little odd.  This method cannot return in the the thread
        // that called the original runQuery(). We should look at reworking this
        // API.
        whenFinished = new Runnable() {
          public void run() {
            retriesPending--;
            try {
              runOne(function, query);
            } catch (IllegalStateException ise) {
              // This can happen if the connection pool was shut down.
              Log.e(MyTracksConstants.TAG, "Failed to rerun query.", ise);
            }
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
    cleanupCalled = true;
    if (retriesPending == 0) {
      androidGDataClient.close();
      androidGDataClient = null;
    }
  }

  public void setAuthManager(AuthManager auth) {
    this.auth = auth;
  }

  public void setRetryOnAuthFailure(boolean retry) {
    retryOnAuthFailure = retry;
  }
}
