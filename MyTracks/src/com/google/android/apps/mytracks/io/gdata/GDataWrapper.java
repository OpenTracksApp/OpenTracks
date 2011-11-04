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

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.io.AuthManager;
import com.google.android.apps.mytracks.io.AuthManager.AuthCallback;

import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * GDataWrapper provides a wrapper around GData operations that maintains the
 * GData client, and provides a method to run GData queries with proper error
 * handling. After a query is run, the wrapper can be queried about the error
 * that occurred.
 *
 * @param <C> the GData service client
 * @author Sandor Dornbush
 */
public class GDataWrapper<C> {

  public static class AuthenticationException extends Exception {
    private Exception exception;
    private static final long serialVersionUID = 1L;
    public AuthenticationException(Exception caught) {
      this.exception = caught;
    }
    public Exception getException() {
      return exception;
    }
  };

  public static class ParseException extends Exception {
    private Exception exception;
    private static final long serialVersionUID = 1L;
    public ParseException(Exception caught) {
      this.exception = caught;
    }
    public Exception getException() {
      return exception;
    }
  };

  public static class ConflictDetectedException extends Exception {
    private Exception exception;
    private static final long serialVersionUID = 1L;
    public ConflictDetectedException(Exception caught) {
      this.exception = caught;
    }
    public Exception getException() {
      return exception;
    }
  };

  public static class HttpException extends Exception {
    private static final long serialVersionUID = 1L;
    private int statusCode;
    private String statusMessage;
    public HttpException(int statusCode, String statusMessage) {
      super();
      this.statusCode = statusCode;
      this.statusMessage = statusMessage;
    }
    public int getStatusCode() {
      return statusCode;
    }
    public String getStatusMessage() {
      return statusMessage;
    }
  };

  /**
   * A QueryFunction is passed in when executing a query. The query function of
   * the class is called with the GData client as a parameter. The function
   * should execute whatever operations it desires on the client without concern
   * for whether the client will throw an error.
   */
  public interface QueryFunction<C> {
    public abstract void query(C client)
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

  private static final int AUTH_TOKEN_INVALIDATE_REFRESH_NUM_RETRIES = 1;
  private static final int AUTH_TOKEN_INVALIDATE_REFRESH_TIMEOUT = 5000;

  private String errorMessage;
  private int errorType;
  private C gdataServiceClient;
  private AuthManager auth;
  private boolean retryOnAuthFailure;

  public GDataWrapper() {
    errorType = ERROR_NO_ERROR;
    errorMessage = null;
    auth = null;
    retryOnAuthFailure = false;
  }

  public void setClient(C gdataServiceClient) {
    this.gdataServiceClient = gdataServiceClient;
  }

  public boolean runAuthenticatedFunction(
      final AuthenticatedFunction function) {
    return runCommon(function, null);
  }

  public boolean runQuery(final QueryFunction<C> query) {
    return runCommon(null, query);
  }

  /**
   * Runs an arbitrary piece of code.
   */
  private boolean runCommon(final AuthenticatedFunction function,
      final QueryFunction<C> query) {
    for (int i = 0; i <= AUTH_TOKEN_INVALIDATE_REFRESH_NUM_RETRIES; i++) {
      runOne(function, query);
      if (errorType == ERROR_NO_ERROR) {
        return true;
      }

      Log.d(Constants.TAG, "GData error encountered: " + errorMessage);
      if (errorType == ERROR_AUTH && auth != null) {
        if (!retryOnAuthFailure || !invalidateAndRefreshAuthToken()) {
          return false;
        }
      }

      Log.d(Constants.TAG, "retrying function/query");
    }
    return false;
  }

  /**
   * Execute a given function or query.  If one is executed, errorType and
   * errorMessage will contain the result/status of the function/query.
   */
  private void runOne(final AuthenticatedFunction function,
      final QueryFunction<C> query) {
    try {
      if (function != null) {
        function.run(this.auth.getAuthToken());
      } else if (query != null) {
        query.query(gdataServiceClient);
      } else {
        throw new IllegalArgumentException(
            "invalid invocation of runOne; one of function/query " +
            "must be non-null");
      }

      errorType = ERROR_NO_ERROR;
      errorMessage = null;

    } catch (AuthenticationException e) {
      Log.e(Constants.TAG, "AuthenticationException", e);
      errorType = ERROR_AUTH;
      errorMessage = e.getMessage();
    } catch (HttpException e) {
      Log.e(Constants.TAG,
          "HttpException, code " + e.getStatusCode() + " message " + e.getMessage(), e);
      errorMessage = e.getMessage();
      if (e.getStatusCode() == 401) {
        errorType = ERROR_AUTH;
      } else {
        errorType = ERROR_CONNECTION;
      }
    } catch (FileNotFoundException e) {
      Log.e(Constants.TAG, "Exception", e);
      errorType = ERROR_AUTH;
      errorMessage = e.getMessage();
    } catch (IOException e) {
      Log.e(Constants.TAG, "Exception", e);
      errorMessage = e.getMessage();
      if (errorMessage != null && errorMessage.contains("503")) {
        errorType = ERROR_INTERNAL;
      } else {
        errorType = ERROR_CONNECTION;
      }
    } catch (ParseException e) {
      Log.e(Constants.TAG, "Exception", e);
      errorType = ERROR_LOCAL;
      errorMessage = e.getMessage();
    } catch (ConflictDetectedException e) {
      Log.e(Constants.TAG, "Exception", e);
      errorType = ERROR_CONFLICT;
      errorMessage = e.getMessage();
    }
  }

  /**
   * Invalidates and refreshes the auth token.  Blocks until the refresh has
   * completed or until we deem the refresh as having timed out.
   *
   * @return true If the invalidate/refresh succeeds, false if it fails or
   *   times out.
   */
  private boolean invalidateAndRefreshAuthToken() {
    Log.d(Constants.TAG, "Retrying due to auth failure");
    // This FutureTask doesn't do anything -- it exists simply to be
    // blocked upon using get().
    final FutureTask<?> whenFinishedFuture = new FutureTask<Object>(new Runnable() {
      public void run() {}
    }, null);

    final AtomicBoolean finalSuccess = new AtomicBoolean(false);
    auth.invalidateAndRefresh(new AuthCallback() {
      @Override
      public void onAuthResult(boolean success) {
        finalSuccess.set(success);
        whenFinishedFuture.run();
      }
    });

    try {
      Log.d(Constants.TAG, "waiting for invalidate");
      whenFinishedFuture.get(AUTH_TOKEN_INVALIDATE_REFRESH_TIMEOUT,
          TimeUnit.MILLISECONDS);
      boolean success = finalSuccess.get();
      Log.d(Constants.TAG, "invalidate finished, success = " + success);
      return success;
    } catch (InterruptedException e) {
      Log.e(Constants.TAG, "Failed to invalidate", e);
    } catch (ExecutionException e) {
      Log.e(Constants.TAG, "Failed to invalidate", e);
    } catch (TimeoutException e) {
      Log.e(Constants.TAG, "Invalidate didn't complete in time", e);
    } finally {
      whenFinishedFuture.cancel(false);
    }

    return false;
  }

  public int getErrorType() {
    return errorType;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setAuthManager(AuthManager auth) {
    this.auth = auth;
  }

  public AuthManager getAuthManager() {
    return auth;
  }

  public void setRetryOnAuthFailure(boolean retry) {
    retryOnAuthFailure = retry;
  }
}
