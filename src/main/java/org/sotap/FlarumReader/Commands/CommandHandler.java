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
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;
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

	private boolean createDiscussion(String title, String content, List<String> tags) {
		for (var tag : tags) {
			if (Files.getTagIdByName(tag) == null) {
				LogUtil.failed("包含一个或多个无效标签。", sender);
				return true;
			}
		}
		req.createDiscussion(l.getToken(), title, content, tags, new FutureCallback<HttpResponse>() {
			public void completed(final HttpResponse re) {
				var r = Requests.toJSON(re.getEntity());
				if (!r.has("data")) {
					var reason = r.getJSONArray("errors").getJSONObject(0).getString("code");
					LogUtil.failed("发送失败。原因： &c" + reason, sender);
					System.out.println("fr-debug: " + r.toString());
				} else {
					var id = r.getJSONObject("data").getString("id");
					LogUtil.success("发送成功。主题地址： &e&nhttps://g.sotap.org/d/" + id, sender);
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

	private boolean replyDiscussion(String id, String content) {
		req.createReply(l.getToken(), id, content, new FutureCallback<HttpResponse>() {
			public void completed(final HttpResponse re) {
				var r = Requests.toJSON(re.getEntity());
				if (!r.has("data")) {
					var reason = r.getJSONArray("errors").getJSONObject(0).getString("code");
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

	private boolean showDiscussion(String id, boolean... save_) {
		var save = save_.length > 0 ? save_[0] : false;
		if (!(sender instanceof Player)) {
			LogUtil.failed("操作必须由玩家进行。", sender);
			return true;
		}
		req.getDiscussion(id, new FutureCallback<HttpResponse>() {
			public void completed(final HttpResponse re) {
				var r = Requests.toJSON(re.getEntity());
				try {
					var disc = new Discussion(r);
					if (save) {
						((Player) sender).getInventory().addItem(disc.getBook());
						LogUtil.success("下载成功，请查看你的背包物品。", sender);
					} else {
						Bukkit.getScheduler().runTask(plugin,
								() -> BookUtil.openPlayer((Player) sender, disc.getBook()));
					}
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
				var r = Requests.toJSON(re.getEntity());
				var mps = new MainPosts(l.getToken(), r);
				var all = mps.getAll();
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

	public void showTags() {
		var tags = Files.getTags().getConfigurationSection("tags").getKeys(false);
		if (tags.size() == 0) {
			LogUtil.info("暂无任何可用标签。", sender);
			return;
		}
		LogUtil.info("&a当前可用标签共 &e" + tags.size() + "&a 个", sender);
		LogUtil.info("&e包括： " + String.join("， ", tags), sender);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("flarumreader")) {
			if (args.length == 0) {
				LogUtil.failed("请提供至少一个参数。", sender);
				return true;
			}
			req = new Requests();
			senderName = sender.getName();
			this.sender = sender;
			l = new Login(senderName);

			if (!args[0].equals("login") && !args[0].equals("reload")) {
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
							var r = Requests.toJSON(re.getEntity());
							if (r.isEmpty()) {
								LogUtil.failed("指令执行时出现问题。", sender);
							} else {
								if (!r.has("token")) {
									LogUtil.failed("用户名或密码错误。", sender);
									return;
								}
								var d = new Date();
								var sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
								if (Files.getLogins().getLong(senderName + ".exp-time") > d.getTime()) {
									LogUtil.failed("你已经登录了。", sender);
									return;
								}
								var token = r.getString("token");
								var id = Integer.toString(r.getInt("userId"));
								var fc = Files.getLogins();
								var cs = fc.createSection(senderName);
								cs.set("token", token);
								cs.set("id", id);
								cs.set("username", args[1]);
								cs.set("login-time", d.getTime());
								cs.set("exp-time", d.getTime() + 604800000);
								fc.set(senderName, cs);
								Files.saveLogins(fc);
								var cal = Calendar.getInstance();
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
					var l = Files.getLogins();
					l.set(senderName, null);
					Files.saveLogins(l);
					LogUtil.success("成功退出。", sender);
					break;
				}

				case "list":
				case "l": {
					if (args.length > 1 && !isInteger(args[1])) {
						LogUtil.failed("页面序号只能是正整数。", sender);
						return true;
					}
					int page = (args.length > 1) ? Integer.parseInt(args[1]) : 1;
					if (page <= 0) {
						LogUtil.failed("页面序号不能小于 1。", sender);
						return true;
					}
					LogUtil.info("加载中...", sender);
					return showList(page);
				}

				case "post":
				case "p": {
					if (args.length < 3) {
						LogUtil.failed("必须指定标题和至少一个标签。", sender);
						return true;
					}
					if (!(sender instanceof Player)) {
						LogUtil.failed("操作必须由玩家进行。", sender);
						return true;
					}
					Player p = (Player) sender;
					var book = p.getInventory().getItemInMainHand();
					String content;
					if (book.getType() == Material.WRITTEN_BOOK || book.getType() == Material.WRITABLE_BOOK) {
						BookMeta meta = (BookMeta) book.getItemMeta();
						var contents = meta.getPages();
						content = String.join("", contents);
					} else {
						LogUtil.failed("必须手持一本已（未）署名的书。", sender);
						return true;
					}
					var title = args[1];
					var tags = Arrays.asList(args).subList(2, args.length);
					LogUtil.info("发送中...", sender);
					return createDiscussion(title, content, tags);
				}

				case "download":
				case "d": {
					if (args.length < 2) {
						LogUtil.failed("必须指定一个序号或帖子 ID。", sender);
						return true;
					}
					var id = getExactId(args[1]);
					if (id.length() == 0) {
						LogUtil.failed("无效参数序号或 ID。", sender);
						return true;
					}
					return showDiscussion(id, true);
				}

				case "view":
				case "v": {
					if (args.length < 2) {
						LogUtil.failed("必须指定一个序号或帖子 ID。", sender);
						return true;
					}
					var id = getExactId(args[1]);
					if (id.length() == 0) {
						LogUtil.failed("无效参数序号或 ID。", sender);
						return true;
					}
					return showDiscussion(id);
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
					var content = "";
					if (args[2].equals("book")) {
						if (!(sender instanceof Player)) {
							LogUtil.failed("操作必须由玩家进行。", sender);
							return true;
						}
						Player p = (Player) sender;
						var book = p.getInventory().getItemInMainHand();
						if (book.getType() == Material.WRITTEN_BOOK || book.getType() == Material.WRITABLE_BOOK) {
							BookMeta meta = (BookMeta) book.getItemMeta();
							var contents = meta.getPages();
							content = String.join("", contents);
						} else {
							LogUtil.failed("手持物品须为已（未）署名的书。", sender);
							return true;
						}
					} else {
						var contentList = Arrays.asList(args).subList(2, args.length);
						for (var t : contentList) {
							content += " ";
							content += t;
						}
					}
					if (content.trim().isEmpty()) {
						LogUtil.failed("内容不能为空。", sender);
						return true;
					}
					var id = getExactId(args[1]);
					if (id.length() == 0) {
						LogUtil.failed("无效参数序号或 ID。", sender);
						return true;
					}
					return replyDiscussion(id, content);
				}

				case "reload": {
					Files.config = Files.load(".", "config.yml");
					Files.updateUserMap();
					Files.updateTags();
					LogUtil.success("已重载配置文件和数据表。", sender);
					break;
				}

				case "help": {
					LogUtil.log("&e点击下方网址打开 Wiki 页面。", sender);
					LogUtil.log("&a&nhttps://wiki.sotap.org/#/plugins/flarum-reader", sender);
					break;
				}

				case "tags": {
					showTags();
					break;
				}

				default: {
					LogUtil.failed("无效参数。", sender);
				}
			}
		}
		return true;
	}

	public boolean isInteger(String intlike) {
		try {
			Integer.parseInt(intlike);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	public String getExactId(String argument) {
		if (argument.startsWith("#")) {
			var idlike = argument.substring(1);
			if (!isInteger(idlike)) {
				return "";
			}
			return idlike;
		} else {
			if (!isInteger(argument)) {
				return "";
			}
			var idlike = currentLists.getString(senderName + "." + argument);
			if (idlike == null) {
				return "";
			}
			return idlike;
		}
	}
}