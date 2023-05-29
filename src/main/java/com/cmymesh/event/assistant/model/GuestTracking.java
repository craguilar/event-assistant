package com.cmymesh.event.assistant.model;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.SecondaryKey;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.io.Serializable;
import java.util.List;

import static com.sleepycat.persist.model.Relationship.ONE_TO_ONE;

@Entity(version = 2)
@Getter
@ToString
public class GuestTracking implements Serializable {

    @PrimaryKey
    private String guestId;

    @SecondaryKey(relate = ONE_TO_ONE)
    private String name; // Composed of firstName + lastName

    private List<MessageResponse> notificationsSent;

    private final transient  LevenshteinDistance distance = new LevenshteinDistance();

    public GuestTracking(String guestId, String name, List<MessageResponse> notificationsSent) {
        this.guestId = guestId;
        this.name = name;
        this.notificationsSent = notificationsSent;
    }

    private GuestTracking() {
        // For deserialization
    }

    public boolean containsSuccessNotification(String templateName) {
        return notificationsSent.stream()
                .filter(m -> !m.isFailedMessage())
                .anyMatch(n -> distance.apply(n.getTemplateName(), templateName) <= 5);
    }

    public void addOrUpdateNotification(MessageResponse response) {
        MessageResponse notification = null;
        int i;
        for (i = 0; i < notificationsSent.size(); i++) {
            if (response.getTemplateName().equalsIgnoreCase(notificationsSent.get(i).getTemplateName())) {
                notification = notificationsSent.get(i);
                break;
            }
        }
        if (notification == null) {
            notificationsSent.add(response);
        } else {
            notificationsSent.set(i, response);
        }
    }

    public boolean containsErrorNotification() {
        return notificationsSent.stream()
                .anyMatch(MessageResponse::isFailedMessage);
    }
}
