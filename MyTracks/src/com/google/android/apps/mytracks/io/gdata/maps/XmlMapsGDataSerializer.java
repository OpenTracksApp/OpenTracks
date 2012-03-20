// Copyright 2010 Google Inc. All Rights Reserved.
package com.google.android.apps.mytracks.io.gdata.maps;

import com.google.wireless.gdata.data.StringUtils;
import com.google.wireless.gdata.parser.ParseException;
import com.google.wireless.gdata.parser.xml.XmlGDataParser;
import com.google.wireless.gdata.parser.xml.XmlParserFactory;
import com.google.wireless.gdata.serializer.xml.XmlEntryGDataSerializer;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

/**
 * Serializer of maps data for GData.
 */
class XmlMapsGDataSerializer extends XmlEntryGDataSerializer {

  private static final String APP_NAMESPACE = "http://www.w3.org/2007/app";

  private MapFeatureEntry entry;
  private XmlParserFactory factory;
  private OutputStream stream;

  public XmlMapsGDataSerializer(XmlParserFactory factory, MapFeatureEntry entry) {
    super(factory, entry);

    this.factory = factory;
    this.entry = entry;
  }

  @Override
  public void serialize(OutputStream out, int format)
      throws IOException, ParseException {
    XmlSerializer serializer = null;
    try {
      serializer = factory.createSerializer();
    } catch (XmlPullParserException e) {
      throw new ParseException("Unable to create XmlSerializer.", e);
    }

    ByteArrayOutputStream printStream;

    if (MapsClient.LOG_COMMUNICATION) {
      printStream = new ByteArrayOutputStream();

      serializer.setOutput(printStream, "UTF-8");
    } else {
      serializer.setOutput(out, "UTF-8");
    }

    serializer.startDocument("UTF-8", Boolean.FALSE);

    declareEntryNamespaces(serializer);
    serializer.startTag(XmlGDataParser.NAMESPACE_ATOM_URI, "entry");

    if (MapsClient.LOG_COMMUNICATION) {
      stream = printStream;
    } else {
      stream = out;
    }
    serializeEntryContents(serializer, format);

    serializer.endTag(XmlGDataParser.NAMESPACE_ATOM_URI, "entry");
    serializer.endDocument();
    serializer.flush();

    if (MapsClient.LOG_COMMUNICATION) {
      Log.d("Request", printStream.toString());
      out.write(printStream.toByteArray());
      stream = out;
    }
  }

  private final void declareEntryNamespaces(XmlSerializer serializer)
      throws IOException {
    serializer.setPrefix(
        "" /* default ns */, XmlGDataParser.NAMESPACE_ATOM_URI);
    serializer.setPrefix(
        XmlGDataParser.NAMESPACE_GD, XmlGDataParser.NAMESPACE_GD_URI);
    declareExtraEntryNamespaces(serializer);
  }

  private final void serializeEntryContents(XmlSerializer serializer,
      int format) throws IOException {
    if (format != FORMAT_CREATE) {
      serializeId(serializer, entry.getId());
    }

    serializeTitle(serializer, entry.getTitle());

    if (format != FORMAT_CREATE) {
      serializeLink(serializer,
          "edit" /* rel */, entry.getEditUri(), null /* type */);
      serializeLink(serializer,
          "alternate" /* rel */, entry.getHtmlUri(), "text/html" /* type */);
    }

    serializeSummary(serializer, entry.getSummary());
    serializeContent(serializer, entry.getContent());
    serializeAuthor(serializer, entry.getAuthor(), entry.getEmail());
    serializeCategory(serializer,
        entry.getCategory(), entry.getCategoryScheme());

    if (format == FORMAT_FULL) {
      serializePublicationDate(serializer, entry.getPublicationDate());
    }

    if (format != FORMAT_CREATE) {
      serializeUpdateDate(serializer, entry.getUpdateDate());
    }

    serializeExtraEntryContents(serializer, format);
  }

  private static void serializeId(XmlSerializer serializer, String id)
      throws IOException {
    if (StringUtils.isEmpty(id)) {
      return;
    }
    serializer.startTag(null /* ns */, "id");
    serializer.text(id);
    serializer.endTag(null /* ns */, "id");
  }

  private static void serializeTitle(XmlSerializer serializer, String title)
      throws IOException {
    if (StringUtils.isEmpty(title)) {
      return;
    }
    serializer.startTag(null /* ns */, "title");
    serializer.text(title);
    serializer.endTag(null /* ns */, "title");
  }

