# Functional features
1. Connects to the discord gateway and awaits for user-commands.
2. Command: "/verify" or perhaps a message component (button) to start the student verification process.
3. Web dashboard to process the entirety of verification.
4. Automations as required.

# Modus operandi

The web server and the bot are separated into their own environments. Both services communicate through the redis PUB-SUB model.

Whenever someone initiates the verification process from discord, the bot will send a message to the server with the following:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "UserSession",
  "type": "object",
  "properties": {
    "id": {
      "type": "integer",
      "description": "The unique session ID."
    },
    "user_id": {
      "type": "string",
      "description": "The unique Discord ID of the user."
    },
    "validity": {
      "type": "integer",
      "description": "The validity period (mins"
    },
    "created_at": {
      "type": "integer",
      "description": "The creation timestamp (epoch)."
    }
  },
  "required": [
    "user_id",
    "validity",
    "created_at"
  ],
  "additionalProperties": false
}
```

The web server processes the request and responds with the following:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "SessionResponse",
  "type": "object",
  "properties": {
    "error": {
      "oneOf": [
        {
          "type": "null",
          "description": "No error occurred."
        },
        {
          "type": "object",
          "description": "Error details if the request failed.",
          "properties": {
            "type": {
              "type": "string",
              "description": "The category or code of the error."
            },
            "msg": {
              "type": "string",
              "description": "A human-readable error message."
            }
          },
          "required": [
            "type",
            "msg"
          ],
          "additionalProperties": false
        }
      ]
    },
    "r": {
      "type": "object",
      "description": "The result payload containing session details.",
      "properties": {
        "id": {
          "type": "string",
          "description": "The unique identifier for the session."
        },
        "user_id": {
          "type": "string",
          "description": "The Discord ID of the user."
        },
        "verified": {
          "type": "boolean",
          "description": "Indicates whether the session or user is verified."
        },
        "time_spent": {
          "type": "integer",
          "description": "The amount of time spent."
        }
      },
      "required": [
        "id",
        "user_id",
        "verified",
        "time_spent"
      ],
      "additionalProperties": false
    }
  },
  "required": [
    "error",
    "r"
  ],
  "additionalProperties": false
}
```