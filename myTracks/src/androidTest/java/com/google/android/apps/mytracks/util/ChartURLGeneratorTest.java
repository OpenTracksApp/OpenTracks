/*
 * Copyright 2009 Google Inc.
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
package com.google.android.apps.mytracks.util;

import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.stats.TripStatistics;

import java.util.Vector;

import junit.framework.TestCase;

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
    TripStatistics stats = t.getTripStatistics();
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
                                                 true);

    assertEquals(
        "http://chart.apis.google.com/chart?&chs=600x350&cht=lxy&"
        + "chtt=Title&chxt=x,y&chxr=0,0,0,0|1,0.0,2100.0,300&chco=009A00&"
        + "chm=B,00AA00,0,0,0&chg=100000,14.285714285714286,1,0&"
        + "chd=e:AAGZMzf.v.5l..,ATJJYY55kkVVCI",
        chart);
  }
}
