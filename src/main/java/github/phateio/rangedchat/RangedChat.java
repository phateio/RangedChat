package github.phateio.rangedchat;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RangedChat extends JavaPlugin implements Listener {
    public static int DISTANCE = 256;
    public static String PREFIX = "[RC] ";
    public static ChatColor PREFIX_COLOR1 = ChatColor.LIGHT_PURPLE;
    public static ChatColor PREFIX_COLOR2 = ChatColor.DARK_RED;
    public static String FORMAT = ChatColor.RESET + "<%player%> %message%";
    public static String ENTER_MESSAGE = ChatColor.LIGHT_PURPLE + "You entered ranged chat mode";
    public static String LEAVE_MESSAGE = ChatColor.AQUA + "You leaved ranged chat mode";
    public static String HOVER_SEEN_MESSAGE = ChatColor.DARK_PURPLE + "Seen by %players%";
    public static String EXTRA_SEEN_MESSAGE = ChatColor.DARK_PURPLE + "Seen by %players% and other %otherCount% players";
    public static String NOONE_SEEN_MESSAGE = ChatColor.DARK_PURPLE + "No one seen your message";

    public static Set<UUID> EnabledPlayerUUID = new HashSet<>();

    @Override
    public void onEnable() {
        initConfig();
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(ignoreCancelled = true)
    public void playerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!EnabledPlayerUUID.contains(player.getUniqueId())) return;
        event.setCancelled(true);

        final String playerName = player.getName();
        final World playerWorld = player.getWorld();
        final String rawMessage = event.getMessage();
        final String message = FORMAT.replace("%player%", playerName).replace("%message%", rawMessage);

        getServer().getConsoleSender().sendMessage(PREFIX + message);

        Location playerLocation = player.getLocation();

        Stream<Player> recipients = event.getRecipients().stream()
                .filter(pl -> pl.getWorld() == playerWorld && pl.getLocation().distance(playerLocation) <= DISTANCE);

        TextComponent hoverMessage;

        if (recipients.count() == 1) {
            hoverMessage = new TextComponent(PREFIX_COLOR2 + PREFIX + message);
            hoverMessage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(NOONE_SEEN_MESSAGE)));
        } else {
            hoverMessage = new TextComponent(PREFIX_COLOR1 + PREFIX + message);

            final String detailPlayersMessage = recipients.limit(9).map(Player::getName).collect(Collectors.joining(", "));

            long overLimit = recipients.count() - 9; // TODO config limit

            final String hoverText = overLimit > 0
                    ? EXTRA_SEEN_MESSAGE.replace("%players%", detailPlayersMessage).replace("%otherCount%", overLimit + "")
                    : HOVER_SEEN_MESSAGE.replace("%players%", detailPlayersMessage);

            hoverMessage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(hoverText)));
        }

        recipients.forEach(p -> p.spigot().sendMessage(hoverMessage));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("rc") || !(sender instanceof Player)) return false;

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        if (EnabledPlayerUUID.contains(uuid)) {
            EnabledPlayerUUID.remove(uuid);
            player.sendMessage(PREFIX + LEAVE_MESSAGE);
        } else {
            EnabledPlayerUUID.add(uuid);
            player.sendMessage(PREFIX + ENTER_MESSAGE);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }

    private void initConfig() {
        FileConfiguration config = getConfig();
        config.addDefault("DISTANCE", DISTANCE);
        config.addDefault("PREFIX", PREFIX);
        config.addDefault("FORMAT", FORMAT);
        config.options().copyDefaults(true);
        saveConfig();
    }

    private void loadConfig() {
        FileConfiguration config = getConfig();
        DISTANCE = config.getInt("DISTANCE");
        PREFIX = config.getString("PREFIX");
        FORMAT = config.getString("FORMAT");
    }
}
