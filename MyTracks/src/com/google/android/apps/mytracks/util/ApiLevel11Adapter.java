package com.google.android.apps.mytracks.util;

import android.app.Activity;
import android.view.Window;

public class ApiLevel11Adapter extends ApiLevel9Adapter {
  @Override
  public void showActionBar(Activity activity) {
    activity.requestWindowFeature(Window.FEATURE_ACTION_BAR);
  }
}
