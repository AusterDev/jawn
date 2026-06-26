package com.github.AusterDev.jawn.core;

import com.github.AusterDev.jawn.cogs.EmailVerificationCog;
import com.github.AusterDev.jawn.cogs.UtilitiesCog;
import com.github.AusterDev.jawn.core.boglog.BotLog;
import com.github.AusterDev.jawn.core.boglog.BotLogType;
import com.github.AusterDev.jawn.core.factory.EmbedFactory;
import com.github.AusterDev.jawn.listeners.CommandListener;
import com.github.AusterDev.jawn.listeners.VerificationButtonListener;
import com.github.AusterDev.jawn.supervisors.CogRegistrar;
import com.github.AusterDev.jawn.supervisors.RedisManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class JawnClient {
    private final JDA jda;
    private final RedisManager redisManager;

    private final CogRegistrar cogRegistrar;
    private final Config config;

    private final Logger logger = LoggerFactory.getLogger(JawnClient.class);

    private final BotLog botLog;

    public JawnClient(Config config) {
        this.jda = JDABuilder.createLight(config.getToken())
                .setStatus(OnlineStatus.DO_NOT_DISTURB)
                .setActivity(Activity.playing("GNU Emacs"))
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .build();

        this.cogRegistrar = new CogRegistrar(this.jda);

        this.redisManager = new RedisManager();
        this.redisManager.startListener(config, this);

        this.config = config;

        this.botLog = new BotLog(this.jda, this.config);
    }

    public RedisManager getRedisManager() { return this.redisManager; }
    public Config getConfig() { return this.config; }
    public BotLog getBotLog() {
        return botLog;
    }

    private void registerListeners() {
        this.jda.addEventListener(new CommandListener(this.cogRegistrar));
        this.jda.addEventListener(new VerificationButtonListener(this));
    }

    private void registerCogs() {
        this.cogRegistrar.registerCog(new UtilitiesCog(this));
        this.cogRegistrar.registerCog(new EmailVerificationCog(this));

        this.cogRegistrar.syncCommands();
    }

    public void verifyUser(boolean verified, String userID, DegreeType degreeType) {
        User user = this.jda.getUserById(userID);
        Role role = this.jda.getRoleById(config.getPostVerifiedRoleId());

        TextChannel verificationLogsChannel = this.jda.
                getChannelById(TextChannel.class, this.config.getVerificationLogsChannel());

        if (verificationLogsChannel == null) {
            logger.error("Verification channel is NULL");
            return;
        }

        if (role == null) {
            logger.error("Post verification role is NULL");
            return;
        }

        if (user == null && !verified) {
            verificationLogsChannel.sendMessageEmbeds(
                    EmbedFactory.createWarningEmbed("User with ID `" + userID + "`' used a non-student email address. Therefore they have been rejected.\n\nAlso, the user has already left the server.`")
            ).queue();
            return;
        }
        if (user == null) {
            verificationLogsChannel.sendMessageEmbeds(
                    EmbedFactory.createWarningEmbed("User with ID `" + userID + "` has been verified to belong from `" + degreeType +"` degree, but no role was assigned as they may have left the server.")
            ).queue();
            return;
        }

        if (!verified) {
            verificationLogsChannel.sendMessageEmbeds(
                    EmbedFactory.createWarningEmbed("User **`" + user.getName() + "`** (ID: `" + userID + "`) used a non-student email address. Therefore they have been rejected.\n\n*The user will be kicked after 2 days of inactivity.*")
            ).queue();
            return;
        }
       try {
           Guild guild = verificationLogsChannel.getGuild();
           guild.addRoleToMember(user, role).queue();

           verificationLogsChannel.sendMessageEmbeds(
                   EmbedFactory.createInfoEmbed("User **`" + user.getName() + "`** (ID: `" + user.getId() + "`) has been successfully verified. They are in the **`" + degreeType + "`** degree.")
           ).queue();
       } catch (Exception e) {
           String logID = this.botLog.generateLogId();
           this.botLog.log(logger, BotLogType.ERROR, logID, "Unexpected error: {}", e);
       }

    }

    public void start() throws InterruptedException {
        this.registerListeners();
        this.registerCogs();

        this.jda.awaitReady();
    }
}
