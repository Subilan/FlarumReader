package org.sotap.FlarumReader.Abstract;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.concurrent.FutureCallback;
import org.json.JSONException;
import org.json.JSONObject;
import org.sotap.FlarumReader.Utils.Calendars;
import org.sotap.FlarumReader.Utils.Requests;

public final class User {
    public JSONObject raw;
    public String name;
    public Date joinTime;
    public String bio;
    public List<String> groups;
    public String id;

    public User(String token, String identifier) {
        Requests req = new Requests();
        req.getUser(token, identifier, new FutureCallback<HttpResponse>() {
            public void completed(final HttpResponse re) {
                var r = Requests.toJSON(re.getEntity());
                User.this.init(r);
            }

            public void failed(final Exception e) {
                e.printStackTrace();
            }

            public void cancelled() {
                return;
            }
        });
    }

    private void init(JSONObject rawJson) {
        this.raw = rawJson;
        if (this.exists()) {
            this.groups = new ArrayList<>();
            var data = this.raw.getJSONObject("data");
            this.name = data.getJSONObject("attributes").getString("username");
            this.joinTime = Calendars.parse(data.getJSONObject("attributes").getString("joinTime"),
                    "yyyy-MM-dd'T'HH:mm:ssXXX");
            try {
                this.bio = data.getJSONObject("attributes").getString("bio");
            } catch (JSONException e) {
                this.bio = null;
            }
            this.id = data.getString("id");
            try {
                var included = this.raw.getJSONArray("included");
                for (int i = 0; i < included.length(); i++) {
                    this.groups.add(included.getJSONObject(i).getJSONObject("attributes").getString("nameSingular"));
                }
            } catch (JSONException e) {
                this.groups = null;
            }
        }
    }

    public boolean exists() {
        try {
            this.raw.getJSONObject("errors");
        } catch (JSONException e) {
            return true;
        }
        return false;
    }
}
