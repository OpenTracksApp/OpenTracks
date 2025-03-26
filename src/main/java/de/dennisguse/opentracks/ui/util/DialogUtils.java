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

package de.dennisguse.opentracks.ui.util;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;

import de.dennisguse.opentracks.R;

/**
 * Utilities for creating dialogs.
 *
 * @author Jimmy Shih
 */
public class DialogUtils {

    private DialogUtils() {
    }

    /**
     * Creates a confirmation dialog.
     *
     * @param context    the context
     * @param titleId    the title
     * @param message    the message
     * @param okListener the listener when OK is clicked
     */
    public static Dialog createConfirmationDialog(final Context context, int titleId, String message, DialogInterface.OnClickListener okListener, DialogInterface.OnClickListener cancelListener) {
        return new AlertDialog.Builder(context)
                .setCancelable(true)
                .setIcon(R.drawable.ic_delete_forever_24dp)
                .setMessage(message)
                .setNegativeButton(android.R.string.cancel, cancelListener)
                .setPositiveButton(android.R.string.ok, okListener)
                .setTitle(titleId).create();
    }

}
