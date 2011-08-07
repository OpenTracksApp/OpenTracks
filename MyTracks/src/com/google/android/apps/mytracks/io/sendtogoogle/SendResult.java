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
package com.google.android.apps.mytracks.io.sendtogoogle;

/**
 * This class represents the the result of the uploading of track data to a
 * single service.
 *
 * @author Matthew Simmons
 */
public class SendResult {
  private final SendType type;
  private final boolean success;

  /**
   * @param type the service to which track data was uploaded
   * @param success true if the uploading succeeded
   */
  public SendResult(SendType type, boolean success) {
    this.type = type;
    this.success = success;
  }

  /** Returns the service to which the track data was uploaded */
  public SendType getType() {
    return type;
  }

  /** Returns true if the uploading succeeded */
  public boolean isSuccess() {
    return success;
  }
}
