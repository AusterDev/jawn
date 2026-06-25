package com.github.AusterDev.jawn.supervisors;

import com.github.AusterDev.jawn.core.Command;
import com.github.AusterDev.jawn.core.CommandOption;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class CommandParser {
    public static SlashCommandData parseCommand(Method method) {
        if (!method.isAnnotationPresent(Command.class)) return null;

        Command cmd = method.getAnnotation(Command.class);
        String name = cmd.name().isEmpty() ? method.getName().toLowerCase() : cmd.name();

        SlashCommandData commandData = Commands.slash(name, cmd.description());

        for (Parameter param : method.getParameters()) {
            if (param.isAnnotationPresent(CommandOption.class)) {
                CommandOption opt = param.getAnnotation(CommandOption.class);
                String optName = opt.name().isEmpty() ? param.getName().toLowerCase() : opt.name();

                OptionType jdaType = getJdaOptionType(param.getType());

                OptionData optionData = new OptionData(jdaType, optName, opt.description())
                        .setRequired(opt.required());

                commandData.addOptions(optionData);

            }
        }
        return commandData;
    }

    private static OptionType getJdaOptionType(Class<?> type) {if (type == String.class) return OptionType.STRING;
        if (type == int.class || type == Integer.class || type == long.class || type == Long.class) return OptionType.INTEGER;
        if (type == boolean.class || type == Boolean.class) return OptionType.BOOLEAN;
        if (type == User.class) return OptionType.USER;
        if (GuildChannel.class.isAssignableFrom(type)) return OptionType.CHANNEL;
        if (type == double.class || type == Double.class) return OptionType.NUMBER;

        return OptionType.STRING;
    }
}
