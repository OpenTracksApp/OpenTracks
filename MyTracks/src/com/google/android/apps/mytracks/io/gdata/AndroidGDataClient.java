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
package com.google.android.apps.mytracks.io.gdata;

import com.google.wireless.gdata.client.GDataClient;
import com.google.wireless.gdata.client.HttpException;
import com.google.wireless.gdata.client.QueryParams;
import com.google.wireless.gdata.data.StringUtils;
import com.google.wireless.gdata.parser.ParseException;
import com.google.wireless.gdata.serializer.GDataSerializer;

import android.text.TextUtils;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * Implementation of a GDataClient using GoogleHttpClient to make HTTP requests.
 * Always issues GETs and POSTs, using the X-HTTP-Method-Override header when a
 * PUT or DELETE is desired, to avoid issues with firewalls, etc., that do not
 * allow methods other than GET or POST.
 */
public class AndroidGDataClient implements GDataClient {

  private static final String TAG = "GDataClient";
  private static final boolean DEBUG = false;
 
  private static final String X_HTTP_METHOD_OVERRIDE = "X-HTTP-Method-Override";

  private static final int MAX_REDIRECTS = 10;

  private final HttpClient httpClient;

  /**
   * Interface for creating HTTP requests. Used by
   * {@link AndroidGDataClient#createAndExecuteMethod}, since HttpUriRequest
   * does not allow for changing the URI after creation, e.g., when you want to
   * follow a redirect.
   */
  private interface HttpRequestCreator {
    HttpUriRequest createRequest(URI uri);
  }

  private static class GetRequestCreator implements HttpRequestCreator {
    public HttpUriRequest createRequest(URI uri) {
      return new HttpGet(uri);
    }
  }

  private static class PostRequestCreator implements HttpRequestCreator {
    private final String mMethodOverride;
    private final HttpEntity mEntity;

    public PostRequestCreator(String methodOverride, HttpEntity entity) {
      mMethodOverride = methodOverride;
      mEntity = entity;
    }

    public HttpUriRequest createRequest(URI uri) {
      HttpPost post = new HttpPost(uri);
      if (mMethodOverride != null) {
        post.addHeader(X_HTTP_METHOD_OVERRIDE, mMethodOverride);
      }
      post.setEntity(mEntity);
      return post;
    }
  }

  // MAJOR TODO: make this work across redirects (if we can reset the
  // InputStream).
  // OR, read the bits into a local buffer (yuck, the media could be large).
  private static class MediaPutRequestCreator implements HttpRequestCreator {
    private final InputStream mMediaInputStream;
    private final String mContentType;

    public MediaPutRequestCreator(InputStream mediaInputStream,
        String contentType) {
      mMediaInputStream = mediaInputStream;
      mContentType = contentType;
    }

    public HttpUriRequest createRequest(URI uri) {
      HttpPost post = new HttpPost(uri);
      post.addHeader(X_HTTP_METHOD_OVERRIDE, "PUT");
      InputStreamEntity entity =
          new InputStreamEntity(mMediaInputStream, -1 /* read until EOF */);
      entity.setContentType(mContentType);
      post.setEntity(entity);
      return post;
    }
  }

  /**
   * Creates a new AndroidGDataClient.
   */
  public AndroidGDataClient() {
    httpClient = new DefaultHttpClient();
  }

  public void close() {
  }

  /*
   * (non-Javadoc)
   *
   * @see GDataClient#encodeUri(java.lang.String)
   */
  public String encodeUri(String uri) {
    String encodedUri;
    try {
      encodedUri = URLEncoder.encode(uri, "UTF-8");
    } catch (UnsupportedEncodingException uee) {
      // should not happen.
      Log.e("JakartaGDataClient", "UTF-8 not supported -- should not happen.  "
          + "Using default encoding.", uee);
      encodedUri = URLEncoder.encode(uri);
    }
    return encodedUri;
  }

  /*
   * (non-Javadoc)
   *
   * @see com.google.wireless.gdata.client.GDataClient#createQueryParams()
   */
  public QueryParams createQueryParams() {
    return new QueryParamsImpl();
  }

