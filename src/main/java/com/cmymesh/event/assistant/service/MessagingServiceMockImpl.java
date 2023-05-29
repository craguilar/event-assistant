package com.cmymesh.event.assistant.service;

import com.cmymesh.event.assistant.model.MessageResponse;
import com.cmymesh.event.assistant.model.MessageStatus;
import com.cmymesh.event.assistant.model.NotificationTemplate;
import com.cmymesh.event.assistant.model.NotificationTemplateComponent;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

@Slf4j
public class MessagingServiceMockImpl implements MessagingService {

    @Override
    public MessageResponse sendTemplateMessage(String toPhoneNumber, NotificationTemplate template, List<NotificationTemplateComponent> components) throws InterruptedException {
        log.info("Sending message to {} with template {}", toPhoneNumber, template.templateName());
        return new MessageResponse(template.templateName(), null, "accepted", null, UUID.randomUUID().toString());
    }
}
