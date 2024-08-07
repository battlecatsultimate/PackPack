package mandarin.packpack.supporter.server.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mandarin.packpack.supporter.StaticStore;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

public class ImgurDataHolder {
    private final JsonObject data;

    private String clientID;

    public ImgurDataHolder(JsonObject object) {
        data = Objects.requireNonNullElseGet(object, JsonObject::new);

        reformatData();
    }

    public void put(String md5, String url, boolean raw) {
        if(!data.has(md5)) {
            JsonObject obj = new JsonObject();

            if(raw)
                obj.addProperty("mp4", url);
            else
                obj.addProperty("gif", url);

            data.add(md5, obj);

            StaticStore.logger.uploadLog("Added new cache, there was no tag called : " + md5 + "\nURL : " + url + "\nRaw? : " + raw);
        } else {
            JsonElement elem = data.get(md5);

            if(elem.isJsonObject()) {
                JsonObject obj = elem.getAsJsonObject();

                StaticStore.logger.uploadLog("Before injecting cache\n\nCache : " + md5 + "\nGIF? : " + obj.has("gif") + "\nMP4? : " + obj.has("mp4"));

                if(raw && obj.has("mp4"))
                    StaticStore.conflictedAnimation.put(md5, url);
                else if(!raw && obj.has("gif"))
                    StaticStore.conflictedAnimation.put(md5, url);

                if(raw && !obj.has("mp4"))
                    obj.addProperty("mp4", url);
                else if(!obj.has("gif"))
                    obj.addProperty("gif", url);

                data.add(md5, obj);

                StaticStore.logger.uploadLog("Added new cache on existing tag\nURL : " + url + "\nRaw? : " + raw);
            } else {
                StaticStore.logger.uploadLog("Non-Json Object cache found\n\nJSON : \n\n" + elem);

                JsonObject obj = new JsonObject();

                if(raw)
                    obj.addProperty("mp4", url);
                else
                    obj.addProperty("gif", url);

                data.add(md5, obj);
            }
        }
    }

    public String get(String md5, boolean gif, boolean raw) {
        if(data.has(md5)) {
            JsonElement elem = data.get(md5);

            if(elem.isJsonPrimitive()) {
                return elem.getAsString();
            } else if(elem.isJsonObject()) {
                JsonObject obj = elem.getAsJsonObject();

                if(gif && !raw && obj.has("gif"))
                    return obj.get("gif").getAsString();
                else if(!gif && raw && obj.has("mp4"))
                    return obj.get("mp4").getAsString();
                else if(gif && raw && obj.has("mp4"))
                    return obj.get("mp4").getAsString();
                else if(!gif && !raw)
                    if(obj.has("mp4"))
                        return obj.get("mp4").getAsString();
                    else if(obj.has("gif"))
                        return obj.get("gif").getAsString();
            }
        }

        return null;
    }

    public boolean removeCache(String code) {
        for(String k : data.keySet()) {
            if(k.equals(code)) {
                data.remove(k);

                return true;
            }
        }

        return false;
    }

    public void clear() {
        Set<String> keys = data.keySet();

        ArrayList<String> ks = new ArrayList<>(keys);

        for(String k : ks) {
            data.remove(k);
        }
    }

    public JsonObject getData() {
        return data;
    }

    public void registerClient(String clientID) {
        this.clientID = clientID;
    }

    public String uploadFile(File image) {
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

        builder.setMode(HttpMultipartMode.LEGACY);

        FileBody fileBody = new FileBody(image);

        builder.addPart(type, fileBody);
        builder.addTextBody("type", "file");

        HttpEntity entity = builder.build();

        post.setEntity(entity);

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            ClassicHttpResponse response = client.executeOpen(null, post, null);

            StringBuilder result = new StringBuilder();

            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

            String line;

            while((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }

            JsonObject obj = JsonParser.parseString(result.toString()).getAsJsonObject();

            StaticStore.logger.uploadLog("Tried to upload file to imgur\nPath : "+image.getAbsolutePath()+"\nSize : "+StaticStore.beautifyFileSize(image)+"\nResult : \n```json\n"+ result +"\n```");

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
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/ImgurDataHolder::uploadFile - Failed to open http client");
        }

        return null;
    }

    public String uploadCatbox(File image) {
        HttpPost post = new HttpPost("https://catbox.moe/user/api.php");

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();

        builder.setMode(HttpMultipartMode.LEGACY);

        FileBody fileBody = new FileBody(image);

        builder.addPart("fileToUpload", fileBody);
        builder.addTextBody("reqtype", "fileupload");

        HttpEntity entity = builder.build();

        post.setEntity(entity);

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            ClassicHttpResponse response = client.executeOpen(null, post, null);

            StringBuilder result = new StringBuilder();

            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

            String line;

            while((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }

            return result.toString().trim();
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/ImgurDataHolder::uploadCatbox - Failed to open http client");
        }

        return null;
    }

    private void reformatData() {
        for(String key : data.keySet()) {
            JsonObject obj = data.getAsJsonObject(key);

            if(obj.has("url")) {
                String url = obj.get("url").getAsString();

                if(url.endsWith("mp4"))
                    obj.addProperty("mp4", url);
                else if(url.endsWith("gif"))
                    obj.addProperty("gif", url);

                obj.remove("url");
            }

            obj.remove("final");

            data.add(key, obj);
        }
    }
}
