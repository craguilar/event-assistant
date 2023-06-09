package com.cmymesh.event.assistant.model;

import com.sleepycat.persist.model.Persistent;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Set;

@Persistent(version = 4)
@Getter
@ToString
public class MessageResponse implements Serializable {
    private String templateName;
    private String errorMessage;
    private String status;
    private String messagingServiceSid;
    private String sid;
    @Setter
    private int numberOfErrorRetries;

    public static final Set<String> FAILED_STATES = Set.of("failed", "undelivered");

    public MessageResponse(String templateName, String errorMessage, String status, String messagingServiceSid, String sid) {
        this.templateName = templateName;
        this.errorMessage = errorMessage;
        this.status = status;
        this.messagingServiceSid = messagingServiceSid;
        this.sid = sid;
        this.numberOfErrorRetries = 0;
    }

    private MessageResponse() {
    }

    public boolean isFailedMessage() {
        return FAILED_STATES.contains(status);
    }
}
