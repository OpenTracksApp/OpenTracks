package de.dennisguse.opentracks.settings;

import android.annotation.SuppressLint;
import android.app.LocaleConfig;
import android.content.Context;
import android.os.Build;
import android.os.LocaleList;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.LocaleManagerCompat;
import androidx.core.os.LocaleListCompat;
import androidx.preference.ListPreference;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import de.dennisguse.opentracks.R;

public class LocalePreference extends ListPreference {

    private String TAG = LocalePreference.class.getSimpleName();

    public LocalePreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    public LocalePreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public LocalePreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LocalePreference(@NonNull Context context) {
        super(context);
        init(context);
    }

    // /data/user/0/de.dennisguse.opentracks.debug/files/androidx.appcompat.app.AppCompatDelegate.application_locales_record_file: open failed: ENOENT (No such file or directory)
    private void init(Context context) {
        setPersistent(false);

        LocaleItem systemDefaultLocale = new LocaleItem("", context.getString(R.string.settings_locale_system_default));

        LocaleItem currentLocale = new LocaleItem(Locale.getDefault().toLanguageTag(), Locale.getDefault().getDisplayName());

        // All available options
        LocaleList supportedLocales = getLocaleListCompat();

        ArrayList<LocaleItem> localeItemsSorting = new ArrayList<>();
        for (int i = 0; i < supportedLocales.size(); i++) {
            Locale current = supportedLocales.get(i);
            localeItemsSorting.add(new LocaleItem(current.toLanguageTag(), current.getDisplayName()));
        }

        localeItemsSorting.removeIf(current -> current.languageTag.equals(currentLocale.languageTag));
        localeItemsSorting.sort(Comparator.comparing(o -> o.displayName));

        ArrayList<LocaleItem> localeItemList = new ArrayList<>();
        localeItemList.add(systemDefaultLocale);
        localeItemList.add(currentLocale);
        localeItemList.addAll(localeItemsSorting);

        ArrayList<String> entries = new ArrayList<>();
        ArrayList<String> entryValues = new ArrayList<>();

        for (LocaleItem current : localeItemList) {
            entries.add(current.displayName);
            entryValues.add(current.languageTag);
        }

        setEntries(entries.toArray(new String[]{}));
        setEntryValues(entryValues.toArray(new String[]{}));

        if (AppCompatDelegate.getApplicationLocales().equals(LocaleListCompat.getEmptyLocaleList())) {
            setValue(systemDefaultLocale.languageTag);
        } else {
            setValue(currentLocale.languageTag);
        }
    }

    @Override
    public void setOnPreferenceChangeListener(@Nullable OnPreferenceChangeListener onPreferenceChangeListener) {
        super.setOnPreferenceChangeListener(onPreferenceChangeListener);
    }

    @Override
    public boolean callChangeListener(Object newValue) {
        LocaleListCompat newLocale = LocaleListCompat.getEmptyLocaleList();
        if (!newValue.equals("")) {
            newLocale = LocaleListCompat.forLanguageTags((String) newValue);
        }
        AppCompatDelegate.setApplicationLocales(newLocale);
        return super.callChangeListener(newValue);
    }

    record LocaleItem(
            String languageTag,
            String displayName
    ) {
    }

    // TODO Get this functionality from any Androidx compat library: should be in LocaleListCompat or LocaleManagerCompat
    // 2024-12-12: on Android 14-: LocaleManagerCompat.getApplicationLocales(getContext()) returned "[]"
    // See: https://stackoverflow.com/questions/78116375/per-app-language-preferences-get-list-of-apps-available-language-programmatic
    @Deprecated
    private LocaleList getLocaleListCompat() {
        var a = LocaleManagerCompat.getApplicationLocales(getContext());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return LocaleConfig.fromContextIgnoringOverride(getContext()).getSupportedLocales();
        }

        @SuppressLint("DiscouragedApi") int localesConfigId = getContext().getResources().getIdentifier("_generated_res_locale_config", "xml", getContext().getPackageName());

        List<String> localeList = new ArrayList<>();
        XmlPullParser xpp = getContext().getResources().getXml(localesConfigId);
        try {
            while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
                if (xpp.getEventType() == XmlPullParser.START_TAG) {
                    if ("locale".equals(xpp.getName()) && xpp.getAttributeCount() > 0 && xpp.getAttributeName(0).equals("name")) {
                        localeList.add(xpp.getAttributeValue(0));
                    }
                }
                xpp.next();
            }
        } catch (IOException | XmlPullParserException e) {
            Log.e(TAG, "Could not load locales: " + e.getMessage());
        }
        Optional<String> locales = localeList.stream().reduce((s1, s2) -> s1 + "," + s2);
        return locales
                .map(LocaleList::forLanguageTags)
                .orElseGet(LocaleList::getEmptyLocaleList);
    }
}