  public static void serializeLink(XmlSerializer serializer, String rel,
      String href, String type) throws IOException {
    if (StringUtils.isEmpty(href)) {
      return;
    }
    serializer.startTag(null /* ns */, "link");
    serializer.attribute(null /* ns */, "rel", rel);
    serializer.attribute(null /* ns */, "href", href);
    if (!StringUtils.isEmpty(type)) {
      serializer.attribute(null /* ns */, "type", type);
    }
    serializer.endTag(null /* ns */, "link");
  }

  private static void serializeSummary(XmlSerializer serializer, String summary)
      throws IOException {
    if (StringUtils.isEmpty(summary)) {
      return;
    }
    serializer.startTag(null /* ns */, "summary");
    serializer.text(summary);
    serializer.endTag(null /* ns */, "summary");
  }

  private void serializeContent(XmlSerializer serializer, String content)
      throws IOException {
    if (content == null) {
      return;
    }
    serializer.startTag(null /* ns */, "content");
    if (content.contains("</Placemark>")) {
      serializer.attribute(
          null /* ns */, "type", "application/vnd.google-earth.kml+xml");
      serializer.flush();
      stream.write(content.getBytes());
    } else {
      serializer.text(content);
    }
    serializer.endTag(null /* ns */, "content");
  }

  private static void serializeAuthor(XmlSerializer serializer, String author,
      String email) throws IOException {
    if (StringUtils.isEmpty(author) || StringUtils.isEmpty(email)) {
      return;
    }
    serializer.startTag(null /* ns */, "author");
    serializer.startTag(null /* ns */, "name");
    serializer.text(author);
    serializer.endTag(null /* ns */, "name");
    serializer.startTag(null /* ns */, "email");
    serializer.text(email);
    serializer.endTag(null /* ns */, "email");
    serializer.endTag(null /* ns */, "author");
  }

  private static void serializeCategory(XmlSerializer serializer,
      String category, String categoryScheme) throws IOException {
    if (StringUtils.isEmpty(category) && StringUtils.isEmpty(categoryScheme)) {
      return;
    }
    serializer.startTag(null /* ns */, "category");
    if (!StringUtils.isEmpty(category)) {
      serializer.attribute(null /* ns */, "term", category);
    }
    if (!StringUtils.isEmpty(categoryScheme)) {
      serializer.attribute(null /* ns */, "scheme", categoryScheme);
    }
    serializer.endTag(null /* ns */, "category");
  }

  private static void serializePublicationDate(XmlSerializer serializer,
      String publicationDate) throws IOException {
    if (StringUtils.isEmpty(publicationDate)) {
      return;
    }
    serializer.startTag(null /* ns */, "published");
    serializer.text(publicationDate);
    serializer.endTag(null /* ns */, "published");
  }

  private static void serializeUpdateDate(XmlSerializer serializer,
      String updateDate) throws IOException {
    if (StringUtils.isEmpty(updateDate)) {
      return;
    }
    serializer.startTag(null /* ns */, "updated");
    serializer.text(updateDate);
    serializer.endTag(null /* ns */, "updated");
  }

  @Override
  protected void serializeExtraEntryContents(XmlSerializer serializer,
      int format) throws IOException {
    Map<String, String> attrs = entry.getAllAttributes();
    for (Map.Entry<String, String> attr : attrs.entrySet()) {
      serializer.startTag("http://schemas.google.com/g/2005", "customProperty");
      serializer.attribute(null, "name", attr.getKey());
      serializer.text(attr.getValue());
      serializer.endTag("http://schemas.google.com/g/2005", "customProperty");
    }
    String privacy = entry.getPrivacy();
    if (!StringUtils.isEmpty(privacy)) {
      serializer.setPrefix("app", APP_NAMESPACE);
      if ("public".equals(privacy)) {
        serializer.startTag(APP_NAMESPACE, "control");
        serializer.startTag(APP_NAMESPACE, "draft");
        serializer.text("no");
        serializer.endTag(APP_NAMESPACE, "draft");
        serializer.endTag(APP_NAMESPACE, "control");
      }
      if ("unlisted".equals(privacy)) {
        serializer.startTag(APP_NAMESPACE, "control");
        serializer.startTag(APP_NAMESPACE, "draft");
        serializer.text("yes");
        serializer.endTag(APP_NAMESPACE, "draft");
        serializer.endTag(APP_NAMESPACE, "control");
      }
    }
  }
}
