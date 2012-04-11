/*
 * Copyright 2012 Google Inc.
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

package com.google.android.apps.mytracks;

/**
 * Callback when an item in the contextual action mode is selected.
 * 
 * @author Jimmy Shih
 */
public interface ContextualActionModeCallback {
    
  /**
   * Invoked when an item is selected.
   * 
   * @param itemId the context menu item id
   * @param id the row id of the item that is selected
   */
  public boolean onClick(int itemId, long id);    
}