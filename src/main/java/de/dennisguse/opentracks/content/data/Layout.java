package de.dennisguse.opentracks.content.data;

import java.util.ArrayList;
import java.util.List;

public class Layout {
    private final String profile;
    private final List<Field> fields = new ArrayList<>();

    public Layout(String profile) {
        this.profile = profile;
    }

    public void addField(String title, boolean visible, boolean primary) {
        fields.add(new Field(title, visible, primary));
    }

    public void addField(Field field) {
        fields.add(field);
    }

    public void removeField(Field field) {
        fields.remove(field);
    }

    public List<Field> getFields() {
         return fields;
    }

    public String getProfile() {
        return profile;
    }

    public static class Field {
        private final String title;
        private boolean visible;
        private boolean primary;

        public Field(String title, boolean visible, boolean primary) {
            this.title = title;
            this.visible = visible;
            this.primary = primary;
        }

        public String getTitle() {
            return title;
        }

        public boolean isVisible() {
            return visible;
        }

        public void toggleVisibility() {
            visible = !visible;
        }

        public boolean isPrimary() {
            return primary;
        }

        public void togglePrimary() {
            primary = !primary;
        }
    }
}
