package com.github.AusterDev.jawn.cogs;

import com.github.AusterDev.jawn.core.annotations.Cog;
import com.github.AusterDev.jawn.core.CogAbstract;
import com.github.AusterDev.jawn.core.Command;
import com.github.AusterDev.jawn.core.JawnClient;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

@Cog(name="utilities", description = "Utility and quality of life commands/features")
public class UtilitiesCog extends CogAbstract {
    private final JawnClient client;

    public UtilitiesCog(JawnClient client) {
        this.client = client;
    }

    @Command(name="latency", description="Fetch API latency")
    public void latency(SlashCommandInteractionEvent event) {
        long gatewayLatency = event.getJDA().getGatewayPing();

        event.reply("API Gateway latency is **`" + gatewayLatency + "`** ms").queue();
    }

    @Command(name="github", description = "Get the github repository link")
    public void github(SlashCommandInteractionEvent event) {
        event.reply("https://github.com/AusterDev/jawn").queue();
    }
}
