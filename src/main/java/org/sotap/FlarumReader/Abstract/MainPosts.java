package org.sotap.FlarumReader.Abstract;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public final class MainPosts {
    public JSONObject mainPage;
    public String token;

    public MainPosts(String token, JSONObject mainPage) {
        this.mainPage = mainPage;
        this.token = token;
    }

    public List<MainPost> getAll() {
        List<MainPost> mpl = new ArrayList<>();
        JSONArray mainPageItems = this.mainPage.getJSONArray("data");
        for (int i = 0; i < mainPageItems.length(); i++) {
            mpl.add(new MainPost(this.token, mainPageItems.getJSONObject(i)));
        }
        return mpl;
    }

    public MainPost get(Integer index) {
        return new MainPost(this.token, this.mainPage.getJSONArray("data").getJSONObject(index));
    }
}
