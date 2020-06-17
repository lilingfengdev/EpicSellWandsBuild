package com.songoda.epicsellwands;

import com.songoda.core.SongodaPlugin;
import com.songoda.core.commands.CommandManager;
import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.configuration.Config;
import com.songoda.core.gui.GuiManager;
import com.songoda.core.hooks.EconomyManager;
import com.songoda.epicsellwands.commands.CommandGive;
import com.songoda.epicsellwands.commands.CommandReload;
import com.songoda.epicsellwands.events.BlockInteractEvent;
import com.songoda.epicsellwands.player.PlayerManager;
import com.songoda.epicsellwands.settings.Settings;
import com.songoda.epicsellwands.wand.Wand;
import com.songoda.epicsellwands.wand.WandManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EpicSellWands extends SongodaPlugin {

    private static EpicSellWands INSTANCE;

    private final Config wandsConfig = new Config(this, "wands.yml");
    private final Config pricesConfig = new Config(this, "prices.yml");

    private WandManager wandManager;
    private CommandManager commandManager;
    private PlayerManager playerManager;
    private final GuiManager guiManager = new GuiManager(this);

    public static EpicSellWands getInstance() {
        return INSTANCE;
    }

    @Override
    public void onPluginLoad() {
        INSTANCE = this;
    }

    @Override
    public void onPluginDisable() {

    }

    @Override
    public void onPluginEnable() {
        // Load Economy
        EconomyManager.load();

        // Setup Config
        Settings.setupConfig();
        this.setLocale(Settings.LANGUGE_MODE.getString(), false);

        // Set Economy & Hologram preference
        EconomyManager.getManager().setPreferredHook(Settings.ECONOMY_PLUGIN.getString());

        // Setup plugin commands
        this.commandManager = new CommandManager(this);
        this.commandManager.addMainCommand("esw")
                .addSubCommand(new CommandReload())
                .addSubCommand(new CommandGive(this));

        // Load wands.
        this.wandManager = new WandManager();
        this.playerManager = new PlayerManager();

        // Load Listeners
        Bukkit.getPluginManager().registerEvents(new BlockInteractEvent(this), this);

        loadWands();
        loadPrices();

        if (Bukkit.getPluginManager().isPluginEnabled("ModdedCore"))
            setupRecipes();
    }

    private void loadWands() {
        if (!new File(this.getDataFolder(), "wands.yml").exists())
            this.saveResource("wands.yml", false);
        wandsConfig.load();

        for (String key : wandsConfig.getKeys(false)) {
            ConfigurationSection wand = wandsConfig.getConfigurationSection(key);

            if (wand == null) continue;

            wandManager.addWand(new Wand(key, wand.getString("Name"),
                    CompatibleMaterial.getMaterial(wand.getString("Type")))
                    .setLore(wand.getStringList("Lore"))
                    .setEnchanted(wand.getBoolean("Enchanted"))
                    .setUses(wand.getInt("Uses"))

                    .setRecipeLayout(wand.getString("Recipe-Layout"))
                    .setRecipeIngredients(wand.getStringList("Recipe-Ingredients")));
        }
    }


    private void setupRecipes() {
        com.songoda.moddedcore.ModdedCore moddedCore = com.songoda.moddedcore.ModdedCore.getInstance();
        com.songoda.moddedcore.items.ItemManager itemManager = moddedCore.getItemManager();
        for (Wand wand : wandManager.getWands()) {

            String recipe = wand.getRecipeLayout();

            if (recipe.length() != 9) continue;

            if (wand.getRecipeIngredients().isEmpty()) continue;

            Map<String, String> ingredients = new HashMap<>();

            for (String ingredient : wand.getRecipeIngredients()) {
                if (!ingredient.contains(",")) continue;
                String[] s = ingredient.split(",");
                String letter = s[0].trim();
                String item = s[1].trim();
                ingredients.put(letter, item);
            }

            List<ItemStack> items = new ArrayList<>();

            for (int i = 0; i < 9; i++) {
                String symbol = String.valueOf(recipe.charAt(i));
                String item = ingredients.get(symbol);

                com.songoda.moddedcore.items.ModdedItem moddedItem = itemManager.getItem(item);
                if (moddedItem == null) {
                    items.add(CompatibleMaterial.getMaterial(item).getItem());
                } else {
                    items.add(moddedItem.asItemStack());
                }
            }

            System.out.println("[EpicSellWands] Added ModdedCore recipe for: " + wand.getKey());
            itemManager.addItem(new com.songoda.moddedcore.items.ModdedItem(this, wand.getKey(), wand.asItemStack(), itemManager.getCategory("TOOLS")));
        }
        moddedCore.getRecipeManager().loadFromFile(this);
    }

    @Override
    public void onConfigReload() {
        wandManager.clearData();
        loadPrices();
        loadWands();

    }

    @Override
    public List<Config> getExtraConfig() {
        return null;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    private void loadPrices() {
        if (!new File(this.getDataFolder(), "prices.yml").exists())
            this.saveResource("prices.yml", false);
        pricesConfig.load();

        for (String key : pricesConfig.getKeys(false)) {
            double price = pricesConfig.getDouble(key);
            CompatibleMaterial material = CompatibleMaterial.getMaterial(key);
            wandManager.addPrice(material, price);
        }
    }

    public WandManager getWandManager() {
        return wandManager;
    }


}
