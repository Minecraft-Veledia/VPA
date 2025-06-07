package de.veledia.vpa.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a method as a sub-command handler within a Command class.
 * The method annotated with @Command will be executed when the corresponding
 * sub-command is called.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Command {
    String name(); // The name of the sub-command (e.g., "set", "add")
    String description() default "A command without a description."; // A short description for help
    String usage() default ""; // How the command is used (e.g., "set <player> <value>")
    String[] aliases() default {}; // Optional: Alternative names for the command
    int minArgs() default 0; // Minimum number of arguments after the sub-command name
    int maxArgs() default -1; // Maximum number of arguments (-1 for unlimited)
    String permission() default ""; // Permission required to execute this command
    String permissionMessage() default "You don't have permission to execute this command!"; // Message if permission is denied
    boolean requiresMainThread() default false; // Whether the command execution needs to be on the main thread
    String[] examples() default {}; // Examples for command usage
}
