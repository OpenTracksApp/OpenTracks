// Copyright 2009 Google Inc. All Rights Reserved.

package com.google.android.apps.mytracks.util;

import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.stats.TripStatistics;

import junit.framework.TestCase;

import java.util.Vector;

/**
 * Tests for the Chart URL generator.
 *
 * @author Sandor Dornbush
 */
public class ChartURLGeneratorTest extends TestCase {

  public void testgetChartUrl() {
    Vector<Double> distances = new Vector<Double>();
    Vector<Double> elevations = new Vector<Double>();
    Track t = new Track();
    TripStatistics stats = t.getStatistics();
    stats.setMinElevation(0);
    stats.setMaxElevation(2000);
    stats.setTotalDistance(100);

    distances.add(0.0);
    elevations.add(10.0);

    distances.add(10.0);
    elevations.add(300.0);

    distances.add(20.0);
    elevations.add(800.0);

    distances.add(50.0);
    elevations.add(1900.0);

    distances.add(75.0);
    elevations.add(1200.0);

    distances.add(90.0);
    elevations.add(700.0);

    distances.add(100.0);
    elevations.add(70.0);

    String chart = ChartURLGenerator.getChartUrl(distances,
                                                 elevations,
                                                 t,
                                                 "Title",
                                                 true,
                                                 false);

    assertEquals(
        "http://chart.apis.google.com/chart?&chs=600x350&cht=lxy&"
        + "chtt=Title&chxt=x,y&chxr=0,0,0,0|1,0.0,2100.0,300&chco=009A00&"
        + "chm=B,00AA00,0,0,0&chg=100000,14.285714285714286,1,0&"
        + "chd=e:AAGZMzf.v.5l..,ATJJYY55kkVVCI",
        chart);
  }
}
