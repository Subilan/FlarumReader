package org.sotap.FlarumReader.Abstract;

import java.util.Date;

import org.json.JSONObject;
import org.sotap.FlarumReader.Utils.Calendars;

public final class MainPost {
    public String id;
    public String title;
    public Date postTime;
    public User author;
    public String authorId;

    public MainPost(String token, JSONObject mpItem) {
        this.id = mpItem.getString("id");
        this.title = mpItem.getJSONObject("attributes").getString("title");
        this.authorId = mpItem.getJSONObject("relationships").getJSONObject("user").getJSONObject("data").getString("id");
        this.postTime = Calendars.parse(mpItem.getJSONObject("attributes").getString("createdAt"), "yyyy-MM-dd'T'HH:mm:ssXXX");
        this.author = new User(token, this.authorId);
    }
}
