# Overview of tested Bluetooth LE sensors

## 0x180D: Heartrate Service

* Decathlon|Geonaute|Kalenji Dual HRM Belt
* moofit heart rate monitor
* Polar H7
* Polar H10
* Polar OH1
  Has updatable firmware that requires a user account at polar.com
* Wahoo Tickr (Model: WFBTHR02)
* Mi Band 3, Amazfit Band 5, Amazfit Bip. These devices are manufactured by Huami, and other Huami devices will very likely work. Theoretically, this sensor must be enabled, but testing reveals that it works even without enabling. For details on how to enable see [Gadgetbridge wiki](https://codeberg.org/Freeyourgadget/Gadgetbridge/wiki/Huami-Heartrate-measurement#bluetooth-heart-rate-sensor)
* Amazfit Neo. But first you have to enable "Discoverable" and "Activity heart rate sharing" in the official Zepp app. (after pairing with OpenTracks you can disable them again)
* Garmin HRM-Dual (Reference: 010-12883-00)

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

### Cadence only

* Garmin Cadence Sensor 2
* Wahoo Cadence (Model: WFPODCAD2)

## 0x1818: Power Measurement

We only support _Instantaneous Power_.

For cycling:
* Assioma Duo Powermeter
* Elite drivo II
* QUARQ Red DZero Powermeter
* Tacx Satori Smart
* Wahoo Kickr v4.0
* Rotor 2INpower DM Road (only supports power measurement, cadence measurement is propietary @ firmware v1.061)

For running:
* Stryd

## 0x1814: running cadence and speed

We do not support type of movement (i.e., walking vs running).

* Polar Stride
* Stryd (incl. power)
* Wahoo Tickr X (Cadence appears to be reported in SPM rather than RPM)

## Tested Barometric Sensor Smartphones

There are smartphones that have a barometric sensor that OpenTracks uses to compute altitude gain and loss.

In our tests there are smartphones whose barometric sensor works well and others don't. Here is a list of the smartphones tested:

|Brand|Model|Android Version|Tests|
|-----|-----|---------------|-----------|
|Google|Pixel|LineageOS 17.1|Ok|
|Google|Pixel 2 XL|LineageOS 17.1|Ok on the bike handlebars but it fails when I carry it in a pocket or backpack|
|Google|Pixel 3|Android 11|Failure|
|Google|Pixel 3a|Android 11|Ok|
|Google|Pixel 3a|Lineage 18.1|Ok|
|Google|Pixel 4a|Android 11|Ok|
|Sony|Xperia XZ1 Compact|LineageOS 16.1|Failure|
