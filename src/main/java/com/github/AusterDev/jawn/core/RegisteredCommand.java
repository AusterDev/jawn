package com.github.AusterDev.jawn.core;

import java.lang.reflect.Method;

public class RegisteredCommand {
    private final CogAbstract instance;
    private final Method method;

    public RegisteredCommand(CogAbstract instance, Method method) {
        this.instance = instance;
        this.method = method;
    }

    public CogAbstract getInstance() { return instance; }
    public Method getMethod() { return method; }
}