/*
 * Copyright 2008 Google Inc.
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

import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

/**
 * Creates previous and next arrows for a given activity.
 *
 * @author Leif Hendrik Wilden
 */
public class NavControls {

  private static final int KEEP_VISIBLE_MILLIS = 4000;
  private static final boolean FADE_CONTROLS = true;

  private static final Animation SHOW_NEXT_ANIMATION =
      new AlphaAnimation(0F, 1F);
  private static final Animation HIDE_NEXT_ANIMATION =
      new AlphaAnimation(1F, 0F);
  private static final Animation SHOW_PREV_ANIMATION =
      new AlphaAnimation(0F, 1F);
  private static final Animation HIDE_PREV_ANIMATION =
      new AlphaAnimation(1F, 0F);

  /**
   * A touchable image view.
   */
  public class TouchLayout extends RelativeLayout {
    private final ImageView arrow;
    private final ImageView icon;

    public TouchLayout(Activity activity, boolean isLeft) {
      super(activity);
      arrow = new ImageView(activity);
      arrow.setImageDrawable(activity.getResources().getDrawable(
          isLeft ? R.drawable.btn_arrow_left : R.drawable.btn_arrow_right));
      icon = new ImageView(activity);
      icon.setVisibility(View.GONE);
      addView(arrow);
      addView(icon);
      DisplayMetrics metrics = new DisplayMetrics();
      activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
      // TODO: This should be a smooth function not a step function.
      if (metrics.heightPixels > 600) {
        icon.setPadding((isLeft ? 25 : 15), 39, 15, 0);
      } else {
        icon.setPadding((isLeft ? 15 : 10), 27, 15, 0);
      }
    }

    public void setIcon(Drawable drawable) {
      icon.setImageDrawable(drawable);
      icon.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
      switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
          setPressed(true);
          hide();
          if (this == prevImage) {
            if (prevRunnable != null) {
              handler.post(prevRunnable);
              return true;
            }
          } else if (this == nextImage) {
            if (nextRunnable != null) {
              handler.post(nextRunnable);
              return true;
            }
          }
          break;
        case MotionEvent.ACTION_UP:
          setPressed(false);
          break;
      }
      return super.onTouchEvent(event);
    }
  }

  private final Context context;
  private final Runnable prevRunnable;
  private final Runnable nextRunnable;
  private final Handler handler = new Handler();
  private final Runnable dismissControls = new Runnable() {
    public void run() {
      hide();
    }
  };
  private final TouchLayout prevImage;
  private final TouchLayout nextImage;

  private boolean isVisible = false;
  private boolean hasNext = true;
  private boolean hasPrev = true;

  public NavControls(Activity activity, ViewGroup container,
      Runnable prevRunnable, Runnable nextRunnable) {
    this.context = activity;
    this.prevRunnable = prevRunnable;
    this.nextRunnable = nextRunnable;
    LayoutParams prevParams = new LayoutParams(
        LayoutParams.WRAP_CONTENT,
        LayoutParams.WRAP_CONTENT);
    prevParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
    prevParams.addRule(RelativeLayout.CENTER_VERTICAL);
    prevImage = new TouchLayout(activity, true);
    prevImage.setLayoutParams(prevParams);
    prevImage.setVisibility(View.INVISIBLE);
    container.addView(prevImage);
    LayoutParams nextParams = new LayoutParams(
        LayoutParams.WRAP_CONTENT,
        LayoutParams.WRAP_CONTENT);
    nextParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
    nextParams.addRule(RelativeLayout.CENTER_VERTICAL);
    nextImage = new TouchLayout(activity, false);
    nextImage.setLayoutParams(nextParams);
    nextImage.setVisibility(View.INVISIBLE);
    container.addView(nextImage);
  }

  public void setHasNext(boolean hasNext) {
    this.hasNext = hasNext;
  }

  public void setHasPrev(boolean hasPrev) {
    this.hasPrev = hasPrev;
  }

  private void keepVisible() {
    if (isVisible && FADE_CONTROLS) {
      handler.removeCallbacks(dismissControls);
      handler.postDelayed(dismissControls, KEEP_VISIBLE_MILLIS);
    }
  }

  public void show() {
    if (!isVisible) {
      if (prevRunnable != null && hasPrev) {
        SHOW_PREV_ANIMATION.setDuration(500);
        SHOW_PREV_ANIMATION.startNow();
        prevImage.setPressed(false);
        prevImage.setAnimation(SHOW_PREV_ANIMATION);
        prevImage.setVisibility(View.VISIBLE);
      }
      if (nextRunnable != null && hasNext) {
        SHOW_NEXT_ANIMATION.setDuration(500);
        SHOW_NEXT_ANIMATION.startNow();
        nextImage.setPressed(false);
        nextImage.setAnimation(SHOW_NEXT_ANIMATION);
        nextImage.setVisibility(View.VISIBLE);
      }
      isVisible = true;
      keepVisible();
    } else {
      keepVisible();
    }
  }

  public void hideNow() {
    handler.removeCallbacks(dismissControls);
    isVisible = false;
    prevImage.clearAnimation();
    prevImage.setVisibility(View.INVISIBLE);
    nextImage.clearAnimation();
    nextImage.setVisibility(View.INVISIBLE);
  }

  public void hide() {
    isVisible = false;
    if (prevRunnable != null) {
      if (hasPrev) {
        prevImage.setAnimation(HIDE_PREV_ANIMATION);
        HIDE_PREV_ANIMATION.setDuration(500);
        HIDE_PREV_ANIMATION.startNow();
      } else {
        prevImage.clearAnimation();
      }
      prevImage.setVisibility(View.INVISIBLE);
    }
    if (nextRunnable != null) {
      if (hasNext) {
        nextImage.setAnimation(HIDE_NEXT_ANIMATION);
        HIDE_NEXT_ANIMATION.setDuration(500);
        HIDE_NEXT_ANIMATION.startNow();
      } else {
        nextImage.clearAnimation();
      }
      nextImage.setVisibility(View.INVISIBLE);
    }
  }

  public void setLeftIcon(int resourceId) {
    prevImage.setIcon(context.getResources().getDrawable(resourceId));
  }

  public void setRightIcon(int resourceId) {
    nextImage.setIcon(context.getResources().getDrawable(resourceId));
  }
}
