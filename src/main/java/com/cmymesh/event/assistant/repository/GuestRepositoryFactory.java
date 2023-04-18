package com.cmymesh.event.assistant.repository;

import com.cmymesh.event.assistant.GuestStorageMode;

public class GuestRepositoryFactory {

    private GuestRepositoryFactory() {
    }

    public static GuestRepository guestService(GuestStorageMode storage) {
        return switch (storage) {
            case LOCAL -> new GuestRepositoryLocalImpl();
            case DYNAMODB -> new GuestRepositoryDynamoDbImpl();
        };
    }
}
