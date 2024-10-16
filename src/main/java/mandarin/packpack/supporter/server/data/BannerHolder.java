package mandarin.packpack.supporter.server.data;

import com.google.gson.*;
import mandarin.packpack.supporter.StaticStore;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class BannerHolder {
    public enum Usage {
        SPRING,
        SUMMER,
        AUTUMN,
        WINTER,
        ANYTIME
    }

    public record BannerData(String author, long userID, File bannerFile, Usage usage) {
        @Override
        public String toString() {
            return "BannerData{" +
                    "author='" + author + '\'' +
                    ", userID=" + userID +
                    ", bannerFile=" + bannerFile.getAbsolutePath() +
                    ", usage=" + usage +
                    '}';
        }

        public JsonObject toJson() {
            JsonObject obj = new JsonObject();

            obj.addProperty("author", author);
            obj.addProperty("userID", userID);
            obj.addProperty("bannerFile", bannerFile.getAbsolutePath());
            obj.addProperty("usage", usage.name());

            return obj;
        }
    }


    public static final List<BannerData> allBanners = new ArrayList<>();

    public static void initializeBannerData(String folderName, String bannerDataName) {
        File bannerFolder = new File("./data/" + folderName);

        if (!bannerFolder.exists()) {
            return;
        }

        File bannerDataFile = new File("./data/" + bannerDataName + ".json");

        if(!bannerDataFile.exists())
            return;

        try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(bannerDataFile), StandardCharsets.UTF_8))) {
            JsonElement obj = JsonParser.parseReader(br);

            br.close();

            if (!(obj instanceof JsonArray arr)) {
                return;
            }

            for (int i = 0; i < arr.size(); i++) {
                if (!(arr.get(i) instanceof JsonObject o)) {
                    StaticStore.logger.uploadLog("W/BannerHolder::initializeBannerData - Invalid banner data at %d : Not Json Object".formatted(i));

                    continue;
                }

                if (!o.has("author")) {
                    StaticStore.logger.uploadLog("W/BannerHolder::initializeBannerData - Invalid banner data at %d : Author data is missing".formatted(i));

                    continue;
                }

                if (!o.has("userID")) {
                    StaticStore.logger.uploadLog("W/BannerHolder::initializeBannerData - Invalid banner data at %d : User ID data is missing".formatted(i));

                    continue;
                }

                if (!o.has("bannerFile")) {
                    StaticStore.logger.uploadLog("W/BannerHolder::initializeBannerData - Invalid banner data at %d : Banner File data is missing".formatted(i));

                    continue;
                }

                if (!o.has("usage")) {
                    StaticStore.logger.uploadLog("W/BannerHolder::initializeBannerData - Invalid banner data at %d : Usage data is missing".formatted(i));

                    continue;
                }

                String author = o.get("author").getAsString();

                JsonElement u = o.get("userID");

                if (!(u instanceof JsonPrimitive p) || !p.isNumber()) {
                    StaticStore.logger.uploadLog("W/BannerHolder::initializeBannerData - Invalid banner data at %d : User ID isn't numeric".formatted(i));

                    continue;
                }

                long userID = u.getAsLong();

                File bannerFile = new File(bannerFolder, o.get("bannerFile").getAsString());

                if (!bannerFile.exists()) {
                    StaticStore.logger.uploadLog("W/BannerHolder::initializeBannerData - Invalid banner data at %d : No such banner file path found = %s".formatted(i, bannerFile.getAbsolutePath()));

                    continue;
                }

                Usage usage = Usage.valueOf(o.get("usage").getAsString());

                allBanners.add(new BannerData(author, userID, bannerFile, usage));
            }
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "Failed to read json file", bannerDataFile);
        }
    }

    public static void fromJson(BannerHolder holder, JsonObject obj) {
        if (obj.has("lastUpdated")) {
            holder.lastUpdated = obj.get("lastUpdated").getAsLong();
        }

        if (obj.has("pickedBanner")) {
            JsonElement e = obj.get("pickedBanner");

            if (!e.isJsonNull() && e instanceof JsonObject o) {
                String author = o.has("author") ? o.get("author").getAsString() : null;
                long userID = o.has("userID") ? o.get("userID").getAsLong() : 0L;
                String filePath = o.has("bannerFile") ? o.get("bannerFile").getAsString() : null;
                Usage usage = o.has("usage") ? Usage.valueOf(o.get("usage").getAsString()) : null;

                holder.pickedBanner = allBanners.stream().filter(b ->
                        b.author.equals(author) && b.userID == userID && b.bannerFile.getAbsolutePath().equals(filePath) && b.usage == usage
                ).findFirst().orElse(null);
            }
        }
    }

    public long lastUpdated = 0L;
    public BannerData pickedBanner;

    @Nullable
    public BannerData pickBanner() {
        if (allBanners.isEmpty()) {
            return null;
        }

        //Define Season
        Calendar c = Calendar.getInstance();

        Usage currentUsage = switch (c.get(Calendar.MONTH)) {
            case 12, 1, 2 -> Usage.WINTER;
            case 3, 4, 5 -> Usage.SPRING;
            case 6, 7, 8 -> Usage.SUMMER;
            case 9, 10, 11 -> Usage.AUTUMN;
            default -> Usage.ANYTIME;
        };

        List<BannerData> matchingBanners = allBanners.stream().filter(b -> (b.usage == currentUsage || b.usage == Usage.ANYTIME) && b != pickedBanner).toList();

        if (matchingBanners.isEmpty()) {
            return null;
        }

        BannerData banner = matchingBanners.get(StaticStore.random.nextInt(0, matchingBanners.size()));
        lastUpdated = System.currentTimeMillis();

        pickedBanner = banner;

        return banner;
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();

        obj.addProperty("lastUpdated", lastUpdated);
        obj.add("pickedBanner", pickedBanner == null ? null : pickedBanner.toJson());

        return obj;
    }
}
