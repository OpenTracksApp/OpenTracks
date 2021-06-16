package de.dennisguse.opentracks;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import de.dennisguse.opentracks.util.PreferencesUtils;

public class PreferenceMetricUnitRule implements TestRule {
    private final Context context;
    private final boolean metricUnits;

    public PreferenceMetricUnitRule(@NonNull Context context, boolean metricUnits) {
        this.context = context;
        this.metricUnits = metricUnits;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                SharedPreferences sharedPreferences = PreferencesUtils.getSharedPreferences(context);
                final boolean previousMetricUnits = PreferencesUtils.isMetricUnits(sharedPreferences, context);

                try {
                    PreferencesUtils.setMetricUnits(sharedPreferences, context, metricUnits);
                    base.evaluate();
                } finally {
                    PreferencesUtils.setMetricUnits(sharedPreferences, context, previousMetricUnits);
                }
            }
        };
    }
}
