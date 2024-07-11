package mandarin.packpack.supporter.event;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import common.CommonStatic;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.TasteApk;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class EventFileGrabber {
    public static Map<CommonStatic.Lang.Locale, Boolean> newWay = new HashMap<>();

    public static final String domain = "nyanko-events-prd.s3.ap-northeast-1.amazonaws.com";
    public static final String region = "ap-northeast-1";
    public static final String service = "s3";
    public static final String request = "aws4_request";
    public static final String algorithm = "AWS4-HMAC-SHA256";
    public static final String slash = "%2F";

    public static final String link = "https://nyanko-events-prd.s3.ap-northeast-1.amazonaws.com/battlecats{1}_production/{2}";
    public static final String[] locale = {"en", "tw", "kr", ""};
    public static final String[] file = {"gatya.tsv", "item.tsv", "sale.tsv"};

    public static String id = null;
    public static String key = null;

    // ----------------------------------------------------
    // |          New way of grabbing event file          |
    // ----------------------------------------------------

    public static String accountCode;
    public static String password;
    public static String passwordRefreshToken;

    public static String jwtToken;
    public static long tokenCreatedAt;

    public static final String newEventLink = "https://nyanko-events.ponosgames.com/battlecats{1}_production/{2}?jwt=";
    public static final String userCreationLink = "https://nyanko-backups.ponosgames.com/?action=createAccount&referenceId=";
    public static final String passwordLink = "https://nyanko-auth.ponosgames.com/v1/users";
    public static final String passwordRefreshLink = "https://nyanko-auth.ponosgames.com/v1/user/password";
    public static final String jwtLink = "https://nyanko-auth.ponosgames.com/v1/tokens";

    public static void initialize() throws Exception {
        File f = new File("./data/privateKey.txt");

        if(!f.exists())
            return;

        BufferedReader reader = new BufferedReader(new FileReader(f));

        id = reader.readLine();
        key = reader.readLine();

        for(CommonStatic.Lang.Locale locale : EventFactor.supportedVersions) {
            TasteApk.VECTOR.put(locale, reader.readLine());
            TasteApk.KEY.put(locale, reader.readLine());
        }

        reader.close();

        if (newWay.isEmpty()) {
            newWay.put(CommonStatic.Lang.Locale.EN, false);
            newWay.put(CommonStatic.Lang.Locale.ZH, false);
            newWay.put(CommonStatic.Lang.Locale.KR, false);
            newWay.put(CommonStatic.Lang.Locale.JP, true);
        }
    }

    public static String getLink(CommonStatic.Lang.Locale loc, int f) {
        if (newWay.containsKey(loc) && newWay.get(loc)) {
            try {
                String link = getNewLink(loc, f);

                if (link != null) {
                    return link;
                }
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "E/EventFileGrabber::getLink - Failed to get event data link from new system");
            }
        }

        return getLinkOld(loc, f);
    }

    public static String getNewLink(CommonStatic.Lang.Locale loc, int f) throws Exception {
        if (loc == null)
            loc = CommonStatic.Lang.Locale.EN;

        int index = ArrayUtils.indexOf(EventFactor.supportedVersions, loc);

        if (index == -1)
            return null;

        if (f < 0)
            f = 0;

        if (f >= file.length)
            f = file.length - 1;

        if (accountCode == null || password == null || passwordRefreshToken == null) {
            generateAccount();

            if (accountCode != null) {
                StaticStore.logger.uploadLog("Successfully generated new BC account because there wasn't any account code yet\n\nAccount code : " + accountCode + "\nPassword : " + password + "\nPassword refresh token : " + passwordRefreshToken);
            } else {
                return null;
            }
        }

        if (accountCode == null)
            return null;

        if (jwtToken == null || System.currentTimeMillis() - tokenCreatedAt > 12 * 60 * 60 * 1000) { // 12h
            jwtToken = generateJWTToken();

            if(jwtToken != null) {
                tokenCreatedAt = System.currentTimeMillis();

                StaticStore.logger.uploadLog("Successfully refreshed JWT token\n\nToken : " + jwtToken);
            }
        }

        if (jwtToken == null) {
            boolean passwordRefreshed = refreshPassword();

            //Password might be expired, regen
            if (!passwordRefreshed) {
                StaticStore.logger.uploadLog("W/EventFileGrabber::getNewLink - Failed to refresh password while there's no jwt token or token is expired, regenerating account...");

                //If failed, account might be banned, regen
                generateAccount();

                if (accountCode != null) {
                    StaticStore.logger.uploadLog("Successfully generated new BC account because account seemed to be banned\n\nAccount code : " + accountCode + "\nPassword : " + password + "\nPassword refresh token : " + passwordRefreshToken);
                } else {
                    return null;
                }
            } else {
                StaticStore.logger.uploadLog("Successfully refreshed password\n\nPassword : " + password + "\nPassword refresh token : " + passwordRefreshToken);
            }

            jwtToken = generateJWTToken();

            if (jwtToken == null && passwordRefreshed) {
                //Even though password is refreshed, but failed? Account might be banned
                generateAccount();

                if (accountCode != null) {
                    StaticStore.logger.uploadLog("Successfully generated new BC account because account seemed to be banned\n\nAccount code : " + accountCode + "\nPassword : " + password + "\nPassword refresh token : " + passwordRefreshToken);
                } else {
                    return null;
                }
            }

            jwtToken = generateJWTToken();

            if(jwtToken != null) {
                tokenCreatedAt = System.currentTimeMillis();

                StaticStore.logger.uploadLog("Successfully refreshed JWT token\n\nToken : " + jwtToken);
            } else {
                return null;
            }
        }

        return newEventLink.replace("{1}", locale[index]).replace("{2}", file[f]) + jwtToken;
    }

    public static String getLinkOld(CommonStatic.Lang.Locale loc, int f) {
        if(loc == null)
            loc = CommonStatic.Lang.Locale.EN;

        int index = ArrayUtils.indexOf(EventFactor.supportedVersions, loc);

        if(index == -1)
            return null;

        if(f < 0)
            f = 0;

        if(f >= file.length)
            f = file.length - 1;

        String l = link.replace("{1}", locale[index]).replace("{2}", file[f]);

        String amz = getAmzDate();

        try {
            return l +
                    "?X-Amz-Algorithm=" + algorithm +
                    "&X-Amz-Credential=" + id + slash + getDate() + slash + region + slash + service + slash + request +
                    "&X-Amz-Date=" + amz +
                    "&X-Amz-Expires=600" +
                    "&X-Amz-SignedHeaders=host" +
                    "&X-Amz-Signature="+getSigningKey(l, amz);
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "Failed to parse event data link");

            return null;
        }
    }

    //Call this method only when it really needs to be regenerated
    private static void generateAccount() throws Exception {
        accountCode = null;
        password = null;
        passwordRefreshToken = null;

        HttpURLConnection connection = (HttpURLConnection) URI.create(userCreationLink).toURL().openConnection();
        connection.setRequestMethod("GET");

        connection.connect();

        if (connection.getResponseCode() != 200) {
            connection.disconnect();

            return;
        }

        StringBuilder result = new StringBuilder();

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        String line;

        while((line = reader.readLine()) != null) {
            result.append(line).append("\n");
        }

        reader.close();

        connection.disconnect();

        String accountData = result.toString().trim();

        if(accountData.isBlank()) {
            return;
        }

        JsonElement element = JsonParser.parseString(accountData);

        if(element == null || !element.isJsonObject()) {
            return;
        }

        JsonObject obj = element.getAsJsonObject();

        if(!obj.has("success") || !obj.get("success").getAsBoolean()) {
            return;
        }

        accountCode = obj.get("accountId").getAsString();

        //Generating password

        long currentTime = System.currentTimeMillis() / 1000;

        JsonObject passwordHeaderData = new JsonObject();

        passwordHeaderData.addProperty("accountCode", accountCode);
        passwordHeaderData.addProperty("accountCreatedAt", String.valueOf(currentTime));
        passwordHeaderData.addProperty("nonce", generateRandomHex(32));

        CloseableHttpClient client = HttpClientBuilder.create().build();

        CloseableHttpResponse passwordResponse = getPostResponse(client, passwordLink, passwordHeaderData.toString());

        if (passwordResponse.getStatusLine().getStatusCode() != 200) {
            passwordResponse.close();
            client.close();

            return;
        }

        StringBuilder passwordResult = new StringBuilder();

        BufferedReader passwordReader = new BufferedReader(new InputStreamReader(passwordResponse.getEntity().getContent()));

        while((line = passwordReader.readLine()) != null) {
            passwordResult.append(line).append("\n");
        }

        passwordReader.close();

        passwordResponse.close();
        client.close();

        String passwordText = passwordResult.toString();

        if (passwordText.isBlank())
            return;

        JsonElement passwordElement = JsonParser.parseString(passwordText);

        if (passwordElement == null || !passwordElement.isJsonObject())
            return;

        JsonObject passwordObject = passwordElement.getAsJsonObject();

        if(!passwordObject.has("payload")) {
           return;
        }

        JsonElement passwordPayload = passwordObject.get("payload");

        if (!passwordPayload.isJsonObject())
            return;

        JsonObject payloadObject = passwordPayload.getAsJsonObject();

        if (!payloadObject.has("password") || !payloadObject.has("passwordRefreshToken"))
            return;

        password = payloadObject.get("password").getAsString();
        passwordRefreshToken = payloadObject.get("passwordRefreshToken").getAsString();
    }

    private static String generateJWTToken() throws Exception {
        if (accountCode == null || password == null) {
            throw new IllegalStateException("E/EventFileGrabber::generateJWTToken - Account code or password found to be null!");
        }

        JsonObject tokenData = new JsonObject();

        tokenData.addProperty("accountCode", accountCode);

        JsonObject clientInfo = new JsonObject();

        JsonObject client = new JsonObject();

        client.addProperty("countryCode", "ja");
        client.addProperty("version", "999999");

        clientInfo.add("client", client);

        JsonObject device = new JsonObject();

        device.addProperty("model", "XQ-BC52");

        clientInfo.add("device", device);

        JsonObject os = new JsonObject();

        os.addProperty("type", "android");
        os.addProperty("version", "Android 13");

        clientInfo.add("os", os);

        tokenData.add("clientInfo", clientInfo);
        tokenData.addProperty("nonce", generateRandomHex(32));
        tokenData.addProperty("password", password);

        CloseableHttpClient httpClient = HttpClientBuilder.create().build();

        CloseableHttpResponse response = getPostResponse(httpClient, jwtLink, tokenData.toString());

        StatusLine statusLine = response.getStatusLine();

        if(statusLine.getStatusCode() != 200) {
            response.close();
            httpClient.close();

            return null;
        }

        StringBuilder result = new StringBuilder();

        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

        String line;

        while((line = reader.readLine()) != null) {
            result.append(line);
        }

        reader.close();

        response.close();
        httpClient.close();

        String text = result.toString();

        if(text.isBlank()) {
            return null;
        }

        JsonElement element = JsonParser.parseString(text);

        if (element == null || !element.isJsonObject())
            return null;

        JsonObject obj = element.getAsJsonObject();

        if (!obj.has("payload"))
            return null;

        JsonElement payloadElement = obj.get("payload");

        if (!payloadElement.isJsonObject())
            return null;

        JsonObject payload = payloadElement.getAsJsonObject();

        if (!payload.has("token"))
            return null;

        return payload.get("token").getAsString();
    }

    private static boolean refreshPassword() throws Exception {
        JsonObject authData = new JsonObject();

        authData.addProperty("accountCode", accountCode);
        authData.addProperty("passwordRefreshToken", passwordRefreshToken);
        authData.addProperty("nonce", generateRandomHex(32));

        CloseableHttpClient client = HttpClientBuilder.create().build();

        CloseableHttpResponse response = getPostResponse(client, passwordRefreshLink, authData.toString());

        StatusLine statusLine = response.getStatusLine();

        if(statusLine.getStatusCode() != 200) {
            System.out.println(statusLine.getStatusCode() + " : " + statusLine.getReasonPhrase());

            response.close();
            client.close();

            return false;
        }

        StringBuilder result = new StringBuilder();

        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

        String line;

        while((line = reader.readLine()) != null) {
            result.append(line);
        }

        String text = result.toString();

        if(text.isBlank())
            return false;

        JsonElement element = JsonParser.parseString(text);

        if (element == null || !element.isJsonObject())
            return false;

        JsonObject obj = element.getAsJsonObject();

        if (!obj.has("payload") || !obj.get("payload").isJsonObject())
            return false;

        JsonObject payload = obj.get("payload").getAsJsonObject();

        if (!payload.has("password") || !payload.has("passwordRefreshToken"))
            return false;

        password = payload.get("password").getAsString();
        passwordRefreshToken = payload.get("passwordRefreshToken").getAsString();

        return true;
    }

    private static CloseableHttpResponse getPostResponse(CloseableHttpClient client, String link, String jsonText) throws Exception {
        long currentTime = System.currentTimeMillis();

        HttpPost post = new HttpPost();

        post.setURI(new URI(link));

        prepareHeader(post, jsonText, currentTime);

        StringEntity entity = new StringEntity(jsonText);
        entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));

        post.setEntity(entity);

        return client.execute(post);
    }

    private static void prepareHeader(HttpPost connection, String jsonText, long timeStamp) throws Exception {
        connection.setHeader("Nyanko-Signature", getNyankoSignature(jsonText));
        connection.setHeader("Nyanko-Signature-Version", String.valueOf(1));
        connection.setHeader("Nyanko-Signature-Algorithm", "HMACSHA256");
        connection.setHeader("Content-Type", "application/json");
        connection.setHeader("Nyanko-Timestamp", String.valueOf(timeStamp));
        connection.setHeader("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 13; XQ-BC52 Build/61.2.A.0.447)");
        connection.setHeader("Connection", "Keep-Alive");
        connection.setHeader("Accept-Encoding", "gzip");
    }

    private static String getNyankoSignature(String jsonText) throws Exception {
        String randomData = generateRandomHex(64);

        return randomData + Hex.encodeHexString(hmacSha256((accountCode + randomData).getBytes(StandardCharsets.UTF_8), jsonText));
    }

    private static String generateRandomHex(int length) {
        Random r = new Random();

        StringBuilder hex = new StringBuilder();

        while(hex.length() < length) {
            hex.append(String.format("%08x", r.nextInt()));
        }

        return hex.toString();
    }

    private static String getSigningKey(String l, String amz) throws Exception {
        byte[] k = ("AWS4" + key).getBytes(StandardCharsets.UTF_8);
        byte[] dateKey = hmacSha256(k, getDate());
        byte[] dateRegionKey = hmacSha256(dateKey, region);
        byte[] dateRegionServiceKey = hmacSha256(dateRegionKey, service);
        byte[] signingKey = hmacSha256(dateRegionServiceKey, request);

        return Hex.encodeHexString(hmacSha256(signingKey, getStringToSign(l, amz)));
    }

    private static String getStringToSign(String l, String amz) {
        return algorithm + "\n" +
                amz + "\n" +
                getDate() + "/" + region + "/" + service + "/" + request + "\n" +
                DigestUtils.sha256Hex(getCanonical(l, amz));
    }

    private static String getCanonical(String l, String amz) {
        return "GET\n" +
                getUri(l) + "\n" +
                "X-Amz-Algorithm=" + algorithm + "&" +
                "X-Amz-Credential=" + id + slash + getDate() + slash + region + slash + service + slash + request + "&" +
                "X-Amz-Date=" + amz + "&" +
                "X-Amz-Expires=600&" +
                "X-Amz-SignedHeaders=host\n" +
                "host:" + domain + "\n" +
                "\n" +
                "host\n" +
                "UNSIGNED-PAYLOAD";
    }

    private static String getAmzDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
                .withLocale(Locale.ENGLISH)
                .withZone(ZoneId.from(ZoneOffset.UTC));

        return formatter.format(Instant.now());
    }

    private static String getDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
                .withLocale(Locale.ENGLISH)
                .withZone(ZoneId.from(ZoneOffset.UTC));

        return formatter.format(Instant.now());
    }

    private static byte[] hmacSha256(byte[] key, String content) throws Exception {
        SecretKeySpec secret = new SecretKeySpec(key, "HmacSHA256");
        Mac hasher = Mac.getInstance("HmacSHA256");

        hasher.init(secret);

        return hasher.doFinal(content.getBytes(StandardCharsets.UTF_8));
    }

    private static String getUri(String l) {
        return l.split(domain)[1];
    }
}
