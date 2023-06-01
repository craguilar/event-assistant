package com.cmymesh.event.assistant.model;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuestTrackingTest {

    @Test
    void testSimilarity() {
        LevenshteinDistance distance = new LevenshteinDistance();
        assertEquals(2, distance.apply("juan_y_paco_savethedate_revised", "jua_y_paco_savethedate_revised "));

    }

    @Test
    void testSuccessNotifications() {
        List<MessageResponse> responses = List.of(new MessageResponse("jua_y_paco_savethedate_revised ", null, "accepted", "1", "1"));
        GuestTracking tracking = new GuestTracking("1", "Test name", new Date(), responses);
        assertTrue(tracking.containsSuccessNotification("juan_y_paco_savethedate_revised"));
    }
}
