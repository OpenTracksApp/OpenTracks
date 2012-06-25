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
package com.google.android.apps.mytracks.io.maps;

import com.google.android.apps.mytracks.io.gdata.maps.MapsMapMetadata;
import com.google.android.maps.mytracks.R;

import android.annotation.TargetApi;
import android.content.Intent;
import android.test.AndroidTestCase;
import android.widget.ArrayAdapter;

import java.util.ArrayList;

/**
 * Tests the {@link ChooseMapActivity}.
 * 
 * @author Youtao Liu
 */
public class ChooseMapActivityTest extends AndroidTestCase {

  private static final String MAP_ID = "mapid";
  private static final String MAP_TITLE = "title";
  private static final String MAP_DESC = "desc";

  private ArrayList<String> mapIds = new ArrayList<String>();
  private ArrayList<MapsMapMetadata> mapDatas = new ArrayList<MapsMapMetadata>();
  private boolean errorDialogShown = false;
  private boolean progressDialogRemoved = false;

  /**
   * Creates a class to override some methods of {@link ChooseMapActivity} to
   * makes it testable.
   * 
   * @author youtaol
   */
  @TargetApi(11)
  public class ChooseMapActivityMock extends ChooseMapActivity {
    /**
     * By overriding this method, avoids to start next activity.
     */
    @Override
    public void startActivity(Intent intent) {}

    /**
     * By overriding this method, avoids to show an error dialog and set the
     * show flag to true.
     */
    @Override
    public void showErrorDialog() {
      errorDialogShown = true;
    }
    
    /**
     * By overriding this method, avoids to show an error dialog and set the
     * show flag to true.
     */
    @Override
    public void removeProgressDialog() {
      progressDialogRemoved = true;
    }
  }

  /**
   * Tests the method
   * {@link ChooseMapActivity#onAsyncTaskCompleted(boolean, ArrayList, ArrayList)}
   * . An alert dialog should be shown when there is no map.
   */
  public void testOnAsyncTaskCompleted_fail() {
    ChooseMapActivityMock chooseMapActivityMock = new ChooseMapActivityMock();
    errorDialogShown = false;
    progressDialogRemoved = false;
    chooseMapActivityMock.onAsyncTaskCompleted(false, null, null);
    assertTrue(progressDialogRemoved);
    assertTrue(errorDialogShown);
  }

  /**
   * Tests the method
   * {@link ChooseMapActivity#onAsyncTaskCompleted(boolean, ArrayList, ArrayList)}
   * . Check the logic when there is only map.
   */
  public void testOnAsyncTaskCompleted_success_oneMap() {
    ChooseMapActivityMock chooseMapActivityMock = new ChooseMapActivityMock();
    chooseMapActivityMock.arrayAdapter = new ArrayAdapter<ChooseMapActivity.ListItem>(getContext(),
        R.layout.choose_map_item);
    simulateMaps(1);
    chooseMapActivityMock.onAsyncTaskCompleted(true, mapIds, mapDatas);
    assertEquals(1, chooseMapActivityMock.arrayAdapter.getCount());
    assertEquals(MAP_ID + "0", chooseMapActivityMock.arrayAdapter.getItem(0).getMapId());
    assertEquals(MAP_TITLE + "0", chooseMapActivityMock.arrayAdapter.getItem(0).getMapData()
        .getTitle());
    assertEquals(MAP_DESC + "0", chooseMapActivityMock.arrayAdapter.getItem(0).getMapData()
        .getDescription());
  }

  /**
   * Tests the method
   * {@link ChooseMapActivity#onAsyncTaskCompleted(boolean, ArrayList, ArrayList)}
   * . Check the logic when there are 10 maps.
   */
  public void testOnAsyncTaskCompleted_success_twoMaps() {
    ChooseMapActivityMock chooseMapActivityMock = new ChooseMapActivityMock();
    chooseMapActivityMock.arrayAdapter = new ArrayAdapter<ChooseMapActivity.ListItem>(getContext(),
        R.layout.choose_map_item);
    simulateMaps(10);
    chooseMapActivityMock.onAsyncTaskCompleted(true, mapIds, mapDatas);
    assertEquals(10, chooseMapActivityMock.arrayAdapter.getCount());
    assertEquals(MAP_ID + "9", chooseMapActivityMock.arrayAdapter.getItem(9).getMapId());
    assertEquals(MAP_TITLE + "9", chooseMapActivityMock.arrayAdapter.getItem(9).getMapData()
        .getTitle());
    assertEquals(MAP_DESC + "9", chooseMapActivityMock.arrayAdapter.getItem(9).getMapData()
        .getDescription());
  }

  /**
   * Simulates map data for the test.
   * 
   * @param number of data should be simulated.
   */
  private void simulateMaps(int number) {
    mapIds = new ArrayList<String>();
    mapDatas = new ArrayList<MapsMapMetadata>();
    for (int i = 0; i < number; i++) {
      mapIds.add(MAP_ID + i);
      MapsMapMetadata metaData = new MapsMapMetadata();
      metaData.setTitle(MAP_TITLE + i);
      metaData.setDescription(MAP_DESC + i);
      metaData.setSearchable(true);
      mapDatas.add(metaData);
    }
  }
}
