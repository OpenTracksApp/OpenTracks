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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import java.io.IOException;

/**
 * Utilities for photos.
 * 
 * @author Jimmy Shih
 */
public class PhotoUtils {

  private static final String TAG = PhotoUtils.class.getSimpleName();

  private PhotoUtils() {}

  /**
   * Sets an image view.
   * 
   * @param imageView the image view
   * @param uri the image uri
   * @param displayWidth the display width
   * @param displayHeight the display height
   */
  public static Bitmap setImageVew(
      ImageView imageView, Uri uri, int displayWidth, int displayHeight) {
    
    // Get the image dimensions
    BitmapFactory.Options options = new BitmapFactory.Options();

    options.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(uri.getPath(), options);

    // Set targetWidth and targetHeight
    int targetWidth = displayWidth;
    int targetHeight = displayHeight;
    if (targetHeight == 0) {
      targetHeight = (int) (targetWidth * ((float) options.outHeight / (float) options.outWidth));
    }
    
    // Set imageWidth and imageHeight based on image rotation
    int rotation = getRotation(uri);
    int imageWidth;
    int imageHeight;

    if (rotation == 0 || rotation == 180) {
      imageWidth = options.outWidth;
      imageHeight = options.outHeight;
    } else {
      imageWidth = options.outHeight;
      imageHeight = options.outWidth;
    }

    // Get a scaled down version of the image
    options.inJustDecodeBounds = false;
    options.inSampleSize = getInSampleSize(imageWidth, imageHeight, targetWidth, targetHeight);
    options.inPurgeable = true;

    Bitmap scaledBitmap = BitmapFactory.decodeFile(uri.getPath(), options);

    // Get the final bitmap after rotating the scaled down image
    Bitmap bitmap;
    if (rotation == 0) {
      bitmap = scaledBitmap;
    } else {
      Matrix matrix = new Matrix();
      matrix.postRotate(rotation);
      bitmap = Bitmap.createBitmap(
          scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
      scaledBitmap.recycle();
    }

    imageView.setImageBitmap(bitmap);
    return bitmap;
  }

  /**
   * Gets the image rotation
   * 
   * @param uri the image uri
   */
  private static int getRotation(Uri uri) {
    try {
      ExifInterface exifInterface = new ExifInterface(uri.getPath());
      switch (exifInterface.getAttributeInt(
          ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        case ExifInterface.ORIENTATION_ROTATE_90:
          return 90;
        case ExifInterface.ORIENTATION_ROTATE_180:
          return 180;
        case ExifInterface.ORIENTATION_ROTATE_270:
          return 270;
        default:
          return 0;
      }
    } catch (IOException e) {
      Log.e(TAG, "Unable to get photo orientation", e);
      return 0;
    }
  }

  /**
   * Gets the in sample size.
   * 
   * @param imageWidth the image width
   * @param imageHeight the image height
   * @param targetWidth the target width
   * @param targetHeight the target height
   */
  private static int getInSampleSize(
      int imageWidth, int imageHeight, int targetWidth, int targetHeight) {
    float widthRatio = 1;
    if (imageWidth > targetWidth) {
      widthRatio = (float) imageWidth / (float) targetWidth;
    }

    float heightRatio = 1;
    if (imageHeight > targetHeight) {
      heightRatio = (float) imageHeight / (float) targetHeight;
    }

    /*
     * To fit within the target area, return the larger sample ratio so the
     * image will not be larger than the target dimensions. Use Math.floor to
     * not under sample.
     */
    return (int) Math.floor(Math.max(widthRatio, heightRatio));
  }
}
