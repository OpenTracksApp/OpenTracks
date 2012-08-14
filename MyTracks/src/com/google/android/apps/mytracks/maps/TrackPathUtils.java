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

package com.google.android.apps.mytracks.maps;

import android.content.Context;
import android.graphics.Paint;

/**
 * Various utility functions for track path painting.
 * 
 * @author Vangelis S.
 */
public class TrackPathUtils {

  private TrackPathUtils() {}

  /**
   * Gets a paint.
   * 
   * @param context the context
   * @param colorId the color id
   */
  public static Paint getPaint(Context context, int colorId) {
    Paint paint = new Paint();
    paint.setColor(context.getResources().getColor(colorId));
    paint.setStrokeWidth(3);
    paint.setStyle(Paint.Style.STROKE);
    paint.setAntiAlias(true);
    return paint;
  }
}