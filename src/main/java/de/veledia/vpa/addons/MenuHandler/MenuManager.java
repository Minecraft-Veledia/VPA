package de.veledia.vpa.addons.MenuHandler;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Manages open custom menus and dispatches inventory click and close events
 * to the correct CustomMenu handler. This class must be registered as a Bukkit Listener.
 * Implemented as a singleton.
 */
public class MenuManager implements Listener {

    private static MenuManager instance; // Singleton instance
    private final JavaPlugin plugin;
    // Map to keep track of which player has which custom menu open
    private final Map<UUID, VelediaMenu> openMenus;

    /**
     * Private constructor for the singleton pattern.
     * @param plugin The main plugin instance.
     */
    private MenuManager(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null.");
        this.openMenus = new HashMap<>();
        // Register this class as a Bukkit Listener when initialized
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().log(Level.INFO, "MenuManager initialized and registered as Listener.");
    }

    /**
     * Initializes the singleton instance of MenuManager.
     * This method should be called once during plugin startup (e.g., in onEnable()).
     * Subsequent calls will return the existing instance.
     * @param plugin The main plugin instance.
     * @return The singleton MenuManager instance.
     */
    public static MenuManager initialize(JavaPlugin plugin) {
        if (instance == null) {
            instance = new MenuManager(plugin);
        }
        return instance;
    }

    /**
     * Gets the singleton instance of MenuManager.
     * If the manager has not been initialized yet, it will log a warning and initialize itself.
     * @param plugin The main plugin instance (used for initial creation if needed).
     * @return The MenuManager instance.
     */
    public static MenuManager getInstance(JavaPlugin plugin) {
        if (instance == null) {
            plugin.getLogger().warning("MenuManager accessed before initialization. Please call MenuManager.initialize(plugin) in your onEnable() method.");
            return initialize(plugin); // Auto-initialize to prevent NullPointerExceptions, but warn.
        }
        return instance;
    }

    /**
     * Registers that a player has opened a specific custom menu.
     * This is crucial for tracking which menu instance is associated with which player.
     * @param player The player who opened the menu.
     * @param menu The CustomMenu instance that was opened.
     */
    public void registerOpenMenu(Player player, VelediaMenu menu) {
        openMenus.put(player.getUniqueId(), menu);
        plugin.getLogger().log(Level.FINE, "Registered open menu for player " + player.getName() + ": " + menu.getTitle());
    }

    /**
     * Handles inventory click events. If the clicked inventory is a custom menu
     * managed by MenuBuilder, it delegates the click handling to the corresponding MenuBuilder instance.
     * @param event The InventoryClickEvent.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Check if the inventory's holder is an instance of MenuBuilder (which implements InventoryHolder).
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof MenuBuilder menuBuilder) {
            // The clicked inventory belongs to one of our custom menus.
            menuBuilder.handleClick(event);
            plugin.getLogger().log(Level.FINEST, "Handled click in custom menu: " + menuBuilder.getTitle() + " at slot " + event.getRawSlot());
        }
        // If it's not a MenuBuilder holder, it could still be a tracked CustomMenu instance
        // that opened an inventory but didn't set itself as holder (less common for this setup).
        // This secondary check ensures robustness, though the primary `holder instanceof MenuBuilder` is usually sufficient.
        else if (event.getWhoClicked() instanceof Player player) {
            VelediaMenu customMenu = openMenus.get(player.getUniqueId());
            // This comparison `event.getInventory().equals(customMenu.createInventory())` can be problematic
            // because `createInventory()` creates a NEW inventory. It's better to rely on InventoryHolder.
            // Keeping it for robustness if `holder instanceof MenuBuilder` somehow fails, but ideally,
            // all custom menus use themselves as InventoryHolder.
            if (customMenu != null && event.getInventory().getHolder() == customMenu) {
                customMenu.handleClick(event);
                plugin.getLogger().log(Level.FINEST, "Handled click in fallback custom menu for player " + player.getName());
            }
        }
    }

    /**
     * Handles inventory close events. When a player closes a custom menu,
     * it is unregistered from the manager and the menu's onClose hook is called.
     * @param event The InventoryCloseEvent.
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            // Remove the menu from our tracking map when a player closes it
            // DO THIS BEFORE calling onClose() to prevent potential infinite loops
            // if onClose() tries to open a new menu.
            VelediaMenu closedMenu = openMenus.remove(player.getUniqueId());
            if (closedMenu != null) {
                // Call the onClose hook of the closed menu
                closedMenu.onClose(player);
                plugin.getLogger().log(Level.FINE, "Unregistered and called onClose for menu of player " + player.getName() + ": " + closedMenu.getTitle());
            }
        }
    }
}
