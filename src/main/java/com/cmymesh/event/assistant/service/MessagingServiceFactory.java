package com.cmymesh.event.assistant.service;

import com.cmymesh.event.assistant.MessagingMode;

public class MessagingServiceFactory {

    private final MessagingService whatsAppService;

    public MessagingServiceFactory() {
        whatsAppService = new MessagingServiceWhatsAppImpl();
    }


    public MessagingService getService(MessagingMode mode) {
        if (MessagingMode.WHATSAPP == mode) {
            return whatsAppService;
        }
        throw new RuntimeException("Not valid mode for notifications %s".formatted(mode));
    }
}