  // follows redirects
  private InputStream createAndExecuteMethod(HttpRequestCreator creator,
      String uriString, String authToken) throws HttpException, IOException {

    HttpResponse response = null;
    int status = 500;
    int redirectsLeft = MAX_REDIRECTS;

    URI uri;
    try {
      uri = new URI(uriString);
    } catch (URISyntaxException use) {
      Log.w(TAG, "Unable to parse " + uriString + " as URI.", use);
      throw new IOException("Unable to parse " + uriString + " as URI: "
          + use.getMessage());
    }

    // we follow redirects ourselves, since we want to follow redirects even on
    // POSTs, which
    // the HTTP library does not do. following redirects ourselves also allows
    // us to log
    // the redirects using our own logging.
    while (redirectsLeft > 0) {

      HttpUriRequest request = creator.createRequest(uri);
      request.addHeader("User-Agent", "Android-GData");
      request.addHeader("Accept-Encoding", "gzip");

      // only add the auth token if not null (to allow for GData feeds that do
      // not require
      // authentication.)
      if (!TextUtils.isEmpty(authToken)) {
        request.addHeader("Authorization", "GoogleLogin auth=" + authToken);
      }
      if (DEBUG) {
        for (Header h : request.getAllHeaders()) {
          Log.v(TAG, h.getName() + ": " + h.getValue());
        }
        Log.d(TAG, "Executing " + request.getRequestLine().toString());
      }

      response = null;

      try {
        response = httpClient.execute(request);
      } catch (IOException ioe) {
        Log.w(TAG, "Unable to execute HTTP request." + ioe);
        throw ioe;
      }

      StatusLine statusLine = response.getStatusLine();
      if (statusLine == null) {
        Log.w(TAG, "StatusLine is null.");
        throw new NullPointerException(
            "StatusLine is null -- should not happen.");
      }

      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, response.getStatusLine().toString());
        for (Header h : response.getAllHeaders()) {
          Log.d(TAG, h.getName() + ": " + h.getValue());
        }
      }
      status = statusLine.getStatusCode();

      HttpEntity entity = response.getEntity();

      if ((status >= 200) && (status < 300) && entity != null) {
        return getUngzippedContent(entity);
      }

