# Overview of tested Bluetooth LE sensors

## HRS: Heartrate Service

* Decathlon|Geonaute|Kalenji Dual HRM Belt
* moofit heart rate monitor
* Polar H7
* Polar H10
* Polar OH1
  Has updatable firmware that requires a user account at polar.com
* Wahoo Tickr (Model: WFBTHR02)

## CSCP: Cycling Cadence and Speed Service

Please note that the according to the specification these sensors _may_ provide speed as well as cadence.
However, often only one value is provided.

### Speed

* Garmin Speed Sensor 2
* Wahoo Speed (Model: WFRPMSPD)

### Cadence

* Garmin Cadence Sensor 2
* Wahoo Cadence (Model: WFPODCAD2)
  This sensor reports cadence data as speed.
  A workaround is in place.

## Cycling Power Measurement

We only support _Instantaneous Power_.

* QUARQ Red DZero Powermeter
* Wahoo Kickr v4.0
  WARNING: we only support physically movements with valid GPS coordinates.
  Using a device indoors (no GPS available) will only show the current power values, but won't store anything.
  Also the distance is not computed.