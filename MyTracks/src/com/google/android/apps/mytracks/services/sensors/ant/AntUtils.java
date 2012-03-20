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
package com.google.android.apps.mytracks.services.sensors.ant;

import com.dsi.ant.AntDefine;
import com.dsi.ant.AntInterface;

import android.content.Context;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility methods for ANT functionality.
 *
 * Prefer use of this class to importing DSI ANT classes into code outside of
 * the sensors package.
 */
public class AntUtils {
  private AntUtils() {}

  /** Returns true if this device supports ANT sensors. */
  public static boolean hasAntSupport(Context context) {
    return AntInterface.hasAntSupport(context);
  }
  
  /**
   * Finds the names of in the messages with the given value
   */
  public static String antMessageToString(byte msg) {
    return findConstByteInClass(AntDefine.class, msg, "MESG_.*_ID");
  }

  /**
   * Finds the names of in the events with the given value
   */
  public static String antEventToStr(byte event) {
    return findConstByteInClass(AntDefine.class, event, ".*EVENT.*");
  }
  
  /**
   * Finds a set of constant static byte field declarations in the class that have the given value
   * and whose name match the given pattern
   * @param cl class to search in
   * @param value value of constant static byte field declarations to match
   * @param regexPattern pattern to match against the name of the field     
   * @return a set of the names of fields, expressed as a string
   */
  private static String findConstByteInClass(Class<?> cl, byte value, String regexPattern)
  {
    Field[] fields = cl.getDeclaredFields();
    Set<String> fieldSet = new HashSet<String>();
    for (Field f : fields) {
      try {
        if (f.getType() == Byte.TYPE &&
            (f.getModifiers() & Modifier.STATIC) != 0 &&
            f.getName().matches(regexPattern) &&
            f.getByte(null) == value) {
          fieldSet.add(f.getName());
        }
      } catch (IllegalArgumentException e) {
        //  ignore
      } catch (IllegalAccessException e) {
        //  ignore
      }
    }
    return fieldSet.toString();
  }
}
