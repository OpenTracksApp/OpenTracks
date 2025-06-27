package de.dennisguse.opentracks.io.file.importer;

import org.junit.Assert;

import java.util.List;

import de.dennisguse.opentracks.data.models.TrackPoint;

public class TrackPointAssert {

    private double delta = 0.001;

    public TrackPointAssert() {
    }

    public void assertEquals(TrackPoint expected, TrackPoint actual) {
        Assert.assertEquals("time", expected.getTime(), actual.getTime());

        Assert.assertEquals("type", expected.getType(), actual.getType());

        Assert.assertEquals("has location,", expected.hasLocation(), actual.hasLocation());
        if (expected.hasLocation()) {
            Assert.assertEquals("latitude", expected.getPosition().latitude(), actual.getPosition().latitude(), 0.001);
            Assert.assertEquals("longitude", expected.getPosition().longitude(), actual.getPosition().longitude(), 0.001);
        }

        Assert.assertEquals("has altitude", expected.hasAltitude(), actual.hasAltitude());
        if (expected.hasAltitude()) {
            Assert.assertEquals("altitude", expected.getAltitude().toM(), actual.getAltitude().toM(), delta);
        }

        Assert.assertEquals("has altitudeGain", expected.hasAltitudeGain(), actual.hasAltitudeGain());
        if (expected.hasAltitudeGain()) {
            Assert.assertEquals("altitudeGain", expected.getAltitudeGain(), actual.getAltitudeGain(), delta);
        }
        Assert.assertEquals("has altitudeLoss", expected.hasAltitudeLoss(), actual.hasAltitudeLoss());
        if (expected.hasAltitudeLoss()) {
            Assert.assertEquals("altitudeLoss", expected.getAltitudeLoss(), actual.getAltitudeLoss(), delta);
        }

        Assert.assertEquals("has speed", expected.hasSpeed(), actual.hasSpeed());
        if (expected.hasSpeed()) {
            Assert.assertEquals("speed", expected.getSpeed().toMPS(), actual.getSpeed().toMPS(), delta);
        }

        Assert.assertEquals("has horizontalAccuracy", expected.hasHorizontalAccuracy(), actual.hasHorizontalAccuracy());
        if (expected.hasHorizontalAccuracy()) {
            Assert.assertEquals("horizontalAccuracy", expected.getHorizontalAccuracy().toM(), actual.getHorizontalAccuracy().toM(), delta);
        }
        Assert.assertEquals("has verticalAccuracy", expected.hasVerticalAccuracy(), actual.hasVerticalAccuracy());
        if (expected.hasVerticalAccuracy()) {
            Assert.assertEquals("verticalAccuracy", expected.getVerticalAccuracy().toM(), actual.getVerticalAccuracy().toM(), delta);
        }

        Assert.assertEquals("has sensorDistance", expected.hasSensorDistance(), actual.hasSensorDistance());
        if (expected.hasSensorDistance()) {
            Assert.assertEquals("sensorDistance", expected.getSensorDistance().toM(), actual.getSensorDistance().toM(), delta);
        }

        Assert.assertEquals("has heartrate", expected.hasHeartRate(), actual.hasHeartRate());
        if (expected.hasHeartRate()) {
            Assert.assertEquals("heartrate", expected.getHeartRate(), actual.getHeartRate());
        }

        Assert.assertEquals("has power", expected.hasPower(), actual.hasPower());
        if (expected.hasPower()) {
            Assert.assertEquals("power", expected.getPower(), actual.getPower());
        }

        Assert.assertEquals("has cadence", expected.hasCadence(), actual.hasCadence());
        if (expected.hasCadence()) {
            Assert.assertEquals("cadence", expected.getCadence(), actual.getCadence());
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
