package com.github.AusterDev.jawn.listeners;

import com.github.AusterDev.jawn.core.CogAbstract;
import com.github.AusterDev.jawn.core.CommandOption;
import com.github.AusterDev.jawn.core.RegisteredCommand;
import com.github.AusterDev.jawn.supervisors.CogRegistrar;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class CommandListener extends ListenerAdapter {
    private final CogRegistrar registrar;

    public CommandListener(CogRegistrar registrar) {
        this.registrar = registrar;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String cmdName = event.getName().toLowerCase();

        RegisteredCommand registeredCmd = registrar.getCommands().get(cmdName);
        if (registeredCmd == null) return;

        Method method = registeredCmd.getMethod();
        CogAbstract cog = registeredCmd.getInstance();

        try {
            Parameter[] parameters = method.getParameters();

            Object[] args = new Object[parameters.length];
            for (int i=0;i < parameters.length;i++) {
                Parameter parameter = parameters[i];
                Class<?> paramType = parameter.getType();

                if (paramType.isAssignableFrom(SlashCommandInteractionEvent.class)) {
                    args[i] = event;
                    continue;
                }

                if (parameter.isAnnotationPresent(CommandOption.class)) {
                    CommandOption option = parameter.getAnnotation(CommandOption.class);
                    String optionName = option.name().isEmpty() ?
                            parameter.getName().toLowerCase() : option.name().toLowerCase();
                    OptionMapping mapping = event.getOption(optionName);

                    if (mapping == null) {
                        args[i] = null;
                    } else {
                        args[i] = this.extractOptionValue(mapping, paramType);
                    }
                }
            }

            method.setAccessible(true);
            method.invoke(cog, args);
        }catch (Exception e) {
            System.err.println("Error executing command " + cmdName);
            e.printStackTrace();
            event.reply("An internal error occurred while executing this command.").setEphemeral(true).queue();
        }
    }

    private Object extractOptionValue(OptionMapping mapping, Class<?> targetType) {
        if (targetType == String.class) return mapping.getAsString();
        if (targetType == int.class || targetType == Integer.class) return mapping.getAsInt();
        if (targetType == long.class || targetType == Long.class) return mapping.getAsLong();
        if (targetType == boolean.class || targetType == Boolean.class) return mapping.getAsBoolean();
        if (targetType == double.class || targetType == Double.class) return mapping.getAsDouble();
        if (targetType == User.class) return mapping.getAsUser();
        if (GuildChannel.class.isAssignableFrom(targetType)) return mapping.getAsChannel();

        return mapping.getAsString();
    }
}
