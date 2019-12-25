package github.phateio.rangedchat;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
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

import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

public class RangedChat extends JavaPlugin {
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

    public static ArrayList<String> EnabledPlayerNameList = new ArrayList();

    private static FileConfiguration config;

    @Override
    public void onEnable() {
        initConfig();
        loadConfig();

        getServer().getPluginManager().registerEvents(new Listener() {

            @EventHandler
            public void playerChat(AsyncPlayerChatEvent event) {
                if (event.isCancelled()) {
                    return;
                }

                Player player = event.getPlayer();
                String playerName = player.getName();
                World playerWorld = player.getWorld();
                String rawMessage = event.getMessage();
                String message = FORMAT.replace("%player%", playerName).replace("%message%", rawMessage);

                if (!EnabledPlayerNameList.contains(playerName)) {
                    return;
                }

                event.setCancelled(true);
                Bukkit.getServer().getConsoleSender().sendMessage(PREFIX + message);

                Location playerLocation = player.getLocation();

                Set<Player> recipients = event.getRecipients().stream().filter(pl -> {
                    return pl.getWorld() == playerWorld && pl.getLocation().distance(playerLocation) <= DISTANCE;
                }).collect(Collectors.toSet());

                TextComponent hoverMessage;

                if (recipients.size() == 1) {
                    hoverMessage = new TextComponent(PREFIX_COLOR2 + PREFIX + message);
                    String detailPlayersMessage = String.join(", ", recipients.stream().map(pl -> pl.getName()).collect(Collectors.toSet()));
                    String finalString = NOONE_SEEN_MESSAGE;
                    hoverMessage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(finalString).create()));
                } else if (recipients.size() < 10) {
                    hoverMessage = new TextComponent(PREFIX_COLOR1 + PREFIX + message);
                    String detailPlayersMessage = String.join(", ", recipients.stream().map(pl -> pl.getName()).collect(Collectors.toSet()));
                    String finalString = HOVER_SEEN_MESSAGE.replace("%players%", detailPlayersMessage);
                    hoverMessage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(finalString).create()));
                } else {
                    hoverMessage = new TextComponent(PREFIX_COLOR1 + PREFIX + message);
                    Set<Player> firstNineRecipients = recipients.stream().limit(9).collect(Collectors.toSet());
                    int otherCount = recipients.size() - firstNineRecipients.size();
                    String detailPlayersMessage = String.join(", ", firstNineRecipients.stream().map(pl -> pl.getName()).collect(Collectors.toSet()));
                    String finalString = EXTRA_SEEN_MESSAGE.replace("%players%", detailPlayersMessage).replace("%otherCount%", Integer.toString(otherCount));
                    hoverMessage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(finalString).create()));
                }

                for (Player pl : recipients) {
                    pl.spigot().sendMessage(hoverMessage);
                }
            }
        }, this);
    }

    @Override
    public void onDisable() {
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("rc") && sender instanceof Player) {
            Player player = (Player) sender;
            String playerName = player.getName();

            if (EnabledPlayerNameList.contains(playerName)) {
                EnabledPlayerNameList.remove(playerName);
                player.sendMessage(PREFIX + LEAVE_MESSAGE);
            } else {
                EnabledPlayerNameList.add(playerName);
                player.sendMessage(PREFIX + ENTER_MESSAGE);
            }
            return true;
        }
        return false;
    }

    private void initConfig() {
        config = getConfig();
        config.addDefault("DISTANCE", DISTANCE);
        config.addDefault("PREFIX", PREFIX);
        config.addDefault("FORMAT", FORMAT);
        config.options().copyDefaults(true);
        saveConfig();
    }

    private void loadConfig() {
        DISTANCE = config.getInt("DISTANCE");
        PREFIX = config.getString("PREFIX");
        FORMAT = config.getString("FORMAT");
    }
}
