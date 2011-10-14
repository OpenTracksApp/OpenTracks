package com.google.android.apps.mytracks.util;

import com.google.android.apps.mytracks.io.backup.BackupPreferencesListener;
import com.google.android.apps.mytracks.io.backup.BackupPreferencesListenerImpl;
import com.google.android.apps.mytracks.services.tasks.FroyoStatusAnnouncerTask;
import com.google.android.apps.mytracks.services.tasks.PeriodicTask;

import android.content.Context;

/**
 * API level 8 specific implementation of the {@link ApiLevelAdapter}.
 *
 * @author Jimmy Shih
 */
public class ApiLevel8Adapter extends ApiLevel5Adapter {
  
  @Override
  public PeriodicTask getPeriodicTask(Context context) {
    return new FroyoStatusAnnouncerTask(context);
  }
  
  @Override
  public BackupPreferencesListener getBackupPreferencesListener(Context context) {
    return new BackupPreferencesListenerImpl(context);
  }
}
