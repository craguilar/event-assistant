package com.cmymesh.event.assistant.model;

import com.cmymesh.event.assistant.MessagingMode;


public record NotificationTemplate(String templateName,
                                   String fromPhoneNumber,
                                   MessagingMode type,
                                   String languageCode,
                                   String freeFormBody,
                                   String ComponentProcessingClass) {
}

