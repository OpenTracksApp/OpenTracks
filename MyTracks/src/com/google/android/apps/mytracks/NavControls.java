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

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

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
   * When touched it changes the navigation control icons accordingly.
   */
  private class TouchLayout extends RelativeLayout implements Runnable {
    private final boolean isLeft;
    private final ImageView icon;

    public TouchLayout(Context context, boolean isLeft) {
      super(context);
      this.isLeft = isLeft;
      this.icon = new ImageView(context);
      icon.setVisibility(View.GONE);
      addView(icon);
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
          shiftIcons(isLeft);

          // Call the user back
          handler.post(this);

          break;
        case MotionEvent.ACTION_UP:
          setPressed(false);
          break;
      }
      return super.onTouchEvent(event);
    }

    @Override
    public void run() {
      touchRunnable.run();
      setPressed(false);
    }
  }

  private final Handler handler = new Handler();
  private final Runnable dismissControls = new Runnable() {
    public void run() {
      hide();
    }
  };

  private final TouchLayout prevImage;
  private final TouchLayout nextImage;
  private final TypedArray leftIcons;
  private final TypedArray rightIcons;
  private final Runnable touchRunnable;

  private boolean isVisible = false;
  private int currentIcons;

  public NavControls(Context context, ViewGroup container,
      TypedArray leftIcons, TypedArray rightIcons,
      Runnable touchRunnable) {
    this.leftIcons = leftIcons;
    this.rightIcons = rightIcons;
    this.touchRunnable = touchRunnable;

    if (leftIcons.length() != rightIcons.length() || leftIcons.length() < 1) {
      throw new IllegalArgumentException("Invalid icons specified");
    }
    if (touchRunnable == null) {
      throw new NullPointerException("Runnable cannot be null");
    }

    LayoutParams prevParams = new LayoutParams(
        LayoutParams.WRAP_CONTENT,
        LayoutParams.WRAP_CONTENT);
    LayoutParams nextParams = new LayoutParams(
        LayoutParams.WRAP_CONTENT,
        LayoutParams.WRAP_CONTENT);

    prevParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
    nextParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
    prevParams.addRule(RelativeLayout.CENTER_VERTICAL);
    nextParams.addRule(RelativeLayout.CENTER_VERTICAL);

    nextImage = new TouchLayout(context, false);
    prevImage = new TouchLayout(context, true);
    nextImage.setLayoutParams(nextParams);
    prevImage.setLayoutParams(prevParams);
    nextImage.setVisibility(View.INVISIBLE);
    prevImage.setVisibility(View.INVISIBLE);

    container.addView(prevImage);
    container.addView(nextImage);

    prevImage.setIcon(leftIcons.getDrawable(0));
    nextImage.setIcon(rightIcons.getDrawable(0));
    this.currentIcons = 0;
  }

  private void keepVisible() {
    if (isVisible && FADE_CONTROLS) {
      handler.removeCallbacks(dismissControls);
      handler.postDelayed(dismissControls, KEEP_VISIBLE_MILLIS);
    }
  }

  public void show() {
    if (!isVisible) {
      SHOW_PREV_ANIMATION.setDuration(500);
      SHOW_PREV_ANIMATION.startNow();
      prevImage.setPressed(false);
      prevImage.setAnimation(SHOW_PREV_ANIMATION);
      prevImage.setVisibility(View.VISIBLE);

      SHOW_NEXT_ANIMATION.setDuration(500);
      SHOW_NEXT_ANIMATION.startNow();
      nextImage.setPressed(false);
      nextImage.setAnimation(SHOW_NEXT_ANIMATION);
      nextImage.setVisibility(View.VISIBLE);

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
    prevImage.setAnimation(HIDE_PREV_ANIMATION);
    HIDE_PREV_ANIMATION.setDuration(500);
    HIDE_PREV_ANIMATION.startNow();
    prevImage.setVisibility(View.INVISIBLE);

    nextImage.setAnimation(HIDE_NEXT_ANIMATION);
    HIDE_NEXT_ANIMATION.setDuration(500);
    HIDE_NEXT_ANIMATION.startNow();
    nextImage.setVisibility(View.INVISIBLE);
  }

  public int getCurrentIcons() {
    return currentIcons;
  }

  private void shiftIcons(boolean isLeft) {
    // Increment or decrement by one, with wrap around
    currentIcons = (currentIcons + leftIcons.length() +
        (isLeft ? -1 : 1)) % leftIcons.length();
    prevImage.setIcon(leftIcons.getDrawable(currentIcons));
    nextImage.setIcon(rightIcons.getDrawable(currentIcons));
  }
}
