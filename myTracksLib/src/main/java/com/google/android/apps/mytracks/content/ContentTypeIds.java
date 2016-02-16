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

package com.google.android.apps.mytracks.content;

/**
 * Utilities for serializing primitive types.
 * 
 * @author Rodrigo Damazio
 */
public class ContentTypeIds {

  private ContentTypeIds() {}

  public static final byte BOOLEAN_TYPE_ID = 0;
  public static final byte LONG_TYPE_ID = 1;
  public static final byte INT_TYPE_ID = 2;
  public static final byte FLOAT_TYPE_ID = 3;
  public static final byte DOUBLE_TYPE_ID = 4;
  public static final byte STRING_TYPE_ID = 5;
  public static final byte BLOB_TYPE_ID = 6;
}
