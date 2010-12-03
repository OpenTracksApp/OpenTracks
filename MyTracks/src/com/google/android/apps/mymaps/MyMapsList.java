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
package com.google.android.apps.mymaps;

import com.google.android.accounts.Account;
import com.google.android.apps.mymaps.MyMapsGDataWrapper.QueryFunction;
import com.google.android.apps.mytracks.AccountChooser;
import com.google.android.apps.mytracks.MyTracks;
import com.google.android.apps.mytracks.io.AuthManager;
import com.google.android.apps.mytracks.io.AuthManagerFactory;
import com.google.android.maps.mytracks.R;
import com.google.wireless.gdata.maps.MapFeatureEntry;
import com.google.wireless.gdata.maps.MapsClient;
import com.google.wireless.gdata.parser.GDataParser;

import android.app.Activity;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

/**
 * Activity which displays the list of "my maps" maps.
 */
public class MyMapsList extends Activity implements ListAdapter {

  private static final int MENU_OPEN = 0;
  private static final int MENU_SHARE = 2;
  private static final int GET_LOGIN = 1;

  private LookupThread listLookup;
  private Vector<String[]> mapsList;
  private Vector<Boolean> publicList;
  private Set<DataSetObserver> observerSet;
  private Handler loadHandler;
  private int contextPosition;
  private AuthManager auth;

  /**
   * Thread which looks up the maps list.
   */
  private class LookupThread extends Thread {
    private boolean finished = false;
    private boolean success = true;

    private GDataParser listParser;

    @Override
    public void run() {
      finished = false;
      listParser = null;
      MyMapsGDataWrapper wrapper = new MyMapsGDataWrapper(MyMapsList.this);
      wrapper.setAuthManager(auth);
      wrapper.setRetryOnAuthFailure(true);

      success = wrapper.runQuery(new QueryFunction() {
        public void query(MapsClient client) throws IOException, Exception {
          listParser = client.getParserForFeed(
              MapFeatureEntry.class, MapsClient.getMapsFeed(),
              auth.getAuthToken());
          listParser.init();
          while (listParser.hasMoreData()) {
            MapFeatureEntry entry =
                (MapFeatureEntry) listParser.readNextEntry(null);
            MyMapsMapMetadata metadata =
                MyMapsGDataConverter.getMapMetadataForEntry(entry);
            String mapid = MyMapsGDataConverter.getMapidForEntry(entry);
            synchronized (mapsList) {
              // Search through the maps list to see if it has the mapid and
              // remove it if so, so that we can replace it with updated info
              for (int i = 0; i < mapsList.size(); ++i) {
                if (mapid.equals(mapsList.get(i)[0])) {
                  mapsList.remove(i);
                  publicList.remove(i);
                  --i;
                }
              }
              mapsList.add(new String[] {
                  mapid, metadata.getTitle(), metadata.getDescription() });
              publicList.add(metadata.getSearchable());
            }
          }
          listParser.close();
          listParser = null;
          success = true;
          finished = true;
        }
      });

      if (listParser != null) {
        listParser.close();
      }
      wrapper.cleanUp();
      if (success || wrapper.getErrorType() != MyMapsGDataWrapper.ERROR_AUTH) {
        finished = true;
      }
    }

    public boolean isFinished() {
      return finished;
    }

    public boolean succeeded() {
      return success;
    }
  }

  private final Runnable checkLoaded = new Runnable() {
    @Override
    public void run() {
      if (listLookup.isFinished()) {
        findViewById(R.id.loading).setVisibility(View.GONE);
        if (!listLookup.succeeded()) {
          findViewById(R.id.failed).setVisibility(View.VISIBLE);
        }
        TextView emptyView = (TextView) findViewById(R.id.mapslist_empty);
        ListView list = (ListView) findViewById(R.id.maplist);
        list.setEmptyView(emptyView);
      } else {
        loadHandler.postDelayed(checkLoaded, 500);
      }
      Iterator<DataSetObserver> iter = observerSet.iterator();
      while (iter.hasNext()) {
        iter.next().onChanged();
      }
    }
  };

  private final OnItemClickListener clickListener = new OnItemClickListener() {
    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position,
        long id) {
      Intent result = new Intent();
      result.putExtra("mapid", (String) getItem(position));
      MyMapsList.this.setResult(RESULT_OK, result);
      finish();
    }
  };

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

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    auth = AuthManagerFactory.getAuthManager(this, GET_LOGIN, null, true,
        MapsClient.SERVICE);

    setContentView(R.layout.list);

    ListView list = (ListView) findViewById(R.id.maplist);
    list.setOnItemClickListener(clickListener);
    list.setOnCreateContextMenuListener(contextMenuListener);

    mapsList = new Vector<String[]>();
    publicList = new Vector<Boolean>();

    listLookup = new LookupThread();
    // TODO fix this for non-froyo devices.
    if (AuthManagerFactory.useModernAuthManager()) {
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          MyTracks.getInstance().getAccountChooser().chooseAccount(
              MyMapsList.this,
              new AccountChooser.AccountHandler() {
                @Override
                public void handleAccountSelected(Account account) {
                  if (account != null) {
                    // The user did not quit and there was a valid google
                    // account.
                    doLogin(account);
                  }
                }
              });
        }
      });
    } else {
      doLogin(null);
    }
  
    observerSet = new HashSet<DataSetObserver>();

    list.setAdapter(this);

    loadHandler = new Handler();
    loadHandler.postDelayed(checkLoaded, 500);
  }

  private void doLogin(final Account account) {
    auth.doLogin(new Runnable() {
      public void run() {
        listLookup.start();
      }
    }, account);
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
        shareIntent.putExtra(Intent.EXTRA_TEXT, String.format(
            getText(R.string.share_map_body_format).toString(),
            mapsList.get(contextPosition)[1],
            MyMapsConstants.MAPSHOP_BASE_URL + "?msa=0&msid="
                + mapsList.get(contextPosition)[0]));
        startActivity(Intent.createChooser(shareIntent,
            getText(R.string.share_map).toString()));
        return true;
    }
    return false;
  }

  @Override
  public boolean areAllItemsEnabled() {
    return true;
  }

  @Override
  public boolean isEnabled(int position) {
    return true;
  }

  @Override
  public int getCount() {
    return mapsList.size();
  }

  @Override
  public Object getItem(int position) {
    return mapsList.get(position)[0];
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public int getItemViewType(int position) {
    return 0;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    if (convertView == null) {
      convertView =
          getLayoutInflater().inflate(R.layout.listitem, parent, false);
    }

    String[] map = mapsList.get(position);
    ((TextView) convertView.findViewById(R.id.maplistitem)).setText(map[1]);
    ((TextView) convertView.findViewById(R.id.maplistdesc)).setText(map[2]);
    TextView publicUnlisted =
      (TextView) convertView.findViewById(R.id.maplistpublic);
    if (publicList.get(position)) {
      publicUnlisted.setTextColor(Color.RED);
      publicUnlisted.setText(R.string.public_map);
    } else {
      publicUnlisted.setTextColor(Color.GREEN);
      publicUnlisted.setText(R.string.unlisted_map);
    }

    return convertView;
  }

  @Override
  public int getViewTypeCount() {
    return 1;
  }

  @Override
  public boolean hasStableIds() {
    return false;
  }

  @Override
  public boolean isEmpty() {
    return mapsList.isEmpty();
  }

  @Override
  public void registerDataSetObserver(DataSetObserver observer) {
    observerSet.add(observer);
  }

  @Override
  public void unregisterDataSetObserver(DataSetObserver observer) {
    observerSet.remove(observer);
  }
}
