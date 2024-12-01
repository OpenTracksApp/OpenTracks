package de.dennisguse.opentracks.settings;

import android.app.LocaleConfig;
import android.content.Context;
import android.os.Build;
import android.os.LocaleList;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.preference.ListPreference;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;

import de.dennisguse.opentracks.R;

public class LocalePreference extends ListPreference {

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

    private void init(Context context) {
        setPersistent(false);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            setEnabled(false);
            return;
        }
        LocaleItem systemDefaultLocale = new LocaleItem("", context.getString(R.string.settings_locale_system_default));

        LocaleItem currentLocale = new LocaleItem(Locale.getDefault().toLanguageTag(), Locale.getDefault().getDisplayName());

        // All available options
        LocaleList supportedLocales = LocaleConfig.fromContextIgnoringOverride(context).getSupportedLocales();
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
}
