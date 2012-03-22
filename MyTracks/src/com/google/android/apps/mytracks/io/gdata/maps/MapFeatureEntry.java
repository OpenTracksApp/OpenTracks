// Copyright 2010 Google Inc. All Rights Reserved.
package com.google.android.apps.mytracks.io.gdata.maps;

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
