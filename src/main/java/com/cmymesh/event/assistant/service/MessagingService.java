package com.cmymesh.event.assistant.service;

import com.cmymesh.event.assistant.model.MessageResponse;
import com.cmymesh.event.assistant.model.NotificationTemplate;
import com.cmymesh.event.assistant.model.NotificationTemplateComponent;

import java.util.List;

public interface MessagingService {

    MessageResponse sendTemplateMessage(String toPhoneNumber, NotificationTemplate template, List<NotificationTemplateComponent> components) throws InterruptedException;

    default MessageResponse sendFreeFormMessage(String toPhoneNumber, NotificationTemplate template, String body) {
        throw new UnsupportedOperationException("Not supported exception");
    }
}
