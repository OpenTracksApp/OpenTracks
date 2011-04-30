package com.google.android.apps.mytracks.services.sensors;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.services.sensors.ant.AntDirectSensorManager;
import com.google.android.apps.mytracks.services.sensors.ant.AntSRMSensorManager;
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
        Constants.SETTINGS_NAME, 0);
    // Let's use default values.
    sharedPreferences.edit().clear().commit();
  }
  
  @SmallTest
  public void testDefaultSettings() throws Exception {
    assertNull(SensorManagerFactory.getSensorManager(getContext()));
  }
  
  @SmallTest
  public void testCreateZephyr() throws Exception {
    assertClassForName(ZephyrSensorManager.class, R.string.zephyr_sensor_type);
  }
  
  @SmallTest
  public void testCreateAnt() throws Exception {
    assertClassForName(AntDirectSensorManager.class, R.string.ant_sensor_type);
  }

  @SmallTest
  public void testCreateAntSRM() throws Exception {
    assertClassForName(AntSRMSensorManager.class, R.string.srm_ant_bridge_sensor_type);
  }

  private void assertClassForName(Class<?> c, int i) {
    sharedPreferences.edit()
        .putString(getContext().getString(R.string.sensor_type_key),
            getContext().getString(i))
        .commit();
    SensorManager sm = SensorManagerFactory.getSensorManager(getContext());
    assertNotNull(sm);
    assertTrue(c.isInstance(sm));
  }
}
