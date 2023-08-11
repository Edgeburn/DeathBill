package com.edgeburnmedia.deathbill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public final class DeathBill extends JavaPlugin implements Listener {
    private static Economy econ = null;
    private double fee = 0.0;
    private List<String> disabledWorlds = new ArrayList<>();

    @Override
    public void onEnable() {
        if (!setupEconomy()) {
            getLogger().severe("Disabled due to missing dependency Vault!");
            getServer().getPluginManager().disablePlugin(this);
        }
        getServer().getPluginManager().registerEvents(this, this);

        saveDefaultConfig();
        loadConfig();
    }

    private void loadConfig() {
        fee = getConfig().getDouble("fee", 0.0);
        disabledWorlds = getConfig().getStringList("disabled-worlds");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player p = e.getPlayer();
        if (!e.isCancelled()) {
            if (!(p.hasPermission("deathbill.exempt") || disabledWorlds.contains(p.getWorld().getName()))) {
                double currentBal = econ.getBalance(p);
                double delta = round(currentBal * fee, 2);
                econ.withdrawPlayer(p, delta);
                p.sendMessage(Component.text()
                        .append(Component.text("You were billed ").color(NamedTextColor.RED))
                        .append(Component.text("$" +
                                String.format("%.2f", delta))
                                .color(NamedTextColor.GREEN))
                        .append(Component.text(" for dying. Your new balance is ").color(NamedTextColor.RED))
                        .append(Component.text("$" +
                                String.format("%.2f", round(econ.getBalance(p), 2)))
                                .color(NamedTextColor.GREEN))
                        .build()
                );
                getLogger().info(String.format("%s was charged %.2f for their recent death. Use the command /eco give %s %.2f to refund them", p.getName(), delta, p.getName(), delta));
                getServer().getOnlinePlayers().forEach(player -> {
                    if (player.hasPermission("deathbill.alert")) {
                        player.sendMessage(Component.text()
                                .append(p.displayName())
                                .append(Component.text(" was charged ").color(NamedTextColor.RED))
                                .append(Component.text("$" +
                                                String.format("%.2f", delta))
                                        .color(NamedTextColor.GREEN))
                                .append(Component.text(" for their recent death.").color(NamedTextColor.RED))
                                .append(Component.text("Click here to refund them").color(NamedTextColor.BLUE).clickEvent(ClickEvent.runCommand("eco give " + p.getName() + delta)))
                                .build()
                        );
                    }
                });
            }
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    /**
     * Round a number to a specific number of decimal places. "Borrowed" from
     * <a href=https://stackoverflow.com/a/2808648>StackOverflow</a>
     * @param value Value to be rounded
     * @param places How many decimal places to round to
     * @return The rounded number
     */
    private static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

}
