package com.github.AusterDev.jawn.supervisors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.AusterDev.jawn.core.Config;
import com.github.AusterDev.jawn.core.JawnClient;
import com.github.AusterDev.jawn.core.boglog.BotLogType;
import com.github.AusterDev.jawn.core.json.VerificationSession;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisManager {
    private RedisCommands<String, String> syncCommands;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void startListener(Config config, JawnClient jawnClient) {
        Logger logger = LoggerFactory.getLogger(RedisManager.class);
        VerificationManager manager = new VerificationManager(jawnClient.getJda(), config, jawnClient.getBotLog());

        RedisURI uri = RedisURI.builder()
                .withHost(config.getRedisHost())
                .withPort(config.getRedisPort())
                .withPassword(config.getRedisPassword())
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
                            return;
                        }

                        VerificationSession session = objectMapper.readValue(jsonSession, VerificationSession.class);

                        manager.listen(session);
                    } catch (IllegalArgumentException e) {
                        jawnClient.getBotLog().log(logger, BotLogType.ERROR, jawnClient.getBotLog().generateLogId(), "Invalid degree type received from redis (web)", e);

                    } catch (Exception e) {
                        jawnClient.getBotLog().log(logger, BotLogType.ERROR, jawnClient.getBotLog().generateLogId(), "Failed to process response from redis (web). Exception\n{}", e);
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
        jawnClient.getBotLog()
                .log(logger, BotLogType.INFO, jawnClient.getBotLog().generateLogId(), "Subscribed to redis: user_verified");
    }

    public RedisCommands<String, String> getSyncCommands() {
        return syncCommands;
    }
}