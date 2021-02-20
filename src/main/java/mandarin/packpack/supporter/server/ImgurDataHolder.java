package mandarin.packpack.supporter.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.Set;

public class ImgurDataHolder {
    private final JsonObject data;

    private String clientID;

    public ImgurDataHolder(JsonObject object) {
        data = Objects.requireNonNullElseGet(object, JsonObject::new);
    }

    public void put(String md5, String url, boolean finalize) {
        if(!data.has(md5)) {
            JsonObject obj = new JsonObject();

            obj.addProperty("url", url);
            obj.addProperty("final", finalize);

            data.add(md5, obj);
        } else {
            JsonElement elem = data.get(md5);

            if(elem.isJsonObject()) {
                JsonObject obj = elem.getAsJsonObject();

                if(obj.has("final")) {
                    boolean fin = obj.get("final").getAsBoolean();

                    if(!fin) {
                        obj.addProperty("url", url);
                        obj.addProperty("final", finalize);
                    }
                } else {
                    obj.addProperty("url", url);
                    obj.addProperty("final", finalize);
                }

                data.add(md5, obj);
            } else {
                JsonObject obj = new JsonObject();

                obj.addProperty("url" , url);
                obj.addProperty("final", false);

                data.add(md5, obj);
            }
        }
    }

    public String get(String md5) {
        if(data.has(md5)) {
            JsonElement elem = data.get(md5);

            if(elem.isJsonPrimitive()) {
                return elem.getAsString();
            } else if(elem.isJsonObject()) {
                JsonObject obj = elem.getAsJsonObject();

                if(obj.has("url")) {
                    return obj.get("url").getAsString();
                }
            }
        }

        return null;
    }

    public void clear() {
        Set<String> keys = data.keySet();

        for(String k : keys) {
            data.remove(k);
        }
    }

    public boolean finalized(String md5) {
        if(data.has(md5)) {
            JsonElement elem = data.get(md5);

            if(elem.isJsonPrimitive()) {
                return false;
            } else if(elem.isJsonObject()) {
                JsonObject object = elem.getAsJsonObject();

                if(object.has("final")) {
                    return object.get("final").getAsBoolean();
                }
            }
        }

        return false;
    }

    public JsonObject getData() {
        return data;
    }

    public void registerClient(String clientID) {
        this.clientID = clientID;
    }

    public String uploadFile(File image) throws Exception {
        String type;
        String link;

        if(image.getName().endsWith("mp4")) {
            type = "video";
            link = "upload";
        } else {
            type = "image";
            link = "image";
        }

        HttpPost post = new HttpPost("https://api.imgur.com/3/"+link);

        post.setHeader("Accept", "application/json");
        post.setHeader("Authorization", "Client-ID "+clientID);

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();

        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

        FileBody fileBody = new FileBody(image);

        builder.addPart(type, fileBody);
        builder.addTextBody("type", "file");

        HttpEntity entity = builder.build();

        post.setEntity(entity);

        CloseableHttpClient client = HttpClientBuilder.create().build();

        HttpResponse response = client.execute(post);

        StringBuilder result = new StringBuilder();

        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

        String line;

        while((line = reader.readLine()) != null) {
            result.append(line).append("\n");
        }

        JsonObject obj = JsonParser.parseString(result.toString()).getAsJsonObject();

        if (obj.has("data")) {
            JsonObject data = obj.get("data").getAsJsonObject();

            if(data.has("link")) {
                return data.get("link").getAsString();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}
