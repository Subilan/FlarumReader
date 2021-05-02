package org.sotap.FlarumReader.Commands;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.concurrent.FutureCallback;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.json.JSONObject;
import org.sotap.FlarumReader.Main;
import org.sotap.FlarumReader.Abstract.Login;
import org.sotap.FlarumReader.Abstract.MainPost;
import org.sotap.FlarumReader.Abstract.MainPosts;
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

            if (args.length == 0) {
                Login l = new Login(senderName);
                if (!l.valid()) {
                    LogUtil.failed("你需要登录才能进行此操作。", sender);
                    return true;
                }
                req.getMainPage(new FutureCallback<HttpResponse>() {
                    public void completed(final HttpResponse re) {
                        JSONObject r = Requests.toJSON(re.getEntity());
                        MainPosts mps = new MainPosts(l.getToken(), r);
                        List<MainPost> all = mps.getAll();
                        MainPost current;
                        for (int i = 0; i < all.size(); i++) {
                            current = all.get(i);
                            LogUtil.log("[&e" + (i + 1) + "&f] &a" + current.title + "&f &8-&r &oby &7&o"
                                    + Files.getUsernameById(current.authorId), sender);
                        }
                    }

                    public void failed(final Exception e) {
                        e.printStackTrace();
                        LogUtil.failed("指令执行时出现问题。", sender);
                    }

                    public void cancelled() {
                        LogUtil.warn("任务被中断。", sender);
                    }
                });
                return true;
            }

            switch (args[0]) {
                case "login":
                case "l": {
                    if (args.length != 3) {
                        LogUtil.failed("用户名或密码不能为空。", sender);
                        return true;
                    }
                    LogUtil.info("登录中...", sender);
                    req.login(args[1], args[2], new FutureCallback<HttpResponse>() {
                        public void completed(final HttpResponse re) {
                            JSONObject r = Requests.toJSON(re.getEntity());
                            if (r.isEmpty()) {
                                LogUtil.failed("指令执行时出现问题。", sender);
                            } else {
                                if (!r.has("token")) {
                                    LogUtil.failed("用户名或密码错误。", sender);
                                    return;
                                }
                                Date d = new Date();
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                                if (Files.getLogins().getLong(senderName + ".exp-time") > d.getTime()) {
                                    LogUtil.failed("你已经登录了。", sender);
                                    return;
                                }
                                String token = r.getString("token");
                                String id = Integer.toString(r.getInt("userId"));
                                FileConfiguration fc = Files.getLogins();
                                ConfigurationSection cs = fc.createSection(senderName);
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
                                LogUtil.success("成功登录账号 &b" + args[1] + "&r，凭证有效期至 &e" + sdf.format(cal.getTime()),
                                        sender);
                            }
                        }

                        public void failed(final Exception e) {
                            e.printStackTrace();
                            LogUtil.failed("指令执行时出现问题。", sender);
                        }

                        public void cancelled() {
                            LogUtil.warn("任务被中断。", sender);
                        }
                    });
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
