package com.cmymesh.event.assistant.model;

import java.time.Instant;

public record MessageReply(String phoneNumber, Instant time, String reply) {
}
