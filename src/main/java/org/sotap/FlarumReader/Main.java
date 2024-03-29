package org.sotap.FlarumReader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Objects;
import org.sotap.FlarumReader.Commands.CommandHandler;
import org.sotap.FlarumReader.Commands.Tab;
import org.sotap.FlarumReader.Utils.Files;
import org.sotap.FlarumReader.Utils.LogUtil;

public final class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        LogUtil.init(this);
        Files.init(this);
        LogUtil.info("初始化用户表中...");
        Files.updateUserMap();
        LogUtil.info("初始化标签中...");
        Files.updateTags();
        Objects.requireNonNull(Bukkit.getPluginCommand("flarumreader")).setExecutor(new CommandHandler(this));
        Objects.requireNonNull(Bukkit.getPluginCommand("flarumreader")).setTabCompleter(new Tab());
        LogUtil.success("插件已&a启用&r。");
    }

    @Override
    public void onDisable() {
        LogUtil.success("插件已&c禁用&r。");
    }
}
