package com.lithium.dog.event;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Models the event message to be sent to dogstatsd.
 */
public class EventMessage {

    private final String title, message, aggregationKey;
    private final Priority priority;
    private final AlertType alertType;
    private final Set<String> tags;

    public EventMessage(String title,
                        String message,
                        String aggregationKey,
                        Priority priority,
                        AlertType alertType,
                        Collection<String> tags) {
        this.title = title;
        this.message = message;
        this.aggregationKey = aggregationKey;
        this.priority = priority;
        this.alertType = alertType;
        this.tags = (tags == null) ? Collections.<String>emptySet() : new LinkedHashSet<>(tags);
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(String.format("_e{%d,%d}:%s|%s", title.length(), message.length(), title, message));
        if (aggregationKey != null) {
            sb.append(String.format("|k:%s", aggregationKey));
        }
        if (priority != null) {
            sb.append(String.format("|p:%s", priority.name()));
        }
        if (alertType != null) {
            sb.append(String.format("|t:%s", alertType.name()));
        }
        if (!tags.isEmpty()) {
            sb.append("|#");
            int i = 0;
            for (String tag : tags) {
                if (i++ > 0) {
                    sb.append(",");
                }
                sb.append(tag);
            }
        }
        return sb.toString();
    }

}
