package jp.matrixflow.nickname;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * /nick: sets the name shown in chat, in join and leave messages and in the player list. The name
 * above a player's head stays the account name. Other plugins see the nickname as the player's
 * display name.
 */
public final class NicknamePlugin extends JavaPlugin implements Listener, TabExecutor {

    private static final String OFF = "off";

    private NicknameStore store;
    private NameRules rules;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        rules = new NameRules(getConfig().getInt("max-length", 16), getConfig().getStringList("reserved"));
        store = new NicknameStore(new File(getDataFolder(), "nicknames.yml"));
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("nick").setExecutor(this);
        getCommand("nick").setTabCompleter(this);
        Bukkit.getOnlinePlayers().forEach(this::apply);
        getLogger().info("Loaded " + store.players().size() + " nickname(s).");
    }

    @Override
    public void onDisable() {
        Bukkit.getOnlinePlayers().forEach(p -> show(p, null));
    }

    // Early, so join messages from other plugins and the greeting by the AI staff see the nickname.
    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        if (apply(p) && event.joinMessage() != null) {
            event.joinMessage(Component.translatable("multiplayer.player.joined", NamedTextColor.YELLOW,
                    p.displayName()));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        if (store.get(p.getUniqueId()).isPresent() && event.quitMessage() != null) {
            event.quitMessage(Component.translatable("multiplayer.player.left", NamedTextColor.YELLOW,
                    p.displayName()));
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player p) {
                Optional<String> nick = store.get(p.getUniqueId());
                sender.sendMessage(nick.map(n -> info("いまの名前は「" + n + "」です。/nick off で元に戻せます。"))
                        .orElseGet(() -> info("名前は変えていません。/nick <名前> で変えられます。")));
                return true;
            }
            return false;
        }
        if (args.length == 1) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("コンソールからは /nick <プレイヤー> <名前|off> を使ってください。");
                return true;
            }
            change(sender, p, args[0]);
            return true;
        }
        if (args.length == 2) {
            if (!sender.hasPermission("nickname.others")) {
                sender.sendMessage(error("ほかの人の名前は変えられません。"));
                return true;
            }
            OfflinePlayer target = findAccount(args[0]);
            if (target == null) {
                sender.sendMessage(error("「" + args[0] + "」というプレイヤーは見つかりません。アカウント名で指定してください。"));
                return true;
            }
            change(sender, target, args[1]);
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.add(OFF);
            if (sender.hasPermission("nickname.others")) {
                Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
            }
        } else if (args.length == 2 && sender.hasPermission("nickname.others")) {
            options.add(OFF);
        }
        String typed = args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(typed)).toList();
    }

    private void change(CommandSender sender, OfflinePlayer target, String requested) {
        UUID id = target.getUniqueId();
        String account = target.getName() != null ? target.getName() : id.toString();
        NameRules.Verdict v = requested.equalsIgnoreCase(OFF)
                ? NameRules.Verdict.reset()
                : rules.check(requested, account, takenBy(id));
        if (v.refused()) {
            sender.sendMessage(error(v.error()));
            return;
        }
        try {
            store.set(id, v.nickname());
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "Could not save nicknames", e);
            sender.sendMessage(error("名前を保存できませんでした。サーバーの管理者に知らせてください。"));
            return;
        }
        Player online = target.getPlayer();
        if (online != null) {
            apply(online);
        }
        String who = sender instanceof Player p && p.getUniqueId().equals(id) ? "" : account + " の";
        sender.sendMessage(v.nickname() == null
                ? info(who + "名前を元に戻しました。")
                : info(who + "名前を「" + v.nickname() + "」にしました。"));
        getLogger().info(account + " nickname: " + (v.nickname() == null ? "(none)" : v.nickname())
                + " (by " + sender.getName() + ")");
    }

    /** Account names of everyone the server knows, and other players' nicknames. */
    private List<String> takenBy(UUID player) {
        List<String> taken = new ArrayList<>(store.othersThan(player));
        for (OfflinePlayer o : Bukkit.getOfflinePlayers()) {
            if (!o.getUniqueId().equals(player) && o.getName() != null) {
                taken.add(o.getName());
            }
        }
        for (Player o : Bukkit.getOnlinePlayers()) {
            if (!o.getUniqueId().equals(player)) {
                taken.add(o.getName());
            }
        }
        return taken;
    }

    private static OfflinePlayer findAccount(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (NameRules.key(p.getName()).equals(NameRules.key(name))) {
                return p;
            }
        }
        for (OfflinePlayer o : Bukkit.getOfflinePlayers()) {
            if (o.getName() != null && NameRules.key(o.getName()).equals(NameRules.key(name))) {
                return o;
            }
        }
        return null;
    }

    /** Shows the stored nickname, if any. Returns true when the player has one. */
    private boolean apply(Player p) {
        Optional<String> nick = store.get(p.getUniqueId());
        show(p, nick.orElse(null));
        return nick.isPresent();
    }

    private static void show(Player p, String nick) {
        Component name = nick == null ? null : Component.text(nick);
        p.displayName(name);
        p.playerListName(name);
    }

    private static Component info(String text) {
        return Component.text(text, NamedTextColor.GRAY);
    }

    private static Component error(String text) {
        return Component.text(text, NamedTextColor.RED);
    }
}
