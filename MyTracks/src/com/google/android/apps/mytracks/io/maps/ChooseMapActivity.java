/*
 * Copyright 2012 Google Inc.
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
package com.google.android.apps.mytracks.io.maps;

import com.google.android.apps.mytracks.io.gdata.maps.MapsMapMetadata;
import com.google.android.apps.mytracks.io.sendtogoogle.SendRequest;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * An activity to choose a Google Map.
 *
 * @author Jimmy Shih
 */
public class ChooseMapActivity extends Activity {

  private static final int PROGRESS_DIALOG = 1;
  private static final int ERROR_DIALOG = 2;
  
  private SendRequest sendRequest;
  private ChooseMapAsyncTask asyncTask;
  private ProgressDialog progressDialog;
  private ArrayAdapter<ListItem> arrayAdapter;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    sendRequest = getIntent().getParcelableExtra(SendRequest.SEND_REQUEST_KEY);
    setContentView(R.layout.choose_map);

    arrayAdapter = new ArrayAdapter<ListItem>(this, R.layout.choose_map_item, new ArrayList<
        ListItem>()) {
      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
          convertView = getLayoutInflater().inflate(R.layout.choose_map_item, parent, false);
        }
        MapsMapMetadata mapData = getItem(position).getMapData();
        
        TextView title = (TextView) convertView.findViewById(R.id.choose_map_list_item_title);
        title.setText(mapData.getTitle());
        
        TextView description = (TextView) convertView.findViewById(
            R.id.choose_map_list_item_description);
        String descriptionText = mapData.getDescription();
        if (descriptionText == null || descriptionText.equals("")) {
          description.setVisibility(View.GONE);
        } else {
          description.setVisibility(View.VISIBLE);
          description.setText(descriptionText);
        }

        TextView searchStatus = (TextView) convertView.findViewById(
            R.id.choose_map_list_item_search_status);
        searchStatus.setTextColor(mapData.getSearchable() ? Color.RED : Color.GREEN);
        searchStatus.setText(mapData.getSearchable() ? R.string.maps_list_public_label
            : R.string.maps_list_unlisted_label);
        return convertView;
      }
    };

    ListView list = (ListView) findViewById(R.id.choose_map_list_view);
    list.setEmptyView(findViewById(R.id.choose_map_empty_view));
    list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        startNextActivity(arrayAdapter.getItem(position).getMapId());
      }
    });
    list.setAdapter(arrayAdapter);

    Object retained = getLastNonConfigurationInstance();
    if (retained instanceof ChooseMapAsyncTask) {
      asyncTask = (ChooseMapAsyncTask) retained;
      asyncTask.setActivity(this);
    } else {
      asyncTask = new ChooseMapAsyncTask(this, sendRequest.getAccount());
      asyncTask.execute();
    }
  }

  @Override
  public Object onRetainNonConfigurationInstance() {
    asyncTask.setActivity(null);
    return asyncTask;
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case PROGRESS_DIALOG:
        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(getString(R.string.maps_list_loading));
        progressDialog.setCancelable(true);
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {
            asyncTask.cancel(true);
            finish();
          }
        });
        progressDialog.setIcon(android.R.drawable.ic_dialog_info);
        progressDialog.setTitle(R.string.generic_progress_title);
        return progressDialog;
      case ERROR_DIALOG:
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setTitle(R.string.generic_error_title);
        builder.setMessage(R.string.maps_list_error);
        builder.setPositiveButton(R.string.generic_ok, new OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int arg1) {
            finish();
          }
        });
        builder.setOnCancelListener(new OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {
            finish();
          }
        });
        return builder.create();
      default:
        return null;
    }
  }

  /**
   * Invokes when the associated AsyncTask completes.
   *
   * @param success true if success
   * @param mapIds an array of map ids
   * @param mapData an array of map data
   */
  public void onAsyncTaskCompleted(
      boolean success, ArrayList<String> mapIds, ArrayList<MapsMapMetadata> mapData) {
    removeDialog(PROGRESS_DIALOG);
    if (success) {
      arrayAdapter.clear();
      // To prevent displaying the emptyView message momentarily before the
      // arrayAdapter is set, don't set the emptyView message in the xml layout.
      // Instead, set it only when needed.
      if (mapIds.size() == 0) {
        TextView emptyView = (TextView) findViewById(R.id.choose_map_empty_view);
        emptyView.setText(R.string.maps_list_no_maps);
      } else {
        for (int i = 0; i < mapIds.size(); i++) {
          arrayAdapter.add(new ListItem(mapIds.get(i), mapData.get(i)));
        }
      }
    } else {
      showDialog(ERROR_DIALOG);
    }
  }

  /**
   * Shows the progress dialog.
   */
  public void showProgressDialog() {
    showDialog(PROGRESS_DIALOG);
  }

  /**
   * Starts the next activity, {@link SendMapsActivity}.
   * 
   * @param mapId the chosen map id
   */
  private void startNextActivity(String mapId) {
    sendRequest.setMapId(mapId);
    Intent intent = new Intent(this, SendMapsActivity.class)
        .putExtra(SendRequest.SEND_REQUEST_KEY, sendRequest);
    startActivity(intent);
    finish();
  }

  /**
   * A class containing {@link ChooseMapActivity} list item.
   *
   * @author Jimmy Shih
   */
  private class ListItem {
    private String mapId;
    private MapsMapMetadata mapData;

    private ListItem(String mapId, MapsMapMetadata mapData) {
      this.mapId = mapId;
      this.mapData = mapData;
    }

    /**
     * Gets the map id.
     */
    public String getMapId() {
      return mapId;
    }

    /**
     * Gets the map data.
     */
    public MapsMapMetadata getMapData() {
      return mapData;
    }
  }
}
