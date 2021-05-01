package org.sotap.FlarumReader.Utils;

import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.sotap.FlarumReader.Main;

public final class LogUtil {
    public final static String SUCCESS = "&r[&a成功&r] ";
    public final static String WARN = "&r[&e警告&r] ";
    public final static String FAILED = "&r[&c失败&r] ";
    public final static String INFO = "&r[&b提示&r] ";
    public static Logger origin;

    public static void init(Main plugin) {
        LogUtil.origin = plugin.getLogger();
    }

    public static String translateColor(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static void success(String message, Player... p) {
        if (p.length > 0) {
            if (p[0] != null) {
                p[0].sendMessage(translateColor(SUCCESS + message));
                return;
            }
        }
        origin.info(translateColor(SUCCESS + message));
    }

    public static void warn(String message, Player... p) {
        if (p.length > 0) {
            if (p[0] != null) {
                p[0].sendMessage(translateColor(WARN + message));
                return;
            }
        }
        origin.info(translateColor(WARN + message));
    }

    public static void failed(String message, Player... p) {
        if (p.length > 0) {
            if (p[0] != null) {
                p[0].sendMessage(translateColor(FAILED + message));
                return;
            }
        }
        origin.info(translateColor(FAILED + message));
    }

    public static void info(String message, Player... p) {
        if (p.length > 0) {
            if (p[0] != null) {
                p[0].sendMessage(translateColor(INFO + message));
                return;
            }
        }
        origin.info(translateColor(INFO + message));
    }
}
