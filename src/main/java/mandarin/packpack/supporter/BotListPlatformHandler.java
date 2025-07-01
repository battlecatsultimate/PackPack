package mandarin.packpack.supporter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.StringEntity;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BotListPlatformHandler {
    private static String topGGToken = null;
    private static String discordBotListToken = null;
    private static String koreanDiscordListToken = null;
    private static String discordBotGGToken = null;

    private static final long discordBotGGRateLimit = 200;

    private static final String topGGDomain = "https://top.gg/api/";
    private static final String discordBotListDomain = "https://discordbotlist.com/api/v1/";
    private static final String koreanDiscordListDomain = "https://koreanbots.dev/api/v2/";
    private static final String discordBogGGDomain = "https://discord.bots.gg/api/v1/";

    public static void initialize() {
        File jsonFile = new File("./data/botListPlatformTokens.json");

        if (!jsonFile.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(jsonFile)) {
            JsonElement element = JsonParser.parseReader(reader);

            if (!element.isJsonObject())
                return;

            JsonObject tokenObject = element.getAsJsonObject();

            if (tokenObject.has("topGG")) {
                topGGToken = tokenObject.get("topGG").getAsString();
            }

            if (tokenObject.has("discordBotList")) {
                discordBotListToken = tokenObject.get("discordBotList").getAsString();
            }

            if (tokenObject.has("koreanDiscordList")) {
                koreanDiscordListToken = tokenObject.get("koreanDiscordList").getAsString();
            }

            if (tokenObject.has("discordBotGG")) {
                discordBotGGToken = tokenObject.get("discordBotGG").getAsString();
            }
        } catch (IOException e) {
            StaticStore.logger.uploadErrorLog(e, "E/BotListPlatformHandler::initialize - Failed to open reader for json file");
        }
    }

    public static void handleUpdatingBotStatus(@Nonnull ShardManager manager, boolean printResponse) {
        String botID = getBotID(manager);

        if (botID == null)
            return;

        handleTopGG(manager, botID, printResponse);
        handleDiscordBotList(manager, botID, printResponse);
        handleKoreanDiscordList(manager, botID, printResponse);
        handleDiscordBotGG(manager, botID, printResponse);
    }

    public static void handleTopGG(@Nonnull ShardManager manager, String botID, boolean printResponse) {
        if (topGGToken == null)
            return;

        String requestLink = topGGDomain + "bots/" + botID + "/stats";

        JsonObject obj = new JsonObject();

        obj.addProperty("server_count", getGuildNumbers(manager));

        JsonArray shards = new JsonArray();

        for (long guildNumber : getShardGuildNumbers(manager)) {
            shards.add(guildNumber);
        }

        obj.add("shards", shards);
        obj.addProperty("shard_count", getShardCount(manager));

        HttpPost post = new HttpPost(requestLink);

        post.addHeader("Authorization", topGGToken);
        post.addHeader("Content-Type", "application/json");
        post.setEntity(new StringEntity(obj.toString()));

        try (
                CloseableHttpClient httpClient = HttpClientBuilder.create().build();
                ClassicHttpResponse response = httpClient.executeOpen(null, post, null)
        ) {
            if (response.getCode() != 200 || printResponse) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
                    StringBuilder result = new StringBuilder();

                    String line;

                    while((line = reader.readLine()) != null) {
                        result.append(line).append("\n");
                    }

                    StaticStore.logger.uploadLog("W/BotListPlatformHandler::handleTopGG - Got non-200 code from top.gg\nStatus Code = %d\nReason Phrase = %s\nBody = %s".formatted(response.getCode(), response.getReasonPhrase(), result.toString()));
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e, "E/BotListPlatformHandler::handleTopGG - Failed to read body from response");
                }
            }
        } catch (IOException e) {
            StaticStore.logger.uploadErrorLog(e, "E/BotListPlatformHandler::handleTopGG - Failed to open http client");
        }
    }

    public static void handleDiscordBotList(@Nonnull ShardManager manager, String botID, boolean printResponse) {
        if (discordBotListToken == null)
            return;

        String requestLink = discordBotListDomain + "bots/" + botID + "/stats";

        long[] guildNumbers = getShardGuildNumbers(manager);

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            for (int i = 0; i < guildNumbers.length; i++) {
                HttpPost post = new HttpPost(requestLink);

                post.addHeader("Authorization", discordBotListToken);
                post.addHeader("Content-Type", "application/json");

                JsonObject bodyObject = new JsonObject();

                bodyObject.addProperty("guilds", guildNumbers[i]);
                bodyObject.addProperty("shard_id", i);

                post.setEntity(new StringEntity(bodyObject.toString()));

                try (
                        ClassicHttpResponse response = httpClient.executeOpen(null, post, null)
                ) {
                    if (response.getCode() != 200 || printResponse) {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
                            StringBuilder result = new StringBuilder();

                            String line;

                            while((line = reader.readLine()) != null) {
                                result.append(line).append("\n");
                            }

                            StaticStore.logger.uploadLog("W/BotListPlatformHandler::handleDiscordBotList - Got non-200 code from discordbotlist.com\nStatus Code = %d\nReason Phrase = %s\nBody = %s".formatted(response.getCode(), response.getReasonPhrase(), result.toString()));
                        } catch (Exception e) {
                            StaticStore.logger.uploadErrorLog(e, "E/BotListPlatformHandler::handleDiscordBotList - Failed to read body from response");
                        }
                    }
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e, "E/BotListPlatformHandler::handleDiscordBotList - Failed to execute http post");
                }
            }
        } catch (IOException e) {
            StaticStore.logger.uploadErrorLog(e, "E/BotListPlatformHandler::handleTopGG - Failed to open http client");
        }
    }

    public static void handleKoreanDiscordList(@Nonnull ShardManager manager, String botID, boolean printResponse) {
        if (koreanDiscordListToken == null)
            return;

        String requestLink = koreanDiscordListDomain + "bots/" + botID + "/stats";

        JsonObject bodyObject = new JsonObject();

        bodyObject.addProperty("servers", getGuildNumbers(manager));
        bodyObject.addProperty("shards", getShardCount(manager));

        HttpPost post = new HttpPost(requestLink);

        post.addHeader("Authorization", koreanDiscordListToken);
        post.addHeader("Content-Type", "application/json");

        post.setEntity(new StringEntity(bodyObject.toString()));

        try (
                CloseableHttpClient client = HttpClientBuilder.create().build();
                ClassicHttpResponse response = client.executeOpen(null, post, null)
        ) {
            if (response.getCode() != 200 || printResponse) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
                    StringBuilder result = new StringBuilder();

                    String line;

                    while((line = reader.readLine()) != null) {
                        result.append(line).append("\n");
                    }

                    StaticStore.logger.uploadLog("W/BotListPlatformHandler::handleKoreanDiscordList - Got non-200 code from koreanbots.com\nStatus Code = %d\nReason Phrase = %s\nBody = %s".formatted(response.getCode(), response.getReasonPhrase(), result.toString()));
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e, "E/BotListPlatformHandler::handleKoreanDiscordList - Failed to read body from response");
                }
            }
        } catch (IOException e) {
            StaticStore.logger.uploadErrorLog(e, "E/BotListPlatformHandler::handleKoreanDiscordList - Failed to open http client");
        }
    }

    public static void handleDiscordBotGG(ShardManager manager, String botID, boolean printResponse) {
        if (discordBotGGToken == null)
            return;

        String requestLink = discordBogGGDomain + "bots/" + botID + "/stats";

        long[] guilds = getShardGuildNumbers(manager);
        long shardCount = getShardCount(manager);

        for (int i = 0; i < guilds.length; i++) {
            JsonObject bodyObject = new JsonObject();

            bodyObject.addProperty("guildCount", guilds[i]);
            bodyObject.addProperty("shardCount", shardCount);
            bodyObject.addProperty("shardId", i);

            HttpPost post = new HttpPost(requestLink);

            post.setHeader("Authorization", discordBotGGToken);
            post.setHeader("Content-Type", "application/json");

            post.setEntity(new StringEntity(bodyObject.toString()));

            long startTime = System.currentTimeMillis();
            long endTime;

            try (
                    CloseableHttpClient httpClient = HttpClientBuilder.create().build();
                    ClassicHttpResponse response = httpClient.executeOpen(null, post, null)
            ) {
                endTime = System.currentTimeMillis();

                if (response.getCode() != 200 || printResponse) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
                        StringBuilder result = new StringBuilder();

                        String line;

                        while((line = reader.readLine()) != null) {
                            result.append(line).append("\n");
                        }

                        StaticStore.logger.uploadLog("W/BotListPlatformHandler::handleDiscordBotGG - Got non-200 code from discord.bots.gg\nStatus Code = %d\nReason Phrase = %s\nBody = %s".formatted(response.getCode(), response.getReasonPhrase(), result.toString()));
                    } catch (Exception e) {
                        StaticStore.logger.uploadErrorLog(e, "E/BotListPlatformHandler::handleDiscordBotGG - Failed to read body from response");
                    }
                }
            } catch (IOException e) {
                endTime = System.currentTimeMillis();
                StaticStore.logger.uploadErrorLog(e, "E/BotListPlatformHandler::handleDiscordBotGG - Failed to open http client");
            }

            long waitTime = discordBotGGRateLimit - (endTime - startTime);

            if (waitTime > 0) {
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    StaticStore.logger.uploadErrorLog(e, "E/BotListPlatformHandler::handleDiscordBotGG - Failed to wait for rate limit");
                    return;
                }
            }
        }
    }

    private static long getGuildNumbers(@Nonnull ShardManager manager) {
        List<JDA> shardList = new ArrayList<>(manager.getShards());
        shardList.sort(Comparator.comparingInt(shard -> shard.getShardInfo().getShardId()));

        long totalSize = 0;

        for (JDA shard : shardList) {
            totalSize += shard.getGuilds().size();
        }

        return totalSize;
    }

    private static long[] getShardGuildNumbers(@Nonnull ShardManager manager) {
        List<JDA> shardList = new ArrayList<>(manager.getShards());
        shardList.sort(Comparator.comparingInt(shard -> shard.getShardInfo().getShardId()));

        long[] guildNumbers = new long[shardList.size()];

        for (int i = 0; i < shardList.size(); i++) {
            guildNumbers[i] = shardList.get(i).getGuilds().size();
        }

        return guildNumbers;
    }

    private static long getShardCount(@Nonnull ShardManager manager) {
        return manager.getShards().size();
    }

    private static String getBotID(@Nonnull ShardManager manager) {
        List<JDA> shards = manager.getShards();

        if (shards.isEmpty()) {
            return null;
        } else {
            return shards.getFirst().getSelfUser().getId();
        }
    }
}
