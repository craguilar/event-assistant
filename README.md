# Event Assistant app

Event Assistant app! Yet another way of reinventing the wheel for sending Event
notifications.

What this programs MUST do is:

- Reading from a Guest data base , implementing the interface GuestRepository.
- Using the guests to:
  - Run validations.
  - Sending notifications.

This program keeps a local data base which tracks state of Guest interactions and
notifications and/or validations , see more on EventAssistantRepository.

This app is meant to be run using the CLI or an IDE .

## Running the app

You will need :

- Java 17
- A compatible version of Maven installed.

Building the app:

```bash
mvn clean install
```

Run the app

```bash
java -jar target/event-assistant-1.0-SNAPSHOT.jar
```

### WhatsApp send message integration

This app integrates with WhatsApp API to send notifications, the high level mental model for notification sending is :

- To send a notification you need a Template , a _Sender_ and a _Receiver_.
- Notification templates are predefined in Meta Business platform which can contain text with variables.
- A _Sender_ is a registered Meta phone number to send messages as the "From" entity.
- A _Receiver_ is a phone number who will receive the instance of a Template .

API Reference https://developers.facebook.com/docs/whatsapp/cloud-api/reference/messages


### WhatsApp webhook

WhatsApp integration allows you to set up a WebHook to process any reply made to your _Sender_ . For this you need to define
a WebHook endpoint , this is the purpose of  _lambda_ package , which thanks to GitHub actions will deploy the code in _index.mjs_
in an EventAssistantWhatsAppWebhook lambda in AWS.

### Berkeley DB Java Edition (JE)

JE is a general-purpose, transaction-protected,  embedded database written in 100% Java (JE makes no JNI calls). As such,
it offers the Java developer safe and efficient in-process storage and management of arbitrary data.

It is used to store Guest Tracking info in EventAssistantRepository.


# References
- Thanks to https://developers.facebook.com/blog/post/2022/11/07/adding-whatsapp-to-your-java-projects/