package com.cmymesh.event.assistant.service;

import com.cmymesh.event.assistant.model.MessageResponse;
import com.cmymesh.event.assistant.model.NotificationTemplate;
import com.cmymesh.event.assistant.model.NotificationTemplateComponent;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Objects;


public class MessagingServiceWhatsAppImpl implements MessagingService {

    private static final Logger LOG = LoggerFactory.getLogger(MessagingServiceWhatsAppImpl.class);

    private final ObjectMapper mapper = new ObjectMapper();

    public MessagingServiceWhatsAppImpl() {
        Objects.requireNonNull(System.getenv("WHATSAPP_TOKEN"), "When sending notifications WHATSAPP_TOKEN needs to be set" );
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public MessageResponse sendTemplateMessage(String toPhoneNumber, NotificationTemplate template, List<NotificationTemplateComponent> components) throws InterruptedException {
        var fromPhoneNumber = template.fromPhoneNumber();
        MessageResponse response;
        try {
            var bodyRequest = getMetaRequest(template, toPhoneNumber, components);
            var authToken = System.getenv("WHATSAPP_TOKEN");
            Objects.requireNonNull(authToken);
            // TODO: Add retries and connection timeout
            var request = HttpRequest.newBuilder()
                    .uri(new URI("https://graph.facebook.com/v13.0/%s/messages".formatted(fromPhoneNumber)))
                    .header("Authorization", "Bearer %s".formatted(authToken))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(bodyRequest)))
                    .build();
            HttpClient http = HttpClient.newHttpClient();
            var httpResponse = http.send(request, HttpResponse.BodyHandlers.ofString());
            var stringResponse = httpResponse.body();
            var metaMessageResponse = mapper.readValue(stringResponse, MetaMessageResponse.class);
            if (httpResponse.statusCode() / 100 == 2) {
                response = new MessageResponse(template.templateName(), null, "accepted", metaMessageResponse.messaging_product, metaMessageResponse.messages.get(0).id);
            } else {
                response = new MessageResponse(template.templateName(), metaMessageResponse.error.message, "failed", "whatsapp", metaMessageResponse.error.fbtrace_id);
                LOG.debug("Response sent {} / {} / {}", bodyRequest, httpResponse, stringResponse);
            }
        } catch (URISyntaxException | IOException e) {
            LOG.error("", e);
            response = new MessageResponse(template.templateName(), e.getMessage(), "failed", "whatsapp", null);
        }
        return response;
    }


    private static MetaMessageRequest getMetaRequest(NotificationTemplate template, String toPhoneNumber, List<NotificationTemplateComponent> components) {
        var language = new Language();
        language.code = template.languageCode();
        var messageTemplate = new Template();
        messageTemplate.name = template.templateName();
        messageTemplate.language = language;
        messageTemplate.components = components;

        var request = new MetaMessageRequest();
        request.messaging_product = "whatsapp";
        request.recipient_type = "individual";
        request.to = toPhoneNumber;
        request.type = "template";
        request.template = messageTemplate;

        return request;
    }

    @ToString
    private static class MetaMessageRequest {
        String messaging_product;
        String recipient_type;
        String to;
        String type;
        Template template;
    }

    @ToString
    private static class Template {
        String name;
        Language language;
        List<NotificationTemplateComponent> components;
    }


    @ToString
    private static class Language {
        String code;
    }

    @ToString
    private static class MetaMessageResponse {
        String messaging_product;
        List<MetaMessagesResponse> messages;
        Error error;
    }

    @ToString
    private static class MetaMessagesResponse {
        String id;
    }

    private static class Error {
        String message;
        String type;
        String fbtrace_id;
    }
}
