package com.cmymesh.event.assistant.service;

import com.cmymesh.event.assistant.MessagingMode;

public class MessagingServiceFactory {

    private final MessagingService whatsAppService;
    private final MessagingServiceMockImpl mockService;

    public MessagingServiceFactory() {
        whatsAppService = new MessagingServiceWhatsAppImpl();
        mockService = new MessagingServiceMockImpl();
    }


    public MessagingService getService(MessagingMode mode) {
        if (MessagingMode.WHATSAPP == mode) {
            return whatsAppService;
        } else if (MessagingMode.DRYRUN == mode) {
            return mockService;
        }

        throw new RuntimeException("Not valid mode for notifications %s".formatted(mode));
    }
}
