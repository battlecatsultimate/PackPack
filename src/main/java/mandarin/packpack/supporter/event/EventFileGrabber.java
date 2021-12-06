package mandarin.packpack.supporter.event;

import mandarin.packpack.supporter.StaticStore;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class EventFileGrabber {
    public static final String domain = "nyanko-events-prd.s3.ap-northeast-1.amazonaws.com";
    public static final String region = "ap-northeast-1";
    public static final String service = "s3";
    public static final String request = "aws4_request";
    public static final String algorithm = "AWS4-HMAC-SHA256";
    public static final String slash = "%2F";

    public static final String link = "https://nyanko-events-prd.s3.ap-northeast-1.amazonaws.com/battlecats{1}_production/{2}";
    public static final String[] locale = {"en", "tw", "kr", ""};
    public static final String[] file = {"gatya.tsv", "item.tsv",  "sale.tsv"};

    public static String id = null;
    public static String key = null;

    public static void initialize() throws Exception {
        File f = new File("./data/privateKey.txt");

        if(!f.exists())
            return;

        BufferedReader reader = new BufferedReader(new FileReader(f));

        id = reader.readLine();
        key = reader.readLine();

        reader.close();
    }

    public static String getLink(int loc, int f) {
        if(loc < 0)
            loc = 0;

        if(loc >= locale.length)
            loc = locale.length - 1;

        if(f < 0)
            f = 0;

        if(f >= file.length)
            f = file.length - 1;

        String l = link.replace("{1}", locale[loc]).replace("{2}", file[f]);

        try {
            return l +
                    "?X-Amz-Algorithm=" + algorithm +
                    "&X-Amz-Credential=" + id + slash + getDate() + slash + region + slash + service + slash + request +
                    "&X-Amz-Date=" + getAmzDate() +
                    "&X-Amz-Expires=600" +
                    "&X-Amz-SignedHeaders=host" +
                    "&X-Amz-Signature="+getSigningKey(l);
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "Failed to parse event data link");

            return null;
        }
    }

    private static String getSigningKey(String l) throws Exception {
        byte[] k = ("AWS4" + key).getBytes(StandardCharsets.UTF_8);
        byte[] dateKey = hmacSha256(k, getDate());
        byte[] dateRegionKey = hmacSha256(dateKey, region);
        byte[] dateRegionServiceKey = hmacSha256(dateRegionKey, service);
        byte[] signingKey = hmacSha256(dateRegionServiceKey, request);

        return Hex.encodeHexString(hmacSha256(signingKey, getStringToSign(l)));
    }

    private static String getStringToSign(String l) {
        return algorithm + "\n" +
                getAmzDate() + "\n" +
                getDate() + "/" + region + "/" + service + "/" + request + "\n" +
                DigestUtils.sha256Hex(getCanonical(l));
    }

    private static String getCanonical(String l) {
        return "GET\n" +
                getUri(l) + "\n" +
                "X-Amz-Algorithm=" + algorithm + "&" +
                "X-Amz-Credential=" + id + slash + getDate() + slash + region + slash + service + slash + request + "&" +
                "X-Amz-Date=" + getAmzDate() + "&" +
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
