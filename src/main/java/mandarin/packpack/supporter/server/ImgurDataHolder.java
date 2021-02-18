package mandarin.packpack.supporter.server;

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

public class ImgurDataHolder {
    private final JsonObject data;

    private String clientID;

    public ImgurDataHolder(JsonObject object) {
        data = Objects.requireNonNullElseGet(object, JsonObject::new);
    }

    public void put(String md5, String url) {
        if(!data.has(md5)) {
            data.addProperty(md5, url);
        }
    }

    public String get(String md5) {
        if(data.has(md5)) {
            return data.get(md5).getAsString();
        }

        return null;
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
