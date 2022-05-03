package de.dennisguse.opentracks.io.file.importer;

import org.junit.Assert;

import java.util.List;

import de.dennisguse.opentracks.data.models.TrackPoint;

public class TrackPointAssert {

    private double delta = 0.001;

    public TrackPointAssert() {
    }

    public void assertEquals(TrackPoint expected, TrackPoint actual) {
        Assert.assertEquals(expected.getTime(), actual.getTime());

        Assert.assertEquals(expected.getType(), actual.getType());

        Assert.assertEquals(expected.hasLocation(), actual.hasLocation());
        if (expected.hasLocation()) {
            Assert.assertEquals(expected.getLatitude(), actual.getLatitude(), 0.001);
            Assert.assertEquals(expected.getLongitude(), actual.getLongitude(), 0.001);
        }

        Assert.assertEquals(expected.hasAltitude(), actual.hasAltitude());
        if (expected.hasAltitude()) {
            Assert.assertEquals(expected.getAltitude().toM(), actual.getAltitude().toM(), delta);
        }

        Assert.assertEquals(expected.hasAltitudeGain(), actual.hasAltitudeGain());
        if (expected.hasAltitudeGain()) {
            Assert.assertEquals(expected.getAltitudeGain(), actual.getAltitudeGain(), delta);
        }
        Assert.assertEquals(expected.hasAltitudeLoss(), actual.hasAltitudeLoss());
        if (expected.hasAltitudeLoss()) {
            Assert.assertEquals(expected.getAltitudeLoss(), actual.getAltitudeLoss(), delta);
        }

        Assert.assertEquals(expected.hasSpeed(), actual.hasSpeed());
        if (expected.hasSpeed()) {
            Assert.assertEquals(expected.getSpeed().toMPS(), actual.getSpeed().toMPS(), delta);
        }

        Assert.assertEquals(expected.hasHorizontalAccuracy(), actual.hasHorizontalAccuracy());
        if (expected.hasHorizontalAccuracy()) {
            Assert.assertEquals(expected.getHorizontalAccuracy().toM(), actual.getHorizontalAccuracy().toM(), delta);
        }
        Assert.assertEquals(expected.hasVerticalAccuracy(), actual.hasVerticalAccuracy());
        if (expected.hasVerticalAccuracy()) {
            Assert.assertEquals(expected.getVerticalAccuracy().toM(), actual.getVerticalAccuracy().toM(), delta);
        }

        Assert.assertEquals(expected.hasSensorDistance(), actual.hasSensorDistance());
        if (expected.hasSensorDistance()) {
            Assert.assertEquals(expected.getSensorDistance().toM(), actual.getSensorDistance().toM(), delta);
        }

        Assert.assertEquals(expected.hasHeartRate(), actual.hasHeartRate());
        if (expected.hasHeartRate()) {
            Assert.assertEquals(expected.getHeartRate(), actual.getHeartRate());
        }

        Assert.assertEquals(expected.hasPower(), actual.hasPower());
        if (expected.hasPower()) {
            Assert.assertEquals(expected.getPower(), actual.getPower());
        }

        Assert.assertEquals(expected.hasCadence(), actual.hasCadence());
        if (expected.hasCadence()) {
            Assert.assertEquals(expected.getCadence(), actual.getCadence());
        }
    }

    public void assertEquals(List<TrackPoint> expected, List<TrackPoint> actual) {
        try {
            Assert.assertEquals(expected.size(), actual.size());
        } catch (AssertionError e) {
            throw new AssertionError("Size difference; expected: " + expected.size() + "; actual: " + actual.size() + "\nExpected: " + expected + "\n actual: " + actual);
        }

        for (int i = 0; i < expected.size(); i++) {
            try {
                assertEquals(expected.get(i), actual.get(i));
            } catch (AssertionError e) {
                throw new AssertionError("Expected: " + i + " to be " + expected.get(i) + "\n actual: " + actual.get(i), e);
            }
        }
        Assert.assertEquals(expected.size(), actual.size());

        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), actual.get(i));
        }
    }

    public TrackPointAssert setDelta(double delta) {
        this.delta = delta;
        return this;
    }
}
