package org.sotap.FlarumReader.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.json.JSONObject;
import org.sotap.FlarumReader.Main;
import org.sotap.FlarumReader.Utils.LogUtil;
import org.sotap.FlarumReader.Utils.Requests;

public final class CommandHandler implements CommandExecutor {
    public final Main plugin;

    public CommandHandler(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("flarumreader")) {
            Requests req = new Requests();

            switch (args[0]) {
                case "login":
                case "l": {
                    if (args.length != 3) {
                        sender.sendMessage(LogUtil.FAILED + "用户名或密码不能为空。");
                        return true;
                    }
                    JSONObject r = req.login(args[1], args[2]);
                    sender.sendMessage(r.toString());
                    if (r.isEmpty()) {
                        sender.sendMessage(LogUtil.FAILED + "指令执行时出现问题。");
                        return true;
                    }
                    sender.sendMessage(r.getString("token"));
                }
            }
        }
        return true;
    }
}
