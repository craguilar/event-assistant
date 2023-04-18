package com.cmymesh.event.assistant.service;

import com.cmymesh.event.assistant.model.Guest;
import com.cmymesh.event.assistant.model.GuestTracking;
import com.cmymesh.event.assistant.model.MessageResponse;
import com.cmymesh.event.assistant.model.NotificationTemplate;
import com.cmymesh.event.assistant.model.NotificationTemplateComponent;
import com.cmymesh.event.assistant.repository.EventAssistantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.utils.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);

    private final MessagingServiceFactory messagingFactory;
    private final EventAssistantRepository eventAssistantService;

    public NotificationService(EventAssistantRepository eventAssistantService) {
        messagingFactory = new MessagingServiceFactory();
        this.eventAssistantService = eventAssistantService;
    }

    public void sendNotifications(List<Guest> guests, NotificationTemplate template) {
        int notificationsSent = 0;
        LOG.info("Sending notifications to {} guests", guests.size());
        for (Guest guest : guests) {
            var guestTracking = eventAssistantService.get(guest.id());
            if (guest.isTentative() || guestTracking != null && guestTracking.containsSuccessNotification(template.templateName())) {
                LOG.trace("Notification {}: Already sent to {} or isTentative or is USA or is Dr", template.templateName(), guestTracking);
                continue;
            }
            var toPhone = guest.phoneNumber();
            var messaging = messagingFactory.getService(template.type());

            var processedBody = template.freeFormbody();
            var guestName = "%s %s".formatted(guest.firstName(), guest.lastName());
            var isFreeForm = StringUtils.isNotBlank(processedBody);
            MessageResponse response = null;
            try {
                if (isFreeForm) {
                    response = messaging.sendFreeFormMessage(toPhone, template, processedBody);
                } else {
                    var components = processTemplate(guest,template);
                    response = messaging.sendTemplateMessage(toPhone, template, components);
                }
                notificationsSent = !response.isFailedMessage() ? notificationsSent + 1 : 0;

            } catch (Exception e) {
                LOG.error("When processing {}", toPhone, e);
            } finally {
                // TODO: Handle validations
                if (response != null) {
                    List<MessageResponse> templates = new ArrayList<>();
                    templates.add(response);
                    eventAssistantService.save(new GuestTracking(guest.id(), guestName, templates));
                }
            }

        }
        LOG.info("Notifications sent {}", notificationsSent);
    }

    private List<NotificationTemplateComponent> processTemplate(Guest guest, NotificationTemplate template) {

        try {
            ComponentTemplate componentTemplate = (ComponentTemplate) this.getClass().getClassLoader()
                    .loadClass("com.cmymesh.event.plugins.%s".formatted(template.ComponentProcessingClass()))
                    .getDeclaredConstructor()
                    .newInstance();
            return componentTemplate.processTemplate(guest);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | ClassNotFoundException |
                 InvocationTargetException e) {
            LOG.error("", e);
            return Collections.emptyList();
        }
    }
}
