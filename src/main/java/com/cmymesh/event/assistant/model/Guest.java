package com.cmymesh.event.assistant.model;

import lombok.Builder;

import java.util.function.Function;

@Builder
public record Guest(String id,
                    String firstName,
                    String lastName,
                    String guestOf,
                    String country,
                    String state,
                    String phoneNumber,
                    boolean isTentative,
                    boolean isNotAttending,
                    int seats) {

    public boolean shouldSkipNotifictaion(){
        return this.isTentative() || this.isNotAttending();
    }

    public GuestValidResponse isValid(Function<Guest, GuestValidResponse> fn) {
        if (this.phoneNumber() == null) {
            return new GuestValidResponse(false, "phone number is null");
        }
        return fn.apply(this);
    }

    public String getFullName() {
        return "%s %s".formatted(this.firstName(), this.lastName());
    }

}


