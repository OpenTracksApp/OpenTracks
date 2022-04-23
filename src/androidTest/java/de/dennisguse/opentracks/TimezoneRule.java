package de.dennisguse.opentracks;

import androidx.annotation.NonNull;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.TimeZone;

public class TimezoneRule implements TestRule {

    private final TimeZone mTimeZone;
    private TimeZone mDeviceTimeZone;


    public TimezoneRule(@NonNull TimeZone timeZone) {
        mTimeZone = timeZone;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    mDeviceTimeZone = TimeZone.getDefault();
                    TimeZone.setDefault(mTimeZone);
                    base.evaluate();
                } finally {
                    TimeZone.setDefault(mDeviceTimeZone);
                }
            }
        };
    }
}
