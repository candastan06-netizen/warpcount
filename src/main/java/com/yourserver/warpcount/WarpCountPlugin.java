package com.yourserver.warpcount;

import com.earth2me.essentials.Essentials;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class WarpCountPlugin extends JavaPlugin {

    private WarpCountGUI warpCountGUI;

    @Override
    public void onEnable() {
        Plugin essentialsPlugin = Bukkit.getPluginManager().getPlugin("Essentials");

        if (!(essentialsPlugin instanceof Essentials)) {
            getLogger().severe("Essentials was not found or is not enabled.");
            getLogger().severe("WarpCountPlugin will be disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Essentials essentials = (Essentials) essentialsPlugin;

        warpCountGUI = new WarpCountGUI(this, essentials);

        if (getCommand("warpcount") == null) {
            getLogger().severe("The warpcount command is missing from plugin.yml.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getCommand("warpcount").setExecutor(warpCountGUI);

        getServer().getPluginManager().registerEvents(warpCountGUI, this);

        warpCountGUI.startRefreshTask();

        getLogger().info("WarpCountPlugin has been enabled.");
    }

    @Override
    public void onDisable() {
        if (warpCountGUI != null) {
            warpCountGUI.stopRefreshTask();
            warpCountGUI.closeAllGuis();
        }

        getLogger().info("WarpCountPlugin has been disabled.");
    }
}
