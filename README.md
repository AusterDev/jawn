# Context
This is designed for the IITM BS: Multi-Stream Hub discord server where we needed to verify the studentship of members and remove inactive accounts.

# Functional features
1. Connects to the discord gateway and awaits for user-commands.
2. Users click on a button component to initiate the verification flow.
3. Web dashboard to process the entirety of verification.
4. Automations as required.

# Modus operandi: student verification

Every student has an email address issued by IIT Madras, I am prompting them to authenticate their emails through Google OAuth. Thus verifying their student credentials. The server only sees their email address and the degree they are in.

For this, I needed a web server and discord bot to receive callback from google and assign roles in the server respectively. Thus I have divided the project into two modules: bot and web-server. Both services communicate through the redis PUB-SUB model.

Whenever someone initiates the verification process from discord, the bot will send a message to the server containing the unique verification session ID.

As for the session data, I am following this:
```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "VerificationSession",
  "type": "object",
  "properties": {
    "session_id": {
      "type": "string",
      "description": "The unique session ID."
    },
    "user_id": {
      "type": "string",
      "description": "The unique Discord ID of the user."
    },
    "degree_type": {
      "type": "string",
      "description": "Degree which the user belongs to."
    },
    "verified": {
      "type": "boolean",
      "description": "Whether the user has been verified."
    },
    "created_at": {
      "type": "integer",
      "description": "The creation timestamp (epoch)."
    }
  },
  "additionalProperties": false
}
```

# Contributions
Just make a PR.
