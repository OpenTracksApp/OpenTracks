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

/**
 * This is a common superclass for ANT message subclasses.
 *
 * @author Matthew Simmons
 */
public class AntMessage {
  protected AntMessage() {}

  /** Build a short value from its constituent bytes */
  protected static short decodeShort(byte b0, byte b1) {
    short value = b0;
    value |= ((short) b1) << 8;
    return value;
  }
}
