package com.cmymesh.event.assistant.model;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.SecondaryKey;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import static com.sleepycat.persist.model.Relationship.ONE_TO_ONE;

@Entity(version = 3)
@Getter
@ToString
public class GuestTracking implements Serializable {

    @PrimaryKey
    private String guestId;

    @SecondaryKey(relate = ONE_TO_ONE)
    private String name; // Composed of firstName + lastName

    @Setter
    private Date timeCreated;

    private List<MessageResponse> notificationsSent;

    private static final int MAX_NUMBER_OF_RETRIES = 3;
    private final transient LevenshteinDistance distance = new LevenshteinDistance();

    public GuestTracking(String guestId, String name, Date timeCreated, List<MessageResponse> notificationsSent) {
        this.guestId = guestId;
        this.name = name;
        this.timeCreated = timeCreated;
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
            return;
        }
        var isSuccess = response.isFailedMessage();
        if (!isSuccess) {
            response.setNumberOfErrorRetries(response.getNumberOfErrorRetries() + 1);
        }
        notificationsSent.set(i, response);
    }

    public boolean containsErrorNotification() {
        return notificationsSent.stream()
                .anyMatch(MessageResponse::isFailedMessage);
    }

    public boolean containsNonRetryableErrorNotification(String templateName) {
        return notificationsSent.stream()
                .filter(MessageResponse::isFailedMessage)
                .filter(m -> m.getNumberOfErrorRetries()>=MAX_NUMBER_OF_RETRIES)
                .anyMatch(n -> distance.apply(n.getTemplateName(), templateName) <= 5);
    }
}