      // TODO: handle 301, 307?
      // TODO: let the http client handle the redirects, if we can be sure we'll
      // never get a
      // redirect on POST.
      if (status == 302) {
        // consume the content, so the connection can be closed.
        entity.consumeContent();
        Header location = response.getFirstHeader("Location");
        if (location == null) {
          if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Redirect requested but no Location " + "specified.");
          }
          break;
        }
        if (Log.isLoggable(TAG, Log.DEBUG)) {
          Log.d(TAG, "Following redirect to " + location.getValue());
        }
        try {
          uri = new URI(location.getValue());
        } catch (URISyntaxException use) {
          if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Unable to parse " + location.getValue() + " as URI.",
                use);
            throw new IOException("Unable to parse " + location.getValue()
                + " as URI.");
          }
          break;
        }
        --redirectsLeft;
      } else {
        break;
      }
    }

    if (Log.isLoggable(TAG, Log.VERBOSE)) {
      Log.v(TAG, "Received " + status + " status code.");
    }
    String errorMessage = null;
    HttpEntity entity = response.getEntity();
    try {
      if (entity != null) {
        InputStream in = entity.getContent();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int bytesRead = -1;
        while ((bytesRead = in.read(buf)) != -1) {
          baos.write(buf, 0, bytesRead);
        }
        // TODO: use appropriate encoding, picked up from Content-Type.
        errorMessage = new String(baos.toByteArray());
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
          Log.v(TAG, errorMessage);
        }
      }
    } finally {
      if (entity != null) {
        entity.consumeContent();
      }
    }
    String exceptionMessage = "Received " + status + " status code";
    if (errorMessage != null) {
      exceptionMessage += (": " + errorMessage);
    }
    throw new HttpException(exceptionMessage, status, null /* InputStream */);
  }

  /**
   * Gets the input stream from a response entity.  If the entity is gzipped
   * then this will get a stream over the uncompressed data.
   *
   * @param entity the entity whose content should be read
   * @return the input stream to read from
   * @throws IOException
   */
  private static InputStream getUngzippedContent(HttpEntity entity)
      throws IOException {
    InputStream responseStream = entity.getContent();
    if (responseStream == null) {
      return responseStream;
    }
    Header header = entity.getContentEncoding();
    if (header == null) {
      return responseStream;
    }
    String contentEncoding = header.getValue();
    if (contentEncoding == null) {
      return responseStream;
    }
    if (contentEncoding.contains("gzip")){
      responseStream = new GZIPInputStream(responseStream);
    }
    return responseStream;
  }

  /*
   * (non-Javadoc)
   *
   * @see GDataClient#getFeedAsStream(java.lang.String, java.lang.String)
   */
  public InputStream getFeedAsStream(String feedUrl, String authToken)
      throws HttpException, IOException {

    InputStream in =
        createAndExecuteMethod(new GetRequestCreator(), feedUrl, authToken);
    if (in != null) {
      return in;
    }
    throw new IOException("Unable to access feed.");
  }

  public InputStream getMediaEntryAsStream(String mediaEntryUrl,
      String authToken) throws HttpException, IOException {

    InputStream in =
        createAndExecuteMethod(new GetRequestCreator(), mediaEntryUrl,
            authToken);

    if (in != null) {
      return in;
    }
    throw new IOException("Unable to access media entry.");
  }

  /*
   * (non-Javadoc)
   *
   * @see GDataClient#createEntry
   */
  public InputStream createEntry(String feedUrl, String authToken,
      GDataSerializer entry) throws HttpException, IOException {

    HttpEntity entity =
        createEntityForEntry(entry, GDataSerializer.FORMAT_CREATE);
    InputStream in =
        createAndExecuteMethod(new PostRequestCreator(null /* override */,
            entity), feedUrl, authToken);
    if (in != null) {
      return in;
    }
    throw new IOException("Unable to create entry.");
  }

  /*
   * (non-Javadoc)
   *
   * @see GDataClient#updateEntry
   */
  public InputStream updateEntry(String editUri, String authToken,
      GDataSerializer entry) throws HttpException, IOException {
    HttpEntity entity =
        createEntityForEntry(entry, GDataSerializer.FORMAT_UPDATE);
    InputStream in =
        createAndExecuteMethod(new PostRequestCreator("PUT", entity), editUri,
            authToken);
    if (in != null) {
      return in;
    }
    throw new IOException("Unable to update entry.");
  }

  /*
   * (non-Javadoc)
   *
   * @see GDataClient#deleteEntry
   */
  public void deleteEntry(String editUri, String authToken)
      throws HttpException, IOException {
    if (StringUtils.isEmpty(editUri)) {
      throw new IllegalArgumentException(
          "you must specify an non-empty edit url");
    }
    InputStream in =
        createAndExecuteMethod(
            new PostRequestCreator("DELETE", null /* entity */), editUri,
            authToken);
    if (in == null) {
      throw new IOException("Unable to delete entry.");
    }
    try {
      in.close();
    } catch (IOException ioe) {
      // ignore
    }
  }

  public InputStream updateMediaEntry(String editUri, String authToken,
      InputStream mediaEntryInputStream, String contentType)
      throws HttpException, IOException {
    InputStream in =
        createAndExecuteMethod(new MediaPutRequestCreator(
            mediaEntryInputStream, contentType), editUri, authToken);
    if (in != null) {
      return in;
    }
    throw new IOException("Unable to write media entry.");
  }

  private HttpEntity createEntityForEntry(GDataSerializer entry, int format)
      throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      entry.serialize(baos, format);
    } catch (IOException ioe) {
      Log.e(TAG, "Unable to serialize entry.", ioe);
      throw ioe;
    } catch (ParseException pe) {
      Log.e(TAG, "Unable to serialize entry.", pe);
      throw new IOException("Unable to serialize entry: " + pe.getMessage());
    }

    byte[] entryBytes = baos.toByteArray();

    if (entryBytes != null && Log.isLoggable(TAG, Log.DEBUG)) {
      try {
        Log.d(TAG, "Serialized entry: " + new String(entryBytes, "UTF-8"));
      } catch (UnsupportedEncodingException uee) {
        // should not happen
        throw new IllegalStateException("UTF-8 should be supported!", uee);
      }
    }

    AbstractHttpEntity entity = new ByteArrayEntity(entryBytes);
    entity.setContentType(entry.getContentType());
    return entity;
  }
}
