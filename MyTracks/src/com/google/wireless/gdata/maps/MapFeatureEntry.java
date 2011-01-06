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
package com.google.wireless.gdata.maps;

import com.google.wireless.gdata.data.Entry;

import java.util.HashMap;
import java.util.Map;

/**
 * GData entry for a map feature.
 */
public class MapFeatureEntry extends Entry {

  private String mPrivacy = null;
  private Map<String, String> mAttributes = new HashMap<String, String>();

  public void setPrivacy(String privacy) {
    mPrivacy = privacy;
  }

  public String getPrivacy() {
    return mPrivacy;
  }

  public void setAttribute(String name, String value) {
    mAttributes.put(name, value);
  }

  public void removeAttribute(String name) {
    mAttributes.remove(name);
  }

  public Map<String, String> getAllAttributes() {
    return mAttributes;
  }
}
