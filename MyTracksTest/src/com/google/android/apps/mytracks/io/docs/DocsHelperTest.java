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
package com.google.android.apps.mytracks.io.docs;

import com.google.android.apps.mytracks.io.gdata.GDataWrapper;
import com.google.wireless.gdata.client.GDataServiceClient;

import android.content.Context;
import android.test.mock.MockContext;

import junit.framework.TestCase;

import java.io.IOException;

/**
 * Tests for {@link DocsHelper}, with the exception of 
 * {@link DocsHelper#addTrackRow}, which is in 
 * {@link DocsHelper_AddTrackRowTest}.
 * 
 * @author Matthew Simmons
 */
public class DocsHelperTest extends TestCase {
  private Context mockContext = new MockContext();
  
  // TODO(simmonmt): Use AndroidMock to mock this class.  We do it the hard
  // way because use of AndroidMock to mock GDataWrapper triggers a compile
  // error in an unrelated file.  Specifically, it causes a NoClassDefFound
  // exception for com.google.wireless.gdata2.client.AuthenticationException, 
  // (wrongly) attributed to the first source file in the project.  Using the 
  // @UsesMocks(GDataWrapper.class) annotation is enough -- you don't have to 
  // touch AndroidMock at all to get this failure.
  // The bug is filed with Android Mock as
  //   http://code.google.com/p/android-mock/issues/detail?id=3
  private class MockGDataWrapper extends GDataWrapper<GDataServiceClient> {
    private final boolean returnValue;
    
    MockGDataWrapper(boolean returnValue) {
      this.returnValue = returnValue;
    }
    
    @Override
    public boolean runQuery(QueryFunction<GDataServiceClient> queryFunction) {
      return returnValue;
    }
  }
  
  public void testCreateSpreadsheet_noException() throws Exception {
    // Our mock GDataWrapper isn't able to affect the return value from
    // DocsHelper#createSpreadsheet.  As such, we're only able to simulate the
    // case where there weren't any GData errors, but not spreadsheet ID was
    // actually returned.  createSpreadsheet is defined to return null in that
    // situation.
    assertNull(new DocsHelper().createSpreadsheet(
        mockContext, new MockGDataWrapper(true), "sheetName"));
  }
  
  public void testCreateSpreadsheet_exception() throws Exception {
    try {
      new DocsHelper().createSpreadsheet(
          mockContext, new MockGDataWrapper(false), "sheetName");
      fail();
    } catch (IOException expected) {}
  }
  
  public void testRequestSpreadsheetId_noException() throws Exception {
    assertNull(new DocsHelper().requestSpreadsheetId(
        new MockGDataWrapper(true), "sheetName"));
  }
  
  public void testRequestSpreadsheetId_exception() throws Exception {
    try {
      new DocsHelper().requestSpreadsheetId(new MockGDataWrapper(false), 
          "sheetName");
      fail();
    } catch (IOException expected) {}
  }
  
  public void testGetWorksheetId_noException() throws Exception {
    assertNull(new DocsHelper().getWorksheetId(new MockGDataWrapper(true), 
        "sheetId"));
  }
  
  public void testGetWorksheetId_exception() throws Exception {
    try {
      new DocsHelper().getWorksheetId(new MockGDataWrapper(false), "sheetId");
      fail();
    } catch (IOException expected) {}
  }
}
