# Overview of tested Bluetooth LE sensors

## HRS: Heartrate Service

* Decathlon|Geonaute|Kalenji Dual HRM Belt
* moofit heart rate monitor
* Polar H7
* Polar H10
* Polar OH1
  Has updatable firmware that requires a user account at polar.com
* Wahoo Tickr (Model: WFBTHR02)
* Mi Band 3, Amazfit Band 5, Amazfit Bip. These devices are manufactured by Huami, and other Huami devices will very likely work. Theoretically, this sensor must be enabled, but testing reveals that it works even without enabling. For details on how to enable see [Gadgetbridge wiki](https://codeberg.org/Freeyourgadget/Gadgetbridge/wiki/Huami-Heartrate-measurement#bluetooth-heart-rate-sensor)

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

_WARNING_: we only support physically movements with valid GPS coordinates.
Using a device indoors (no GPS available) will only show the current power values, but won't store anything.
Also the distance is not computed.

* Assioma Duo Powermeter
* Elite drivo II
* QUARQ Red DZero Powermeter
* Tacx Satori Smart
* Wahoo Kickr v4.0

