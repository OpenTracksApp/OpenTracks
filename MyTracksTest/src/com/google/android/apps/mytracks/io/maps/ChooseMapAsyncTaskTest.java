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

/**
 * Tests {@link ChooseMapAsyncTask}.
 * 
 * @author Youtao Liu
 */
public class ChooseMapAsyncTaskTest extends AndroidTestCase {

  private ChooseMapActivity chooseMapActivityMock;
  private Account account;
  private final String accountName = "AccountName";
  private final String accountType = "AccountType";
  private boolean getMapsStatus = false;

  public class ChooseMapAsyncTaskTMock extends ChooseMapAsyncTask {

    public ChooseMapAsyncTaskTMock(ChooseMapActivity activity, Account account) {
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

  @Override
  @UsesMocks({ ChooseMapActivity.class })
  protected void setUp() throws Exception {
    super.setUp();
    chooseMapActivityMock = AndroidMock.createMock(ChooseMapActivity.class);
    account = new Account(accountName, accountType);
    AndroidMock.expect(chooseMapActivityMock.getApplicationContext()).andReturn(getContext());
  }

  /**
   * Tests the method {@link ChooseMapAsyncTask#setActivity(ChooseMapActivity)}
   * when the task is completed.
   */
  public void testSetActivity_completed() {
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
    AndroidMock.replay(chooseMapActivityMock);
    ChooseMapAsyncTaskTMock chooseMapAsyncTaskTMock = new ChooseMapAsyncTaskTMock(
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

}
