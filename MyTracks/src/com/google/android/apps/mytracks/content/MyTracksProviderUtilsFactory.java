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

import android.content.Context;

/**
 * A factory to return a new {@link MyTracksProviderUtils} that can access the
 * Database content provider, {@link DatabaseProvider}.
 *
 * @author jshih@google.com (Jimmy Shih)
 */
public class MyTracksProviderUtilsFactory {

  /**
   * Creates a new instance of {@link MyTracksProviderUtils} that uses the given
   * context to access the Database content provider, {@link DatabaseProvider}.
   */
  public static MyTracksProviderUtils get(Context context) {
    return new MyTracksProviderUtilsImpl(
        context.getContentResolver(), MyTracksProviderUtils.DATABASE_AUTHORITY);
  }
}
