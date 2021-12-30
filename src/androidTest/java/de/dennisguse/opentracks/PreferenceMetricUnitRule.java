package de.dennisguse.opentracks;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import de.dennisguse.opentracks.settings.PreferencesUtils;

public class PreferenceMetricUnitRule implements TestRule {
    private final boolean metricUnits;

    public PreferenceMetricUnitRule(boolean metricUnits) {
        this.metricUnits = metricUnits;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                final boolean previousMetricUnits = PreferencesUtils.isMetricUnits();

                try {
                    PreferencesUtils.setMetricUnits(metricUnits);
                    base.evaluate();
                } finally {
                    PreferencesUtils.setMetricUnits(previousMetricUnits);
                }
            }
        };
    }
}
