package com.cmymesh.event.assistant;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class CmdOptions {

    public static final String EVENT_ID_OPTION = "eventId";
    public static final String STORAGE_MODE_OPTION = "storageMode";
    public static final String APP_RUNNING_MODE_OPTION = "runningMode";
    public static final String SENDER_PHONE_ID_OPTION = "senderPhoneId";

    private CmdOptions() {
        // Utility class adding private constructor
    }

    public static CommandLine parseCmdOptions(String[] args) throws ParseException {
        Options options = new Options();

        Option eventId = new Option("e", EVENT_ID_OPTION, true, "EventId");
        eventId.setRequired(true);
        options.addOption(eventId);

        Option storageMode = new Option("s", STORAGE_MODE_OPTION, true, "Storage mode LOCAL or DYNAMODB");
        options.addOption(storageMode);

        String runningModes = Stream.of(AppRunningMode.values())
                .map(Enum::toString)
                .collect(Collectors.joining(","));
        Option appRunningMode = new Option("r", APP_RUNNING_MODE_OPTION, true, "App Running modes %s".formatted(runningModes));
        options.addOption(appRunningMode);
        Option senderPhoneId = new Option("p",SENDER_PHONE_ID_OPTION, true ,"Sender Phone Id ");
        options.addOption(senderPhoneId);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            return parser.parse(options, args);
        } catch (ParseException e) {
            log.error("", e);
            formatter.printHelp("event-assistant", options);
            throw e;
        }
    }
}
