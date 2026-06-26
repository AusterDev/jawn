package com.github.AusterDev.jawn.cogs;

import com.github.AusterDev.jawn.core.CommandOption;
import com.github.AusterDev.jawn.core.annotations.Cog;
import com.github.AusterDev.jawn.core.CogAbstract;
import com.github.AusterDev.jawn.core.Command;
import com.github.AusterDev.jawn.core.JawnClient;
import com.github.AusterDev.jawn.core.factory.EmbedFactory;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

@Cog(name = "Email verification", description = "Commands to verify student emails")
public class EmailVerificationCog extends CogAbstract {
    private final JawnClient client;

    public EmailVerificationCog(JawnClient client) {
        this.client = client;
    }

    @Command(name = "verify-message", description = "Setup the verification entry point (Staff Only)")
    public void verifyMessage(
            SlashCommandInteractionEvent event,
            @CommandOption(name = "message", description = "The prompt message to be displayed above the button") String userMessage
    ) {
        if (event.getMember() == null || !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.replyEmbeds(EmbedFactory.createWarningEmbed("You must have Administrator permission to execute this command."))
                    .setEphemeral(true)
                    .queue();
            return;
        }

        event.deferReply(true).queue();

        Button initVerificationButton = Button.primary("jawn:init_verification", "Verify");

        event.getHook()
                .sendMessageEmbeds(EmbedFactory.createInfoEmbed(
                        "The configuration block has been dropped into this channel.\nEdit anytime via `/verify-message-edit`."
                ))
                .queue();

        event.getChannel()
                .sendMessage(userMessage)
                .setComponents(ActionRow.of(initVerificationButton))
                .queue();
    }
}