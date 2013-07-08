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

package com.google.android.apps.mytracks.fragments;

import com.google.android.apps.mytracks.util.EulaUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.KeyEvent;
import android.widget.TextView;

/**
 * A DialogFragment to show EULA.
 * 
 * @author Jimmy Shih
 */
public class EulaDialogFragment extends DialogFragment {

  /**
   * Interface for caller of this dialog fragment.
   * 
   * @author Jimmy Shih
   */
  public interface EulaCaller {

    /**
     * Called when eula is done.
     */
    public void onEulaDone();
  }

  public static final String EULA_DIALOG_TAG = "eulaDialog";
  private static final String KEY_HAS_ACCEPTED = "hasAccepted";
  private static final String GOOGLE_URL = "m.google.com";
  private static final String KOREAN = "ko";

  /**
   * Creates a new instance of {@link EulaDialogFragment}.
   * 
   * @param hasAccepted true if the user has accepted the eula.
   */
  public static EulaDialogFragment newInstance(boolean hasAccepted) {
    Bundle bundle = new Bundle();
    bundle.putBoolean(KEY_HAS_ACCEPTED, hasAccepted);

    EulaDialogFragment eulaDialogFragment = new EulaDialogFragment();
    eulaDialogFragment.setArguments(bundle);
    return eulaDialogFragment;
  }

  private EulaCaller caller;
  private FragmentActivity fragmentActivity;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      caller = (EulaCaller) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(
          activity.toString() + " must implement " + EulaCaller.class.getSimpleName());
    }
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    fragmentActivity = getActivity();

    boolean hasAccepted = getArguments().getBoolean(KEY_HAS_ACCEPTED);

    SpannableString message = new SpannableString(getEulaText());
    Linkify.addLinks(message, Linkify.WEB_URLS);

    AlertDialog.Builder builder = new AlertDialog.Builder(fragmentActivity).setMessage(message)
        .setTitle(R.string.eula_title);

    if (hasAccepted) {
      builder.setPositiveButton(R.string.generic_ok, null);
    } else {
      builder.setNegativeButton(R.string.eula_decline, new DialogInterface.OnClickListener() {
          @Override
        public void onClick(DialogInterface dialog, int which) {
          caller.onEulaDone();
        }
      }).setOnKeyListener(new DialogInterface.OnKeyListener() {
          @Override
        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
          if (keyCode == KeyEvent.KEYCODE_SEARCH) {
            return true;
          }
          return false;
        }
      }).setPositiveButton(R.string.eula_accept, new DialogInterface.OnClickListener() {
          @Override
        public void onClick(DialogInterface dialog, int which) {
          EulaUtils.setAcceptEula(fragmentActivity);
          caller.onEulaDone();
        }
      });
    }   
    return builder.create();
  }

  @Override
  public void onStart() {
    super.onStart();   
    TextView textView = (TextView) getDialog().findViewById(android.R.id.message);
    textView.setMovementMethod(LinkMovementMethod.getInstance());
    textView.setTextAppearance(fragmentActivity, R.style.TextSmall);
  }
  
  @Override
  public void onCancel(DialogInterface arg0) {
    caller.onEulaDone();
  }

  /**
   * Gets the EULA text.
   */
  private String getEulaText() {
    String tos = getString(R.string.eula_date) + "\n\n" + getString(R.string.eula_body, GOOGLE_URL)
        + "\n\n" + getString(R.string.eula_footer, GOOGLE_URL) + "\n\n"
        + getString(R.string.eula_copyright_year);
    boolean isKorean = getResources().getConfiguration().locale.getLanguage().equals(KOREAN);
    if (isKorean) {
      tos += "\n\n" + getString(R.string.eula_korean);
    }
    return tos;
  }
}