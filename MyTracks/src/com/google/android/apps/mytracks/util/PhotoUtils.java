/*
 * Copyright 2013 Google Inc.
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

import com.google.android.apps.mytracks.services.tasks.BitmapLoader;

import android.net.Uri;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

/**
 * Utilities for photos.
 * 
 * @author Jimmy Shih
 */
public class PhotoUtils {

  private PhotoUtils() {}

  /**
   * Sets an image view.
   * 
   * @param imageView the image view
   * @param uri the image uri
   * @param displayWidth the display width
   * @param displayHeight the display height
   */
  public static void setImageVew(
      ImageView imageView, Uri uri, int displayWidth, int displayHeight) {
    if (cancelBitmapLoader(imageView, uri)) {
      BitmapLoader bitmapLoader = new BitmapLoader(imageView, uri, displayWidth, displayHeight);
      WeakReference<BitmapLoader> bitmapLoaderReference = new WeakReference<BitmapLoader>(
          bitmapLoader);
      imageView.setTag(bitmapLoaderReference);
      bitmapLoader.execute();
    }
  }

  /**
   * Gets the image view bitmap loader.
   * 
   * @param imageView the image view
   */
  public static BitmapLoader getBitmapLoader(ImageView imageView) {
    if (imageView != null) {
      Object object = imageView.getTag();
      if (object instanceof WeakReference<?>) {
        @SuppressWarnings("unchecked")
        WeakReference<BitmapLoader> bitmapLoaderReference = (WeakReference<BitmapLoader>) object;
        return bitmapLoaderReference.get();
      }
    }
    return null;
  }

  /**
   * Cancels the image view bitmap loader.
   * 
   * @param imageView the image view
   * @param uri the uri
   * @return false if the bitmap loader shouldn't be canceled. True if there is
   *         no bitmap loader or the bitmap loader is cancelled.
   */
  private static boolean cancelBitmapLoader(ImageView imageView, Uri uri) {
    BitmapLoader bitmapLoaderAsyncTask = getBitmapLoader(imageView);

    if (bitmapLoaderAsyncTask != null) {
      if (bitmapLoaderAsyncTask.getUri().equals(uri)) {
        // same bitmap loader is already in progress, don't cancel
        return false;
      } else {
        // cancel previous bitmap loader
        bitmapLoaderAsyncTask.cancel(true);
      }
    }
    // imageview has no bitmap loader, or an existing bitmap loader is cancelled
    return true;
  }
}
