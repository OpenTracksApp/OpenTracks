The libraries contained here in binary format are licensed according to their
own licenses, which allow their usage for the My Tracks application. Any other
applications that wish to use these files should consult the appropriate
licenses or the JAR's authors.

Android Support Package:
android-support-v4-r7-googlemaps.jar
- see https://github.com/petedoyle/android-support-v4-googlemaps

Ant+:
antlib.jar - see http://www.thisisant.com/pages/developer-zone/android-api

Google APIs Client Library for Java:
commons-codec-1.3.jar
commons-logging-1.1.1.jar
google-api-client-1.5.0-beta.jar
google-http-client-1.5.0-beta.jar
google-oauth-client-1.5.0-beta.jar
gson-1.6.jar
guava-r09.jar
jackson-core-asl-1.6.7.jar
jsr305-1.3.9.jar
- see http://code.google.com/p/google-api-java-client/
- google-api-client-1.5.0-beta.jar depends on protobuf-java-2.2.0.jar, however,
  MyTracksLib already includes protobuf-java.2.3.0-lite.jar. Thus using the
  newer version, protobuf-java.2.3.0-lite.jar.
- google-api-client-1.5.0-beta.jar also depends on xpp3-1.1.4c.jar, however,
  some of its classes are already included in the android framework, thus not
  including xpp3-1.1.4c.jar.

Google Analytics:
libGoogleAnalytics.jar
- see http://code.google.com/apis/analytics/docs/mobile/android.html

Others:
google-common.jar