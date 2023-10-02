package com.cmymesh.event.assistant;

import com.cmymesh.event.assistant.model.GuestValidations;
import com.cmymesh.event.assistant.model.NotificationTemplate;
import com.cmymesh.event.assistant.repository.EventAssistantRepository;
import com.cmymesh.event.assistant.repository.GuestRepositoryFactory;
import com.cmymesh.event.assistant.repository.MessagesRepositoryDynamoDb;
import com.cmymesh.event.assistant.repository.TemplateRepository;
import com.cmymesh.event.assistant.service.MessageReplyReader;
import com.cmymesh.event.assistant.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.ParseException;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.io.File;
import java.time.Duration;

import static com.cmymesh.event.assistant.AppRunningMode.TRACKING_MESSAGE_REPLIES;
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
        var senderPhoneId = cmd.getOptionValue(CmdOptions.SENDER_PHONE_ID_OPTION);
        requireNonNull(eventId, "Event Id must be not null");
        if (TRACKING_MESSAGE_REPLIES.equals(runningMode)) {
            requireNonNull(senderPhoneId, "When tracking replies Phone Id must be not null");
        }
        log.info("Running program with parameters eventId [{}] , storageMode [{}] ,runningMode [{}]"
                , eventId, guestStorageMode, runningMode);
        run(eventId, dataStorePath, guestStorageMode, runningMode, senderPhoneId);
    }

    private static void run(String eventId, File dataStorePath, GuestStorageMode guestStorageMode,
                            AppRunningMode runningMode, String senderPhoneId) throws InterruptedException {

        try (var eventAssistant = new EventAssistantRepository(dataStorePath)) {

            var ddb = DynamoDbClient.builder().credentialsProvider(ProfileCredentialsProvider.create()).build();
            var guestFactory = new GuestRepositoryFactory(ddb);
            var guestService = guestFactory.guestService(guestStorageMode);
            // Execute
            switch (runningMode) {
                case GUEST_VALIDATE -> GuestValidations.validate(guestService.listGuests(eventId));
                case TRACKING_FAILED_REPORT -> GuestValidations.failedTracking(eventAssistant);
                case TRACKING_GUEST_RECONCILIATION ->
                        GuestValidations.guestAndTrackingReconciliation(guestService.listGuests(eventId), eventAssistant);
                case TRACKING_DUMP -> eventAssistant.dump();
                case TRACKING_MESSAGE_REPLIES -> {
                    var messageRepository = new MessagesRepositoryDynamoDb(ddb);
                    var replyReader = new MessageReplyReader(guestService, messageRepository);
                    replyReader.getRepliesReport(eventId, senderPhoneId);
                }
                case SEND_NOTIFICATIONS -> {
                    var templateService = new TemplateRepository();
                    var notificationService = new NotificationService(eventAssistant);
                    var templates = templateService.listTemplates(eventId);
                    log.info("Sending {} templates for {}", templates.size(), eventId);
                    for (NotificationTemplate template : templates) {
                        var guests = guestService.listGuests(eventId);
                        log.info("Before sending notification [{}] as [{}] to [{}] guests, breathe for 1 minute.... ",
                                template.templateName(), template.type(), guests.size());
                        Thread.sleep(Duration.ofMinutes(1).toMillis());
                        notificationService.sendNotifications(guests, template);
                    }

                }
                default -> throw new RuntimeException("Invalid runningMode");
            }
        }
    }
}