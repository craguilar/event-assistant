package com.cmymesh.event.assistant.repository;

import com.cmymesh.event.assistant.GuestStorageMode;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class GuestRepositoryFactory {

    private final DynamoDbClient ddb;

    public GuestRepositoryFactory(DynamoDbClient ddb) {
        this.ddb = ddb;
    }

    public GuestRepository guestService(GuestStorageMode storage) {
        return switch (storage) {
            case LOCAL -> new GuestRepositoryLocalImpl();
            case DYNAMODB -> new GuestRepositoryDynamoDbImpl(ddb);
        };
    }
}
