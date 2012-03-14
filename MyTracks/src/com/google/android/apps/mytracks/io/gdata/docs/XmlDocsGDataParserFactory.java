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
package com.google.android.apps.mytracks.io.gdata.docs;

import com.google.wireless.gdata.client.GDataParserFactory;
import com.google.wireless.gdata.data.Entry;
import com.google.wireless.gdata.parser.GDataParser;
import com.google.wireless.gdata.parser.ParseException;
import com.google.wireless.gdata.parser.xml.XmlGDataParser;
import com.google.wireless.gdata.parser.xml.XmlParserFactory;
import com.google.wireless.gdata.serializer.GDataSerializer;
import com.google.wireless.gdata.serializer.xml.XmlEntryGDataSerializer;

import java.io.InputStream;

import org.xmlpull.v1.XmlPullParserException;

/**
 * Factory of Xml parsers for gdata maps data.
 */
public class XmlDocsGDataParserFactory implements GDataParserFactory {
  private XmlParserFactory xmlFactory;

  public XmlDocsGDataParserFactory(XmlParserFactory xmlFactory) {
    this.xmlFactory = xmlFactory;
  }

  @Override
  public GDataParser createParser(InputStream is) throws ParseException {
    try {
      return new XmlGDataParser(is, xmlFactory.createParser());
    } catch (XmlPullParserException e) {
      e.printStackTrace();
      return null;
    }
  }

  @SuppressWarnings("rawtypes")
  @Override
  public GDataParser createParser(Class cls, InputStream is)
      throws ParseException {
    try {
      return createParserForClass(is);
    } catch (XmlPullParserException e) {
      e.printStackTrace();
      return null;
    }
  }

  private GDataParser createParserForClass(InputStream is)
      throws ParseException, XmlPullParserException {
    return new XmlGDataParser(is, xmlFactory.createParser());
  }

  @Override
  public GDataSerializer createSerializer(Entry en) {
    return new XmlEntryGDataSerializer(xmlFactory, en);
  }
}