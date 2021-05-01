package org.sotap.FlarumReader.Commands;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.json.JSONObject;
import org.sotap.FlarumReader.Main;
import org.sotap.FlarumReader.Utils.Files;
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
            String senderName = sender.getName();

            switch (args[0]) {
                case "login":
                case "l": {
                    if (args.length != 3) {
                        LogUtil.failed("用户名或密码不能为空。", sender);
                        return true;
                    }
                    LogUtil.info("登录中...", sender);
                    JSONObject r = req.login(args[1], args[2]);
                    if (r.isEmpty()) {
                        LogUtil.failed("指令执行时出现问题。", sender);
                    } else {
                        String token = r.getString("token");
                        String id = Integer.toString(r.getInt("userId"));
                        FileConfiguration fc = Files.getLogins();
                        ConfigurationSection cs = fc.createSection(senderName);
                        Date d = new Date();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                        cs.set("token", token);
                        cs.set("id", id);
                        cs.set("username", args[1]);
                        cs.set("login-time", d.getTime());
                        cs.set("exp-time", d.getTime() + 604800000);
                        fc.set(senderName, cs);
                        Files.saveLogins(fc);
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(d);
                        cal.add(Calendar.DATE, 7);
                        LogUtil.success("成功登录账号 &b" + args[1] + "&r，凭证有效期至 &e" + sdf.format(cal.getTime()), sender);
                    }
                }
                break;

                case "logout":
                case "t": {
                    FileConfiguration l = Files.getLogins();
                    l.set(senderName, null);
                    Files.saveLogins(l);
                    LogUtil.success("成功退出。", sender);
                }
            }
        }
        return true;
    }
}
