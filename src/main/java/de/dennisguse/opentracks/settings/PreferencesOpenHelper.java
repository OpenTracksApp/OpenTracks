package de.dennisguse.opentracks.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.ui.customRecordingLayout.CsvLayoutUtils;

class PreferencesOpenHelper {

    private final int version;

    private PreferencesOpenHelper(int version) {
        this.version = version;
    }

    static PreferencesOpenHelper newInstance(int version) {
        return new PreferencesOpenHelper(version);
    }

    void check() {
        int lastVersion = PreferencesUtils.getInt(R.string.prefs_last_version_key, 0);
        if (version > lastVersion) {
            onUpgrade();
        } else if (version < lastVersion) {
            onDowngrade();
        }
    }

    private void onUpgrade() {
        PreferencesUtils.setInt(R.string.prefs_last_version_key, version);
        for (int i = 1; i <= version; i++) {
            switch (i) {
                case 1 -> upgradeFrom0to1();
                case 2 -> upgradeFrom1to2();
                default -> throw new RuntimeException("Not implemented: upgrade to " + version);
            }
        }
    }

    private void upgradeFrom0to1() {
        String preferenceValue = PreferencesUtils.getString(R.string.stats_custom_layouts_key, "");
        if (preferenceValue.isEmpty()) {
            PreferencesUtils.setString(R.string.stats_custom_layouts_key, PreferencesUtils.buildDefaultLayout());
        }
    }

    private void upgradeFrom1to2() {
        String csvVersion1CustomLayout = PreferencesUtils.getString(R.string.stats_custom_layouts_key, PreferencesUtils.buildDefaultLayout());
        ArrayList<String> parts = new ArrayList<>();
        Collections.addAll(parts, csvVersion1CustomLayout.split(CsvLayoutUtils.ITEM_SEPARATOR));

        if (parts.size() < 2) {
            PreferencesUtils.setString(R.string.stats_custom_layouts_key, PreferencesUtils.buildDefaultLayout());
            return;
        }

        if (!parts.get(1).matches("\\d+")) {
            parts.add(1, String.valueOf(PreferencesUtils.getLayoutColumnsByDefault()));
        }
        PreferencesUtils.setString(R.string.stats_custom_layouts_key, parts.stream().collect(Collectors.joining(CsvLayoutUtils.ITEM_SEPARATOR)));
    }

    private void onDowngrade() {
        PreferencesUtils.setString(R.string.stats_custom_layouts_key, PreferencesUtils.buildDefaultLayout());
    }
}
