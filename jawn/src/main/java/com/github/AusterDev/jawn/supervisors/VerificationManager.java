package com.github.AusterDev.jawn.supervisors;

import com.github.AusterDev.jawn.core.Config;
import com.github.AusterDev.jawn.core.boglog.BotLog;
import com.github.AusterDev.jawn.core.boglog.BotLogType;
import com.github.AusterDev.jawn.core.factory.EmbedFactory;
import com.github.AusterDev.jawn.core.json.VerificationSession;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class VerificationManager {
    private final JDA jda;
    private final Config config;
    private final Logger logger = LoggerFactory.getLogger(VerificationManager.class);
    private final BotLog botLog;

    public VerificationManager(JDA jda, Config config, BotLog botLog) {
        this.jda = jda;
        this.config = config;
        this.botLog = botLog;
    }

    private TextChannel getVerificationChannel() {
        return this.jda.getTextChannelById(config.getVerificationLogsChannel());
    }


    private void verifyUser(VerificationSession session, TextChannel channel, User user, Role role) {
        channel.getGuild().addRoleToMember(user, role).queue(
                success -> logger.info("Successfully assigned role to {}", user.getName()),
                error -> botLog.log(logger, BotLogType.ERROR, botLog.generateLogId(),
                        "Could not assign role (ID: {}) to {} (ID: {})\n\nException:\n{}",
                        role.getId(), user.getName(), user.getId(), error.getMessage())
        );

        user.openPrivateChannel().queue(privateChannel -> {
            privateChannel.sendMessage(":wave: Hey " + user.getAsMention() + "! You have been successfully verified! Jawn has detected that you are in the **" + session.getDegreeType() + "** degree and assigned the role already. If you did not receive your role then please contact the staff.\n\n*Jawn is an open project live on github. To audit the code, or make to contribute, please visit the github repository.\nThank you for trusting Jawn!*\n\nRegards,\nIITM BS Multi-Stream Hub" + "\n[wlc](https://media0.giphy.com/media/v1.Y2lkPTc5MGI3NjExMm9wZWVvaW84MTA1OXZ2NWhoNGF3MWdtdHkybzZ2cGVtdzZuM2MxcSZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/bPDzcb6OADZ9m/giphy.gif)").queue(
                    success -> {},
                    failure -> botLog.log(logger, BotLogType.ERROR, botLog.generateLogId(), "Failed to DM {}\n\nException:\n{}", user.getName(), failure.getMessage()));
        }, failure -> {
            botLog.log(logger, BotLogType.ERROR, botLog.generateLogId(), "Failed to open DM {}\n\nException:\n{}", user.getName(), failure.getMessage());
        });

        channel.sendMessageEmbeds(
                EmbedFactory.createInfoEmbed(config.getEmojiSuccess() + "**" + user.getName() + "** (ID: " + user.getId() + ") has been verified.\n\n**Detected degree**: `" + session.getDegreeType() + "`\n**Session ID**: `" + session.getSessionId() + "`")
        ).queue();
    }

    private void rejectUser(VerificationSession session, TextChannel channel, User user) {
        String reason = session.getReason() == null ? "No reason provided" : session.getReason();

        user.openPrivateChannel().queue(privateChannel -> {
            privateChannel.sendMessage(config.getEmojiFailure() + " Hey " + user.getAsMention() + ", your verification application has been rejected.\n\n**Reason**: *" + reason + "*\n\nIf you believe this was an error, please reach out to the server administration or open a support ticket.\n\nRegards,\nIITM BS Multi-Stream Hub" + "\n[facepalm](https://tenor.com/view/speed-ishowspeed-facepalm-disappointed-gif-13266694906676622299)").queue(
                    success -> {},
                    failure -> botLog.log(logger, BotLogType.ERROR, botLog.generateLogId(), "Failed to send rejection DM to {}\n\nException:\n{}", user.getName(), failure.getMessage()));
        }, failure -> {
            botLog.log(logger, BotLogType.ERROR, botLog.generateLogId(), "Failed to open DM for rejection to {}\n\nException:\n{}", user.getName(), failure.getMessage());
        });

        channel.sendMessageEmbeds(
                EmbedFactory.createInfoEmbed(config.getEmojiFailure() + " **" + user.getName() + "** (ID: " + user.getId() + ") was **REJECTED**.\n\n**Reason**```py\n'" + reason + "'\n\n```**Session ID**: `" + session.getSessionId() + "`")
        ).queue();
    }

    public void listen(VerificationSession session) {
        TextChannel channel = this.getVerificationChannel();
        if (channel == null) {
            botLog.log(logger, BotLogType.ERROR, botLog.generateLogId(), "Verification channel is NULL");
            return;
        }

        User user = this.jda.getUserById(session.getUserId());
        Role role = this.jda.getRoleById(config.getPostVerifiedRoleId());

        if (user == null || role == null) {
            botLog.log(logger, BotLogType.ERROR, botLog.generateLogId(), "Could not process verification. Either User or Role was not found via ID.");
            return;
        }

        if (session.isVerified()) {
            this.verifyUser(session, channel, user, role);
        } else {
            this.rejectUser(session, channel, user);
        }
    }
}