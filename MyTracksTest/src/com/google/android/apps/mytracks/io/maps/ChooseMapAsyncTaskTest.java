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

import com.google.android.testing.mocking.AndroidMock;
import com.google.android.testing.mocking.UsesMocks;

import android.accounts.Account;
import android.test.AndroidTestCase;

import java.util.ArrayList;

/**
 * Tests {@link ChooseMapAsyncTask}.
 * 
 * @author Youtao Liu
 */
public class ChooseMapAsyncTaskTest extends AndroidTestCase {

  private ChooseMapActivity chooseMapActivityMock;
  private Account account;
  private static final String ACCOUNT_NAME = "AccountName";
  private static final String ACCOUNT_TYPE = "AccountType";
  private boolean getMapsStatus = false;

  public class ChooseMapAsyncTaskMock extends ChooseMapAsyncTask {

    public ChooseMapAsyncTaskMock(ChooseMapActivity activity, Account account) {
      super(activity, account);
    }

    /**
     * Creates this method to override {@link ChooseMapAsyncTask#getMaps()}.
     * 
     * @return mock the return value of getMaps().
     */
    @Override
    boolean getMaps() {
      return getMapsStatus;
    }
  }

  /**
   * Tests {@link ChooseMapAsyncTask#setActivity(ChooseMapActivity)} when the
   * task is completed. Makes sure it calls
   * {@link ChooseMapActivity#onAsyncTaskCompleted(boolean, ArrayList, ArrayList)}
   * .
   */
  public void testSetActivity_completed() {
    setup();
    chooseMapActivityMock.onAsyncTaskCompleted(false, null, null);
    AndroidMock.replay(chooseMapActivityMock);
    ChooseMapAsyncTask chooseMapAsyncTask = new ChooseMapAsyncTask(chooseMapActivityMock, account);
    chooseMapAsyncTask.setCompleted(true);
    chooseMapAsyncTask.setActivity(chooseMapActivityMock);
    AndroidMock.verify(chooseMapActivityMock);
  }

  /**
   * Test {@link ChooseMapAsyncTask#setActivity(ChooseMapActivity)} when the
   * task is not completed. Makes sure
   * {@link ChooseMapActivity#onAsyncTaskCompleted(boolean, ArrayList, ArrayList)}
   * is not invoked.
   */
  public void testSetActivity_notCompleted() {
    setup();
    AndroidMock.replay(chooseMapActivityMock);
    ChooseMapAsyncTask chooseMapAsyncTask = new ChooseMapAsyncTask(chooseMapActivityMock, account);
    chooseMapAsyncTask.setCompleted(false);
    chooseMapAsyncTask.setActivity(chooseMapActivityMock);
    AndroidMock.verify(chooseMapActivityMock);
  }

  /**
   * Tests {@link ChooseMapAsyncTask#setActivity(ChooseMapActivity)} when the
   * activity is null. Makes sure
   * {@link ChooseMapActivity#onAsyncTaskCompleted(boolean, ArrayList, ArrayList)}
   * is not invoked.
   */
  public void testSetActivity_nullActivity() {
    setup();
    AndroidMock.replay(chooseMapActivityMock);
    ChooseMapAsyncTask chooseMapAsyncTask = new ChooseMapAsyncTask(chooseMapActivityMock, account);
    chooseMapAsyncTask.setCompleted(true);
    chooseMapAsyncTask.setActivity(null);
    AndroidMock.verify(chooseMapActivityMock);
  }

  /**
   * Tests the method {@link ChooseMapAsyncTask#onPostExecute(Boolean)} when the
   * result is true. Makes sure
   * {@link ChooseMapActivity#onAsyncTaskCompleted(boolean, ArrayList, ArrayList)}
   * is invoked.
   */
  public void testOnPostExecute_trueResult() {
    setup();
    chooseMapActivityMock.onAsyncTaskCompleted(true, null, null);
    AndroidMock.replay(chooseMapActivityMock);
    ChooseMapAsyncTask chooseMapAsyncTask = new ChooseMapAsyncTask(chooseMapActivityMock, account);
    chooseMapAsyncTask.onPostExecute(true);
    AndroidMock.verify(chooseMapActivityMock);
  }

