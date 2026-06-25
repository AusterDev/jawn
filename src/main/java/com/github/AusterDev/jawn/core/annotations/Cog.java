package com.github.AusterDev.jawn.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Cog {
    // Name of the cog
    String name() default "";

    // Description of the cog
    String description() default "";
}
