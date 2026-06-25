package com.github.AusterDev.jawn.supervisors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.AusterDev.jawn.core.Config;
import com.github.AusterDev.jawn.core.DegreeType;
import com.github.AusterDev.jawn.core.JawnClient;
import com.github.AusterDev.jawn.core.json.VerificationSession;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

public class RedisManager {
    private RedisCommands<String, String> syncCommands;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void startListener(Config config, JawnClient jawnClient) {
        RedisURI uri = RedisURI.builder()
                .withHost(config.getRedisHost())
                .withPort(config.getRedisPort())
                .build();

        RedisClient redisClient = RedisClient.create(uri);

        this.syncCommands = redisClient.connect().sync();

        StatefulRedisPubSubConnection<String, String> connection = redisClient.connectPubSub();

        connection.addListener(new RedisPubSubListener<String, String>() {
            @Override
            public void message(String channel, String message) {
                if ("user_verified".equals(channel)) {
                    try {
                        String jsonSession = syncCommands.get("session:" + message);

                        if (jsonSession == null) {
                            System.err.println("Session data expired or missing for ID: " + message);
                            return;
                        }

                        VerificationSession session = objectMapper.readValue(jsonSession, VerificationSession.class);

                            String userId = session.getUserId();

                            DegreeType degree = DegreeType.valueOf(session.getDegreeType().toUpperCase());

                            jawnClient.verifyUser(session.isVerified(), userId, degree);

                    } catch (IllegalArgumentException e) {
                        System.err.println("Invalid DegreeType received in session payload.");
                    } catch (Exception e) {
                        System.err.println("Failed to process user verification over Redis Pub/Sub:");
                        e.printStackTrace();
                    }
                }
            }

            @Override public void message(String pattern, String channel, String message) {}
            @Override public void subscribed(String channel, long count) {}
            @Override public void psubscribed(String pattern, long count) {}
            @Override public void unsubscribed(String channel, long count) {}
            @Override public void punsubscribed(String pattern, long count) {}
        });

        connection.async().subscribe("user_verified");
        System.out.println("Successfully subscribed non-blockingly to user_verified!");
    }

    public RedisCommands<String, String> getSyncCommands() {
        return syncCommands;
    }
}