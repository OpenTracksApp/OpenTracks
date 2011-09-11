package com.google.android.apps.mytracks;

import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

public class MyMapsListAdapter implements ListAdapter {
  private Vector<String[]> mapsList;
  private Vector<Boolean> publicList;
  private Set<DataSetObserver> observerSet;
  private final Activity activity;

  public MyMapsListAdapter(Activity activity) {
    this.activity = activity;

    mapsList = new Vector<String[]>();
    publicList = new Vector<Boolean>();
    observerSet = new HashSet<DataSetObserver>();
  }

  public void addMapListing(String mapId, String title,
      String description, boolean isPublic) {
    synchronized (mapsList) {
      // Search through the maps list to see if it has the mapid and
      // remove it if so, so that we can replace it with updated info
      for (int i = 0; i < mapsList.size(); ++i) {
        if (mapId.equals(mapsList.get(i)[0])) {
          mapsList.remove(i);
          publicList.remove(i);
          --i;
        }
      }
      mapsList.add(new String[] { mapId, title, description });
      publicList.add(isPublic);
    }

    Iterator<DataSetObserver> iter = observerSet.iterator();
    while (iter.hasNext()) {
      iter.next().onChanged();
    }
  }
  
  public String[] getMapListingArray(int position) {
    return mapsList.get(position);
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
          activity.getLayoutInflater().inflate(R.layout.listitem, parent, false);
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
