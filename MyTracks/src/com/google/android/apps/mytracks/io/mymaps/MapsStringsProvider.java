// Copyright 2011 Google Inc. All Rights Reserved.
package com.google.android.apps.mytracks.io.mymaps;

/**
 * Interface to provide i18n'ed string resources to the Maps library.
 *
 * @author Rodrigo Damazio
 */
public interface MapsStringsProvider {
  /** Returns the contents of R.string.new_map_description. */
  String getNewMapDescription();

  /** Returns the contents of R.string.start. */
  String getStart();

  /** Returns the contents of R.string.end. */
  String getEnd();
}
