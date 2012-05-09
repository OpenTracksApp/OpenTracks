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

package com.google.android.apps.mytracks.util;

import android.content.Context;
import android.content.Intent;

/**
 * Utilities for creating intents.
 * 
 * @author Jimmy Shih
 */
public class IntentUtils {

  private IntentUtils() {}

  /**
   * Creates an intent with {@link Intent#FLAG_ACTIVITY_CLEAR_TOP} and
   * {@link Intent#FLAG_ACTIVITY_NEW_TASK}.
   * 
   * @param context the context
   * @param cls the class
   */
  public static final Intent newIntent(Context context, Class<?> cls) {
    return new Intent(context, cls).addFlags(
        Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
  }
}
