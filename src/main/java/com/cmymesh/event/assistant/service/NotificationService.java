package com.cmymesh.event.assistant.service;

import com.cmymesh.event.assistant.ApplicationConstants;
import com.cmymesh.event.assistant.MessagingMode;
import com.cmymesh.event.assistant.model.Guest;
import com.cmymesh.event.assistant.model.GuestTracking;
import com.cmymesh.event.assistant.model.GuestValidResponse;
import com.cmymesh.event.assistant.model.GuestValidations;
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
import java.util.Date;
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
        int notificationsSkipped = 0;
        LOG.info("[{}] Preparing notifications to {} guests ", template.templateName(), guests.size());
        for (Guest guest : guests) {
            var guestTracking = eventAssistantService.get(guest.id());
            var notificationAlreadySent = guestTracking != null && guestTracking.containsSuccessNotification(template.templateName());
            var notificationRetriesExhausted = guestTracking != null && guestTracking.containsNonRetryableErrorNotification(template.templateName());
            if (guest.isTentative() || notificationAlreadySent || notificationRetriesExhausted) {
                LOG.trace("Notification {}: For {} Already sent or isTentative or retries exhausted ", template.templateName(), guest.getFullName());
                notificationsSkipped++;
                continue;
            }
            var guestName = guest.getFullName();
            // Handle add or Update logic
            if (guestTracking == null) {
                guestTracking = new GuestTracking(guest.id(), guestName, new Date(), new ArrayList<>());
            }
            MessageResponse response = null;

            try {
                response = send(guest, template);
                notificationsSent = !response.isFailedMessage() ? notificationsSent + 1 : notificationsSent;
            } catch (Exception e) {
                LOG.error("When processing {}", guest);
            } finally {
                if (response != null && !template.type().equals(MessagingMode.DRYRUN)) {
                    guestTracking.addOrUpdateNotification(response);
                    eventAssistantService.save(guestTracking);
                }
            }

        }
        LOG.info("Notifications sent {} , skipped {}", notificationsSent, notificationsSkipped);
    }

    private MessageResponse send(Guest guest, NotificationTemplate template) throws InterruptedException {
        GuestValidResponse guestValidation = guest.isValid(GuestValidations.VALIDATE_PHONE);
        if (!guestValidation.isValid()) {
            return new MessageResponse(template.templateName(), guestValidation.message(), "failed", "whatsapp", null);
        }

        var messaging = messagingFactory.getService(template.type());
        var processedBody = template.freeFormBody();
        var isFreeForm = StringUtils.isNotBlank(processedBody);
        var toPhone = guest.phoneNumber();
        if (isFreeForm) {
            return messaging.sendFreeFormMessage(toPhone, template, processedBody);
        }
        var components = processTemplate(guest, template);
        return messaging.sendTemplateMessage(toPhone, template, components);
    }


    /**
     * Decided to implement a "dynamic" template processing engine where application users can define their own template
     * processors by adding classes to "com.cmymesh.event.assistant.service" package that implements from {@link ComponentTemplate}
     * and loading the classes during runtime here.
     *
     * @param guest    the current processing guest.
     * @param template the current processing template.
     * @return a list of NotificationTemplateComponent used by the Notification engine.
     */
    private List<NotificationTemplateComponent> processTemplate(Guest guest, NotificationTemplate template) {

        try {
            ComponentTemplate componentTemplate = (ComponentTemplate) this.getClass().getClassLoader()
                    .loadClass(ApplicationConstants.PLUGINS_BASE_PACKAGE + ".%s".formatted(template.ComponentProcessingClass()))
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
