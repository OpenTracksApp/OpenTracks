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

import android.os.Parcel;
import android.os.Parcelable;

/**
 * An enumeration of the types of waypoints that can be created.
 *
 * @author Sandor Dornbush
 */
public enum WaypointType implements Parcelable {

  MARKER,
  STATISTICS;
  
  public static class Creator implements Parcelable.Creator<WaypointType> {

    public WaypointType createFromParcel(Parcel source) {
      int i = source.readInt();
      for (WaypointType type : WaypointType.values()) {
        if (i == type.ordinal()) {
          return type;
        }
      }
      return null;
    }

    public WaypointType[] newArray(int size) {
      return new WaypointType[size];
    }
  }

  public static final Creator CREATOR = new Creator();
  
  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel parcel, int arg1) {
    parcel.writeInt(ordinal());
  }

}