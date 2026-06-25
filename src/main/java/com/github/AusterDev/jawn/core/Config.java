package com.github.AusterDev.jawn.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

public class Config {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    private @interface EnvProperty {
        String value();
    }

    @EnvProperty("DISCORD_BOT_TOKEN")
    private String token;

    @EnvProperty("POST_VERIFIED_ROLE_ID")
    private String postVerifiedRoleId;

    @EnvProperty("REDIS_HOST")
    private String redisHost;

    @EnvProperty("REDIS_PORT")
    private int redisPort;

    @EnvProperty("GOOGLE_CLIENT_ID")
    private String googleClientId;

    @EnvProperty("WEB_HOST")
    private String webHost;

    @EnvProperty("VERIFICATION_LOGS_CHANNEL")
    private String verificationLogsChannel;

    @EnvProperty("BOT_LOGS_CHANNEL_ID")
    private String botLogsChannelId;

    private Config() {}

    /**
     * Automated loading wrapper. Iterates through all declared fields,
     */
    public static Config load() throws IllegalStateException {
        Config config = new Config();
        Field[] fields = Config.class.getDeclaredFields();

        for (Field field : fields) {
            if (!field.isAnnotationPresent(EnvProperty.class)) {
                continue;
            }

            EnvProperty property = field.getAnnotation(EnvProperty.class);
            String propertyKey = property.value();
            String systemValue = System.getProperty(propertyKey);

            if (systemValue == null || systemValue.isBlank()) {
                throw new IllegalStateException("Missing required system property flag: -D" + propertyKey);
            }

            try {
                field.setAccessible(true);

                if (field.getType() == int.class) {
                    int parsedInt = Integer.parseInt(systemValue.trim());

                    if (propertyKey.equals("REDIS_PORT") && (parsedInt <= 0 || parsedInt > 65535)) {
                        throw new IllegalStateException("System property 'REDIS_PORT' must be a valid port range (1-65535): " + parsedInt);
                    }
                    field.setInt(config, parsedInt);
                } else {
                    field.set(config, systemValue);
                }
            } catch (NumberFormatException e) {
                throw new IllegalStateException("System property '" + propertyKey + "' must be a valid integer: " + systemValue);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Failed to securely inject property value into field: " + field.getName(), e);
            }
        }

        return config;
    }

    public String getToken() { return token; }
    public String getPostVerifiedRoleId() { return postVerifiedRoleId; }
    public String getRedisHost() { return redisHost; }
    public int getRedisPort() { return redisPort; }
    public String getGoogleClientId() { return googleClientId; }
    public String getWebHost() { return webHost; }
    public String getVerificationLogsChannel() { return verificationLogsChannel; }
    public String getBotLogsChannelId() { return botLogsChannelId; }
}