  /**
   * Tests the method {@link ChooseMapAsyncTask#onPostExecute(Boolean)} when the
   * result is false. Makes sure
   * {@link ChooseMapActivity#onAsyncTaskCompleted(boolean, ArrayList, ArrayList)}
   * is invoked.
   */
  public void testOnPostExecute_falseResult() {
    setup();
    chooseMapActivityMock.onAsyncTaskCompleted(false, null, null);
    AndroidMock.replay(chooseMapActivityMock);
    ChooseMapAsyncTask chooseMapAsyncTask = new ChooseMapAsyncTask(chooseMapActivityMock, account);
    chooseMapAsyncTask.onPostExecute(false);
    AndroidMock.verify(chooseMapActivityMock);
  }

  /**
   * Tests the method {@link ChooseMapAsyncTask#retryUpload()}. Make sure can
   * not retry again after have retried once and failed.
   */
  public void testRetryUpload() throws Exception {
    setup();
    AndroidMock.replay(chooseMapActivityMock);
    ChooseMapAsyncTaskMock chooseMapAsyncTaskTMock = new ChooseMapAsyncTaskMock(
        chooseMapActivityMock, account);
    chooseMapAsyncTaskTMock.setCanRetry(false);
    getMapsStatus = true;
    assertFalse(chooseMapAsyncTaskTMock.retryUpload());
    AndroidMock.verify(chooseMapActivityMock);
  }

  /**
   * Tests the method {@link ChooseMapAsyncTask#retryUpload()}. Make sure can
   * retry after get maps failed and never retry before.
   */
  public void testRetryUpload_retryOnce() throws Exception {
    setup();
    AndroidMock.replay(chooseMapActivityMock);
    ChooseMapAsyncTaskMock chooseMapAsyncTaskTMock = new ChooseMapAsyncTaskMock(
        chooseMapActivityMock, account);
    chooseMapAsyncTaskTMock.setCanRetry(true);
    getMapsStatus = false;
    assertFalse(chooseMapAsyncTaskTMock.retryUpload());
    // Can only retry once.
    assertFalse(chooseMapAsyncTaskTMock.getCanRetry());
    AndroidMock.verify(chooseMapActivityMock);
  }

  /**
   * Tests the method {@link ChooseMapAsyncTask#retryUpload()}. Make sure will
   * not retry after get maps successfully.
   */
  public void testRetryUpload_successGetMaps() throws Exception {
    setup();
    AndroidMock.replay(chooseMapActivityMock);
    ChooseMapAsyncTaskMock chooseMapAsyncTaskTMock = new ChooseMapAsyncTaskMock(
        chooseMapActivityMock, account);
    chooseMapAsyncTaskTMock.setCanRetry(true);
    getMapsStatus = true;
    assertTrue(chooseMapAsyncTaskTMock.retryUpload());
    // Can only retry once.
    assertFalse(chooseMapAsyncTaskTMock.getCanRetry());
    AndroidMock.verify(chooseMapActivityMock);
  }

  /**
   * Initials setup for test.
   */
  void setup() {
    setupChooseMapActivityMock();
    account = new Account(ACCOUNT_NAME, ACCOUNT_TYPE);
  }

  /**
   * Create a mock object of ChooseMapActivity.
   */
  @UsesMocks(ChooseMapActivity.class)
  private void setupChooseMapActivityMock() {
    chooseMapActivityMock = AndroidMock.createMock(ChooseMapActivity.class);
    // This is used in the constructor of ChooseMapAsyncTask.
    AndroidMock.expect(chooseMapActivityMock.getApplicationContext()).andReturn(getContext()).anyTimes();
  }

}
