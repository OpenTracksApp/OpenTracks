// Copyright 2010 Google Inc. All Rights Reserved.
package com.google.android.apps.mytracks.io.file;

import com.google.android.apps.mytracks.content.MyTracksLocation;
import com.google.android.apps.mytracks.content.Sensor;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint;

import android.test.AndroidTestCase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Base class for track format writer tests, which sets up a fake track and
 * gives auxiliary methods for verifying XML output.
 *
 * @author Rodrigo Damazio
 */
public abstract class TrackFormatWriterTest extends AndroidTestCase {

  // All the user-provided strings have "]]>" to ensure that proper escaping is
  // being done.
  protected static final String TRACK_NAME = "Home]]>";
  protected static final String TRACK_CATEGORY = "Hiking";
  protected static final String TRACK_DESCRIPTION = "The long ]]> journey home";
  protected static final String WAYPOINT1_NAME = "point]]>1";
  protected static final String WAYPOINT1_CATEGORY = "Statistics";
  protected static final String WAYPOINT1_DESCRIPTION = "point 1]]>description";
  protected static final String WAYPOINT2_NAME = "point]]>2";
  protected static final String WAYPOINT2_CATEGORY = "Waypoint";
  protected static final String WAYPOINT2_DESCRIPTION = "point 2]]>description";
  private static final int BUFFER_SIZE = 10240;
  protected Track track;
  protected MyTracksLocation location1, location2, location3, location4;
  protected Waypoint wp1, wp2;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    track = new Track();
    track.setName(TRACK_NAME);
    track.setCategory(TRACK_CATEGORY);
    track.setDescription(TRACK_DESCRIPTION);

    location1 = new MyTracksLocation("mock");
    location2 = new MyTracksLocation("mock");
    location3 = new MyTracksLocation("mock");
    location4 = new MyTracksLocation("mock");
    populateLocations(location1, location2, location3, location4);

    wp1 = new Waypoint();
    wp2 = new Waypoint();
    wp1.setLocation(location2);
    wp1.setName(WAYPOINT1_NAME);
    wp1.setCategory(WAYPOINT1_CATEGORY);
    wp1.setDescription(WAYPOINT1_DESCRIPTION);
    wp2.setLocation(location3);
    wp2.setName(WAYPOINT2_NAME);
    wp2.setCategory(WAYPOINT2_CATEGORY);
    wp2.setDescription(WAYPOINT2_DESCRIPTION);
  }

  /**
   * Populates a list of locations with values.
   *
   * @param locations a list of locations
   */
  private void populateLocations(MyTracksLocation... locations) {
    for (int i = 0; i < locations.length; i++) {
      MyTracksLocation location = locations[i];
      location.setLatitude(i);
      location.setLongitude(-i);
      location.setAltitude(i * 10);
      location.setBearing(i * 100);
      location.setAccuracy(i * 1000);
      location.setSpeed(i * 10000);
      location.setTime(i * 100000);
      Sensor.SensorData.Builder power = Sensor.SensorData.newBuilder().setValue(100 + i)
          .setState(Sensor.SensorState.SENDING);
      Sensor.SensorData.Builder cadence = Sensor.SensorData.newBuilder().setValue(200 + i)
          .setState(Sensor.SensorState.SENDING);
      Sensor.SensorData.Builder heartRate = Sensor.SensorData.newBuilder().setValue(300 + i)
          .setState(Sensor.SensorState.SENDING);     
      Sensor.SensorDataSet sensorDataSet = Sensor.SensorDataSet.newBuilder().setPower(power)
          .setCadence(cadence).setHeartRate(heartRate).build();
      location.setSensorDataSet(sensorDataSet);
    }
  }

  /**
   * Makes the right sequence of calls to the writer in order to write the fake
   * track in {@link #track}.
   *
   * @param writer the writer to write to
   * @return the written contents
   */
  protected String writeTrack(TrackFormatWriter writer) throws Exception {
    OutputStream output = new ByteArrayOutputStream(BUFFER_SIZE);
    writer.prepare(output);
    writer.writeHeader(track);
    writer.writeBeginWaypoints();
    writer.writeWaypoint(wp1);
    writer.writeWaypoint(wp2);
    writer.writeEndWaypoints();
    writer.writeBeginTrack(track, location1);
    writer.writeOpenSegment();
    writer.writeLocation(location1);
    writer.writeLocation(location2);
    writer.writeCloseSegment();
    writer.writeOpenSegment();
    writer.writeLocation(location3);
    writer.writeLocation(location4);
    writer.writeCloseSegment();
    writer.writeEndTrack(track, location4);
    writer.writeFooter();
    writer.close();
    return output.toString();
  }

  /**
   * Gets the text data contained inside a tag.
   *
   * @param parent the parent of the tag containing the text
   * @param elementName the name of the tag containing the text
   * @return the text contents
   */
  protected String getChildTextValue(Element parent, String elementName) {
    Element child = getChildElement(parent, elementName);
    assertTrue(child.hasChildNodes());
    NodeList children = child.getChildNodes();
    int length = children.getLength();
    assertTrue(length > 0);

    // The children may be a sucession of text elements, just concatenate them
    String result = "";
    for (int i = 0; i < length; i++) {
      Text textNode = (Text) children.item(i);
      result += textNode.getNodeValue();
    }
    return result;
  }

  /**
   * Returns all child elements of a given parent which have the given name.
   *
   * @param parent the parent to get children from
   * @param elementName the element name to look for
   * @param expectedChildren the number of children we're expected to find
   * @return a list of the found elements
   */
  protected List<Element> getChildElements(Node parent, String elementName,
      int expectedChildren) {
    assertTrue(parent.hasChildNodes());
    NodeList children = parent.getChildNodes();
    int length = children.getLength();
    List<Element> result = new ArrayList<Element>();
    for (int i = 0; i < length; i++) {
      Node childNode = children.item(i);
      if (childNode.getNodeType() == Node.ELEMENT_NODE
          && childNode.getNodeName().equalsIgnoreCase(elementName)) {
        result.add((Element) childNode);
      }
    }
    assertTrue(children.toString(), result.size() == expectedChildren);
    return result;
  }

  /**
   * Returns the single child element of the given parent with the given type.
   *
   * @param parent the parent to get a child from
   * @param elementName the name of the child to look for
   * @return the child element
   */
  protected Element getChildElement(Node parent, String elementName) {
    return getChildElements(parent, elementName, 1).get(0);
  }

  /**
   * Parses the given XML contents and returns a DOM {@link Document} for it.
   */
  protected Document parseXmlDocument(String contents)
      throws FactoryConfigurationError, ParserConfigurationException,
          SAXException, IOException {
    DocumentBuilderFactory builderFactory =
        DocumentBuilderFactory.newInstance();
    builderFactory.setCoalescing(true);
    // TODO: Somehow do XML validation on Android
    // builderFactory.setValidating(true);
    builderFactory.setNamespaceAware(true);
    builderFactory.setIgnoringComments(true);
    builderFactory.setIgnoringElementContentWhitespace(true);
    DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
    Document doc = documentBuilder.parse(
        new InputSource(new StringReader(contents)));
    return doc;
  }
}
