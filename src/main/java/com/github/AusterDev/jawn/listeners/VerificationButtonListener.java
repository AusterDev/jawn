package com.github.AusterDev.jawn.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.AusterDev.jawn.core.JawnClient;
import com.github.AusterDev.jawn.core.boglog.BotLog;
import com.github.AusterDev.jawn.core.boglog.BotLogType;
import com.github.AusterDev.jawn.core.factory.EmbedFactory;
import com.github.AusterDev.jawn.core.json.VerificationSession;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.sync.RedisCommands;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class VerificationButtonListener extends ListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(VerificationButtonListener.class);
    private static final long SESSION_TIMEOUT_SECONDS = 60L;

    private final JawnClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BotLog botLog;

    public VerificationButtonListener(JawnClient client) {
        this.client = client;
        this.botLog = this.client.getBotLog();
    }

    private String buildGoogleOAuthUrl(String sessionId) {
        String clientId = this.client.getConfig().getGoogleClientId();
        String redirectUri = this.client.getConfig().getWebHost() + "/jawn/verify";
        String scope = "https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile";

        return "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                "&response_type=code" +
                "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8) +
                "&state=" + URLEncoder.encode(sessionId, StandardCharsets.UTF_8) +
                "&prompt=select_account";
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!"jawn:init_verification".equals(event.getComponentId())) {
            return;
        }

        event.deferReply(true).queue();

        String userId = event.getUser().getId();
        RedisCommands<String, String> redis = this.client.getRedisManager().getSyncCommands();

        try {
            String existingSessionId = redis.get("user_session:" + userId);

            if (existingSessionId != null) {
                String url = this.buildGoogleOAuthUrl(existingSessionId);
                Button linkButton = Button.link(url, "Continue Verification");

                event.getHook()
                        .sendMessageEmbeds(EmbedFactory.createWarningEmbed(
                                "You already have an open verification request processing. Please complete it using the secure link below or wait for it to expire."
                        ))
                        .setComponents(ActionRow.of(linkButton))
                        .queue();
                return;
            }

            String newSessionId = UUID.randomUUID().toString();
            VerificationSession session = new VerificationSession(userId, newSessionId);
            String jsonSession = objectMapper.writeValueAsString(session);

            redis.setex("session:" + newSessionId, SESSION_TIMEOUT_SECONDS, jsonSession);
            redis.set("user_session:" + userId, newSessionId, SetArgs.Builder.ex(SESSION_TIMEOUT_SECONDS));

            String url = this.buildGoogleOAuthUrl(newSessionId);
            Button linkButton = Button.link(url, "Verify with Google");

            event.getHook()
                    .sendMessageEmbeds(EmbedFactory.createInfoEmbed(
                            "Your request has been authenticated. Click the button below to sign in using your student email account.\n\n" +
                                    "*Your session will safely transition out of scope in 60 seconds.*"
                    ))
                    .setComponents(ActionRow.of(linkButton))
                    .queue();

        } catch (Exception e) {
            String errorCode = this.botLog.generateLogId();
            this.botLog.log(logger, BotLogType.ERROR, errorCode, "Failed executing step allocations inside interactive session flow for: {}", userId, e);

            event.getHook()
                    .sendMessageEmbeds(EmbedFactory.createErrorEmbed(errorCode))
                    .queue();
        }
    }
}