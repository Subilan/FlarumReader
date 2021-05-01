package org.sotap.FlarumReader.Utils;

import org.apache.http.client.CookieStore;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

public final class Requests {
    private JSONObject empty;
    public CookieStore cs;

    public Requests() {
        this.empty = new JSONObject();
        this.cs = new BasicCookieStore();
    }

    private CloseableHttpClient get() {
        return HttpClients.custom().setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build()).setDefaultCookieStore(this.cs).build();
    }

    public JSONObject login(String name, String password) {
        CloseableHttpClient client = this.get();
        JSONObject data = new JSONObject();
        data.put("identification", name);
        data.put("password", password);
        data.put("lifetime", 604800);
        try {
            HttpPost post = new HttpPost("https://g.sotap.org/api/token");
            StringEntity params = new StringEntity(data.toString());
            post.setHeader("content-type", "application/json");
            post.setEntity(params);
            CloseableHttpResponse r = client.execute(post);
            if (r != null) {
                JSONObject result = new JSONObject(EntityUtils.toString(r.getEntity()));
                client.close();
                return result;
            } else {
                return empty;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return empty;
        }
    }
}
