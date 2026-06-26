package com.github.AusterDev.jawn.core.boglog;

import com.github.AusterDev.jawn.core.Config;
import com.github.AusterDev.jawn.core.factory.EmbedFactory;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;

import java.util.UUID;

public class BotLog {
    private final JDA jda;
    private final Config config;

    public BotLog(JDA jda, Config config) {
        this.jda = jda;
        this.config = config;
    }

    public String generateLogId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /*
    * Logs to the BOT_LOGS_CHANNEL
    * */
    public void log(Logger logger, BotLogType type, String logId, String format, Object... arg) {
        String toLog = "[LogID : " + logId + "] : " + format;

        switch (type) {
            case INFO -> logger.info(toLog, arg);
            case WARNING -> logger.warn(toLog, arg);
            case ERROR -> logger.error(toLog, arg);
        }

        TextChannel channel = this.jda.getTextChannelById(this.config.getBotLogsChannelId());

        if (channel == null) {
            logger.error("[Related to ID : {}] Bot logs channel is null", logId);
            return;
        }
        channel.sendMessageEmbeds(
                EmbedFactory.createInfoEmbed(toLog)
        ).queue();
    }

    public void log(Logger logger, BotLogType type, String logId, String message) {
        String toLog = "[LogID : " + logId + "] : " + message;

        switch (type) {
            case INFO -> logger.info(message);
            case WARNING -> logger.warn(message);
            case ERROR -> logger.error(message);
        }

        TextChannel channel = this.jda.getTextChannelById(this.config.getBotLogsChannelId());

        if (channel == null) {
            logger.error("[Related to ID : {}] Bot logs channel is null", logId);
            return;
        }
        try {
            channel.sendMessageEmbeds(
                    EmbedFactory.createInfoEmbed(toLog)
            ).queue();
        } catch (Exception e) {
            logger.error("[Related to ID : {}] Failed to send report to logging channel", logId);
        }
    }
}
