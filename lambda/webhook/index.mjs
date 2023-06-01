/* eslint-disable max-len */
import {createRequire} from 'module';

const require = createRequire(import.meta.url);
const https = require('https');
const aws = require('aws-sdk');
const TABLE_NAME = 'messages';

aws.config.update({region: process.env.AWS_REGION});
// eslint-disable-next-line no-unused-vars
const ddb = new aws.DynamoDB({apiVersion: '2012-08-10'});

const doPostRequest = (host, path, data) => {
  return new Promise((resolve, reject) => {
    const options = {
      host: host,
      path: path,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
    };

    // create the request object with the callback with the result
    const req = https.request(options, (res) => {
      resolve(JSON.stringify(res.statusCode));
    });

    // handle the possible errors
    req.on('error', (e) => {
      reject(e.message);
    });

    // do the request
    req.write(JSON.stringify(data));

    // finish the request
    req.end();
  });
};

const putItems = async (params) => {
  try {
    return await ddb.putItem(params).promise();
    // Handle your result here!
  } catch (err) {
    console.log(err);
  }
};

export const handler = async (event) => {
  console.log(event);
  const method = event.requestContext.httpMethod;
  const path = event.path;
  if (path !== '/webhook') {
    return {
      statusCode: 403,
    };
  }
  if (method === 'GET') {
    console.log('Proceeding with WebHook Validation.');
    const mode = event.queryStringParameters['hub.mode'];
    const token = event.queryStringParameters['hub.verify_token'];
    const challenge = event.queryStringParameters['hub.challenge'];
    const verifyToken = process.env.VERIFY_TOKEN;
    if (mode === 'subscribe' && token === verifyToken) {
      // Respond with 200 OK and challenge token from the request
      return {
        statusCode: 200,
        body: challenge,
      };
    } else {
      // Responds with '403 Forbidden' if verify tokens do not match
      return {
        statusCode: 403,
      };
    }
  } else if (method === 'POST' && event.body) {
    console.log('Proceeding with WebHook event processing.');
    const body = JSON.parse(event.body);
    const bodyEntry = body.entry;
    const changes = bodyEntry[0].changes;
    // Not of our interest simply return ok
    if (!bodyEntry || !changes || !changes[0] || !changes[0].value) {
      return {statusCode: 200};
    }
    const value = changes[0].value;
    console.log('Value object :' + JSON.stringify(value, null, 2));

    const phoneNumberId = value.metadata.phone_number_id;
    if (value.messages) {
      if (value.messages &&
        value.messages[0] &&
        value.messages[0].text &&
        value.messages[0].from) {
        const contact = value.contacts[0] ? value.contacts[0].profile.name : '';
        const from = value.messages[0].from; // extract the phone number
        const msgBody = value.messages[0].text.body; // extract the message text
        const timestamp = value.messages[0].timestamp;
        console.log(`Reply from ${contact}(${from}): ` + JSON.stringify(msgBody, null, 2));
        const token = process.env.WHATSAPP_TOKEN;
        const path = '/v16.0/' + phoneNumberId +'/messages?access_token=' +token;

        await doPostRequest('graph.facebook.com', path, {
          messaging_product: 'whatsapp',
          to: from,
          text: {body: `Hola ${contact}! Este es un mensaje automatizado, gracias por tu respuesta!`},
        }).then((result) => {
          console.log(`WhatsApp status code: ${result}`);
          const params = {
            TableName: TABLE_NAME,
            Item: {
              'id': {S: phoneNumberId},
              'type': {S: `RECIPIENT-${from}-${timestamp}`},
              'document': {S: msgBody},
            },
          };
          return putItems(params);
        }).then(() => {
          console.log('Succesfully put the message in dynamodb');
        }).catch((err) =>
          console.error(`Error for the event: ${JSON.stringify(err)} => ${err}`));
      }
    } else if (value.statuses && value.statuses[0] && value.statuses[0].status === 'failed') {
      processErrors(phoneNumberId, value.statuses[0]);
      console.log(`Failure to process message to: ${value.statuses[0].recipient_id} : ${JSON.stringify(value.statuses[0], null, 2)}`);
      const params = {
        TableName: TABLE_NAME,
        Item: {
          'id': {S: phoneNumberId},
          'type': {S: `RECIPIENT-${value.statuses[0].recipient_id}-${value.statuses[0].timestamp}`},
          'document': {S: JSON.stringify(value.statuses[0], null, 2)},
        },
      };
      await putItems(params).then((result) => {
        console.log('Success when putting record to dynamo');
      }).catch((err) =>
        console.error(`Error putting the records => ${err}`));
    }
    return {statusCode: 200};
  } else {
    // Return a '404 Not Found' if event is not from a WhatsApp API
    return {statusCode: 404};
  }
};
