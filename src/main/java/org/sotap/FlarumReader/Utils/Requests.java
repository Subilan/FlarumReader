package org.sotap.FlarumReader.Utils;

import java.util.List;

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
import org.json.JSONArray;
import org.json.JSONObject;

public final class Requests {
    public static JSONObject empty = new JSONObject();
    public CookieStore cs;
    public String adminName;
    public String adminPassword;
    public String site;

    public Requests() {
        this.cs = new BasicCookieStore();
        this.adminName = Files.config.getString("admin-name");
        this.adminPassword = Files.config.getString("admin-password");
        this.site = Files.config.getString("site");
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
            HttpPost post = new HttpPost(this.site + "/api/token");
            StringEntity params = new StringEntity(data.toString());
            post.setHeader("content-type", "application/json");
            post.setEntity(params);
            client.execute(post, callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getMainPage(int page, FutureCallback<HttpResponse> callback) {
        CloseableHttpAsyncClient client = get();
        client.start();
        try {
            HttpGet get = new HttpGet(this.site + "/api/discussions?page[limit]=10&page[offset]=" + (page - 1) * 10);
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

    public void createDiscussion(String token, String title, String content, List<String> tags_,
            FutureCallback<HttpResponse> callback) {
        CloseableHttpAsyncClient client = get();
        client.start();
        JSONObject innerData = new JSONObject();
        JSONObject attributes = new JSONObject();
        JSONObject relationships = new JSONObject();
        JSONArray tags = new JSONArray();
        attributes.put("content", content);
        attributes.put("title", title);
        String currentId;
        for (String tag : tags_) {
            currentId = Files.getTagIdByName(tag);
            tags.put(new JSONObject().put("id", currentId).put("type", "tags"));
        }
        relationships.put("tags", new JSONObject().put("data", tags));
        innerData.put("attributes", attributes);
        innerData.put("relationships", relationships);
        innerData.put("type", "discussions");
        JSONObject data = new JSONObject().put("data", innerData);
        try {
            HttpPost post = new HttpPost(this.site + "/api/discussions");
            StringEntity params = new StringEntity(data.toString(), "UTF-8");
            post.addHeader("content-type", "application/json; charset=UTF-8");
            post.setEntity(params);
            post.addHeader("Authorization", "Token " + token);
            client.execute(post, callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createReply(String token, String id, String content, FutureCallback<HttpResponse> callback) {
        CloseableHttpAsyncClient client = get();
        client.start();
        JSONObject innerData = new JSONObject();
        JSONObject attributes = new JSONObject();
        JSONObject relationships = new JSONObject();
        attributes.put("content", content);
        relationships.put("discussion",
                new JSONObject().put("data", new JSONObject().put("type", "discussions").put("id", id)));
        innerData.put("attributes", attributes);
        innerData.put("relationships", relationships);
        innerData.put("type", "posts");
        JSONObject data = new JSONObject().put("data", innerData);
        try {
            HttpPost post = new HttpPost(this.site + "/api/posts");
            StringEntity params = new StringEntity(data.toString(), "UTF-8");
            post.addHeader("content-type", "application/json; charset=UTF-8");
            post.setEntity(params);
            post.addHeader("Authorization", "Token " + token);
            client.execute(post, callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getDiscussion(String id, FutureCallback<HttpResponse> callback) {
        this.login(this.adminName, this.adminPassword, new FutureCallback<HttpResponse>() {
            public void completed(final HttpResponse re) {
                String token = toJSON(re.getEntity()).getString("token");
                CloseableHttpAsyncClient client = get();
                client.start();
                try {
                    HttpGet get = new HttpGet(site + "/api/discussions/" + id);
                    get.addHeader("Authorization", "Token " + token);
                    client.execute(get, callback);
                } catch (Exception e) {
                    e.printStackTrace();
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

    public void getUser(String token, String identifier, FutureCallback<HttpResponse> callback) {
        CloseableHttpAsyncClient client = get();
        client.start();
        try {
            HttpGet get = new HttpGet(this.site + "/api/users/" + identifier);
            get.addHeader("Authorization", "Token " + token);
            client.execute(get, callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getUserMap() {
        this.login(this.adminName, this.adminPassword, new FutureCallback<HttpResponse>() {
            public void completed(final HttpResponse re) {
                String token = toJSON(re.getEntity()).getString("token");
                CloseableHttpAsyncClient client = get();
                client.start();
                Integer offset = 0;
                Integer limit = 50;
                while (offset < Files.config.getInt("max-users")) {
                    HttpGet get = new HttpGet(site + "/api/users?page[limit]=" + limit + "&page[offset]=" + offset);
                    get.addHeader("Authorization", "Token " + token);
                    client.execute(get, new FutureCallback<HttpResponse>() {
                        public void completed(final HttpResponse re) {
                            try {
                                Files.writeUserMap(re);
                            } catch (Exception e) {
                                e.printStackTrace();
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

    public void getTags() {
        this.login(this.adminName, this.adminPassword, new FutureCallback<HttpResponse>() {
            public void completed(final HttpResponse re) {
                String token = toJSON(re.getEntity()).getString("token");
                CloseableHttpAsyncClient client = get();
                client.start();
                HttpGet get = new HttpGet(site + "/api");
                get.addHeader("Authorization", "Token " + token);
                client.execute(get, new FutureCallback<HttpResponse>() {
                    public void completed(final HttpResponse re) {
                        try {
                            Files.writeTags(re);
                        } catch (Exception e) {
                            e.printStackTrace();
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

            public void failed(final Exception e) {
                e.printStackTrace();
            }

            public void cancelled() {
                return;
            }
        });
    }
}
