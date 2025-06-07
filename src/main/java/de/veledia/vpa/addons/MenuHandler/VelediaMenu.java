package de.veledia.vpa.addons.MenuHandler;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

/**
 * Interface for custom menus. Defines how a menu is created, handles click events,
 * and manages its lifecycle.
 */
public interface VelediaMenu {

    /**
     * Creates the Bukkit Inventory object for this menu.
     * @return The created Inventory.
     */
    Inventory createInventory();

    /**
     * Handles a click event within this menu.
     * @param event The InventoryClickEvent.
     */
    void handleClick(InventoryClickEvent event);

    /**
     * Gets the title of the menu.
     * @return The menu title.
     */
    String getTitle();

    /**
     * Gets the size of the menu (number of slots).
     * @return The menu size.
     */
    int getSize();

    /**
     * Opens this menu for a specific player.
     * @param player The player to open the menu for.
     */
    void open(Player player);

    /**
     * Sets a context object for the menu, allowing it to be dynamically generated.
     * This can be any object relevant to the menu's purpose (e.g., a specific entity, player data).
     * @param context The object representing the context.
     */
    void setContext(Object context);

    /**
     * Gets the context object of the menu.
     * @return The context object.
     */
    Object getContext();

    /**
     * Called right before the inventory is created and opened for the player.
     * Use this to dynamically set or update items based on the player or other context.
     * @param player The player for whom the menu is being opened.
     */
    default void onOpen(Player player) {
        // Default implementation does nothing.
        // Override this method in subclasses to add custom behavior before opening the menu.
    }

    /**
     * Called when the menu is closed by a player.
     * Use this for cleanup or to save data.
     * @param player The player who closed the menu.
     */
    default void onClose(Player player) {
        // Default implementation does nothing.
        // Override this method in subclasses to add custom behavior when the menu is closed.
    }


}
