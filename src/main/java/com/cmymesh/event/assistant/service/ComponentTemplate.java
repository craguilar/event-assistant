package com.cmymesh.event.assistant.service;


import com.cmymesh.event.assistant.model.Guest;
import com.cmymesh.event.assistant.model.NotificationTemplateComponent;

import java.util.List;

@FunctionalInterface
public interface ComponentTemplate {
    List<NotificationTemplateComponent> processTemplate(Guest guest);
}
