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
package com.google.android.apps.mytracks.io.file;

import static com.google.android.testing.mocking.AndroidMock.expect;

import com.google.android.apps.mytracks.io.file.TempFileCleaner;
import com.google.android.testing.mocking.AndroidMock;
import com.google.android.testing.mocking.UsesMocks;

import android.test.AndroidTestCase;

import java.io.File;

/**
 * @author Sandor Dornbush
 */
public class TempFileCleanerTest extends AndroidTestCase {
  
  @UsesMocks({
    File.class,
  })

  public void test_noDir() {
    File dir = AndroidMock.createMock(File.class, "/no_file");
    TempFileCleaner cleaner = new TempFileCleaner(0);
    expect(dir.exists()).andStubReturn(false);
    AndroidMock.replay(dir);
    assertEquals(0, cleaner.cleanTmpDirectory(dir));
    AndroidMock.verify(dir);
  }

  public void test_emptyDir() {
    File dir = AndroidMock.createMock(File.class, "/no_file");
    TempFileCleaner cleaner = new TempFileCleaner(0);
    expect(dir.exists()).andStubReturn(true);
    expect(dir.listFiles()).andStubReturn(new File[0]);
    AndroidMock.replay(dir);
    assertEquals(0, cleaner.cleanTmpDirectory(dir));
    AndroidMock.verify(dir);
  }

  public void test_newFile() {
    File dir = AndroidMock.createMock(File.class, "/no_file");
    long now = 100000000;
    TempFileCleaner cleaner = new TempFileCleaner(now);
    expect(dir.exists()).andStubReturn(true);
    File file = AndroidMock.createMock(File.class, "/no_file/foo");
    expect(file.lastModified()).andStubReturn(now);
    File[] list = { file };
    expect(dir.listFiles()).andStubReturn(list);
    
    AndroidMock.replay(dir, file);
    assertEquals(0, cleaner.cleanTmpDirectory(dir));
    AndroidMock.verify(dir, file);
  }

  public void test_oldFile() {
    File dir = AndroidMock.createMock(File.class, "/no_file");
    long now = 100000000;
    TempFileCleaner cleaner = new TempFileCleaner(now);
    expect(dir.exists()).andStubReturn(true);
    File file = AndroidMock.createMock(File.class, "/no_file/foo");
    expect(file.lastModified()).andStubReturn(now - 3600001);
    expect(file.delete()).andStubReturn(true);
    File[] list = { file };
    expect(dir.listFiles()).andStubReturn(list);
    
    AndroidMock.replay(dir, file);
    assertEquals(1, cleaner.cleanTmpDirectory(dir));
    AndroidMock.verify(dir, file);
  }
}
