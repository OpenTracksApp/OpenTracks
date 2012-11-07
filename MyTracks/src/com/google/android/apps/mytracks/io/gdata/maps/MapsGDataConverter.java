// Copyright 2009 Google Inc. All Rights Reserved.
package com.google.android.apps.mytracks.io.gdata.maps;

import com.google.wireless.gdata.data.Entry;
import com.google.wireless.gdata.data.StringUtils;

import android.graphics.Color;
import android.util.Log;

import java.io.IOException;
import java.io.StringWriter;

import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

/**
 * Converter from GData objects to Maps objects.
 */
public class MapsGDataConverter {

  private final XmlSerializer xmlSerializer;

  public MapsGDataConverter() throws XmlPullParserException {
    xmlSerializer = XmlPullParserFactory.newInstance().newSerializer();
  }

  public static MapsMapMetadata getMapMetadataForEntry(
      MapFeatureEntry entry) {
    MapsMapMetadata metadata = new MapsMapMetadata();
    if ("public".equals(entry.getPrivacy())) {
      metadata.setSearchable(true);
    } else {
      metadata.setSearchable(false);
    }
    metadata.setTitle(entry.getTitle());
    metadata.setDescription(entry.getSummary());

    String editUri = entry.getEditUri();
    if (editUri != null) {
      metadata.setGDataEditUri(editUri);
    }

    return metadata;
  }

  public static String getMapidForEntry(Entry entry) {
    return MapsClient.getMapIdFromMapEntryId(entry.getId());
  }

  public static Entry getMapEntryForMetadata(MapsMapMetadata metadata) {
    MapFeatureEntry entry = new MapFeatureEntry();
    entry.setEditUri(metadata.getGDataEditUri());
    entry.setTitle(metadata.getTitle());
    entry.setSummary(metadata.getDescription());
    entry.setPrivacy(metadata.getSearchable() ? "public" : "unlisted");
    entry.setAuthor("android");
    entry.setEmail("nobody@google.com");
    return entry;
  }

  public MapFeatureEntry getEntryForFeature(MapsFeature feature) {
    MapFeatureEntry entry = new MapFeatureEntry();
    entry.setTitle(feature.getTitle());
    entry.setAuthor("android");
    entry.setEmail("nobody@google.com");
    entry.setCategoryScheme("http://schemas.google.com/g/2005#kind");
    entry.setCategory("http://schemas.google.com/g/2008#mapfeature");
    entry.setEditUri("");
    if (!StringUtils.isEmpty(feature.getAndroidId())) {
      entry.setAttribute("_androidId", feature.getAndroidId());
    }
    try {
      StringWriter writer = new StringWriter();
      xmlSerializer.setOutput(writer);
      xmlSerializer.startTag(null, "Placemark");
      xmlSerializer.attribute(null, "xmlns", "http://earth.google.com/kml/2.2");
      xmlSerializer.startTag(null, "Style");
      if (feature.getType() == MapsFeature.MARKER) {
        xmlSerializer.startTag(null, "IconStyle");
        xmlSerializer.startTag(null, "Icon");
        xmlSerializer.startTag(null, "href");
        xmlSerializer.text(feature.getIconUrl());
        xmlSerializer.endTag(null, "href");
        xmlSerializer.endTag(null, "Icon");
        xmlSerializer.endTag(null, "IconStyle");
      } else {
        xmlSerializer.startTag(null, "LineStyle");
        xmlSerializer.startTag(null, "color");
        int color = feature.getColor();
        // Reverse the color because KML is ABGR and Android is ARGB
        xmlSerializer.text(Integer.toHexString(
            Color.argb(Color.alpha(color), Color.blue(color),
            Color.green(color), Color.red(color))));
        xmlSerializer.endTag(null, "color");
        xmlSerializer.startTag(null, "width");
        xmlSerializer.text(Integer.toString(feature.getLineWidth()));
        xmlSerializer.endTag(null, "width");
        xmlSerializer.endTag(null, "LineStyle");

        if (feature.getType() == MapsFeature.SHAPE) {
          xmlSerializer.startTag(null, "PolyStyle");
          xmlSerializer.startTag(null, "color");
          int fcolor = feature.getFillColor();
          // Reverse the color because KML is ABGR and Android is ARGB
          xmlSerializer.text(Integer.toHexString(Color.argb(Color.alpha(fcolor),
              Color.blue(fcolor), Color.green(fcolor), Color.red(fcolor))));
          xmlSerializer.endTag(null, "color");
          xmlSerializer.startTag(null, "fill");
          xmlSerializer.text("1");
          xmlSerializer.endTag(null, "fill");
          xmlSerializer.startTag(null, "outline");
          xmlSerializer.text("1");
          xmlSerializer.endTag(null, "outline");
          xmlSerializer.endTag(null, "PolyStyle");
        }
      }
      xmlSerializer.endTag(null, "Style");
      xmlSerializer.startTag(null, "name");
      xmlSerializer.text(feature.getTitle());
      xmlSerializer.endTag(null, "name");
      xmlSerializer.startTag(null, "description");
      xmlSerializer.cdsect(feature.getDescription());
      xmlSerializer.endTag(null, "description");
      StringBuilder pointBuilder = new StringBuilder();
      for (int i = 0; i < feature.getPointCount(); ++i) {
        if (i > 0) {
          pointBuilder.append('\n');
        }
        pointBuilder.append(feature.getPoint(i).getLongitude());
        pointBuilder.append(',');
        pointBuilder.append(feature.getPoint(i).getLatitude());
        pointBuilder.append(",0.000000");
      }
      String pointString = pointBuilder.toString();
      if (feature.getType() == MapsFeature.MARKER) {
        xmlSerializer.startTag(null, "Point");
        xmlSerializer.startTag(null, "coordinates");
        xmlSerializer.text(pointString);
        xmlSerializer.endTag(null, "coordinates");
        xmlSerializer.endTag(null, "Point");
      } else if (feature.getType() == MapsFeature.LINE) {
        xmlSerializer.startTag(null, "LineString");
        xmlSerializer.startTag(null, "tessellate");
        xmlSerializer.text("1");
        xmlSerializer.endTag(null, "tessellate");
        xmlSerializer.startTag(null, "coordinates");
        xmlSerializer.text(pointString);
        xmlSerializer.endTag(null, "coordinates");
        xmlSerializer.endTag(null, "LineString");
      } else {
        xmlSerializer.startTag(null, "Polygon");
        xmlSerializer.startTag(null, "outerBoundaryIs");
        xmlSerializer.startTag(null, "LinearRing");
        xmlSerializer.startTag(null, "tessellate");
        xmlSerializer.text("1");
        xmlSerializer.endTag(null, "tessellate");
        xmlSerializer.startTag(null, "coordinates");
        xmlSerializer.text(pointString + "\n"
            + Double.toString(feature.getPoint(0).getLongitude())
            + ","
            + Double.toString(feature.getPoint(0).getLatitude())
            + ",0.000000");
        xmlSerializer.endTag(null, "coordinates");
        xmlSerializer.endTag(null, "LinearRing");
        xmlSerializer.endTag(null, "outerBoundaryIs");
        xmlSerializer.endTag(null, "Polygon");
      }
      xmlSerializer.endTag(null, "Placemark");
      xmlSerializer.flush();
      entry.setContent(writer.toString());
      Log.d("My Google Maps", "Edit URI: " + entry.getEditUri());
    } catch (IOException e) {
      e.printStackTrace();
    }
    return entry;
  }
}
