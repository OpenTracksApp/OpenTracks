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

package com.google.android.apps.mytracks.services.tasks;

import com.google.android.apps.mytracks.util.PhotoUtils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * A bitmap loader.
 * 
 * @author Jimmy Shih
 */
public class BitmapLoader extends AsyncTask<Void, Void, Bitmap> {
  private static final String TAG = BitmapLoader.class.getSimpleName();

  private final WeakReference<ImageView> imageViewReference;
  private final Uri uri;
  private final int targetWidth;
  private final int targetHeight;
  private final boolean fitWithin;

  public BitmapLoader(
      ImageView imageView, Uri uri, int targetWidth, int targetHeight, boolean fitWithin) {

    // Use a WeakReference to ensure the ImageView can be garbage collected
    imageViewReference = new WeakReference<ImageView>(imageView);
    this.uri = uri;
    this.targetWidth = targetWidth;
    this.targetHeight = targetHeight;
    this.fitWithin = fitWithin;
  }

  public Uri getUri() {
    return uri;
  }

  @Override
  protected Bitmap doInBackground(Void... params) {

    // Get the image dimensions
    BitmapFactory.Options options = new BitmapFactory.Options();

    options.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(uri.getPath(), options);
    
    if (options.outWidth == 0 || options.outHeight == 0) {
      return null;
    }

    // Set imageWidth and imageHeight based on image rotation
    int rotation = getRotation();
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
    options.inSampleSize = getInSampleSize(imageWidth, imageHeight);
    options.inPurgeable = true;

    Bitmap scaledBitmap = BitmapFactory.decodeFile(uri.getPath(), options);
    if (scaledBitmap == null) {
      return null;
    }
    
    // Get the final bitmap after rotating the scaled down image
    Bitmap bitmap;
    if (rotation == 0 && fitWithin) {
      bitmap = scaledBitmap;
    } else {
      Matrix matrix = new Matrix();
      matrix.postRotate(rotation);
      int xOffset = 0;
      int yOffset = 0;
      int width = scaledBitmap.getWidth();
      int height = scaledBitmap.getHeight();
      if (rotation == 0 || rotation == 180) {
        if (!fitWithin && height > targetHeight) {
          xOffset = (height - targetHeight) / 2;
          height = targetHeight;
        }
      } else {
        if (!fitWithin && width > targetHeight) {
          yOffset = (width - targetHeight) / 2;
          width = targetHeight;
        }
      }
      bitmap = Bitmap.createBitmap(scaledBitmap, yOffset, xOffset, width, height, matrix, true);
      scaledBitmap.recycle();
    }
    return bitmap;
  }

  @Override
  protected void onPostExecute(Bitmap bitmap) {
    if (isCancelled()) {
      bitmap = null;
    }
    // If imageView is still around, set bitmap
    if (imageViewReference != null && bitmap != null) {
      ImageView imageView = imageViewReference.get();
      if (imageView != null) {
        BitmapLoader bitmapLoader = PhotoUtils.getBitmapLoader(imageView);
        if (this == bitmapLoader) {
          imageView.setImageBitmap(bitmap);
        }
      }
    }
  }

  private int getRotation() {
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
   */
  private int getInSampleSize(int imageWidth, int imageHeight) {
    float widthRatio = 1;
    if (imageWidth > targetWidth) {
      widthRatio = (float) imageWidth / (float) targetWidth;
    }

    float heightRatio = 1;
    if (imageHeight > targetHeight) {
      heightRatio = (float) imageHeight / (float) targetHeight;
    }

    double size;
    if (fitWithin) {
      /*
       * To fit within the target area, return the larger sample ratio so the
       * image will not be larger than the target dimensions.
       */
      size = Math.max(widthRatio, heightRatio);
    } else {
      /*
       * To fill the target area, return the smaller ratio so the image will
       * cover both dimensions.
       */
      size = Math.min(widthRatio, heightRatio);
    }
    // Use Math.floor to not under-sample.
    return (int) Math.floor(size);
  }
}
