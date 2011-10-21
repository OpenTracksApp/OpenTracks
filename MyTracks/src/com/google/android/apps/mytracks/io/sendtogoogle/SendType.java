/*
 * Copyright 2011 Google Inc.
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
package com.google.android.apps.mytracks.io.sendtogoogle;

import com.google.android.maps.mytracks.R;

/**
 * Enumerates the services to which we can upload track data.
 *
 * @author Matthew Simmons
 */
public enum SendType {
  MYMAPS(R.string.sendtogoogle_google_mymaps,
      R.string.sendtogoogle_google_mymaps_url),
  FUSION_TABLES(R.string.sendtogoogle_google_fusiontables,
      R.string.sendtogoogle_google_fusiontables_url),
  DOCS(R.string.sendtogoogle_google_docs,
      R.string.sendtogoogle_google_docs_url);

  private int serviceName;
  private int serviceUrl;

  private SendType(int serviceName, int serviceUrl) {
    this.serviceName = serviceName;
    this.serviceUrl = serviceUrl;
  }

  /** Returns the resource ID for the printable (short) name of the service */
  public int getServiceName() {
    return serviceName;
  }

  /** Returns the resource ID for the service's URL */
  public int getServiceUrl() {
    return serviceUrl;
  }
}
