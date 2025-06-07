package de.veledia.vpa.addons.CommandHandler;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Base interface for all command handlers.
 * While actual command methods are annotated, this interface can be
 * used to group common functionality or mark classes as command containers.
 */
public interface VelediaCommand {
    /**
     * Set the plugin instance for this command handler.
     * Useful if the command handler needs to access plugin-specific services.
     * @param plugin The JavaPlugin instance.
     */
    void setPlugin(JavaPlugin plugin);

    /**
     * Handles tab completion for this command handler.
     * This method will be called when a player attempts to tab-complete arguments
     * for a sub-command managed by this handler.
     * @param sender The sender of the command.
     * @param args The arguments provided after the sub-command name.
     * @return A list of possible completions.
     */
    List<String> onTabComplete(CommandSender sender, String[] args);
}
