package com.cmymesh.event.assistant.model;

import com.cmymesh.event.assistant.repository.EventAssistantRepository;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GuestValidations {

    private static final Logger LOG = LoggerFactory.getLogger(GuestValidations.class);
    private static final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

    public static final Function<Guest, GuestValidResponse> VALIDATE_PHONE = (Guest g) -> {
        Phonenumber.PhoneNumber phone;
        try {
            phone = phoneNumberUtil.parse(
                    g.phoneNumber(),
                    Phonenumber.PhoneNumber.CountryCodeSource.UNSPECIFIED.name());
        } catch (NumberParseException e) {
            return new GuestValidResponse(false, "Invalid format phone number %s".formatted(e.getMessage()));
        }
        if (!phoneNumberUtil.isValidNumber(phone)) {
            return new GuestValidResponse(false, "Invalid phone number");
        }
        return new GuestValidResponse(true, "");
    };

    private GuestValidations() {

    }

    public static void validate(List<Guest> guests) {
        int hasError = 0;
        for (Guest guest : guests) {

            GuestValidResponse response = guest.isValid(VALIDATE_PHONE);
            if (!response.isValid()) {
                LOG.info("{} {} - {}", guest.firstName(), guest.lastName(), response.message());
                hasError++;
            }
        }
        LOG.info("Total guests {} - {} of them have error", guests.size(), hasError);
    }

    public static void failedTracking(EventAssistantRepository assistantService) {
        var guests = assistantService.listAll().stream().filter(GuestTracking::containsErrorNotification).toList();
        for (var guest : guests) {
            LOG.info("TRACKING: {}", guest.getName());
        }
    }

    /**
     * Extracts GUESTS - TRACKING = {PENDING TRACKING} and TRACKING - GUESTS = {NOT IN SOURCE}.
     */
    public static void guestAndTrackingReconciliation(List<Guest> guests, EventAssistantRepository assistantService) {
        var tracking = assistantService.listAll();
        var guestSet = guests.stream().map(Guest::id).collect(Collectors.toSet());
        var trackingSet = tracking.stream().map(GuestTracking::getGuestId).collect(Collectors.toSet());
        guests.stream()
                .filter(val -> !trackingSet.contains(val.id()))
                .forEach(guest -> LOG.info("PENDING_TRACKING: {} {}", guest.firstName(), guest.lastName()));
        tracking.stream()
                .filter(val -> !guestSet.contains(val.getGuestId()))
                .forEach(guest -> LOG.info("GUEST_NOT_IN_SRC: {}", guest.getName()));
    }
}
