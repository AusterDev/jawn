package com.github.AusterDev.jawn.core.factory;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import java.awt.Color;
import java.time.Instant;

public class EmbedFactory {
    private static final Color COLOR_ERROR = Color.decode("#FB5012");
    private static final Color COLOR_WARNING = Color.decode("#CBBAED");
    private static final Color COLOR_INFO = Color.decode("#093824");

    /**
     * Generates a standardized error embed.
     */
    public static MessageEmbed createErrorEmbed(String errorCode) {
        return new EmbedBuilder()
                .setDescription(
                        "Jawn ran into an unexpected error. Please contact the developer to report this behaviour along with the error ID."
                )
                .addField("Error ID: ", "```fix\nERR-" + errorCode + "\n```", true)
                .setColor(COLOR_ERROR)
                .setTimestamp(Instant.now())
                .build();
    }

    /**
     * Generates a standardized warning embed.
     */
    public static MessageEmbed createWarningEmbed(String description) {
        return new EmbedBuilder()
                .setDescription(
                        description
                )
                .setColor(COLOR_WARNING)
                .setTimestamp(Instant.now())
                .build();
    }

    /**
     * Generates a standardized success or informational embed.
     */
    public static MessageEmbed createInfoEmbed(String description) {
        return new EmbedBuilder()
                .setDescription(
                        description
                )
                .setColor(COLOR_INFO)
                .setTimestamp(Instant.now())
                .build();
    }
}