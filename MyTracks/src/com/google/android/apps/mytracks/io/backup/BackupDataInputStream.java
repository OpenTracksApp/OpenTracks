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

import android.app.backup.BackupDataInput;

import java.io.IOException;
import java.io.InputStream;

/**
 * Input stream which reads from backup data.
 * This is exactly the same as {@link android.app.backup.BackupDataInputStream},
 * but for some odd reason Android doesn't want us instantiating that one.
 *
 * @author Rodrigo Damazio
 */
class BackupDataInputStream extends InputStream {
  private final BackupDataInput data;
  private final byte[] oneByte = new byte[1];

  public BackupDataInputStream(BackupDataInput data) {
    this.data = data;
  }

  @Override
  public int read() throws IOException {
    data.readEntityData(oneByte, 0, 1);
    return oneByte[0];
  }

  @Override
  public int read(byte[] b) throws IOException {
    return data.readEntityData(b, 0, b.length);
  }

  @Override
  public int read(byte[] b, int offset, int length) throws IOException {
    return data.readEntityData(b, offset, length);
  }
}