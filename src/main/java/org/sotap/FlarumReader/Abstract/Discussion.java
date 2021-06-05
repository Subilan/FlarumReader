package org.sotap.FlarumReader.Abstract;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sotap.FlarumReader.Utils.Calendars;
import org.sotap.FlarumReader.Utils.Files;

public final class Discussion {
    public String id;
    public String title;
    public String author;
    public Date createTime;
    public List<Reply> replyList;

    public Discussion(JSONObject object) {
        JSONObject base = object.getJSONObject("data");
        JSONObject attr = base.getJSONObject("attributes");
        JSONArray included = object.getJSONArray("included");
        replyList = new ArrayList<>();
        this.id = base.getString("id");
        this.title = attr.getString("title");
        this.createTime = Calendars.parse(attr.getString("createdAt"), Calendars.UTC_FORMAT_PATTERN);
        JSONObject current;
        for (int i = 0; i < included.length(); i++) {
            current = included.getJSONObject(i);
            if (current.getString("type").equals("posts")) {
                if (current.getJSONObject("attributes").getString("contentType").equals("comment")) {
                    replyList.add(new Reply(current));
                }
            }
        }
    }

    public void getGUI() {
        // to be continued
    }
}

final class Reply {
    public String content;
    public String author;
    public String id;
    public Date createTime;

    public Reply(JSONObject object) {
        JSONObject attr = object.getJSONObject("attributes");
        JSONObject relations = object.getJSONObject("relationships");
        this.id = object.getString("id");
        this.createTime = Calendars.parse(attr.getString("createdAt"), Calendars.UTC_FORMAT_PATTERN);
        this.content = attr.getString("content");
        this.author = Files.getUsernameById(relations.getJSONObject("user").getJSONObject("data").getString("id"));
    }
}
