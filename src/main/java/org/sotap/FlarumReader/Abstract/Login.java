package org.sotap.FlarumReader.Abstract;

import java.util.Date;

import org.bukkit.configuration.ConfigurationSection;
import org.sotap.FlarumReader.Utils.Files;

public final class Login {
    public ConfigurationSection loginData;
    public String name;

    public Login(String name) {
        this.name = name;
        this.loginData = Files.getLogins().getConfigurationSection(name);
    }

    public boolean exists() {
        return this.loginData != null;
    }

    public boolean isExpired() {
        var d = new Date();
        if (this.exists()) {
            return d.getTime() > this.loginData.getLong("exp-time");
        }
        // not good
        return true;
    }

    public boolean valid() {
        if (this.exists()) {
            if (!this.isExpired()) {
                return true;
            }
        }
        return false;
    }

    public String getToken() {
        return this.loginData.getString("token");
    }
}
