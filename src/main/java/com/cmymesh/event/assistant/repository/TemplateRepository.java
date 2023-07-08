package com.cmymesh.event.assistant.repository;

import com.cmymesh.event.assistant.model.NotificationTemplate;
import com.cmymesh.event.assistant.MessagingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class TemplateRepository {

    private static final Logger LOG = LoggerFactory.getLogger(TemplateRepository.class);
    private static final Map<String, List<NotificationTemplate>> db = new HashMap<>();

    public TemplateRepository() {
        var uri = ClassLoader.getSystemResource("templates.local");
        if (uri == null) {
            throw new MissingResourceException("Missing resource templates.local", GuestRepositoryLocalImpl.class.getName(), "templates.local");
        }
        AtomicInteger count = new AtomicInteger();
        try (var lines = Files.lines(Paths.get(uri.toURI()))) {

            Objects.requireNonNull(lines);
            lines
                    .map(line -> line.split(","))
                    .forEach(fields -> {
                        List<NotificationTemplate> existing = db.getOrDefault(fields[0], new ArrayList<>());
                        existing.add(new NotificationTemplate(fields[1], fields[2], MessagingMode.valueOf(fields[3]), fields[4], fields[5],fields[6]));
                        count.getAndIncrement();
                        db.put(fields[0], existing);
                    });
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
        LOG.debug("Loaded {} events and {} templates", db.size(), count.get());
    }

    public List<NotificationTemplate> listTemplates(String eventId) {
        return db.getOrDefault(eventId, Collections.emptyList());
    }
}
