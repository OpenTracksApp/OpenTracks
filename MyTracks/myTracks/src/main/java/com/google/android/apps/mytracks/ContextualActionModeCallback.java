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

import android.view.Menu;

/**
 * Callback when items in the contextual action mode are selected.
 * 
 * @author Jimmy Shih
 */
public interface ContextualActionModeCallback {

  /**
   * Invoked to prepare the menu for the selected items.
   * 
   * @param menu the menu
   * @param positions the selected items' positions
   * @param ids the selected items' ids, if available
   * @param showSelectAll true to show select all
   */
  public void onPrepare(Menu menu, int[] positions, long[] ids, boolean showSelectAll);

  /**
   * Invoked when items are selected.
   * 
   * @param itemId the context menu item id
   * @param positions the selected items' positions
   * @param ids the selected items' ids, if available
   */
  public boolean onClick(int itemId, int[] positions, long[] ids);
}