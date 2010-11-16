/*
 * Copyright 2009 Google Inc.
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
package com.google.android.apps.mymaps;

/**
 * Metadata about a "my maps" map.
 */
public class MyMapsMapMetadata {

  private String title;
  private String description;
  private String gdataEditUri;
  private boolean searchable;

  public MyMapsMapMetadata() {
    title = "";
    description = "";
    gdataEditUri = "";
    searchable = false;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = new String(title);
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = new String(description);
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
    this.gdataEditUri = new String(editUri);
  }
}
