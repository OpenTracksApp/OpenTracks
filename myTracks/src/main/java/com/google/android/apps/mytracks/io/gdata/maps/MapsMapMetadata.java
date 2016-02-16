// Copyright 2009 Google Inc. All Rights Reserved.
package com.google.android.apps.mytracks.io.gdata.maps;

/**
 * Metadata about a Google Maps map.
 */
public class MapsMapMetadata {

  private String title;
  private String description;
  private String gdataEditUri;
  private boolean searchable;

  public MapsMapMetadata() {
    title = "";
    description = "";
    gdataEditUri = "";
    searchable = false;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean getSearchable() {
    return searchable;
  }

  public void setSearchable(boolean searchable) {
    this.searchable = searchable;
  }

  public String getGDataEditUri() {
    return gdataEditUri;
  }

  public void setGDataEditUri(String editUri) {
    this.gdataEditUri = editUri;
  }
}
