package de.veledia.vpa.addons.CommandHandler;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Utility class for parsing command arguments and converting them to desired types.
 * Supports basic primitive types, String, CommandSender, Player, World, Material.
 */
public class ArgumentParser {
    private final JavaPlugin plugin;

    public ArgumentParser(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Parses the raw string arguments into an array of objects matching the method's parameters.
     * The first parameter of the method is expected to be CommandSender, which is handled automatically.
     *
     * @param sender The CommandSender who executed the command.
     * @param rawArgs The raw string arguments from the command.
     * @param method The method being invoked.
     * @param expectedParamTypes A map of expected parameter names to their types, ordered.
     * @return An array of parsed arguments, ready for method invocation.
     * @throws IllegalArgumentException If an argument cannot be parsed to its expected type or is missing.
     */
    public Object[] parseArguments(CommandSender sender, String[] rawArgs, Method method, LinkedHashMap<String, Class<?>> expectedParamTypes) throws IllegalArgumentException {
        Parameter[] methodParameters = method.getParameters();
        Object[] parsedArgs = new Object[methodParameters.length];
        int rawArgIndex = 0; // Index for rawArgs array

        // Handle CommandSender as the first parameter if present
        if (methodParameters.length > 0 && methodParameters[0].getType() == CommandSender.class) {
            parsedArgs[0] = sender; // First argument is always the sender
        }

        // Iterate through method's parameters, starting from the second one (if CommandSender was first)
        // or from the first one if CommandSender is not a parameter.
        // We use the `expectedParamTypes` map to determine the order and types of arguments
        // as declared in the method signature, which might not directly match `rawArgs` count if `CommandSender` is included.
        int parameterIndex = (parsedArgs[0] == sender) ? 1 : 0;

        for (Map.Entry<String, Class<?>> entry : expectedParamTypes.entrySet()) {
            String paramName = entry.getKey();
            Class<?> targetType = entry.getValue();

            // If we've run out of raw arguments but still expect parameters, it's an error.
            if(rawArgIndex >= rawArgs.length){
                throw new IllegalArgumentException("Missing argument for '" + paramName + "' (expected type: " + targetType.getSimpleName() + ").");
            }

            String rawArg = rawArgs[rawArgIndex];
            Object value = null;
            try {
                if (targetType == String.class) {
                    value = rawArg;
                } else if (targetType == int.class || targetType == Integer.class) {
                    value = Integer.parseInt(rawArg);
                } else if (targetType == double.class || targetType == Double.class) {
                    value = Double.parseDouble(rawArg);
                } else if (targetType == boolean.class || targetType == Boolean.class) {
                    value = Boolean.parseBoolean(rawArg);
                } else if (targetType == Player.class) {
                    Player player = Bukkit.getPlayer(rawArg);
                    if (player == null) {
                        throw new IllegalArgumentException("Player '" + rawArg + "' not found or offline.");
                    }
                    value = player;
                } else if (targetType == World.class) {
                    World world = Bukkit.getWorld(rawArg);
                    if (world == null) {
                        throw new IllegalArgumentException("World '" + rawArg + "' not found.");
                    }
                    value = world;
                } else if (targetType == Material.class) {
                    Material material = Material.matchMaterial(rawArg);
                    if (material == null) {
                        throw new IllegalArgumentException("Material '" + rawArg + "' not found.");
                    }
                    value = material;
                } else {
                    // Fallback for unsupported types, log a warning
                    plugin.getLogger().log(Level.WARNING, "Unsupported parameter type for command argument: " + targetType.getSimpleName());
                    value = rawArg; // Treat as string if type cannot be parsed
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid number format for argument '" + paramName + "'. Expected a number.");
            } catch (IllegalArgumentException e) {
                // Re-throw specific errors from player/world/material parsing
                throw e;
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to parse argument '" + paramName + "' (" + rawArg + ") to type " + targetType.getSimpleName(), e);
                throw new IllegalArgumentException("Failed to parse argument '" + paramName + "'. See console for details.");
            }

            parsedArgs[parameterIndex++] = value;
            rawArgIndex++;
        }
        return parsedArgs;
    }
}
