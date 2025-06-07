package de.veledia.vpa.addons.MenuHandler;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * A comprehensive builder class for creating custom Minecraft GUIs (menus) with pagination,
 * dynamic content, and various inventory types. Implements CustomMenu and InventoryHolder.
 */
public class MenuBuilder implements VelediaMenu, InventoryHolder {

    protected final JavaPlugin plugin;
    protected String title;
    protected int size; // Total number of slots for the current page
    protected InventoryType inventoryType; // Type of inventory (e.g., CHEST, FURNACE)

    // For pagination
    protected int currentPage = 0;
    // Each map represents a page: PageNumber -> (Slot -> MenuItem)
    protected List<Map<Integer, MenuItem>> pages = new ArrayList<>();

    // Navigation items (common across pages, can be null)
    protected MenuItem nextPageItem;
    protected MenuItem prevPageItem;

    protected Object context; // Optional context object for dynamic menus

    /**
     * Constructs a new MenuBuilder for a CHEST type inventory.
     * @param plugin The main plugin instance.
     * @param title The title of the inventory displayed to the player.
     * @param rows The number of rows for the CHEST inventory (1-6).
     */
    public MenuBuilder(JavaPlugin plugin, String title, int rows) {
        this(plugin, title, rows, InventoryType.CHEST);
    }

    /**
     * Constructs a new MenuBuilder with a specified InventoryType.
     * For CHEST type, rows must be between 1-6. For other types, rows is ignored and size is fixed.
     * @param plugin The main plugin instance.
     * @param title The title of the inventory displayed to the player.
     * @param rows The number of rows for CHEST inventory. Ignored for other types.
     * @param type The InventoryType of the menu (e.g., InventoryType.CHEST, InventoryType.HOPPER).
     */
    public MenuBuilder(JavaPlugin plugin, String title, int rows, InventoryType type) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null.");
        this.title = Objects.requireNonNull(title, "Title cannot be null.");
        this.inventoryType = Objects.requireNonNull(type, "InventoryType cannot be null.");

        if (type == InventoryType.CHEST) {
            if (rows < 1 || rows > 6) {
                throw new IllegalArgumentException("Menu rows for CHEST must be between 1 and 6.");
            }
            this.size = rows * 9;
        } else {
            // For other types, size is fixed by Bukkit
            this.size = type.getDefaultSize();
        }

