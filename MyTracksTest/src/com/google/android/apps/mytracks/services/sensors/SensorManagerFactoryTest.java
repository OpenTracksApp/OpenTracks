package com.google.android.apps.mytracks.services.sensors;

import com.google.android.apps.mytracks.MyTracksSettings;
import com.google.android.maps.mytracks.R;

import android.content.SharedPreferences;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

public class SensorManagerFactoryTest extends AndroidTestCase {

  private SharedPreferences sharedPreferences;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    
    sharedPreferences = getContext().getSharedPreferences(
        MyTracksSettings.SETTINGS_NAME, 0);
    // Let's use default values.
    sharedPreferences.edit().clear().commit();
  }
  
  @SmallTest
  public void testDefaultSettings() throws Exception {
    assertNull(SensorManagerFactory.getSensorManager(getContext()));
  }
  
  @SmallTest
  public void testCreateZephyr() throws Exception {
    sharedPreferences.edit()
      .putString(getContext().getString(R.string.sensor_type_key),
          getContext().getString(R.string.zephyr_sensor_type))
      .commit();
    SensorManager sm = SensorManagerFactory.getSensorManager(getContext());
    assertNotNull(sm);
    assertTrue(sm instanceof ZephyrSensorManager);
  }
}
