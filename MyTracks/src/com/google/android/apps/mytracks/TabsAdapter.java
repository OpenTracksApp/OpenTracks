/*
 * Copyright 2013q Google Inc.
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

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabWidget;

import java.util.ArrayList;

/**
 * This is a helper class that implements the management of tabs and all details
 * of connecting a ViewPager with associated TabHost. It relies on a trick.
 * Normally a tab host has a simple API for supplying a View or Intent that each
 * tab will show. This is not sufficient for switching between pages. So instead
 * we make the content part of the tab host 0dp high (it is not shown) and the
 * TabsAdapter supplies its own dummy view to show as the tab content. It
 * listens to changes in tabs, and takes care of switch to the correct paged in
 * the ViewPager whenever the selected tab changes.
 * <p>
 * Copied from the FragmentTabsPager sample in the support library.
 * 
 * @author Jimmy Shih
 */
public class TabsAdapter extends FragmentPagerAdapter
    implements TabHost.OnTabChangeListener, ViewPager.OnPageChangeListener {

  private final Context context;
  private final TabHost tabHost;
  private final ViewPager viewPager;
  private final ArrayList<TabInfo> tabInfos = new ArrayList<TabInfo>();

  /**
   * An object to hold a tab's info.
   * 
   * @author Jimmy Shih
   */
  private static final class TabInfo {

    private final Class<?> clss;
    private final Bundle bundle;

    public TabInfo(Class<?> clss, Bundle bunlde) {
      this.clss = clss;
      this.bundle = bunlde;
    }
  }

  /**
   * A dummy {@link TabContentFactory} that creates an empty view to satisfy the
   * {@link TabHost} API.
   * 
   * @author Jimmy Shih
   */
  private static class DummyTabFactory implements TabHost.TabContentFactory {

    private final Context context;

    public DummyTabFactory(Context context) {
      this.context = context;
    }

    @Override
    public View createTabContent(String tag) {
      View view = new View(context);
      view.setMinimumWidth(0);
      view.setMinimumHeight(0);
      return view;
    }
  }

  public TabsAdapter(FragmentActivity activity, TabHost tabHost, ViewPager viewPager) {
    super(activity.getSupportFragmentManager());
    this.context = activity;
    this.tabHost = tabHost;
    this.viewPager = viewPager;
    this.tabHost.setOnTabChangedListener(this);
    this.viewPager.setAdapter(this);
    this.viewPager.setOnPageChangeListener(this);
  }

  public void addTab(TabHost.TabSpec tabSpec, Class<?> clss, Bundle bundle) {
    tabSpec.setContent(new DummyTabFactory(context));

    TabInfo info = new TabInfo(clss, bundle);
    
    tabInfos.add(info);
    tabHost.addTab(tabSpec);
    notifyDataSetChanged();
  }

  @Override
  public int getCount() {
    return tabInfos.size();
  }

  @Override
  public Fragment getItem(int position) {
    TabInfo info = tabInfos.get(position);
    return Fragment.instantiate(context, info.clss.getName(), info.bundle);
  }

  @Override
  public void onTabChanged(String tabId) {
    int position = tabHost.getCurrentTab();
    viewPager.setCurrentItem(position);
  }

  @Override
  public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

  @Override
  public void onPageSelected(int position) {
    /*
     * Unfortunately when TabHost changes the current tab, it kindly also takes
     * care of putting focus on it when not in touch mode. The jerk. This hack
     * tries to prevent this from pulling focus out of our ViewPager.
     */
    TabWidget tabWidget = tabHost.getTabWidget();
    int oldFocusability = tabWidget.getDescendantFocusability();
    tabWidget.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
    tabHost.setCurrentTab(position);
    tabWidget.setDescendantFocusability(oldFocusability);
  }

  @Override
  public void onPageScrollStateChanged(int state) {}
}