        // Initialize with at least one page
        pages.add(new HashMap<>());
    }

    /**
     * Sets the title of the menu.
     * @param title The new title.
     * @return The MenuBuilder instance for chaining.
     */
    public MenuBuilder title(String title) {
        this.title = Objects.requireNonNull(title, "Title cannot be null.");
        return this;
    }

    /**
     * Adds a new empty page to the menu.
     * @return The MenuBuilder instance for chaining.
     */
    public MenuBuilder addPage() {
        pages.add(new HashMap<>());
        return this;
    }

    /**
     * Places a MenuItem at a specific slot on the current page.
     * @param slot The inventory slot (0 to size-1).
     * @param item The MenuItem to place.
     * @return The MenuBuilder instance for chaining.
     * @throws IllegalArgumentException If the slot is out of bounds for the current page size.
     */
    public MenuBuilder setItem(int slot, MenuItem item) {
        return setItem(currentPage, slot, item);
    }

    /**
     * Places a MenuItem at a specific slot on a specified page.
     * @param page The page number (0-indexed).
     * @param slot The inventory slot (0 to size-1).
     * @param item The MenuItem to place.
     * @return The MenuBuilder instance for chaining.
     * @throws IllegalArgumentException If the page or slot is out of bounds.
     */
    public MenuBuilder setItem(int page, int slot, MenuItem item) {
        if (page < 0 || page >= pages.size()) {
            throw new IllegalArgumentException("Page " + page + " does not exist. Add it with addPage() first.");
        }
        if (slot < 0 || slot >= size) {
            throw new IllegalArgumentException("Slot " + slot + " is out of bounds for a menu of size " + size);
        }
        pages.get(page).put(slot, Objects.requireNonNull(item, "MenuItem cannot be null."));
        return this;
    }

    /**
     * Fills a range of slots on the current page with a placeholder MenuItem.
     * Useful for filling empty space or borders.
     * @param startSlot The starting slot (inclusive).
     * @param endSlot The ending slot (inclusive).
     * @param item The MenuItem to use as a filler.
     * @return The MenuBuilder instance for chaining.
     */
    public MenuBuilder fillSlots(int startSlot, int endSlot, MenuItem item) {
        return fillSlots(currentPage, startSlot, endSlot, item);
    }

    /**
     * Fills a range of slots on a specified page with a placeholder MenuItem.
     * @param page The page number (0-indexed).
     * @param startSlot The starting slot (inclusive).
     * @param endSlot The ending slot (inclusive).
     * @param item The MenuItem to use as a filler.
     * @return The MenuBuilder instance for chaining.
     */
    public MenuBuilder fillSlots(int page, int startSlot, int endSlot, MenuItem item) {
        for (int i = startSlot; i <= endSlot; i++) {
            setItem(page, i, item);
        }
        return this;
    }

    /**
     * Fills all empty slots on the current page with a placeholder MenuItem.
     * @param item The MenuItem to use as a filler.
     * @return The MenuBuilder instance for chaining.
     */
    public MenuBuilder fillEmptySlots(MenuItem item) {
        return fillEmptySlots(currentPage, item);
    }

    /**
     * Fills all empty slots on a specified page with a placeholder MenuItem.
     * @param page The page number (0-indexed).
     * @param item The MenuItem to use as a filler.
     * @return The MenuBuilder instance for chaining.
     */
    public MenuBuilder fillEmptySlots(int page, MenuItem item) {
        if (page < 0 || page >= pages.size()) {
            throw new IllegalArgumentException("Page " + page + " does not exist. Add it with addPage() first.");
        }
        for (int i = 0; i < size; i++) {
            if (!pages.get(page).containsKey(i)) {
                setItem(page, i, item);
            }
        }
        return this;
    }

    /**
     * Sets the item that navigates to the next page.
     * This item's click action will automatically advance the page.
     * @param itemTemplate The MenuItem whose appearance (material, name, lore, custom model data, sound) will be used for the next page button.
     * Its click action is IGNORED and replaced with the navigation logic.
     * @param slot The slot where the item should be placed on each page.
     * @return The MenuBuilder instance for chaining.
     */
    public MenuBuilder setNextPageItem(MenuItem itemTemplate, int slot) {
        // Debug-Ausgabe
        plugin.getLogger().info("Setze NextPageItem: Aktuelle Seite = " + currentPage);
        plugin.getLogger().info("Anzahl der Seiten = " + pages.size());

        this.nextPageItem = new MenuItem.Builder(itemTemplate.getItemStack().getType(), itemTemplate.getItemStack().getItemMeta().getDisplayName())
                .lore(itemTemplate.getItemStack().getItemMeta().hasLore() ? itemTemplate.getItemStack().getItemMeta().getLore().toArray(new String[0]) : new String[]{"Go to the next page"})
                .customModelData(itemTemplate.getItemStack().getItemMeta().hasCustomModelData() ? itemTemplate.getItemStack().getItemMeta().getCustomModelData() : 0)
                .withClickSound(itemTemplate.clickSound, itemTemplate.soundVolume, itemTemplate.soundPitch) // Preserve sound if set
                .onLeftClick(e -> {
                    e.getWhoClicked().sendMessage(itemTemplate.getItemStack().getItemMeta().getDisplayName());
                    if (currentPage < pages.size() - 1) {
                        currentPage++;
                        open((Player) e.getWhoClicked()); // Re-open the menu to show next page
                    } else {
                        e.getWhoClicked().sendMessage(ChatColor.RED + "You are on the last page.");
                    }
                })
                .build();
        // Do NOT call setItem(currentPage, slot, this.nextPageItem) here.
        // The item is placed in createInventory() on all relevant pages.
        return this;
    }

    /**
     * Sets the item that navigates to the previous page.
     * This item's click action will automatically go back one page.
     * @param itemTemplate The MenuItem whose appearance (material, name, lore, custom model data, sound) will be used for the previous page button.
     * Its click action is IGNORED and replaced with the navigation logic.
     * @param slot The slot where the item should be placed on each page.
     * @return The MenuBuilder instance for chaining.
     */
    public MenuBuilder setPrevPageItem(MenuItem itemTemplate, int slot) {
        this.prevPageItem = new MenuItem.Builder(itemTemplate.getItemStack().getType(), itemTemplate.getItemStack().getItemMeta().getDisplayName())
                .lore(itemTemplate.getItemStack().getItemMeta().hasLore() ? itemTemplate.getItemStack().getItemMeta().getLore().toArray(new String[0]) : new String[]{"Go to the previous page"})
                .customModelData(itemTemplate.getItemStack().getItemMeta().hasCustomModelData() ? itemTemplate.getItemStack().getItemMeta().getCustomModelData() : 0)
                .withClickSound(itemTemplate.clickSound, itemTemplate.soundVolume, itemTemplate.soundPitch) // Preserve sound if set
                .onLeftClick(e -> {
                    if (currentPage > 0) {
                        currentPage--;
                        open((Player) e.getWhoClicked()); // Re-open the menu to show previous page
                    } else {
                        e.getWhoClicked().sendMessage(ChatColor.RED + "You are on the first page.");
                    }
                })
                .build();
        // Do NOT call setItem(currentPage, slot, this.prevPageItem) here.
        // The item is placed in createInventory() on all relevant pages.
        return this;
    }

    @Override
    public Inventory createInventory() {
        // Create the inventory for the current page based on its type and size
        Inventory inventory = Bukkit.createInventory(this, inventoryType, title);

        // Get items for the current page
        Map<Integer, MenuItem> currentItems = pages.get(currentPage);
        currentItems.forEach((slot, menuItem) -> inventory.setItem(slot, menuItem.getItemStack()));

        // Add navigation items if applicable and they exist.
        // These are placed last to ensure they overwrite any fillers or background items at their slots.
        if (pages.size() > 1) { // Only show navigation if there's more than one page
            if (currentPage < pages.size() - 1 && nextPageItem != null) {
                // Place next page item, for example, at the last slot of the inventory
                inventory.setItem(size - 1, nextPageItem.getItemStack());
            }
            if (currentPage > 0 && prevPageItem != null) {
                // Place previous page item, for example, at the first slot of the last row
                inventory.setItem(size - 9, prevPageItem.getItemStack());
            }
        }
        return inventory;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        // Always cancel clicks in custom menus by default to prevent players from taking items out
        event.setCancelled(true);

        MenuItem clickedItem = null;

        // First, check for page-specific items stored in the current page map
        clickedItem = pages.get(currentPage).get(event.getRawSlot());

        // log the click event for debugging
        plugin.getLogger().info("Clicked item at slot " + event.getRawSlot() + " in page " + currentPage + ": " + (clickedItem != null ? clickedItem.getItemStack().getType() : "null"));

        // If not a page-specific item, check if it's one of the global navigation items
        // The slots (size - 1) and (size - 9) are examples. Adjust if your buttons are elsewhere.
        if (clickedItem == null) {
            if (nextPageItem != null && event.getRawSlot() == (size - 1)) {
                clickedItem = nextPageItem;
            } else if (prevPageItem != null && event.getRawSlot() == (size - 9)) {
                clickedItem = prevPageItem;
            }
        }

        // Execute the action if an item was clicked
        if (clickedItem != null) {
            clickedItem.executeClickAction(event);
        }
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public void open(Player player) {
        // Call the onOpen hook before creating/opening the inventory
        onOpen(player);

        Inventory inventory = createInventory();
        player.openInventory(inventory);

        // Register the open menu with the MenuManager.
        // Use a small delay (1 tick) to ensure the inventory is fully opened and
        // any InventoryCloseEvent from a previous menu is processed before registration.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (MenuManager.getInstance(plugin) != null) {
                MenuManager.getInstance(plugin).registerOpenMenu(player, this);
            }
        }, 1L);
        // Am Ende Ihrer Menü-Initialisierung
        player.sendMessage("Anzahl der Seiten: " + pages.size());
        player.sendMessage("Aktuelle Seite: " + currentPage);
    }

    @Override
    public void setContext(Object context) {
        this.context = context;
    }

    @Override
    public Object getContext() {
        return context;
    }

    // Default implementations for onOpen and onClose are in the CustomMenu interface.
    // They can be overridden anonymously or in a subclass if needed.

    @Override
    public Inventory getInventory() {
        // This method is required by InventoryHolder.
        // It's primarily used by Bukkit internally to link the inventory to its holder.
        // For our builder, it's often not directly used to retrieve the current inventory state.
        return null;
    }
}
