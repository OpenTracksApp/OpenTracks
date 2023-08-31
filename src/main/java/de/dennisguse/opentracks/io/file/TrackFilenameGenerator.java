package de.dennisguse.opentracks.io.file;

import androidx.annotation.NonNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.util.FileUtils;

public class TrackFilenameGenerator {

    public static final String UUID_KEY = "{uuid}";
    public static final String TRACKNAME_KEY = "{name}";
    public static final String ACTIVITY_TYPE_KEY = "{category}";
    public static final String STARTTIME_TIME_KEY = "{time}";
    public static final String STARTTIME_DATE_KEY = "{date}";

    public static String getAllOptions() {
        return Stream.of(UUID_KEY, TRACKNAME_KEY, ACTIVITY_TYPE_KEY, STARTTIME_TIME_KEY, STARTTIME_DATE_KEY)
                .collect(Collectors.joining(", "));
    }

    public static String format(@NonNull String name, @NonNull TrackFileFormat trackFileFormat) {
        return FileUtils.sanitizeFileName(name + "." + trackFileFormat.getExtension());
    }

    private final String template;

    public TrackFilenameGenerator(@NonNull String template) {
        this.template = template;
    }

    public String format(@NonNull Track track, @NonNull TrackFileFormat trackFileFormat) {
        Map<String, String> values = new HashMap<>();

        values.put(UUID_KEY, track.getUuid().toString().substring(0, 8));
        values.put(TRACKNAME_KEY, track.getName());
        values.put(ACTIVITY_TYPE_KEY, track.getActivityTypeLocalized());
        values.put(STARTTIME_TIME_KEY, track.getStartTime().toLocalTime().toString());
        values.put(STARTTIME_DATE_KEY, track.getStartTime().toLocalDate().toString());

        return format(format(template, values), trackFileFormat);
    }

    private static String format(String template, Map<String, String> values) {
        StringBuilder templateCompiler = new StringBuilder(template);
        List<String> valueList = new ArrayList<>();

        Matcher keyMatcher = Pattern
                .compile("\\{(\\w+)\\}")
                .matcher(template);

        while (keyMatcher.find()) {
            String key = keyMatcher.group();

            if (!values.containsKey(key)) {
                throw new TemplateInvalidException(key);
            }

            int index = templateCompiler.indexOf(key);
            if (index != -1) {
                templateCompiler.replace(index, index + key.length(), "%s");
                valueList.add(values.get(key));
            }
        }

        String templateCompiled = templateCompiler.toString();
        if (templateCompiled.contains("{") || templateCompiled.contains("}")) {
            throw new TemplateInvalidException(template);
        }

        return String.format(templateCompiled, valueList.toArray());
    }

    public boolean isValid() {
        try {
            getExample();
            return !template.isEmpty();
        } catch (TemplateInvalidException | NullPointerException e) {
            return false;
        }
    }

    public String getExample() {
        Track track = new Track();
        track.setName("Berlin");
        track.setUuid(UUID.fromString("f81d4fae-7dec-11d0-a765-00a0c91e6bf6"));
        track.getTrackStatistics().setStartTime(Instant.ofEpochMilli(0));

        return format(track, TrackFileFormat.KMZ_WITH_TRACKDETAIL_AND_SENSORDATA_AND_PICTURES);
    }

    public static class TemplateInvalidException extends RuntimeException {
        public TemplateInvalidException(String invalidTemplate) {
            super(invalidTemplate);
        }
    }
}
