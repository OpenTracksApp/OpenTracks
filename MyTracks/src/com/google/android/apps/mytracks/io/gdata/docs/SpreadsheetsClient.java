/*
 * Copyright 2007 Google Inc.
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
package com.google.android.apps.mytracks.io.gdata.docs;

import com.google.wireless.gdata.client.GDataClient;
import com.google.wireless.gdata.client.GDataParserFactory;
import com.google.wireless.gdata.client.GDataServiceClient;
import com.google.wireless.gdata.client.HttpException;
import com.google.wireless.gdata.data.Entry;
import com.google.wireless.gdata.data.StringUtils;
import com.google.wireless.gdata.parser.GDataParser;
import com.google.wireless.gdata.parser.ParseException;
import com.google.wireless.gdata.serializer.GDataSerializer;
import com.google.wireless.gdata2.client.AuthenticationException;

import java.io.IOException;
import java.io.InputStream;

/**
 * GDataServiceClient for accessing Google Spreadsheets. This client can access
 * and parse all of the Spreadsheets feed types: Spreadsheets feed, Worksheets
 * feed, List feed, and Cells feed. Read operations are supported on all feed
 * types, but only the List and Cells feeds support write operations. (This is a
 * limitation of the protocol, not this API. Such write access may be added to
 * the protocol in the future, requiring changes to this implementation.)
 * 
 * Only 'private' visibility and 'full' projections are currently supported.
 */
public class SpreadsheetsClient extends GDataServiceClient {
  /** The name of the service, dictated to be 'wise' by the protocol. */
  public static final String SERVICE = "wise";

  /** Standard base feed url for spreadsheets. */
  public static final String SPREADSHEETS_BASE_FEED_URL =
      "http://spreadsheets.google.com/feeds/spreadsheets/private/full";

  /**
   * Represents an entry in a GData Spreadsheets meta-feed.
   */
  public static class SpreadsheetEntry extends Entry { }

  /**
   * Represents an entry in a GData Worksheets meta-feed.
   */
  public static class WorksheetEntry extends Entry { }

  /**
   * Creates a new SpreadsheetsClient. Uses the standard base URL for
   * spreadsheets feeds.
   * 
   * @param client The GDataClient that should be used to authenticate requests,
   *        retrieve feeds, etc
   */
  public SpreadsheetsClient(GDataClient client,
      GDataParserFactory spreadsheetFactory) {
    super(client, spreadsheetFactory);
  }

  @Override
  public String getServiceName() {
    return SERVICE;
  }

  /**
   * Returns a parser for the specified feed type.
   * 
   * @param feedEntryClass the Class of entry type that will be parsed, which
   *        lets this method figure out which parser to create
   * @param feedUri the URI of the feed to be fetched and parsed
   * @param authToken the current authToken to use for the request
   * @return a parser for the indicated feed
   * @throws AuthenticationException if the authToken is not valid
   * @throws ParseException if the response from the server could not be parsed
   */
  private GDataParser getParserForTypedFeed(
      Class<? extends Entry> feedEntryClass, String feedUri, String authToken)
      throws AuthenticationException, ParseException, IOException {
    GDataClient gDataClient = getGDataClient();
    GDataParserFactory gDataParserFactory = getGDataParserFactory();

    try {
      InputStream is = gDataClient.getFeedAsStream(feedUri, authToken);
      return gDataParserFactory.createParser(feedEntryClass, is);
    } catch (HttpException e) {
      convertHttpExceptionForReads("Could not fetch parser feed.", e);
      return null; // never reached
    }
  }

  /**
   * Converts an HTTP exception that happened while reading into the equivalent
   * local exception.
   */
  public void convertHttpExceptionForReads(String message, HttpException cause)
      throws AuthenticationException, IOException {
    switch (cause.getStatusCode()) {
      case HttpException.SC_FORBIDDEN:
      case HttpException.SC_UNAUTHORIZED:
        throw new AuthenticationException(message, cause);
      case HttpException.SC_GONE:
      default:
        throw new IOException(message + ": " + cause.getMessage());
    }
  }

  @Override
  public Entry createEntry(String feedUri, String authToken, Entry entry)
      throws ParseException, IOException {

    GDataParserFactory factory = getGDataParserFactory();
    GDataSerializer serializer = factory.createSerializer(entry);

    InputStream is;
    try {
      is = getGDataClient().createEntry(feedUri, authToken, serializer);
    } catch (HttpException e) {
      convertHttpExceptionForWrites(entry.getClass(),
          "Could not update entry.", e);
      return null; // never reached.
    }

    GDataParser parser = factory.createParser(entry.getClass(), is);
    try {
      return parser.parseStandaloneEntry();
    } finally {
      parser.close();
    }
  }

