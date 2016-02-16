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
package com.google.android.apps.mytracks.io.gdata;

import com.google.wireless.gdata.client.QueryParams;

import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Simple implementation of the QueryParams interface.
 */
// TODO: deal with categories
public class QueryParamsImpl extends QueryParams {

  private final Map<String, String> mParams = new HashMap<String, String>();

  @Override
  public void clear() {
    setEntryId(null);
    mParams.clear();
  }

  @Override
  public String generateQueryUrl(String feedUrl) {

    if (TextUtils.isEmpty(getEntryId()) && mParams.isEmpty()) {
      // nothing to do
      return feedUrl;
    }

    // handle entry IDs
    if (!TextUtils.isEmpty(getEntryId())) {
      if (!mParams.isEmpty()) {
        throw new IllegalStateException("Cannot set both an entry ID "
            + "and other query paramters.");
      }
      return feedUrl + '/' + getEntryId();
    }

    // otherwise, append the querystring params.
    StringBuilder sb = new StringBuilder();
    sb.append(feedUrl);
    Set<String> params = mParams.keySet();
    boolean first = true;
    if (feedUrl.contains("?")) {
      first = false;
    } else {
      sb.append('?');
    }
    for (String param : params) {
      if (first) {
        first = false;
      } else {
        sb.append('&');
      }
      sb.append(param);
      sb.append('=');
      String value = mParams.get(param);
      String encodedValue = null;

      try {
        encodedValue = URLEncoder.encode(value, "UTF-8");
      } catch (UnsupportedEncodingException uee) {
        // Should not happen
        throw new IllegalStateException("Cannot encode " + value, uee);
      }
      sb.append(encodedValue);
    }
    return sb.toString();
  }

  @Override
  public String getParamValue(String param) {
    if (!(mParams.containsKey(param))) {
      return null;
    }
    return mParams.get(param);
  }

  @Override
  public void setParamValue(String param, String value) {
    mParams.put(param, value);
  }
}
