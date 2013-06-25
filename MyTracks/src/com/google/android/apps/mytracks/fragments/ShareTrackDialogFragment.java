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

import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FilterQueryProvider;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Toast;

/**
 * A DialogFragment to share a track.
 * 
 * @author Jimmy Shih
 */
public class ShareTrackDialogFragment extends DialogFragment {

  /**
   * Interface for caller of this dialog fragment.
   * 
   * @author Jimmy Shih
   */
  public interface ShareTrackCaller {

    /**
     * Called when share track is done.
     * 
     * @param emails the added emails
     * @param makePublic true to make the track public
     */
    public void onShareTrackDone(String emails, boolean makePublic);
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
  private FragmentActivity fragmentActivity;
  private MultiAutoCompleteTextView multiAutoCompleteTextView;
  private CheckBox publicCheckBox;
  private CheckBox inviteCheckBox;

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
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    fragmentActivity = getActivity();
    View view = fragmentActivity.getLayoutInflater().inflate(R.layout.share_track, null);

    multiAutoCompleteTextView = (MultiAutoCompleteTextView) view.findViewById(
        R.id.share_track_emails);
    multiAutoCompleteTextView.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());

    SimpleCursorAdapter adapter = new SimpleCursorAdapter(fragmentActivity,
        R.layout.add_emails_item, getCursor(fragmentActivity, null), new String[] {
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
        return getCursor(fragmentActivity, constraint);
      }
    });
    multiAutoCompleteTextView.setAdapter(adapter);

    publicCheckBox = (CheckBox) view.findViewById(R.id.share_track_public);
    publicCheckBox.setChecked(PreferencesUtils.getBoolean(
        fragmentActivity, R.string.share_track_public_key,
        PreferencesUtils.SHARE_TRACK_PUBLIC_DEFAULT));

    inviteCheckBox = (CheckBox) view.findViewById(R.id.share_track_invite);
    inviteCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
        @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        multiAutoCompleteTextView.setVisibility(isChecked ? View.VISIBLE : View.GONE);
      }
    });
    inviteCheckBox.setChecked(PreferencesUtils.getBoolean(
        fragmentActivity, R.string.share_track_invite_key,
        PreferencesUtils.SHARE_TRACK_INVITE_DEFAULT));

    return new AlertDialog.Builder(fragmentActivity).setNegativeButton(
        R.string.generic_cancel, null)
        .setPositiveButton(R.string.generic_ok, new DialogInterface.OnClickListener() {
            @Override
          public void onClick(DialogInterface dialog, int which) {
            if (!publicCheckBox.isChecked() && !inviteCheckBox.isChecked()) {
              Toast.makeText(fragmentActivity, R.string.share_track_no_selection, Toast.LENGTH_LONG)
                  .show();
              return;
            }
            String acl = multiAutoCompleteTextView.getText().toString().trim();
            if (!publicCheckBox.isChecked() && acl.equals("")) {
              Toast.makeText(fragmentActivity, R.string.share_track_no_emails, Toast.LENGTH_LONG)
                  .show();
              return;
            }
            PreferencesUtils.setBoolean(
                fragmentActivity, R.string.share_track_public_key, publicCheckBox.isChecked());
            PreferencesUtils.setBoolean(
                fragmentActivity, R.string.share_track_invite_key, inviteCheckBox.isChecked());
            caller.onShareTrackDone(acl, publicCheckBox.isChecked());
          }
        }).setTitle(R.string.share_track_title).setView(view).create();
  }

  /**
   * Gets the cursor
   * 
   * @param activity the activity
   * @param constraint the constraint
   */
  private Cursor getCursor(Activity activity, CharSequence constraint) {
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
    return activity.getContentResolver().query(uri, projection, selection, null, order);
  }
}