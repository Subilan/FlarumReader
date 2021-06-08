package org.sotap.FlarumReader.Commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public final class Tab implements TabCompleter {
    private static final String[] BASE = { "list", "view", "reply", "login", "logout", "reload", "download" };

    public Tab() {
    }

    public List<String> getAvailableCommands(Player p) {
        List<String> available = new ArrayList<>();
        for (String cmd : BASE) {
            if (p.hasPermission("flarumreader." + cmd)) {
                available.add(cmd);
            }
        }
        return available;
    }

    public List<String> getResult(String arg, List<String> commands) {
        List<String> result = new ArrayList<>();
        StringUtil.copyPartialMatches(arg, commands, result);
        Collections.sort(result);
        return result;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("flarumreader")) {
            if (args.length == 1) {
                result = getResult(args[0], getAvailableCommands((Player) sender));
            } else {
                result = null;
            }
        }
        return result;
    }
}
