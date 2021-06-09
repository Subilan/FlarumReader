package org.sotap.FlarumReader.Utils;

import java.io.File;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.json.JSONObject;
import org.sotap.FlarumReader.Main;

public final class Files {
    public static String cwd;
    public static FileConfiguration config;

    public static void init(Main plugin) {
        cwd = plugin.getDataFolder().getPath();
        config = plugin.getConfig();
        Files.load(".", "maps.yml");
        Files.load(".", "tags.yml");
    }

    public static String getUsernameById(String id) {
        var maps = getMaps();
        return maps.getString("userMap." + id + ".username");
    }

    public static String getIdByUsername(String username) {
        var maps = getMaps();
        return maps.getString("userMap." + username + ".id");
    }

    public static void writeUserMap(HttpResponse res) {
        var maps = getMaps();
        var r = Requests.toJSON(res.getEntity());
        var data = r.getJSONArray("data");
        JSONObject current;
        for (int i = 0; i < data.length(); i++) {
            current = data.getJSONObject(i);
            maps.set("userMap." + current.getString("id") + ".username", current.getJSONObject("attributes").getString("username"));
            maps.set("userMap." + current.getJSONObject("attributes").getString("username") + ".id", current.getString("id"));
        }
        save(maps, "./maps.yml");
    }

    public static void updateUserMap() {
        var req = new Requests();
        req.getUserMap();
    }

    public static FileConfiguration getMaps() {
        return Files.load(".", "maps.yml");
    }

    public static FileConfiguration getLogins() {
        return Files.load(".", "logins.yml");
    }

    public static File getFile(File folder, String name) throws IOException {
        File file = new File(folder, name);
        if (!folder.exists()) {
            var state = folder.mkdir();
            if (!state) {
                throw new IOException("cannot create file directory automatically");
            }
        }
        if (!file.exists()) {
            try {
                var state = file.createNewFile();
                if (!state) {
                    throw new IOException("cannot create file automatically.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    public static FileConfiguration load(String path, String name) {
        try {
            return YamlConfiguration.loadConfiguration(getFile(
                    new File(path.replace(path.length() == 1 ? "." : "./", path.length() == 1 ? cwd : cwd + "/")),
                    name));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean isEmptyConfiguration(ConfigurationSection config) {
        if (config == null)
            return true;
        return config.getKeys(false).size() == 0;
    }

    public static void save(FileConfiguration data, String targetFile) {
        try {
            data.save(targetFile.replace("./", cwd + "/"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveLogins(FileConfiguration data) {
        save(data, "./logins.yml");
    }

    public static FileConfiguration getTags() {
        return Files.load(".", "tags.yml");
    }

    public static void updateTags() {
        var req = new Requests();
        req.getTags();
    }

    public static void writeTags(HttpResponse re) {
        FileConfiguration tags = getTags();
        tags.set("tags", null);
        save(tags, "./tags.yml");
        var r = Requests.toJSON(re.getEntity());
        var data = r.getJSONArray("included");
        JSONObject current;
        for (int i = 0; i < data.length(); i++) {
            current = data.getJSONObject(i);
            if (!current.getString("type").equals("tags")) continue;
            tags.set("tags." + current.getJSONObject("attributes").getString("name"), current.getString("id"));
        }
        save(tags, "./tags.yml");
    }

    public static String getTagIdByName(String name) {
        var tags = getTags();
        return tags.getString("tags." + name);
    }
}
