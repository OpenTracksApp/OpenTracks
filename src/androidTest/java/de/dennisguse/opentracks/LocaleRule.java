package de.dennisguse.opentracks;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import androidx.test.core.app.ApplicationProvider;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.Locale;

public class LocaleRule implements TestRule {

    private final Locale[] mLocales;
    private Locale mDeviceLocale;


    public LocaleRule(Locale... locales) {
        assert locales != null && locales.length > 0;
        mLocales = locales;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    mDeviceLocale = Locale.getDefault();
                    for (Locale locale : mLocales) {
                        setLocale(locale);
                        base.evaluate();
                    }
                } finally {
                    if (mDeviceLocale != null) {
                        setLocale(mDeviceLocale);
                    }
                }
            }
        };
    }

    private void setLocale(Locale locale) {
        Resources resources = ApplicationProvider.getApplicationContext().getResources();
        Locale.setDefault(locale);
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        resources.updateConfiguration(config, displayMetrics);
    }

}
