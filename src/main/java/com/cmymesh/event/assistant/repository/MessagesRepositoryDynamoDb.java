package com.cmymesh.event.assistant.repository;

import com.cmymesh.event.assistant.model.MessageReply;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessagesRepositoryDynamoDb {

    private final DynamoDbClient ddb;

    public MessagesRepositoryDynamoDb(DynamoDbClient ddb) {
        this.ddb = ddb;
    }

    public List<MessageReply> getReplies(String toPhoneId) {

        Map<String, Condition> conditions = new HashMap<>();
        conditions.put("id", Condition.builder()
                .comparisonOperator("EQ")
                .attributeValueList(List.of(AttributeValue.builder().s(toPhoneId).build()))
                .build());
        conditions.put("type", Condition.builder()
                .comparisonOperator("BEGINS_WITH")
                .attributeValueList(List.of(AttributeValue.builder().s("RECIPIENT-").build()))
                .build());
        QueryRequest request = QueryRequest.builder()
                .tableName("messages")
                .keyConditions(conditions)
                .build();
        QueryResponse response = ddb.query(request);
        List<MessageReply> replies = new ArrayList<>();
        for (Map<String, AttributeValue> item : response.items()) {
            var type = item.get("type").s();
            if (type.startsWith("RECIPIENT-ERROR")) {
                continue;
            }
            var details = type.split("-");
            var phone = details[1];
            var timeStamp = Instant.ofEpochSecond(Long.parseLong(details[2]));
            var message = item.get("document").s();
            var reply = new MessageReply(phone, timeStamp, message);
            replies.add(reply);
        }
        return replies;
    }
}
