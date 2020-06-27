package eu.rex2go.sneakswitch;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;

public class SneakSwitch extends JavaPlugin implements Listener, CommandExecutor {

    private final String prefix = ChatColor.AQUA + "[SneakSwitch]";

    private int time = 40;
    private GameMode defaultGameMode = GameMode.SURVIVAL;
    private String noPermissionMessage, noPlayerMessage, toggleEnableMessage, toggleDisableMessage,
            reloadSuccessMessage, reloadFailMessage;

    private HashMap<UUID, GameMode> playerSneakMap = new HashMap();
    private ArrayList<UUID> noToggleList = new ArrayList<UUID>();

    @Override
    public void onEnable() {
        setupConfig();

        // Register command
        Bukkit.getPluginCommand("sneakswitch").setExecutor(this);

        // Register listener
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void setupConfig() {
        saveDefaultConfig();

        try {
            time = getConfig().getInt("time");

            defaultGameMode = GameMode.valueOf(getConfig().getString("defaultGameMode").toUpperCase());

            noPermissionMessage = ChatColor.translateAlternateColorCodes('&', getConfig().getString("noPermission"));
            noPlayerMessage = ChatColor.translateAlternateColorCodes('&', getConfig().getString("noPlayer"));
            toggleEnableMessage = ChatColor.translateAlternateColorCodes('&', getConfig().getString("toggleEnable"));
            toggleDisableMessage = ChatColor.translateAlternateColorCodes('&', getConfig().getString("toggleDisable"));
            reloadFailMessage = ChatColor.translateAlternateColorCodes('&', getConfig().getString("reloadFail"));
            reloadSuccessMessage = ChatColor.translateAlternateColorCodes('&', getConfig().getString("reloadSuccess"));
        } catch (Exception ignored) {
            getLogger().log(Level.SEVERE, "A config error occured. Try deleting the config file and reloading the " +
                    "server.");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("sneakswitch")) {
            return false;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(noPlayerMessage);
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            if (!player.hasPermission("sneakswitch.toggle")) {
                sendMessage(player, noPermissionMessage);
                return true;
            }

            if (noToggleList.contains(player.getUniqueId())) {
                noToggleList.remove(player.getUniqueId());
                sendMessage(player, toggleEnableMessage);
                return true;
            }

            noToggleList.add(player.getUniqueId());
            sendMessage(player, toggleDisableMessage);
            return true;
        }

        String argument = args[0];

        if (argument.equalsIgnoreCase("reload")) {
            if (!player.hasPermission("sneakswitch.reload")) {
                sendMessage(player, noPermissionMessage);
                return true;
            }

            try {
                reloadConfig();
                sendMessage(player, reloadSuccessMessage);
            } catch (Exception e) {
                sendMessage(player, reloadFailMessage.replaceAll("%error%", e.getMessage()));
            }

            return true;
        }

        player.sendMessage(ChatColor.GRAY + "/sneakswitch <reload>");
        return true;
    }

    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        final Player player = event.getPlayer();

        if (player.isSneaking() || !player.hasPermission("sneakswitch.toggle") || noToggleList.contains(player.getUniqueId())) {
            return;
        }

        if (playerSneakMap.containsKey(player.getUniqueId())) {
            if (player.getGameMode() != GameMode.CREATIVE) {
                player.setGameMode(GameMode.CREATIVE);
            } else {
                player.setGameMode(playerSneakMap.get(player.getUniqueId()));
            }

            playerSneakMap.remove(player.getUniqueId());
        } else {
            playerSneakMap.put(player.getUniqueId(), player.getGameMode() == GameMode.CREATIVE ? defaultGameMode :
                    player.getGameMode());

            new BukkitRunnable() {

                @Override
                public void run() {
                    playerSneakMap.remove(player.getUniqueId());
                }

            }.runTaskLater(this, time);
        }
    }

    private void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(prefix + " " + message);
    }
}
