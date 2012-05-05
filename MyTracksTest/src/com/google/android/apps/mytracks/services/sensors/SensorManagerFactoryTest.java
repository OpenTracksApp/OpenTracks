package com.google.android.apps.mytracks.services.sensors;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

public class SensorManagerFactoryTest extends AndroidTestCase {

  private SharedPreferences sharedPreferences;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    sharedPreferences = getContext().getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    // Let's use default values.
    sharedPreferences.edit().clear().apply();
  }

  @SmallTest
  public void testDefaultSettings() throws Exception {
    assertNull(SensorManagerFactory.getInstance().getSensorManager(getContext()));
  }

  @SmallTest
  public void testCreateZephyr() throws Exception {
    assertClassForName(ZephyrSensorManager.class, R.string.sensor_type_value_zephyr);
  }

  @SmallTest
  public void testCreatePolar() throws Exception {
    assertClassForName(PolarSensorManager.class, R.string.sensor_type_value_polar);
  }

  private void assertClassForName(Class<?> c, int i) {
    PreferencesUtils.setString(getContext(), R.string.sensor_type_key, getContext().getString(i));
    SensorManager sm = SensorManagerFactory.getInstance().getSensorManager(getContext());
    assertNotNull(sm);
    assertTrue(c.isInstance(sm));
    SensorManagerFactory.getInstance().releaseSensorManager(sm);
  }
}
