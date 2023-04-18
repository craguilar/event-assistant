package com.cmymesh.event.assistant.repository;

import com.cmymesh.event.assistant.model.Guest;

import java.util.List;

public interface GuestRepository {

    /**
     * @param eventId associated to the Guests to fetch
     * @return a list of {@link Guest} from the configured data source.
     */
    List<Guest> listGuests(String eventId);
}