  /**
   * Fetches a GDataParser for the indicated feed. The parser can be used to
   * access the contents of URI. WARNING: because we cannot reliably infer the
   * feed type from the URI alone, this method assumes the default feed type!
   * This is probably NOT what you want. Please use the getParserFor[Type]Feed
   * methods.
   * 
   * @param feedEntryClass
   * @param feedUri the URI of the feed to be fetched and parsed
   * @param authToken the current authToken to use for the request
   * @return a parser for the indicated feed
   * @throws ParseException if the response from the server could not be parsed
   */
  @SuppressWarnings("rawtypes")
  @Override
  public GDataParser getParserForFeed(
      Class feedEntryClass, String feedUri, String authToken)
      throws ParseException, IOException {
    try {
      return getParserForTypedFeed(SpreadsheetEntry.class, feedUri, authToken);
    } catch (AuthenticationException e) {
      throw new IOException("Authentication Failure: " + e.getMessage());
    }
  }

  /**
   * Returns a parser for a Worksheets meta-feed.
   * 
   * @param feedUri the URI of the feed to be fetched and parsed
   * @param authToken the current authToken to use for the request
   * @return a parser for the indicated feed
   * @throws AuthenticationException if the authToken is not valid
   * @throws ParseException if the response from the server could not be parsed
   */
  public GDataParser getParserForWorksheetsFeed(
      String feedUri, String authToken)
      throws AuthenticationException, ParseException, IOException {
    return getParserForTypedFeed(WorksheetEntry.class, feedUri, authToken);
  }

  /**
   * Updates an entry. The URI to be updated is taken from <code>entry</code>.
   * Note that only entries in List and Cells feeds can be updated, so
   * <code>entry</code> must be of the corresponding type; other types will
   * result in an exception.
   * 
   * @param entry the entry to be updated; must include its URI
   * @param authToken the current authToken to be used for the operation
   * @return An Entry containing the re-parsed version of the entry returned by
   *         the server in response to the update
   * @throws ParseException if the server returned an error, if the server's
   *         response was unparseable (unlikely), or if <code>entry</code> is of
   *         a read-only type
   * @throws IOException on network error
   */
  @Override
  public Entry updateEntry(Entry entry, String authToken)
      throws ParseException, IOException {
    GDataParserFactory factory = getGDataParserFactory();
    GDataSerializer serializer = factory.createSerializer(entry);

    String editUri = entry.getEditUri();
    if (StringUtils.isEmpty(editUri)) {
      throw new ParseException("No edit URI -- cannot update.");
    }

    InputStream is;
    try {
      is = getGDataClient().updateEntry(editUri, authToken, serializer);
    } catch (HttpException e) {
      convertHttpExceptionForWrites(entry.getClass(),
          "Could not update entry.", e);
      return null; // never reached
    }

    GDataParser parser = factory.createParser(entry.getClass(), is);
    try {
      return parser.parseStandaloneEntry();
    } finally {
      parser.close();
    }
  }

  /**
   * Converts an HTTP exception which happened while writing to the equivalent
   * local exception.
   */
  @SuppressWarnings("rawtypes")
  private void convertHttpExceptionForWrites(
      Class entryClass, String message, HttpException cause)
      throws ParseException, IOException {
    switch (cause.getStatusCode()) {
      case HttpException.SC_CONFLICT:
        if (entryClass != null) {
          InputStream is = cause.getResponseStream();
          if (is != null) {
            parseEntry(entryClass, cause.getResponseStream());
          }
        }
        throw new IOException(message);
      case HttpException.SC_BAD_REQUEST:
        throw new ParseException(message + ": " + cause);
      case HttpException.SC_FORBIDDEN:
      case HttpException.SC_UNAUTHORIZED:
        throw new IOException(message);
      default:
        throw new IOException(message + ": " + cause.getMessage());
    }
  }

  /**
   * Parses one entry from the input stream.
   */
  @SuppressWarnings("rawtypes")
  private Entry parseEntry(Class entryClass, InputStream is)
      throws ParseException, IOException {
    GDataParser parser = null;
    try {
      parser = getGDataParserFactory().createParser(entryClass, is);
      return parser.parseStandaloneEntry();
    } finally {
      if (parser != null) {
        parser.close();
      }
    }
  }
}
