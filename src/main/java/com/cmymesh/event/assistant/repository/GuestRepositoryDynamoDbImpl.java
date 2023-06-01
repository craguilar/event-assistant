package com.cmymesh.event.assistant.repository;

import com.cmymesh.event.assistant.model.Guest;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuestRepositoryDynamoDbImpl implements GuestRepository {

    private final DynamoDbClient ddb;

    public GuestRepositoryDynamoDbImpl() {
        /*
          I opt for the client to derive the correct region from either AWS_REGION environment variable or the
          default profile in the aws config file . This might not be great and could have performance penalties, but
          it really simplifies this class and configuration injection.
         */
        ddb = DynamoDbClient.builder()
                .credentialsProvider(ProfileCredentialsProvider.create())
                .build();
    }

    @Override
    public List<Guest> listGuests(String eventId) {
        Map<String, Condition> conditions = new HashMap<>();
        conditions.put("id", Condition.builder()
                .comparisonOperator("EQ")
                .attributeValueList(List.of(AttributeValue.builder().s(eventId).build()))
                .build());
        conditions.put("entityType", Condition.builder()
                .comparisonOperator("BEGINS_WITH")
                .attributeValueList(List.of(AttributeValue.builder().s("GUEST-").build()))
                .build());
        QueryRequest request = QueryRequest.builder()
                .tableName("events")
                .keyConditions(conditions)
                .build();
        QueryResponse response = ddb.query(request);
        List<Guest> guests = new ArrayList<>();
        for (Map<String, AttributeValue> item : response.items()) {
            Guest guest = new Guest(
                    item.get("entityType").s(),
                    item.get("firstName").s(),
                    item.get("lastName").s(),
                    item.get("guestOf").s(),
                    item.get("country").s(),
                    item.get("state").s(),
                    item.get("phone").s(),
                    item.get("isTentative").bool(),
                    Integer.parseInt(item.get("numberOfSeats").n()));

            guests.add(guest);
        }
        return guests;
    }
}
