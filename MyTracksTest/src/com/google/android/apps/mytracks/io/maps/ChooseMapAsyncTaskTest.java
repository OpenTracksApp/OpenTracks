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

import android.accounts.Account;
import android.test.AndroidTestCase;

/**
 * Tests {@link ChooseMapAsyncTask}.
 * 
 * @author Youtao Liu
 */
public class ChooseMapAsyncTaskTest extends AndroidTestCase {

  private ChooseMapActivity chooseMapActivityMock;
  private Account account;
  private final String ACCOUNT_NAME = "AccountName";
  private final String ACCOUNT_TYPE = "AccountType";
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
   * Tests the method {@link ChooseMapAsyncTask#setActivity(ChooseMapActivity)}
   * when the task is completed.
   */
  public void testSetActivity_completed() {
    createMockObjects();
    chooseMapActivityMock.onAsyncTaskCompleted(false, null, null);
    AndroidMock.replay(chooseMapActivityMock);
    ChooseMapAsyncTask chooseMapAsyncTask = new ChooseMapAsyncTask(chooseMapActivityMock, account);
    chooseMapAsyncTask.setCompleted(true);
    chooseMapAsyncTask.setActivity(chooseMapActivityMock);
    AndroidMock.verify(chooseMapActivityMock);
  }

  /**
   * Tests the method {@link ChooseMapAsyncTask#setActivity(ChooseMapActivity)}
   * when the task is not completed.
   */
  public void testSetActivity_notCompleted() {
    createMockObjects();
    AndroidMock.replay(chooseMapActivityMock);
    ChooseMapAsyncTask chooseMapAsyncTask = new ChooseMapAsyncTask(chooseMapActivityMock, account);
    chooseMapAsyncTask.setCompleted(false);
    chooseMapAsyncTask.setActivity(chooseMapActivityMock);
    AndroidMock.verify(chooseMapActivityMock);
  }

  /**
   * Tests the method {@link ChooseMapAsyncTask#setActivity(ChooseMapActivity)}
   * when the activity is null.
   */
  public void testSetActivity_nullActivity() {
    createMockObjects();
    AndroidMock.replay(chooseMapActivityMock);
    ChooseMapAsyncTask chooseMapAsyncTask = new ChooseMapAsyncTask(chooseMapActivityMock, account);
    chooseMapAsyncTask.setCompleted(true);
    chooseMapAsyncTask.setActivity(null);
  }

  /**
   * Tests the method {@link ChooseMapAsyncTask#onPostExecute(Boolean)} when the
   * result is true.
   */
  public void testOnPostExecute_trueResult() {
    createMockObjects();
    chooseMapActivityMock.onAsyncTaskCompleted(true, null, null);
    AndroidMock.replay(chooseMapActivityMock);
    ChooseMapAsyncTask chooseMapAsyncTask = new ChooseMapAsyncTask(chooseMapActivityMock, account);
    chooseMapAsyncTask.onPostExecute(true);
    AndroidMock.verify(chooseMapActivityMock);
  }

  /**
   * Tests the method {@link ChooseMapAsyncTask#onPostExecute(Boolean)} when the
   * result is false.
   */
  public void testOnPostExecute_falseResult() {
    createMockObjects();
    chooseMapActivityMock.onAsyncTaskCompleted(false, null, null);
    AndroidMock.replay(chooseMapActivityMock);
    ChooseMapAsyncTask chooseMapAsyncTask = new ChooseMapAsyncTask(chooseMapActivityMock, account);
    chooseMapAsyncTask.onPostExecute(false);
    AndroidMock.verify(chooseMapActivityMock);
  }

  /**
   * Tests the method {@link ChooseMapAsyncTas#retryUpload()}.
   */
  public void testRetryUpload() throws Exception {
    createMockObjects();
    AndroidMock.replay(chooseMapActivityMock);
    ChooseMapAsyncTaskMock chooseMapAsyncTaskTMock = new ChooseMapAsyncTaskMock(
        chooseMapActivityMock, account);
    chooseMapAsyncTaskTMock.setCanRetry(true);
    getMapsStatus = false;
    assertFalse(chooseMapAsyncTaskTMock.retryUpload());
    // Can only retry once.
    assertFalse(chooseMapAsyncTaskTMock.getCanRetry());

    chooseMapAsyncTaskTMock.setCanRetry(true);
    getMapsStatus = true;
    assertTrue(chooseMapAsyncTaskTMock.retryUpload());
    // Can only retry once.
    assertFalse(chooseMapAsyncTaskTMock.getCanRetry());

    chooseMapAsyncTaskTMock.setCanRetry(false);
    getMapsStatus = true;
    assertFalse(chooseMapAsyncTaskTMock.retryUpload());
    AndroidMock.verify(chooseMapActivityMock);
  }
  
  /**
   * Initials chooseMapActivityMock and account for test.
   */
  private void createMockObjects() {
    chooseMapActivityMock = AndroidMock.createMock(ChooseMapActivity.class);
    account = new Account(ACCOUNT_NAME, ACCOUNT_TYPE);
    // This is used in the constructor of ChooseMapAsyncTask.
    AndroidMock.expect(chooseMapActivityMock.getApplicationContext()).andReturn(getContext());
  }

}
