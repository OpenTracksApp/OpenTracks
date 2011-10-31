/*
 * Copyright 2008 Google Inc.
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * Utility functions for android resources.
 *
 * @author Sandor Dornbush
 */
public class ResourceUtils {
  public static CharSequence readFile(Context activity, int id) {
    BufferedReader in = null;
    try {
      in = new BufferedReader(new InputStreamReader(
          activity.getResources().openRawResource(id)));
      String line;
      StringBuilder buffer = new StringBuilder();
      while ((line = in.readLine()) != null) {
        buffer.append(line).append('\n');
      }
      return buffer;
    } catch (IOException e) {
      return "";
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException e) {
          // Ignore
        }
      }
    }
  }

  public static void readBinaryFileToOutputStream(
      Context activity, int id, OutputStream os) {
    BufferedInputStream in = null;
    BufferedOutputStream out = null;
    try {
      in = new BufferedInputStream(
          activity.getResources().openRawResource(id));
      out = new BufferedOutputStream(os);
      int b;
      while ((b = in.read()) != -1) {
        out.write(b);
      }
      out.flush();
    } catch (IOException e) {
      return;
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException e) {
          // Ignore
        }
      }

      if (out != null) {
        try {
          out.close();
        } catch (IOException e) {
          // Ignore
        }
      }
    }
  }

  private ResourceUtils() {
  }
}
