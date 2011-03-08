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

import com.google.android.accounts.Account;
import com.google.android.apps.mytracks.io.AuthManager;
import com.google.android.apps.mytracks.io.AuthManagerFactory;
import com.google.android.apps.mytracks.io.MyMapsFactory;
import com.google.android.apps.mytracks.io.mymaps.MapsFacade;
import com.google.android.apps.mytracks.io.mymaps.MapsService;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
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
  private static final int MENU_OPEN = 0;
  private static final int MENU_SHARE = 2;
  private static final int GET_LOGIN = 1;

  private MapsFacade mapsClient;
  private AuthManager auth;
  private MyMapsListAdapter listAdapter;

  private int contextPosition;

  private final OnCreateContextMenuListener contextMenuListener =
      new OnCreateContextMenuListener() {
        @Override
        public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
          AdapterView.AdapterContextMenuInfo info =
              (AdapterView.AdapterContextMenuInfo) menuInfo;
          contextPosition = info.position;
          menu.add(0, MENU_OPEN, 0, R.string.open_map);
          menu.add(0, MENU_SHARE, 0, R.string.share_map);
        }
      };

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
        MapsService.getServiceName());

    setContentView(R.layout.list);

    listAdapter = new MyMapsListAdapter(this);

    ListView list = (ListView) findViewById(R.id.maplist);
    list.setOnItemClickListener(clickListener);
    list.setOnCreateContextMenuListener(contextMenuListener);
    list.setAdapter(listAdapter);

    startLogin();
  }

  private void startLogin() {
    // Starts in the UI thread.
    // TODO fix this for non-froyo devices.
    if (AuthManagerFactory.useModernAuthManager()) {
      MyTracks.getInstance().getAccountChooser().chooseAccount(
          MyMapsList.this,
          new AccountChooser.AccountHandler() {
            @Override
            public void handleAccountSelected(Account account) {
              // Account selection happens in the UI thread.
              if (account != null) {
                // The user did not quit and there was a valid google
                // account.
                doLogin(account);
              }
            }
          });
    } else {
      doLogin(null);
    }
  }

  private void doLogin(final Account account) {
    // Starts in the UI thread.
    auth.doLogin(new Runnable() {
      public void run() {
        // Runs in UI thread.
        mapsClient = MyMapsFactory.newMapsClient(MyMapsList.this, auth);

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

  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    switch (item.getItemId()) {
      case MENU_OPEN:
        clickListener.onItemClick(null, null, contextPosition, 0);
        return true;
      case MENU_SHARE:
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT,
            getText(R.string.share_map_subject));
        String[] listItem = (String[]) listAdapter.getMapListingArray(contextPosition);
        shareIntent.putExtra(Intent.EXTRA_TEXT, String.format(
            getText(R.string.share_map_body_format).toString(),
            listItem[1],
            MapsService.buildMapUrl(listItem[0])));
        startActivity(Intent.createChooser(shareIntent,
            getText(R.string.share_map).toString()));
        return true;
    }
    return false;
  }
}
