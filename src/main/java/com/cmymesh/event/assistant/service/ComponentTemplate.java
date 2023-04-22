package com.cmymesh.event.assistant.service;


import com.cmymesh.event.assistant.model.Guest;
import com.cmymesh.event.assistant.model.NotificationTemplateComponent;

import java.util.List;

/**
 * Decided to implement a "dynamic" template processing engine where application users can define their own template
 * processors by adding classes to "com.cmymesh.event.assistant.service" package that implements from {@link ComponentTemplate}
 * this class.
 */
@FunctionalInterface
public interface ComponentTemplate {
    List<NotificationTemplateComponent> processTemplate(Guest guest);
}
