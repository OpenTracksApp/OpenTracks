# Overview of tested Bluetooth LE sensors

## 0x180D: Heartrate Service

* Decathlon/Geonaute/Kalenji Dual HRM Belt
* moofit heart rate monitor
* Polar H7
* Polar H10
* Polar OH1
  Has updatable firmware that requires a user account at polar.com
* Wahoo Tickr (Model: WFBTHR02)
* Mi Band 3, Amazfit Band 5, Amazfit Bip. These devices are manufactured by Huami, and other Huami devices will very likely work. Theoretically, this sensor must be enabled, but testing reveals that it works even without enabling. For details on how to enable see [Gadgetbridge wiki](https://codeberg.org/Freeyourgadget/Gadgetbridge/wiki/Huami-Heartrate-measurement#bluetooth-heart-rate-sensor)
* Amazfit Neo. But first you have to enable "Discoverable" and "Activity heart rate sharing" in the official Zepp app. (after pairing with OpenTracks you can disable them again)
* Garmin HRM-Dual (Reference: 010-12883-00)
* CYCPLUS H1
* Mojawa Haptifit Terra - all you have to do is connect the app to device called "HT"
* Decathlon HRM Band

## 0x1816: Cycling Cadence and Speed Service

Please note that the according to the specification these sensors _may_ provide speed as well as cadence.
However, often only one value is provided.

### Speed & Cadence

* Wahoo Blue SC

### Speed only

* Garmin Speed Sensor 2
  * Has updatable firmware that requires an account for garmin.com
* Wahoo Speed (Model: WFRPMSPD)
* CooSpo cadence and speed sensor (model: BK467)
* CYCPLUS S3

### Cadence only

* Garmin Cadence Sensor 2
* Wahoo Cadence (Model: WFPODCAD2)
* CYCPLUS C3

## 0x1818: Power Measurement

We only support _Instantaneous Power_.

For cycling (incl. cadence):
* Assioma Duo Powermeter
* Elite drivo II
* QUARQ Red DZero Powermeter
* Tacx Satori Smart
* Wahoo Kickr v4.0
* Magene PES P505
* Rotor 2INpower DM Road (only supports power measurement, cadence measurement is proprietary @ firmware v1.061)

For running:
* Stryd

## 0x1814: running cadence and speed

We do not support type of movement (i.e., walking vs running).

* Polar Stride
* Stryd (incl. power)
* Wahoo Tickr X (Cadence appears to be reported in SPM rather than RPM)

## Barometer (gain/loss)

### 0x2A6D: Environmental Sensing Service
Sadly, Bluetooth sensors providing _air pressure_ via Environmental Sensing Service seem to be very rare.

* [Flybeeper Pressure Sensor](https://market.flybeeper.com/device/ps1)
* NoTrustIssues: https://codeberg.org/OpenTracksApp/NoTrustIssues
  An Android app that exposes the internal barometer via Environmental Sensing Service.

### Internal: tested Barometric Sensor Smartphones

OpenTracks derives the altitude gain and altitude loss using the internal barometric sensor of a device (if present).
However, barometric sensor seem to be prone to a lot of noise.
This may either be a malfunctioning sensor, a case that prevents the device from measuring, or something else. 

To check if the barometric sensor of a device is working with OpenTracks:
1. Record a track (ca. 1min) with the device stationary.
   Expectation: 0m gain and 0m loss should.
2. Record a track (ca. 1min) while shaking the device.
   Expectation: 0m gain and 0m loss should.
3. Record a track (ca. 1min) while moving the device up (less than 3m difference between highest and lowest point).
   Expectation: 0m gain and 0m loss should.
4. Record a track walking up some stairs or taking an elevator.
   Expectation: some gain and some loss is recorded that seems realistic.

If any of these tests do **not** show expected values for gain/loss, OpenTracks will not show useful values.

Please note that OpenTracks:
* only records gain/loss if +/- 3m to the initial/previous of are measured to the previous
* solely uses the barometric sensor for gain/loss; GPS is not used for this.

In our tests there are smartphones whose barometric sensor works well and others don't. 

Here is a list of the smartphones tested:

| Brand   | Model              | Android Version | Tests                                                                          |
|---------|--------------------|-----------------|--------------------------------------------------------------------------------|
| Google  | Pixel              | LineageOS 17.1  | Ok                                                                             |
| Google  | Pixel 2 XL         | LineageOS 17.1  | Ok on the bike handlebars but it fails when I carry it in a pocket or backpack |
| Google  | Pixel 3            | Android 11      | Failure                                                                        |
| Google  | Pixel 3a           | Android 11      | Ok                                                                             |
| Google  | Pixel 3a           | Lineage 18.1    | Ok                                                                             |
| Google  | Pixel 4a           | Android 11      | Ok                                                                             |
| Google  | Nexus 4            | LineageOS 18.1  | Ok                                                                             |
| Samsung | S4                 | Android 11      | Ok                                                                             |
| Sony    | Xperia XZ1 Compact | LineageOS 16.1  | Failure                                                                        |
