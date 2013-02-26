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

import com.google.android.apps.mytracks.util.ApiAdapterFactory;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.test.AndroidTestCase;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Tests for {@link PreferenceBackupHelper}.
 *
 * @author Rodrigo Damazio
 */
public class PreferenceBackupHelperTest extends AndroidTestCase {
  private Map<String, ?> preferenceValues;
  private SharedPreferences preferences;
  private PreferenceBackupHelper preferenceBackupHelper;

  /**
   * Mock shared preferences editor which does not persist state.
   */
  private class MockPreferenceEditor implements SharedPreferences.Editor {
    private Map<String, Object> newPreferences = new HashMap<String, Object>(preferenceValues);

    @Override
    public Editor clear() {
      newPreferences.clear();
      return this;
    }

    @Override
    public boolean commit() {
      apply();
      return true;
    }

    @Override
    public void apply() {
      preferenceValues = newPreferences;
    }

    @Override
    public Editor putBoolean(String key, boolean value) {
      return put(key, value);
    }

    @Override
    public Editor putFloat(String key, float value) {
      return put(key, value);
    }

    @Override
    public Editor putInt(String key, int value) {
      return put(key, value);
    }

    @Override
    public Editor putLong(String key, long value) {
      return put(key, value);
    }

    @Override
    public Editor putString(String key, String value) {
      return put(key, value);
    }

    public Editor putStringSet(String key, Set<String> value) {
      return put(key, value);
    }

    private <T> Editor put(String key, T value) {
      newPreferences.put(key, value);
      return this;
    }

    @Override
    public Editor remove(String key) {
      newPreferences.remove(key);
      return this;
    }
  }

  /**
   * Mock shared preferences which does not persist state.
   */
  private class MockPreferences implements SharedPreferences {
    @Override
    public boolean contains(String key) {
      return preferenceValues.containsKey(key);
    }

    @Override
    public Editor edit() {
      return new MockPreferenceEditor();
    }

    @Override
    public Map<String, ?> getAll() {
      return preferenceValues;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
      return get(key, defValue);
    }

    @Override
    public float getFloat(String key, float defValue) {
      return get(key, defValue);
    }

    @Override
    public int getInt(String key, int defValue) {
      return get(key, defValue);
    }

    @Override
    public long getLong(String key, long defValue) {
      return get(key, defValue);
    }

    @Override
    public String getString(String key, String defValue) {
      return get(key, defValue);
    }

    public Set<String> getStringSet(String key, Set<String> defValue) {
      return get(key, defValue);
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(
        OnSharedPreferenceChangeListener listener) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(
        OnSharedPreferenceChangeListener listener) {
      throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    private <T> T get(String key, T defValue) {
      Object value = preferenceValues.get(key);
      if (value == null) return defValue;
      return (T) value;
    }
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    preferenceValues = new HashMap<String, Object>();
    preferences = new MockPreferences();
    preferenceBackupHelper = new PreferenceBackupHelper(getContext());
  }

  public void testExportImportPreferences() throws Exception {
    // Populate with some initial values
    Editor editor = preferences.edit();
    editor.clear();
    editor.putBoolean("bool1", true);
    editor.putBoolean("bool2", false);
    editor.putFloat("flt1", 3.14f);
    editor.putInt("int1", 42);
    editor.putLong("long1", 123456789L);
    editor.putString("str1", "lolcat");
    ApiAdapterFactory.getApiAdapter().applyPreferenceChanges(editor);

    // Export it
    byte[] exported = preferenceBackupHelper.exportPreferences(preferences);

    // Mess with the previous values
    editor = preferences.edit();
    editor.clear();
    editor.putString("str2", "Shouldn't be there after restore");
    editor.putBoolean("bool2", true);
    ApiAdapterFactory.getApiAdapter().applyPreferenceChanges(editor);

    // Import it back
    preferenceBackupHelper.importPreferences(exported, preferences);

    assertFalse(preferences.contains("str2"));
    assertTrue(preferences.getBoolean("bool1", false));
    assertFalse(preferences.getBoolean("bool2", true));
    assertEquals(3.14f, preferences.getFloat("flt1", 0.0f));
    assertEquals(42, preferences.getInt("int1", 0));
    assertEquals(123456789L, preferences.getLong("long1", 0));
    assertEquals("lolcat", preferences.getString("str1", ""));
  }
}
