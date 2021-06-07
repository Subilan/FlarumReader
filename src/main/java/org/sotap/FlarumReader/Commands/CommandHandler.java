package org.sotap.FlarumReader.Commands;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.concurrent.FutureCallback;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.json.JSONObject;
import org.sotap.FlarumReader.Main;
import org.sotap.FlarumReader.Abstract.Discussion;
import org.sotap.FlarumReader.Abstract.Login;
import org.sotap.FlarumReader.Abstract.MainPost;
import org.sotap.FlarumReader.Abstract.MainPosts;
import org.sotap.FlarumReader.Utils.Files;
import org.sotap.FlarumReader.Utils.LogUtil;
import org.sotap.FlarumReader.Utils.Requests;

import xyz.upperlevel.spigot.book.BookUtil;

public final class CommandHandler implements CommandExecutor {
	public final Main plugin;
	private String senderName;
	private Requests req;
	private CommandSender sender;
	private Login l;
	private MemoryConfiguration currentLists;

	public CommandHandler(Main plugin) {
		this.plugin = plugin;
		this.currentLists = new MemoryConfiguration();
	}

	private boolean replyDiscussion(int index, boolean isId, String content) {
		if (!(sender instanceof Player)) {
			LogUtil.failed("操作必须由玩家进行。", sender);
			return true;
		}
		String currentListId = currentLists.getString(senderName + "." + index);
		if (currentListId == null && !isId) {
			LogUtil.failed("你必须查看某一页面才能使用数字序号。", sender);
			return true;
		}
		String id = isId ? Integer.toString(index) : currentListId;
		req.createReply(l.getToken(), id, content, new FutureCallback<HttpResponse>(){
			public void completed(final HttpResponse re) {
				JSONObject r = Requests.toJSON(re.getEntity());
				if (!r.has("data")) {
					String reason = r.getJSONArray("errors").getJSONObject(0).getString("code");
					LogUtil.failed("发送失败。原因： &c" + reason, sender);
					System.out.println("fr-debug: " + r.toString());
				} else {
					LogUtil.success("发送成功。原帖 &e&nhttps://g.sotap.org/d/" + id, sender);
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

	private boolean showDiscussion(int index, boolean isId) {
		if (!(sender instanceof Player)) {
			LogUtil.failed("操作必须由玩家进行。", sender);
			return true;
		}
		String currentListId = currentLists.getString(senderName + "." + index);
		if (currentListId == null && !isId) {
			LogUtil.failed("你必须查看某一页面才能使用数字序号。", sender);
			return true;
		}
		String id = isId ? Integer.toString(index) : currentListId;
		req.getDiscussion(id, new FutureCallback<HttpResponse>() {
			public void completed(final HttpResponse re) {
				JSONObject r = Requests.toJSON(re.getEntity());
				try {
					Discussion disc = new Discussion(r);
					Bukkit.getScheduler().runTask(plugin, () -> BookUtil.openPlayer((Player) sender, disc.getBook()));
				} catch (Exception e) {
					e.printStackTrace();
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

	private boolean showList(int page) {
		req.getMainPage(page, new FutureCallback<HttpResponse>() {
			public void completed(final HttpResponse re) {
				JSONObject r = Requests.toJSON(re.getEntity());
				MainPosts mps = new MainPosts(l.getToken(), r);
				List<MainPost> all = mps.getAll();
				MainPost current;
				if (all.size() > 0)
					LogUtil.log("&e===== &a&l第 &b" + page + " &a&l页 &r&e=====", sender);
				for (int i = 0; i < all.size(); i++) {
					current = all.get(i);
					currentLists.set(senderName + "." + (i + 1), current.id);
					LogUtil.mainThreadTitle(current.title, Files.getUsernameById(current.authorId), sender,
							"[&e" + (i + 1) + "&r] ");
				}
				if (all.size() > 0)
					LogUtil.log("&e=================", sender);
				if (all.size() < 10) {
					if (all.size() > 0) {
						LogUtil.log("&e已经是最后一页。", sender);
					} else {
						LogUtil.failed("没有更多内容。", sender);
					}
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

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("flarumreader")) {
			req = new Requests();
			senderName = sender.getName();
			this.sender = sender;
			l = new Login(senderName);

			if (!args[0].equals("login")) {
				if (!l.valid()) {
					LogUtil.failed("你需要登录才能进行此操作。", sender);
					return true;
				}
			}

			if (args.length == 0) {
				return showList(1);
			}

			switch (args[0]) {
				case "login": {
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

				case "logout": {
					FileConfiguration l = Files.getLogins();
					l.set(senderName, null);
					Files.saveLogins(l);
					LogUtil.success("成功退出。", sender);
					break;
				}

				case "list":
				case "l": {
					try {
						int page = (args.length > 1) ? Integer.parseInt(args[1]) : 1;
						if (page <= 0) {
							LogUtil.failed("页面序号不能小于 1。", sender);
							return true;
						}
						LogUtil.info("加载中...", sender);
						return showList(page);
					} catch (NumberFormatException e) {
						LogUtil.failed("页面序号只能是正整数。", sender);
					}
					break;
				}

				case "view":
				case "v": {
					if (args.length < 2) {
						LogUtil.failed("必须指定一个序号或帖子 ID。", sender);
						return true;
					}
					if (args[1].startsWith("#")) {
						try {
							int id = Integer.parseInt(args[1].substring(1));
							LogUtil.info("加载中...", sender);
							return showDiscussion(id, true);
						} catch (NumberFormatException e) {
							LogUtil.failed("无效的帖子 ID。", sender);
						}
					} else {
						try {
							int index = Integer.parseInt(args[1]);
							if (!(index >= 1 && index <= 10)) {
								LogUtil.failed("序号必须在 1 到 10 之间（包含端点）。", sender);
								return true;
							}
							LogUtil.info("加载中...", sender);
							return showDiscussion(index, false);
						} catch (NumberFormatException e) {
							LogUtil.failed("无效的序号。", sender);
						}
					}
					break;
				}

				case "reply":
				case "r": {
					if (args.length < 2) {
						LogUtil.failed("必须指定一个序号或帖子 ID。", sender);
						return true;
					}
					if (args.length < 3) {
						LogUtil.failed("必须指定发帖内容。", sender);
						return true;
					}
					String content = "";
					if (args[2].equals("book")) {
						if (!(sender instanceof Player)) {
							LogUtil.failed("操作必须由玩家进行。", sender);
							return true;
						}
						Player p = (Player) sender;
						ItemStack book = p.getInventory().getItemInMainHand();
						if (book.getType() == Material.WRITTEN_BOOK || book.getType() == Material.WRITABLE_BOOK) {
							BookMeta meta = (BookMeta) book.getItemMeta();
							List<String> contents = meta.getPages();
							content = String.join("", contents);
						} else {
							LogUtil.failed("手持物品须为已（未）署名的书。", sender);
							return true;
						}
					} else {
						List<String> contentList = Arrays.asList(args).subList(2, args.length);
						for (String t : contentList) {
							content += " ";
							content += t;
						}
					}
					if (content.trim().isEmpty()) {
						LogUtil.failed("内容不能为空。", sender);
						return true;
					}
					if (args[1].startsWith("#")) {
						try {
							int id = Integer.parseInt(args[1].substring(1));
							LogUtil.info("发送中...", sender);
							return replyDiscussion(id, true, content);
						} catch (NumberFormatException e) {
							LogUtil.failed("无效的帖子 ID。", sender);
						}
					} else {
						try {
							int index = Integer.parseInt(args[1]);
							if (!(index >= 1 && index <= 10)) {
								LogUtil.failed("序号必须在 1 到 10 之间（包含端点）。", sender);
								return true;
							}
							LogUtil.info("发送中...", sender);
							return replyDiscussion(index, false, content);
						} catch (NumberFormatException e) {
							LogUtil.failed("无效的序号。", sender);
						}
					}
				}

				case "reload": {
					Files.config = Files.load(".", "config.yml");
					Files.updateUserMap();
					LogUtil.success("已重载配置文件和用户表。", sender);
					break;
				}

				case "help": {
					LogUtil.log("&e点击下方网址打开 Wiki 页面。", sender);
					LogUtil.log("&a&nhttps://wiki.sotap.org/#/plugins/flarum-reader", sender);
					break;
				}

				default: {
					LogUtil.failed("无效参数。", sender);
				}
			}
		}
		return true;
	}
}
