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
package com.google.android.apps.mytracks;

import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.apps.mytracks.io.AuthManager;
import com.google.android.apps.mytracks.io.AuthManager.AuthCallback;
import com.google.android.apps.mytracks.io.AuthManagerFactory;
import com.google.android.apps.mytracks.io.mymaps.MapsFacade;
import com.google.android.apps.mytracks.io.mymaps.MyMapsConstants;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Activity which displays the list of current My Maps tracks for the user.
 * Returns RESULT_OK if the user picked a map, and returns "mapid" as an extra.
 *
 * @author Rodrigo Damazio
 */
public class MyMapsList extends Activity implements MapsFacade.MapsListCallback {
  private static final int GET_LOGIN = 1;

  public static final String EXTRA_ACCOUNT_NAME = "accountName";
  public static final String EXTRA_ACCOUNT_TYPE = "accountType";

  private MapsFacade mapsClient;
  private AuthManager auth;
  private MyMapsListAdapter listAdapter;

  private final OnItemClickListener clickListener =
      new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View v, int position,
            long id) {
          Intent result = new Intent();
          result.putExtra("mapid", (String) listAdapter.getItem(position));
          setResult(RESULT_OK, result);
          finish();
        }
      };

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    auth = AuthManagerFactory.getAuthManager(this, GET_LOGIN, null, true,
        MyMapsConstants.SERVICE_NAME);

    setContentView(R.layout.list);

    listAdapter = new MyMapsListAdapter(this);

    ListView list = (ListView) findViewById(R.id.maplist);
    list.setOnItemClickListener(clickListener);
    list.setAdapter(listAdapter);

    startLogin();
  }

  private void startLogin() {
    // Starts in the UI thread.
    // TODO fix this for non-froyo devices.
    if (AuthManagerFactory.useModernAuthManager()) {
      Intent intent = getIntent();
      String accountName = intent.getStringExtra(EXTRA_ACCOUNT_NAME);
      String accountType = intent.getStringExtra(EXTRA_ACCOUNT_TYPE);
      if (accountName == null || accountType == null) {
        Log.e(TAG, "Didn't receive account name or type");
        setResult(RESULT_CANCELED);
        finish();
        return;
      }

      doLogin(auth.getAccountObject(accountName, accountType));
    } else {
      doLogin(null);
    }
  }

  private void doLogin(final Object account) {
    // Starts in the UI thread.
    auth.doLogin(new AuthCallback() {
      @Override
      public void onAuthResult(boolean success) {
        if (!success) {
          setResult(RESULT_CANCELED);
          finish();
          return;
        }

        // Runs in UI thread.
        mapsClient = new MapsFacade(MyMapsList.this, auth);

        startLookup();
      }
    }, account);
  }

  private void startLookup() {
    // Starts in the UI thread.
    new Thread() {
      @Override
      public void run() {
        // Communication with Maps happens in its own thread.
        // This will call onReceivedMapListing below.
        final boolean success = mapsClient.getMapsList(MyMapsList.this);

        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            // Updating the UI when done happens in the UI thread.
            onLookupDone(success);
          }
        });
      }
    }.start();
  }

  @Override
  public void onReceivedMapListing(final String mapId, final String title,
      final String description, final boolean isPublic) {
    // Starts in the communication thread.
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        // Updating the list with new contents happens in the UI thread.
        listAdapter.addMapListing(mapId, title, description, isPublic);
      }
    });
  }

  private void onLookupDone(boolean success) {
    // Starts in the UI thread.
    findViewById(R.id.loading).setVisibility(View.GONE);
    if (!success) {
      findViewById(R.id.failed).setVisibility(View.VISIBLE);
    }
    TextView emptyView = (TextView) findViewById(R.id.mapslist_empty);
    ListView list = (ListView) findViewById(R.id.maplist);
    list.setEmptyView(emptyView);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode,
      Intent data) {
    if (requestCode == GET_LOGIN) {
      auth.authResult(resultCode, data);
    }
    super.onActivityResult(requestCode, resultCode, data);
  }
}
