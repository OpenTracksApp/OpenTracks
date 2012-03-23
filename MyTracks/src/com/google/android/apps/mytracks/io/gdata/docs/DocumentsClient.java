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
package com.google.android.apps.mytracks.io.gdata.docs;

import com.google.wireless.gdata.client.GDataClient;
import com.google.wireless.gdata.client.GDataParserFactory;
import com.google.wireless.gdata.client.GDataServiceClient;

/**
 * GDataServiceClient for accessing Google Documents. This is not a full
 * implementation.
 */
public class DocumentsClient extends GDataServiceClient {
  /** The name of the service, dictated to be 'wise' by the protocol. */
  public static final String SERVICE = "writely";

  /**
   * Creates a new DocumentsClient.
   * 
   * @param client The GDataClient that should be used to authenticate requests,
   *        retrieve feeds, etc
   * @param parserFactory The GDataParserFactory that should be used to obtain
   *        GDataParsers used by this client
   */
  public DocumentsClient(GDataClient client, GDataParserFactory parserFactory) {
    super(client, parserFactory);
  }

  @Override
  public String getServiceName() {
    return SERVICE;
  }
}
