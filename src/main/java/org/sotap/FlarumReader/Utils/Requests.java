package org.sotap.FlarumReader.Utils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

public final class Requests {
    public static JSONObject empty = new JSONObject();
    public CookieStore cs;

    public Requests() {
        this.cs = new BasicCookieStore();
    }

    private CloseableHttpAsyncClient get() {
        return HttpAsyncClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
                .setDefaultCookieStore(this.cs).build();
    }

    public void login(String name, String password, FutureCallback<HttpResponse> callback) {
        CloseableHttpAsyncClient client = get();
        client.start();
        JSONObject data = new JSONObject();
        data.put("identification", name);
        data.put("password", password);
        data.put("lifetime", 604800);
        try {
            HttpPost post = new HttpPost("https://g.sotap.org/api/token");
            StringEntity params = new StringEntity(data.toString());
            post.setHeader("content-type", "application/json");
            post.setEntity(params);
            client.execute(post, callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getMainPage(FutureCallback<HttpResponse> callback) {
        CloseableHttpAsyncClient client = get();
        client.start();
        try {
            HttpGet get = new HttpGet("https://g.sotap.org/api/discussions?page[limit]=10");
            client.execute(get, callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static JSONObject toJSON(HttpEntity ent) {
        try {
            return new JSONObject(EntityUtils.toString(ent));
        } catch (Exception e) {
            return empty;
        }
    }

    public void getUser(String token, String identifier, FutureCallback<HttpResponse> callback) {
        CloseableHttpAsyncClient client = get();
        client.start();
        try {
            HttpGet get = new HttpGet("https://g.sotap.org/api/users/" + identifier);
            get.addHeader("Authorization", "Token " + token);
            client.execute(get, callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getUserMap() {
        this.login("Subilan", "subilan1999", new FutureCallback<HttpResponse>(){
            public void completed(final HttpResponse re) {
                String token = toJSON(re.getEntity()).getString("token");
                CloseableHttpAsyncClient client = get();
                client.start();
                Integer offset = 0;
                Integer limit = 50;
                while (offset < 500) {
                    HttpGet get = new HttpGet("https://g.sotap.org/api/users?page[limit]=" + limit + "&page[offset]=" + offset);
                    get.addHeader("Authorization", "Token " + token);
                    client.execute(get, new FutureCallback<HttpResponse>(){
                        public void completed(final HttpResponse re) {
                            try {
                                Files.writeMaps(re);
                            } catch (Exception e) {
                                return;
                            }
                        }
    
                        public void failed(final Exception e) {
                            e.printStackTrace();
                        }
    
                        public void cancelled() {
                            return;
                        }
                    });
                    offset += 50;
                }
            }

            public void failed(final Exception e) {
                e.printStackTrace();
            }

            public void cancelled() {
                return;
            }
        });
    }
}
