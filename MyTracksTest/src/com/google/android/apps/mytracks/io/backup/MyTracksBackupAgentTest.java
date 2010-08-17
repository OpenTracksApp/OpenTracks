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
package com.google.android.apps.mytracks.io.backup;

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils.Factory;
import com.google.android.apps.mytracks.testing.TestingProviderUtilsFactory;
import com.google.android.testing.mocking.AndroidMock;
import com.google.android.testing.mocking.UsesMocks;

import android.os.ParcelFileDescriptor;

import junit.framework.TestCase;

/**
 * Tests for {@link MyTracksBackupAgent}.
 *
 * @author Rodrigo Damazio
 */
public class MyTracksBackupAgentTest extends TestCase {
  // Database dumpers
  private DatabaseDumper trackDumper;
  private DatabaseDumper waypointDumper;
  private DatabaseDumper pointDumper;

  // Database importers
  private DatabaseImporter trackImporter;
  private DatabaseImporter waypointImporter;
  private DatabaseImporter pointImporter;

  // Other dependencies
  private BackupStateManager stateManager;
  private PreferenceBackupHelper preferencesHelper;
  private MyTracksProviderUtils providerUtils;
  private Factory oldProviderUtilsFactory;

  /**
   * Testable version of the backup agent, with dependencies mocked out.
   */
  private class TestableBackupAgent extends MyTracksBackupAgent {
    @Override
    protected BackupStateManager createStateManager(
        ParcelFileDescriptor oldState, ParcelFileDescriptor newState) {
      return stateManager;
    }

    @Override
    protected PreferenceBackupHelper createPreferenceBackupHelper() {
      return preferencesHelper;
    }

    @Override
    protected void ensureDumpers() {
      super.trackDumper = MyTracksBackupAgentTest.this.trackDumper;
      super.waypointDumper = MyTracksBackupAgentTest.this.waypointDumper;
      super.pointDumper = MyTracksBackupAgentTest.this.pointDumper;
    }

    @Override
    protected void ensureImporters() {
      super.trackImporter = MyTracksBackupAgentTest.this.trackImporter;
      super.waypointImporter = MyTracksBackupAgentTest.this.waypointImporter;
      super.pointImporter = MyTracksBackupAgentTest.this.pointImporter;
    }
  }

  @UsesMocks({ DatabaseDumper.class, DatabaseImporter.class })
  @Override
  protected void setUp() throws Exception {
    super.setUp();

    trackDumper = AndroidMock.createMock(DatabaseDumper.class);
    waypointDumper = AndroidMock.createMock(DatabaseDumper.class);
    pointDumper = AndroidMock.createMock(DatabaseDumper.class);
    trackImporter = AndroidMock.createMock(DatabaseImporter.class);
    waypointImporter = AndroidMock.createMock(DatabaseImporter.class);
    pointImporter = AndroidMock.createMock(DatabaseImporter.class);
    stateManager = AndroidMock.createMock(BackupStateManager.class);
    preferencesHelper = AndroidMock.createMock(PreferenceBackupHelper.class);
    providerUtils = AndroidMock.createMock(MyTracksProviderUtils.class);
    oldProviderUtilsFactory =
        TestingProviderUtilsFactory.installWithInstance(providerUtils);
  }

  @Override
  protected void tearDown() throws Exception {
    TestingProviderUtilsFactory.restoreOldFactory(oldProviderUtilsFactory);
    super.tearDown();
  }

  public void testOnBackup() {
    fail("Not yet implemented");
  }

  public void testOnRestore() {
    fail("Not yet implemented");
  }

}
