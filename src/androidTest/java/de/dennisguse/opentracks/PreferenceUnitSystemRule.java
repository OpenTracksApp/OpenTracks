package de.dennisguse.opentracks;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.settings.UnitSystem;

public class PreferenceUnitSystemRule implements TestRule {
    private final UnitSystem unit;

    public PreferenceUnitSystemRule(UnitSystem unit) {
        this.unit = unit;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                final UnitSystem previousUnitSystem = PreferencesUtils.getUnitSystem();

                try {
                    PreferencesUtils.setUnit(unit);
                    base.evaluate();
                } finally {
                    PreferencesUtils.setUnit(previousUnitSystem);
                }
            }
        };
    }
}
