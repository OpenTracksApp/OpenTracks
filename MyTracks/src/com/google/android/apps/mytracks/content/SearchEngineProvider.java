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
package com.google.android.apps.mytracks.content;

import android.content.Context;
import android.content.SearchRecentSuggestionsProvider;
import android.provider.SearchRecentSuggestions;

/**
 * Content provider for search suggestions.
 *
 * @author Rodrigo Damazio
 */
public class SearchEngineProvider extends SearchRecentSuggestionsProvider {

  private static final String AUTHORITY = "com.google.android.maps.mytracks.search";
  private static final int MODE = DATABASE_MODE_QUERIES;

  public SearchEngineProvider() {
    setupSuggestions(AUTHORITY, MODE);
  }

  // TODO: Also add suggestions from the database.

  /**
   * Creates and returns a helper for adding recent queries or clearing the recent query history.
   */
  public static SearchRecentSuggestions newHelper(Context context) {
    return new SearchRecentSuggestions(context, AUTHORITY, MODE);
  }
}
