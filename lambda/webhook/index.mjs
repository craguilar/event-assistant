import {createRequire} from 'module';

const require = createRequire(import.meta.url);
const https = require('https');

const doPostRequest = (host, path, data) => {

    return new Promise((resolve, reject) => {
        const options = {
            host: host,
            path: path,
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        };

        //create the request object with the callback with the result
        const req = https.request(options, (res) => {
            resolve(JSON.stringify(res.statusCode));
        });

        // handle the possible errors
        req.on('error', (e) => {
            reject(e.message);
        });

        //do the request
        req.write(JSON.stringify(data));

        //finish the request
        req.end();
    });
};

export const handler = async (event) => {

    const method = event.requestContext.http.method;
    const path = event.requestContext.http.path;
    if (path !== "/webhook") {
        return {
            statusCode: 403
        };
    }

    if (method === "GET") {
        const mode = event.queryStringParameters["hub.mode"];
        const token = event.queryStringParameters["hub.verify_token"];
        const challenge = event.queryStringParameters["hub.challenge"];
        const verify_token = process.env.VERIFY_TOKEN;
        if (mode === "subscribe" && token === verify_token) {
            // Respond with 200 OK and challenge token from the request
            return {
                statusCode: 200,
                body: challenge
            };
        } else {
            // Responds with '403 Forbidden' if verify tokens do not match
            return {
                statusCode: 403
            };
        }
    } else if (method === "POST" && event.body) {
        const body = JSON.parse(event.body);
        const bodyEntry = body.entry;
        console.log(bodyEntry);
        if (bodyEntry &&
            bodyEntry[0].changes &&
            bodyEntry[0].changes[0] &&
            bodyEntry[0].changes[0].value.messages &&
            bodyEntry[0].changes[0].value.messages[0] &&
            bodyEntry[0].changes[0].value.messages[0].text &&
            bodyEntry[0].changes[0].value.messages[0].from) {

            const phone_number_id = bodyEntry[0].changes[0].value.metadata.phone_number_id;
            const from = bodyEntry[0].changes[0].value.messages[0].from; // extract the phone number from the webhook payload
            const msg_body = bodyEntry[0].changes[0].value.messages[0].text.body; // extract the message text from the webhook payload

            console.log('Reply from:' + from + ':' + JSON.stringify(msg_body, null, 2));
            const token = process.env.WHATSAPP_TOKEN;
            const path = "/v12.0/" +
                phone_number_id +
                "/messages?access_token=" +
                token;
            await doPostRequest("graph.facebook.com", path, {
                messaging_product: "whatsapp",
                to: from,
                text: {body: "Este es un mensaje automatizado, gracias por tu respuesta!"},
            })
                .then(result => console.log(`Status code: ${result}`))
                .catch(err => console.error(`Error doing the request for the event: ${JSON.stringify(event)} => ${err}`));

        }
        return {statusCode: 200};
    } else {
        // Return a '404 Not Found' if event is not from a WhatsApp API
        return {statusCode: 404};
    }
};