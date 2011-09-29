The libraries contained here in binary format are licensed according to their
own licenses, which allow their usage for the My Tracks application. Any other
applications that wish to use these files should consult the appropriate
licenses or the JAR's authors.

google-api-client/
- see http://code.google.com/p/google-api-java-client/.
- contains Google API client library, google-api-client-1.5.0-beta.jar, and its
  dependencies.
- google-api-client-1.5.0-beta.jar depends on protobuf-java-2.2.0.jar, however,
  MyTracksLib already includes protobuf-java.2.3.0-lite.jar. Thus using the
  newer version, protobuf-java.2.3.0-lite.jar.
- google-api-client-1.5.0-beta.jar also depends on xpp3-1.1.4c.jar, however,
  some of its classes are already included in the android framework, thus not
  including xpp3-1.1.4c.jar.

google-common.jar

googleloginclient-helper.jar

libGoogleAnalytics.jar
- see http://code.google.com/mobile/analytics/docs/.
