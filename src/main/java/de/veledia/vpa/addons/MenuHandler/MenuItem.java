package de.veledia.vpa.addons.MenuHandler;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class MenuItem {

    private final ItemStack itemStack;
    private Consumer<InventoryClickEvent> leftClickAction;
    private Consumer<InventoryClickEvent> rightClickAction;
    public Sound clickSound; // Optional sound to play on click
    public float soundVolume = 1.0f; // Default volume
    public float soundPitch = 1.0f; // Default pitch

    private MenuItem(Material material, String name, List<String> lore, int customModelData,
                     Consumer<InventoryClickEvent> leftClickAction, Consumer<InventoryClickEvent> rightClickAction,
                     Sound clickSound, float soundVolume, float soundPitch) {
        this.itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null){
            meta.customName(Component.text(name));
            if (lore != null && !lore.isEmpty()) {
                meta.lore(lore.stream().map(Component::text).toList());
            }
            if (customModelData > 0) {
                meta.setCustomModelData(customModelData);
            }
            itemStack.setItemMeta(meta);
        }
        this.leftClickAction = leftClickAction;
        this.rightClickAction = rightClickAction;
        this.clickSound = clickSound;
        this.soundVolume = soundVolume;
        this.soundPitch = soundPitch;
    }

    /**
     * Gets the ItemStack representation of this MenuItem.
     * @return The ItemStack.
     */
    public ItemStack getItemStack() {
        return itemStack;
    }

    /**
     * Executes the appropriate click action based on the click type.
     * Plays a sound if configured.
     * @param event The InventoryClickEvent.
     */
    public void executeClickAction(InventoryClickEvent event){
        if (clickSound != null && event.getWhoClicked() instanceof Player player){
            player.playSound(player.getLocation(), clickSound, soundVolume, soundPitch);
        }

        if (event.isLeftClick() && leftClickAction != null) {
            leftClickAction.accept(event);
        } else if (event.isRightClick() && rightClickAction != null) {
            rightClickAction.accept(event);
        }
    }

    /**
     * Builder class for constructing MenuItem instances.
     * Provides a fluent API for setting all properties.
     */
    public static class Builder {
        private Material material;
        private String name;
        private List<String> lore;
        private int customModelData = 0; // Default to 0
        private Consumer<InventoryClickEvent> leftClickAction;
        private Consumer<InventoryClickEvent> rightClickAction;
        private Sound clickSound;
        private float soundVolume = 1.0f; // Default volume
        private float soundPitch = 1.0f; // Default pitch

        /**
         * Initializes the builder with required material and name.
         * @param material The base material of the item.
         * @param name The display name of the item.
         */
        public Builder(Material material, String name){
            this.material = Objects.requireNonNull(material, "Material cannot be null");
            this.name = Objects.requireNonNull(name, "Name cannot be null");
            this.lore = new ArrayList<>();
        }

        /**
         * Adds lore lines to the item.
         * @param lines An array of strings for the item's lore.
         * @return The Builder instance.
         */
        public Builder lore(String... lines){
            this.lore.addAll(Arrays.asList(lines));
            return this;
        }

        /**
         * Sets the custom model data for the item (for resource packs).
         * @param data The custom model data value.
         * @return The Builder instance.
         */
        public Builder customModelData(int data){
            if (data < 0) {
                throw new IllegalArgumentException("Custom model data must be non-negative");
            }
            this.customModelData = data;
            return this;
        }

        /**
         * Sets the action to be performed on a left click.
         * @param action The Consumer to execute on left click.
         * @return The Builder instance.
         */
        public Builder onLeftClick(Consumer<InventoryClickEvent> action) {
            this.leftClickAction = action;
            return this;
        }

        /**
         * Sets the action to be performed on a right click.
         * @param action The Consumer to execute on right click.
         * @return The Builder instance.
         */
        public Builder onRightClick(Consumer<InventoryClickEvent> action) {
            this.rightClickAction = action;
            return this;
        }

        /**
         * Sets an optional sound to play when the item is clicked.
         * @param sound The Sound enum to play.
         * @return The Builder instance.
         */
        public Builder withClickSound(Sound sound) {
            this.clickSound = sound;
            return this;
        }

        /**
         * Sets an optional sound to play when the item is clicked, with custom volume and pitch.
         * @param sound The Sound enum to play.
         * @param volume The volume of the sound (1.0f is default).
         * @param pitch The pitch of the sound (1.0f is default).
         * @return The Builder instance.
         */
        public Builder withClickSound(Sound sound, float volume, float pitch) {
            this.clickSound = sound;
            this.soundVolume = volume;
            this.soundPitch = pitch;
            return this;
        }

        /**
         * Builds the MenuItem instance.
         * @return The constructed MenuItem.
         */
        public MenuItem build() {
            return new MenuItem(material, name, lore, customModelData,
                    leftClickAction, rightClickAction, clickSound, soundVolume, soundPitch);
        }
    }
}
