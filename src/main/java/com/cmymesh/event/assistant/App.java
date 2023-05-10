package com.cmymesh.event.assistant;

import com.cmymesh.event.assistant.model.GuestValidations;
import com.cmymesh.event.assistant.model.NotificationTemplate;
import com.cmymesh.event.assistant.repository.EventAssistantRepository;
import com.cmymesh.event.assistant.repository.GuestRepositoryFactory;
import com.cmymesh.event.assistant.repository.TemplateRepository;
import com.cmymesh.event.assistant.service.NotificationService;

import java.io.File;
import java.util.Objects;

/**
 * Event Assistant app! Yet another way of reinventing the wheel for sending Event notifications.
 */
public class App {

    public static void main(String[] args) {

        // TODO: Parameter parsing
        String eventId = null;
        var guestStorageMode = GuestStorageMode.DYNAMODB;
        var runningMode = AppRunningMode.DUMP_TRACKING;
        var dataStorePath = new File("./bdb.data");

        Objects.requireNonNull(eventId, "Event Id must be not null");
        run(eventId, dataStorePath, guestStorageMode, runningMode);
    }

    private static void run(String eventId, File dataStorePath, GuestStorageMode guestStorageMode, AppRunningMode runningMode) {

        try (var eventAssistant = new EventAssistantRepository(dataStorePath)) {
            var templateService = new TemplateRepository();
            var notificationService = new NotificationService(eventAssistant);
            var guestService = GuestRepositoryFactory.guestService(guestStorageMode);

            // Execute
            switch (runningMode) {
                case VALIDATE -> GuestValidations.validate(guestService.listGuests(eventId));
                case TRACKING_FAILED_REPORT -> GuestValidations.failedTracking(eventAssistant);
                case SEND_NOTIFICATIONS -> {
                    for (NotificationTemplate template : templateService.listTemplates(eventId)) {
                        notificationService.sendNotifications(guestService.listGuests(eventId), template);
                    }
                }
                case DUMP_TRACKING -> eventAssistant.dump();
                default -> throw new RuntimeException("Invalid runningMode");
            }
        }
    }
}
