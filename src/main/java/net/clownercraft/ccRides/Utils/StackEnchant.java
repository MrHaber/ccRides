package net.clownercraft.ccRides.Utils;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.inventory.ItemStack;

/**
 * Enchantment to give items a glowing effect.
 */
public class StackEnchant extends EnchantmentWrapper {

    public StackEnchant() {
      super("ccglow");
    }
    @Override
    public boolean canEnchantItem(ItemStack itemStack) {
        return false;
    }

    @Override
    public int getStartLevel() {
        return 0;
    }

    @Override
    public int getMaxLevel() {
        return 0;
    }

    @Override
    public boolean isCursed() {
        return false;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean isTreasure() {
        return false;
    }

    @Override
    public boolean conflictsWith(Enchantment enchantment) {
        return false;
    }

    @Override
    public EnchantmentTarget getItemTarget() {
        return null;
    }
}
