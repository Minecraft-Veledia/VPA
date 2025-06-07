package de.veledia.vpa.addons.CommandHandler;

import de.veledia.vpa.annotations.Command;
import de.veledia.vpa.annotations.SubCommandParam;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Manages top-level commands and their annotated sub-commands.
 * Scans classes for @Command annotations and handles argument parsing,
 * permission checks, and execution on the correct thread.
 */
public class CommandManager implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final String mainCommandName;
    private final Map<String, SubCommandMeta> subCommands; // Stores metadata about each sub-command
    private final BukkitScheduler scheduler;
    private final ArgumentParser argumentParser; // New ArgumentParser instance

    /**
     * Constructor for the CommandManager.
     * @param plugin The main plugin instance.
     * @param mainCommandName The name of the top-level command (e.g., "myplugin").
     */
    public CommandManager(JavaPlugin plugin, String mainCommandName) {
        this.plugin = plugin;
        this.mainCommandName = mainCommandName;
        this.subCommands = new HashMap<>();
        this.scheduler = plugin.getServer().getScheduler();
        this.argumentParser = new ArgumentParser(plugin); // Initialize the ArgumentParser

        // Register the main command with Bukkit
        Objects.requireNonNull(plugin.getCommand(mainCommandName)).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand(mainCommandName)).setTabCompleter(this);
    }

    /**
     * Registers all @Command annotated methods from a given command handler class.
     * Each annotated method becomes a sub-command.
     * @param handler The instance of the class containing command methods.
     */
    public void registerCommand(VelediaCommand handler){
        handler.setPlugin(plugin); // Set plugin instance in the handler

        for (Method method : handler.getClass().getMethods()) {
            if (method.isAnnotationPresent(Command.class)) {
                Command commandAnnotation = method.getAnnotation(Command.class);
                String commandName = commandAnnotation.name().toLowerCase();

                // Build parameter map for argument parsing
                LinkedHashMap<String, Class<?>> methodParams = new LinkedHashMap<>();
                Parameter[] parameters = method.getParameters();

                // Skip the first parameter if it's CommandSender (handled implicitly)
                int startIndex = (parameters.length > 0 && parameters[0].getType() == CommandSender.class) ? 1 : 0;

                for (int i = startIndex; i < parameters.length; i++) {
                    Parameter param = parameters[i];
                    SubCommandParam paramAnnotation = param.getAnnotation(SubCommandParam.class);
                    if (paramAnnotation != null) {
                        methodParams.put(paramAnnotation.value(), param.getType());
                    } else {
                        // Fallback: If no @SubCommandParam, use parameter name (Java 8+ for real names)
                        methodParams.put(param.getName(), param.getType()); // Default naming for unnamed params
                    }
                }

                SubCommandMeta meta = new SubCommandMeta(
                        handler,
                        method,
                        commandAnnotation.name(),
                        commandAnnotation.description(),
                        commandAnnotation.usage(),
                        Arrays.asList(commandAnnotation.aliases()),
                        commandAnnotation.minArgs(),
                        commandAnnotation.maxArgs(),
                        commandAnnotation.permission(),
                        commandAnnotation.permissionMessage(),
                        commandAnnotation.requiresMainThread(),
                        Arrays.asList(commandAnnotation.examples()),
                        methodParams // Store the parameter types
                );

                subCommands.put(commandName, meta);
                for (String alias : meta.aliases()) {
                    subCommands.put(alias.toLowerCase(), meta);
                }
                plugin.getLogger().log(Level.INFO, "Registered sub-command: /" + mainCommandName + " " + meta.name());
            }
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, org.bukkit.command.@NotNull Command cmd, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!cmd.getName().equalsIgnoreCase(mainCommandName)) {
            return false;
        }

        // Handle 'help' command
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelpMessage(sender, args.length > 1 ? args[1] : null);
            return true;
        }

        String subCommandName = args[0].toLowerCase();
        SubCommandMeta subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            sender.sendMessage(ChatColor.RED + "Unknown sub-command: " + ChatColor.YELLOW + args[0] + ChatColor.RED + ". Use " + ChatColor.YELLOW + "/" + mainCommandName + " help" + ChatColor.RED + " for a list of commands.");
            return true;
        }

        // Permission check
        if (!subCommand.permission().isEmpty() && !sender.hasPermission(subCommand.permission())) {
            String msg = subCommand.permissionMessage();
            sender.sendMessage(msg != null && !msg.isEmpty() ? ChatColor.RED + msg : ChatColor.RED + "You don't have permission to execute this command.");
            return true;
        }

        String[] commandArgs = Arrays.copyOfRange(args, 1, args.length);

        // Argument count validation
        if (commandArgs.length < subCommand.minArgs() || (subCommand.maxArgs() != -1 && commandArgs.length > subCommand.maxArgs())) {
            sender.sendMessage(ChatColor.RED + "Incorrect usage. Use: " + ChatColor.YELLOW + "/" + mainCommandName + " " + subCommand.usage());
            if (!subCommand.examples().isEmpty()) {
                sender.sendMessage(ChatColor.GRAY + "Examples:");
                subCommand.examples().forEach(example ->
                        sender.sendMessage(ChatColor.GRAY + "  " + ChatColor.YELLOW + "/" + mainCommandName + " " + example));
            }
            return true;
        }

        // Prepare arguments for method invocation using ArgumentParser
        Object[] parsedArgs;
        try {
            parsedArgs = argumentParser.parseArguments(sender, commandArgs, subCommand.method(), subCommand.parameterTypes());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Error parsing arguments: " + ChatColor.YELLOW + e.getMessage());
            sender.sendMessage(ChatColor.RED + "Usage: " + ChatColor.YELLOW + "/" + mainCommandName + " " + subCommand.usage());
            return true;
        }

        // Execution logic on the correct thread (Paper optimization)
        Runnable commandTask = () -> {
            try {
                subCommand.method().invoke(subCommand.handler(), parsedArgs);
            } catch (InvocationTargetException e) {
                // Unpack the real exception from InvocationTargetException
                Throwable cause = e.getCause();
                sender.sendMessage(ChatColor.RED + "An unexpected error occurred while executing the command.");
                plugin.getLogger().log(Level.SEVERE, "Error executing command '" + subCommand.name() + "' by " + sender.getName() + ": " + cause.getMessage(), cause);
                if (sender instanceof Player) { // Only send stack trace to console, not player
                    plugin.getLogger().log(Level.SEVERE, "Stack trace for command error:", cause);
                }
            } catch (IllegalAccessException e) {
                sender.sendMessage(ChatColor.RED + "An internal error occurred (IllegalAccess).");
                plugin.getLogger().log(Level.SEVERE, "Illegal access during command execution for '" + subCommand.name() + "': " + e.getMessage(), e);
            }
        };

        if (subCommand.requiresMainThread()) {
            scheduler.runTask(plugin, commandTask); // Synchronous execution
        } else {
            scheduler.runTaskAsynchronously(plugin, commandTask); // Asynchronous execution
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, org.bukkit.command.@NotNull Command cmd, @NotNull String alias, @NotNull String @NotNull [] args) {
        if (!cmd.getName().equalsIgnoreCase(mainCommandName)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            // Suggest sub-commands if sender has permission
            return subCommands.values().stream()
                    .filter(subCmd -> subCmd.permission().isEmpty() || sender.hasPermission(subCmd.permission()))
                    .map(SubCommandMeta::name)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .distinct() // Remove duplicates if aliases are registered
                    .sorted()
                    .collect(Collectors.toList());
        } else if (args.length > 1) {
            String subCommandName = args[0].toLowerCase();
            SubCommandMeta subCommand = subCommands.get(subCommandName);
            if (subCommand != null && (subCommand.permission().isEmpty() || sender.hasPermission(subCommand.permission()))) {
                // Delegate tab completion to the CustomCommand handler
                String[] tabArgs = Arrays.copyOfRange(args, 1, args.length);
                return subCommand.handler().onTabComplete(sender, tabArgs);
            }
        }
        return Collections.emptyList();
    }

    /**
     * Sends help messages for the main command or a specific sub-command.
     * @param sender The command sender.
     * @param specificSubCommand The name of a specific sub-command to show help for (or null for general help).
     */
    private void sendHelpMessage(CommandSender sender, String specificSubCommand) {
        if (specificSubCommand != null) {
            SubCommandMeta cmd = subCommands.get(specificSubCommand.toLowerCase());
            if (cmd != null && (cmd.permission().isEmpty() || sender.hasPermission(cmd.permission()))) {
                sender.sendMessage(ChatColor.GOLD + "--- Help for " + ChatColor.YELLOW + "/" + mainCommandName + " " + cmd.name() + ChatColor.GOLD + " ---");
                sender.sendMessage(ChatColor.AQUA + "Description: " + ChatColor.GRAY + cmd.description());
                sender.sendMessage(ChatColor.AQUA + "Usage: " + ChatColor.YELLOW + "/" + mainCommandName + " " + cmd.usage());
                if (!cmd.aliases().isEmpty()) {
                    sender.sendMessage(ChatColor.AQUA + "Aliases: " + ChatColor.GRAY + String.join(", ", cmd.aliases()));
                }
                if (!cmd.examples().isEmpty()) {
                    sender.sendMessage(ChatColor.AQUA + "Examples:");
                    cmd.examples().forEach(example -> sender.sendMessage(ChatColor.YELLOW + "  /" + mainCommandName + " " + example));
                }
                if (!cmd.permission().isEmpty()) {
                    sender.sendMessage(ChatColor.AQUA + "Permission: " + ChatColor.GRAY + cmd.permission());
                }
                sender.sendMessage(ChatColor.GOLD + "--------------------");
                return;
            } else {
                sender.sendMessage(ChatColor.RED + "Unknown command or no permission for: " + ChatColor.YELLOW + specificSubCommand);
            }
        }

        sender.sendMessage(ChatColor.GOLD + "--- " + plugin.getName() + " Commands (" + ChatColor.YELLOW + "/" + mainCommandName + " help <cmd>" + ChatColor.GOLD + ") ---");
        subCommands.values().stream()
                .filter(subCmd -> subCmd.permission().isEmpty() || sender.hasPermission(subCmd.permission())) // Filter by permission
                .distinct() // Handle aliases by only showing unique command entries
                .sorted(Comparator.comparing(SubCommandMeta::name)) // Sort alphabetically
                .forEach(cmd -> sender.sendMessage(ChatColor.YELLOW + "/" + mainCommandName + " " + cmd.usage() + ChatColor.GRAY + " - " + cmd.description()));
        sender.sendMessage(ChatColor.GOLD + "--------------------");
    }



    /**
     * Record to hold metadata about a registered sub-command.
     * Using a record for conciseness with Java 16+.
     */
    private record SubCommandMeta(
            VelediaCommand handler,
            Method method,
            String name,
            String description,
            String usage,
            List<String> aliases,
            int minArgs,
            int maxArgs,
            String permission,
            String permissionMessage,
            boolean requiresMainThread,
            List<String> examples,
            LinkedHashMap<String, Class<?>> parameterTypes // New: stores expected parameter types
    ) {}
}
