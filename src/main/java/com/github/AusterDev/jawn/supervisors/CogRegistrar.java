package com.github.AusterDev.jawn.supervisors;

import com.github.AusterDev.jawn.core.*;
import com.github.AusterDev.jawn.core.annotations.Cog;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CogRegistrar {
    private Map<String, CogAbstract> cogs;
    private JDA jda;
    private final Map<String, RegisteredCommand> commands = new HashMap<>();
    private final List<SlashCommandData> globalCommandDataList = new ArrayList<>();

    public CogRegistrar(JDA jda) {
        this.jda = jda;

        this.cogs = new HashMap<>();
    }

    public void registerCog(CogAbstract cog) {
        Class<?> clazz = cog.getClass();

        if (clazz.isAnnotationPresent(Cog.class)) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Command.class)) {
                    Command cmdAnnotation = method.getAnnotation(Command.class);

                    String commandName = cmdAnnotation.name().isEmpty() ?
                            method.getName().toLowerCase() : cmdAnnotation.name().toLowerCase();

                    SlashCommandData commandData = CommandParser.parseCommand(method);
                    if (commandData != null) {
                        globalCommandDataList.add(commandData);

                        commands.put(commandName, new RegisteredCommand(cog, method));
                    }
                }
            }
        }
    }

    public Map<String, RegisteredCommand> getCommands() {
        return commands;
    }

    public void syncCommands() {
        if (!globalCommandDataList.isEmpty()) {
            jda.updateCommands().addCommands(globalCommandDataList).queue(
                    success -> System.out.println("Successfully pushed " + globalCommandDataList.size() + " commands to Discord!"),
                    error -> System.err.println("Failed to update Discord commands: " + error.getMessage())
            );
        }
    }
}
