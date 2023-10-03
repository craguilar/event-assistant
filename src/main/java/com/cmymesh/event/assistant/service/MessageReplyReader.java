package com.cmymesh.event.assistant.service;

import com.cmymesh.event.assistant.exception.InvalidInputException;
import com.cmymesh.event.assistant.exception.InvalidPhoneException;
import com.cmymesh.event.assistant.model.Guest;
import com.cmymesh.event.assistant.model.GuestValidations;
import com.cmymesh.event.assistant.model.MessageReply;
import com.cmymesh.event.assistant.repository.GuestRepository;
import com.cmymesh.event.assistant.repository.MessagesRepositoryDynamoDb;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageReplyReader {

    private static final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();


    private final GuestRepository guestRepository;
    private final MessagesRepositoryDynamoDb messagesRepository;

    public MessageReplyReader(GuestRepository guestRepository, MessagesRepositoryDynamoDb messagesRepository) {
        this.guestRepository = guestRepository;
        this.messagesRepository = messagesRepository;
    }

    public void getRepliesReport(String eventId, String repliesToPhoneId) {
        if (StringUtils.isBlank(eventId) || StringUtils.isBlank(repliesToPhoneId)) {
            throw new InvalidInputException("Invalid parameters eventId and repliesToPhoneId MUST be not null");
        }
        Map<Long, List<MessageReply>> replies = new HashMap<>();
        Map<Long, String> guests = new HashMap<>();

        for (Guest guest : guestRepository.listGuests(eventId)) {
            if (!guest.isValid(GuestValidations.VALIDATE_PHONE).isValid()) {
                continue;
            }
            Phonenumber.PhoneNumber phoneNumber = null;
            try {
                phoneNumber = phoneNumberUtil.parse(
                        guest.phoneNumber(),
                        Phonenumber.PhoneNumber.CountryCodeSource.UNSPECIFIED.name());
            } catch (NumberParseException e) {
                throw new InvalidPhoneException(e);
            }
            guests.put(phoneNumber.getNationalNumber(), guest.getFullName());
        }
        for (MessageReply reply : messagesRepository.getReplies(repliesToPhoneId)) {
            // localize number
            var phone = reply.phoneNumber().substring(Math.max(reply.phoneNumber().length() - 10, 0));

            var messages = replies.getOrDefault(Long.parseLong(phone), new ArrayList<>());
            messages.add(reply);
            replies.put(Long.parseLong(phone), messages);
        }
        for (Map.Entry<Long, List<MessageReply>> entry : replies.entrySet()) {
            var fullName = guests.getOrDefault(entry.getKey(), entry.getKey().toString());
            System.out.println(fullName + " replied: ");
            entry.getValue().forEach(m -> System.out.println("\t " + m.time() + " [" + m.reply() + "]"));
        }
    }
}
