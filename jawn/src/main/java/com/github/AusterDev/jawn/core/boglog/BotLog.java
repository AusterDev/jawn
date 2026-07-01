package com.github.AusterDev.jawn.core.boglog;

import com.github.AusterDev.jawn.core.Config;
import com.github.AusterDev.jawn.core.factory.EmbedFactory;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.slf4j.Logger;
import org.slf4j.helpers.MessageFormatter;

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

    private String parseEmoji(String emojiConfig) {
        if (emojiConfig == null || emojiConfig.isEmpty()) return "";
        if (emojiConfig.matches("\\d+")) {
            return Emoji.fromCustom("emoji", Long.parseUnsignedLong(emojiConfig), false).getAsMention() + " ";
        }
        return emojiConfig + " ";
    }

    public void log(Logger logger, BotLogType type, String logId, String format, Object... arg) {
        String rawMessage = MessageFormatter.arrayFormat(format, arg).getMessage();
        String toLog = "[LogID : " + logId + "] : " + rawMessage;

        switch (type) {
            case INFO -> logger.info(toLog);
            case WARNING -> logger.warn(toLog);
            case ERROR -> logger.error(toLog);
        }

        TextChannel channel = this.jda.getTextChannelById(this.config.getBotLogsChannelId());
        if (channel == null) {
            logger.error("[Related to ID : {}] Bot logs channel is null", logId);
            return;
        }

        String emoji = parseEmoji(config.getEmojiInfo());
        String logType = "[INFO] ";

        if (type == BotLogType.ERROR || type == BotLogType.WARNING) {
            emoji = parseEmoji(config.getEmojiFailure());
            logType = (type == BotLogType.ERROR) ? "[ERROR] " : "[WARN] ";
        }

        channel.sendMessageEmbeds(
                EmbedFactory.createInfoEmbed(emoji + " Logging from class **`" + logger.getName() + "`**\n```sh\n" + logType + toLog + "\n```")
        ).queue();
    }

    public void log(Logger logger, BotLogType type, String logId, String message) {
        String toLog = "[LogID : " + logId + "] : " + message;

        switch (type) {
            case INFO -> logger.info(toLog);
            case WARNING -> logger.warn(toLog);
            case ERROR -> logger.error(toLog);
        }

        TextChannel channel = this.jda.getTextChannelById(this.config.getBotLogsChannelId());
        if (channel == null) {
            logger.error("[Related to ID : {}] Bot logs channel is null", logId);
            return;
        }

        String emoji = parseEmoji(config.getEmojiInfo());
        String logType = "[INFO] ";

        if (type == BotLogType.ERROR || type == BotLogType.WARNING) {
            emoji = parseEmoji(config.getEmojiFailure());
            logType = (type == BotLogType.ERROR) ? "[ERROR] " : "[WARN] ";
        }

        try {
            channel.sendMessageEmbeds(
                    EmbedFactory.createInfoEmbed(emoji + " Logging from class **`" + logger.getName() + "`**\n```sh\n" + logType + toLog + "\n```")
            ).queue();
        } catch (Exception e) {
            logger.error("[Related to ID : {}] Failed to send report to logging channel", logId);
        }
    }
}