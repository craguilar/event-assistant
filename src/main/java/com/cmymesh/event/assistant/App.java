package com.cmymesh.event.assistant;

import com.cmymesh.event.assistant.model.GuestValidations;
import com.cmymesh.event.assistant.model.NotificationTemplate;
import com.cmymesh.event.assistant.repository.EventAssistantRepository;
import com.cmymesh.event.assistant.repository.GuestRepositoryFactory;
import com.cmymesh.event.assistant.repository.TemplateRepository;
import com.cmymesh.event.assistant.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.time.Duration;

import static java.util.Objects.requireNonNull;

/**
 * Event Assistant app! Yet another way of reinventing the wheel for sending Event notifications.
 */
@Slf4j
public class App {

    public static void main(String[] args) throws ParseException, InterruptedException {
        var cmd = CmdOptions.parseCmdOptions(args);
        var eventId = cmd.getOptionValue(CmdOptions.EVENT_ID_OPTION);
        var guestStorageMode = GuestStorageMode
                .valueOf(cmd.getOptionValue(CmdOptions.STORAGE_MODE_OPTION, GuestStorageMode.DYNAMODB.toString()));
        var runningMode = AppRunningMode
                .valueOf(cmd.getOptionValue(CmdOptions.APP_RUNNING_MODE_OPTION, AppRunningMode.GUEST_VALIDATE.toString()));
        var dataStorePath = new File("./bdb.data");
        requireNonNull(eventId, "Event Id must be not null");
        log.info("Running program with parameters eventId [{}] , storageMode [{}] ,runningMode [{}]"
                ,eventId,guestStorageMode,runningMode);
        run(eventId, dataStorePath, guestStorageMode, runningMode);
    }

    private static void run(String eventId, File dataStorePath, GuestStorageMode guestStorageMode,
                            AppRunningMode runningMode) throws InterruptedException {

        try (var eventAssistant = new EventAssistantRepository(dataStorePath)) {
            var templateService = new TemplateRepository();
            var notificationService = new NotificationService(eventAssistant);
            var guestService = GuestRepositoryFactory.guestService(guestStorageMode);

            // Execute
            switch (runningMode) {
                case GUEST_VALIDATE -> GuestValidations.validate(guestService.listGuests(eventId));
                case TRACKING_FAILED_REPORT -> GuestValidations.failedTracking(eventAssistant);
                case TRACKING_GUEST_RECONCILIATION ->
                        GuestValidations.guestAndTrackingReconciliation(guestService.listGuests(eventId), eventAssistant);
                case TRACKING_DUMP -> eventAssistant.dump();
                case SEND_NOTIFICATIONS -> {
                    for (NotificationTemplate template : templateService.listTemplates(eventId)) {
                        log.info("Before sending notification [{}] as [{}] breathe for 1 minute.... ",
                                template.templateName(), template.type());
                        Thread.sleep(Duration.ofMinutes(1).toMillis());
                        notificationService.sendNotifications(guestService.listGuests(eventId), template);
                    }

                }
                default -> throw new RuntimeException("Invalid runningMode");
            }
        }
    }
}
