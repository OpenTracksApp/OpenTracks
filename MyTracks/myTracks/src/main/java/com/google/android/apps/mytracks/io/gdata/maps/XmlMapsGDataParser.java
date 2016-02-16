// Copyright 2009 Google Inc. All Rights Reserved.
package com.google.android.apps.mytracks.io.gdata.maps;

import com.google.wireless.gdata.data.Entry;
import com.google.wireless.gdata.data.Feed;
import com.google.wireless.gdata.data.XmlUtils;
import com.google.wireless.gdata.parser.ParseException;
import com.google.wireless.gdata.parser.xml.XmlGDataParser;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Parser for XML gdata maps data.
 */
class XmlMapsGDataParser extends XmlGDataParser {

  public XmlMapsGDataParser(InputStream is, XmlPullParser xpp)
      throws ParseException {
    super(is, xpp);
  }

  @Override
  protected Feed createFeed() {
    return new Feed();
  }

  @Override
  protected Entry createEntry() {
    return new MapFeatureEntry();
  }

  @Override
  protected void handleExtraElementInFeed(Feed feed) {
    // Do nothing
  }

  @Override
  protected void handleExtraLinkInEntry(
      String rel, String type, String href, Entry entry)
      throws XmlPullParserException, IOException {
    if (!(entry instanceof MapFeatureEntry)) {
      throw new IllegalArgumentException("Expected MapFeatureEntry!");
    }
    if (rel.endsWith("#view")) {
      return;
    }
    super.handleExtraLinkInEntry(rel, type, href, entry);
  }

  /**
   * Parses the current entry in the XML document. Assumes that the parser is
   * currently pointing just after an &lt;entry&gt;.
   * 
   * @param plainEntry The entry that will be filled.
   * @throws XmlPullParserException Thrown if the XML cannot be parsed.
   * @throws IOException Thrown if the underlying inputstream cannot be read.
   */
  @Override
  protected void handleEntry(Entry plainEntry)
      throws XmlPullParserException, IOException, ParseException {
    XmlPullParser parser = getParser();
    if (!(plainEntry instanceof MapFeatureEntry)) {
      throw new IllegalArgumentException("Expected MapFeatureEntry!");
    }
    MapFeatureEntry entry = (MapFeatureEntry) plainEntry;

    int eventType = parser.getEventType();
    entry.setPrivacy("public");
    while (eventType != XmlPullParser.END_DOCUMENT) {
      switch (eventType) {
        case XmlPullParser.START_TAG:
          String name = parser.getName();
          if ("entry".equals(name)) {
            // stop parsing here.
            return;
          } else if ("id".equals(name)) {
            entry.setId(XmlUtils.extractChildText(parser));
          } else if ("title".equals(name)) {
            entry.setTitle(XmlUtils.extractChildText(parser));
          } else if ("link".equals(name)) {
            String rel = parser.getAttributeValue(null /* ns */, "rel");
            String type = parser.getAttributeValue(null /* ns */, "type");
            String href = parser.getAttributeValue(null /* ns */, "href");
            if ("edit".equals(rel)) {
              entry.setEditUri(href);
            } else if ("alternate".equals(rel) && "text/html".equals(type)) {
              entry.setHtmlUri(href);
            } else {
              handleExtraLinkInEntry(rel, type, href, entry);
            }
          } else if ("summary".equals(name)) {
            entry.setSummary(XmlUtils.extractChildText(parser));
          } else if ("content".equals(name)) {
            StringBuilder contentBuilder = new StringBuilder();
            int parentDepth = parser.getDepth();
            while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
              int etype = parser.next();
              switch (etype) {
                case XmlPullParser.START_TAG:
                  contentBuilder.append('<');
                  contentBuilder.append(parser.getName());
                  contentBuilder.append('>');
                  break;
                case XmlPullParser.TEXT:
                  contentBuilder.append("<![CDATA[");
                  contentBuilder.append(parser.getText());
                  contentBuilder.append("]]>");
                  break;
                case XmlPullParser.END_TAG:
                  if (parser.getDepth() > parentDepth) {
                    contentBuilder.append("</");
                    contentBuilder.append(parser.getName());
                    contentBuilder.append('>');
                  }
                  break;
              }
              if (etype == XmlPullParser.END_TAG
                  && parser.getDepth() == parentDepth) {
                break;
              }
            }
            entry.setContent(contentBuilder.toString());
          } else if ("category".equals(name)) {
            String category = parser.getAttributeValue(null /* ns */, "term");
            if (category != null && category.length() > 0) {
              entry.setCategory(category);
            }
            String categoryScheme =
                parser.getAttributeValue(null /* ns */, "scheme");
            if (categoryScheme != null && category.length() > 0) {
              entry.setCategoryScheme(categoryScheme);
            }
          } else if ("published".equals(name)) {
            entry.setPublicationDate(XmlUtils.extractChildText(parser));
          } else if ("updated".equals(name)) {
            entry.setUpdateDate(XmlUtils.extractChildText(parser));
          } else if ("deleted".equals(name)) {
            entry.setDeleted(true);
          } else if ("draft".equals(name)) {
            String draft = XmlUtils.extractChildText(parser);
            entry.setPrivacy("yes".equals(draft) ? "unlisted" : "public");
          } else if ("customProperty".equals(name)) {
            String attrName = parser.getAttributeValue(null, "name");
            String attrValue = XmlUtils.extractChildText(parser);
            entry.setAttribute(attrName, attrValue);
          } else if ("deleted".equals(name)) {
            entry.setDeleted(true);
          } else {
            handleExtraElementInEntry(entry);
          }
          break;
        default:
          break;
      }

      eventType = parser.next();
    }
  }
}
