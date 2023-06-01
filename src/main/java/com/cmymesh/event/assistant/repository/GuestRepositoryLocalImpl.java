package com.cmymesh.event.assistant.repository;

import com.cmymesh.event.assistant.model.Guest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Objects;

public class GuestRepositoryLocalImpl implements GuestRepository {

    private static final Logger LOG = LoggerFactory.getLogger(GuestRepositoryLocalImpl.class);

    @Override
    public List<Guest> listGuests(String eventId) {

        var uri = ClassLoader.getSystemResource("guests.local");
        if (uri == null) {
            throw new MissingResourceException("Missing resource guests.local", GuestRepositoryLocalImpl.class.getName(), "guests.local");
        }
        try (var lines = Files.lines(Paths.get(uri.toURI()))) {
            Objects.requireNonNull(lines);
            return lines
                    .map(line -> line.split(","))
                    .filter(fields -> fields.length == 9)
                    .map(fields ->
                            Guest.builder()
                                    .id(fields[0])
                                    .firstName(fields[1])
                                    .lastName(fields[2])
                                    .guestOf(fields[3])
                                    .country(fields[4])
                                    .state(fields[5])
                                    .phoneNumber(fields[6])
                                    .isTentative(Boolean.getBoolean(fields[7]))
                                    .seats(Integer.parseInt(fields[8]))
                                    .build()
                    ).toList();
        } catch (URISyntaxException | IOException e) {
            LOG.error("{}", e);
            throw new RuntimeException(e);
        }
    }
}