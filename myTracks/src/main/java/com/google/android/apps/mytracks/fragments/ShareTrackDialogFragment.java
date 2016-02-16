/*
 * Copyright 2013 Google Inc.
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

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.util.AccountUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FilterQueryProvider;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * A DialogFragment to share a track.
 * 
 * @author Jimmy Shih
 */
public class ShareTrackDialogFragment extends AbstractMyTracksDialogFragment {

  /**
   * Interface for caller of this dialog fragment.
   * 
   * @author Jimmy Shih
   */
  public interface ShareTrackCaller {

    /**
     * Called when share track is done.
     * 
     * @param trackId the track id
     * @param makePublic true to make the track public
     * @param emails the emails to share the track with
     * @param account the google drive account
     */
    public void onShareTrackDone(long trackId, boolean makePublic, String emails, Account account);
  }

  public static final String SHARE_TRACK_DIALOG_TAG = "shareTrackDialog";

  private static final String KEY_TRACK_ID = "trackId";

  public static ShareTrackDialogFragment newInstance(long trackId) {
    Bundle bundle = new Bundle();
    bundle.putLong(KEY_TRACK_ID, trackId);

    ShareTrackDialogFragment shareleTrackDialogFragment = new ShareTrackDialogFragment();
    shareleTrackDialogFragment.setArguments(bundle);
    return shareleTrackDialogFragment;
  }

  private ShareTrackCaller caller;
  private Account[] accounts;

  // UI elements
  private CheckBox publicCheckBox;
  private CheckBox inviteCheckBox;
  private MultiAutoCompleteTextView multiAutoCompleteTextView;
  private Spinner accountSpinner;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      caller = (ShareTrackCaller) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(
          activity.toString() + " must implement " + ShareTrackCaller.class.getSimpleName());
    }
  }

  @Override
  protected Dialog createDialog() {
    FragmentActivity fragmentActivity = getActivity();
    accounts = AccountManager.get(fragmentActivity).getAccountsByType(Constants.ACCOUNT_TYPE);

    if (accounts.length == 0) {
      return new AlertDialog.Builder(fragmentActivity).setMessage(
          R.string.send_google_no_account_message).setTitle(R.string.send_google_no_account_title)
          .setPositiveButton(R.string.generic_ok, null).create();
    }

    // Get all the views
    View view = fragmentActivity.getLayoutInflater().inflate(R.layout.share_track, null);
    publicCheckBox = (CheckBox) view.findViewById(R.id.share_track_public);
    inviteCheckBox = (CheckBox) view.findViewById(R.id.share_track_invite);
    multiAutoCompleteTextView = (MultiAutoCompleteTextView) view.findViewById(
        R.id.share_track_emails);
    accountSpinner = (Spinner) view.findViewById(R.id.share_track_account);
    
    // Setup publicCheckBox
    publicCheckBox.setChecked(PreferencesUtils.getBoolean(
        fragmentActivity, R.string.share_track_public_key,
        PreferencesUtils.SHARE_TRACK_PUBLIC_DEFAULT));

    // Setup inviteCheckBox
    inviteCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
        @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        multiAutoCompleteTextView.setVisibility(isChecked ? View.VISIBLE : View.GONE);
      }
    });
    inviteCheckBox.setChecked(PreferencesUtils.getBoolean(
        fragmentActivity, R.string.share_track_invite_key,
        PreferencesUtils.SHARE_TRACK_INVITE_DEFAULT));

    // Setup multiAutoCompleteTextView
    multiAutoCompleteTextView.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
    SimpleCursorAdapter adapter = new SimpleCursorAdapter(fragmentActivity,
        R.layout.add_emails_item, getAutoCompleteCursor(fragmentActivity, null), new String[] {
            ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.CommonDataKinds.Email.DATA },
        new int[] { android.R.id.text1, android.R.id.text2 }, 0);
    adapter.setCursorToStringConverter(new SimpleCursorAdapter.CursorToStringConverter() {
        @Override
      public CharSequence convertToString(Cursor cursor) {
        int index = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA);
        return cursor.getString(index).trim();
      }
    });
    adapter.setFilterQueryProvider(new FilterQueryProvider() {
        @Override
      public Cursor runQuery(CharSequence constraint) {
        return getAutoCompleteCursor(getActivity(), constraint);
      }
    });
    multiAutoCompleteTextView.setAdapter(adapter);

    // Setup accountSpinner
    accountSpinner.setVisibility(accounts.length > 1 ? View.VISIBLE : View.GONE);
    AccountUtils.setupAccountSpinner(fragmentActivity, accountSpinner, accounts);
    
    return new AlertDialog.Builder(fragmentActivity).setNegativeButton(
        R.string.generic_cancel, null)
        .setPositiveButton(R.string.generic_ok, new DialogInterface.OnClickListener() {
            @Override
          public void onClick(DialogInterface dialog, int which) {
            FragmentActivity context = getActivity();
            if (!publicCheckBox.isChecked() && !inviteCheckBox.isChecked()) {
              Toast.makeText(context, R.string.share_track_no_selection, Toast.LENGTH_LONG).show();
              return;
            }
            String acl = multiAutoCompleteTextView.getText().toString().trim();
            if (!publicCheckBox.isChecked() && acl.equals("")) {
              Toast.makeText(context, R.string.share_track_no_emails, Toast.LENGTH_LONG).show();
              return;
            }
            PreferencesUtils.setBoolean(
                context, R.string.share_track_public_key, publicCheckBox.isChecked());
            PreferencesUtils.setBoolean(
                context, R.string.share_track_invite_key, inviteCheckBox.isChecked());
            Account account = accounts.length > 1 ? accounts[accountSpinner
                .getSelectedItemPosition()]
                : accounts[0];
            AccountUtils.updateShareTrackAccountPreference(context, account);
            caller.onShareTrackDone(
                getArguments().getLong(KEY_TRACK_ID), publicCheckBox.isChecked(), acl, account);
          }
        }).setTitle(R.string.share_track_title).setView(view).create();
  }

  /**
   * Gets the auto complete cursor.
   * 
   * @param constraint the constraint
   */
  private Cursor getAutoCompleteCursor(FragmentActivity fragmentActivity, CharSequence constraint) {
    String order = ContactsContract.Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC";
    String selection = ContactsContract.Contacts.IN_VISIBLE_GROUP + " = '1'";
    if (constraint != null) {
      selection += " AND (" + ContactsContract.Contacts.DISPLAY_NAME + " LIKE '%" + constraint
          + "%' OR " + ContactsContract.CommonDataKinds.Email.DATA + " LIKE '%" + constraint
          + "%' )";
    }
    String[] projection = new String[] { ContactsContract.Contacts._ID,
        ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.CommonDataKinds.Email.DATA };
    Uri uri = ContactsContract.CommonDataKinds.Email.CONTENT_URI;
    return fragmentActivity.getContentResolver().query(uri, projection, selection, null, order);
  }
}