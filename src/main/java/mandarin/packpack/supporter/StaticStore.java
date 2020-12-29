package mandarin.packpack.supporter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.rest.util.Color;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class StaticStore {
    public static String ratingChannel = "";

    public static boolean initialized = false;

    public static String serverPrefix = "p!";
    public static Map<String, String> prefix = new HashMap<>();

    public static Color[] rainbow = {Color.of(217, 65, 68), Color.of(217, 128, 65), Color.of(224, 213, 85)
    , Color.of(118, 224, 85), Color.of(85, 169, 224), Color.of(185, 85, 224)};

    public static boolean checkingBCU = false;
    public static boolean analyzing = false;

    public static final Random random = new Random();

    public static final String MANDARIN_SMELL = "460409259021172781";

    public static String downPack = "./pack/download";
    public static String tempPack = "./pack/download";

    public static final String ERROR_MSG = "`INTERNAL_ERROR`";

    public static final String DEV_ID = "490941233963728896";
    public static final String MOD_ID = "490935132564357131";
    public static final String MEMBER_ID = "632835571655507968";
    public static final String PRE_MEMBER_ID = "490940081738350592";
    public static final String MUTED_ID = "563745009912774687";
    public static final String BCU_PC_USER_ID = "490940151501946880";
    public static final String BCU_ANDROId_USER_ID = "787391428916543488";

    public static final String BOT_COMMANDS = "508042127352266755";
    public static final String GET_ACCESS = "632836623931015185";

    public static String rolesToString(Set<Snowflake> roles) {
        StringBuilder builder = new StringBuilder();

        for(Snowflake role : roles) {
            builder.append(role.asString()).append(", ");
        }

        return builder.toString();
    }

    public static String roleNameFromID(MessageCreateEvent event, String id) {
        AtomicReference<String> role = new AtomicReference<>("NULL");

        event.getGuild().subscribe(g -> g.getRoleById(Snowflake.of(id)).subscribe(r -> role.set(r.getName())));

        return role.get();
    }

    public static File getDownPackFile() {
        int index = 0;

        while(true) {
            File f = new File(downPack+(index == 0 ? "" : Integer.toString(index))+".pack.bcuzip");

            if(f.exists())
                index++;
            else
                return f;
        }
    }

    public static File getTempPackFile() {
        int index = 0;

        while(true) {
            File f = new File(tempPack+(index == 0 ? "" : Integer.toString(index))+".tmp");

            if(f.exists())
                index++;
            else
                return f;
        }
    }

    public static String getPrefix(String id) {
        String pre = prefix.get(id);

        return Objects.requireNonNullElse(pre, serverPrefix);
    }

    public static String getCommand(String message, String prefix) {
        if(message.isBlank() || prefix.isBlank())
            return "";

        String[] list = message.split(" ");

        if(list[0].startsWith(prefix))
            return list[0].replaceFirst(prefix, "");
        else
            return "";
    }

    public static JsonObject mapToJson(Map<String, String> map) {
        JsonObject obj = new JsonObject();

        int i = 0;

        for(String key : map.keySet()) {
            String value = map.get(key);

            if(value == null) {
                System.out.println("Warning! : Key "+key+" returns null!");
                continue;
            }

            JsonObject set = new JsonObject();

            set.addProperty("key", key);
            set.addProperty("val", value);

            obj.add(Integer.toString(i), set);

            i++;
        }

        return obj;
    }

    public static void jsonToMap(JsonObject obj) {
        int i = 0;

        while(true) {
            if(obj.has(Integer.toString(i))) {
                JsonObject set = obj.getAsJsonObject(Integer.toString(i));

                if(set.has("key") && set.has("val")) {
                    prefix.put(set.get("key").getAsString(), set.get("val").getAsString());
                }

                i++;
            } else {
                break;
            }
        }
    }

    public static JsonObject getJsonFile(String name) {
        File f = new File("./data/"+name+".json");

        if(!f.exists())
            return null;

        try {
            BufferedReader br = new BufferedReader(new FileReader(f));

            JsonElement obj = JsonParser.parseReader(br);

            return obj == null ? null : obj.isJsonObject() ? obj.getAsJsonObject() : null;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void saveServerInfo() {
        JsonObject obj = new JsonObject();

        obj.addProperty("rating", ratingChannel);
        obj.addProperty("serverpre", serverPrefix);
        obj.add("prefix", mapToJson(prefix));

        try {
            File folder = new File("./data/");

            if(!folder.exists()) {
                boolean res = folder.mkdirs();

                if(!res) {
                    System.out.println("Can't create folder " + folder.getAbsolutePath());
                    return;
                }
            }

            File file = new File(folder.getAbsolutePath(), "serverinfo.json");

            if(!file.exists()) {
                boolean res = file.createNewFile();

                if(!res) {
                    System.out.println("Can't create file " + file.getAbsolutePath());
                    return;
                }
            }

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);

            String json = obj.toString();
            JsonNode tree = mapper.readTree(json);

            FileWriter writer = new FileWriter("./data/serverinfo.json");

            writer.append(mapper.writeValueAsString(tree));
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void readServerInfo() {
        JsonObject obj = getJsonFile("serverinfo");

        if(obj != null) {
            if(obj.has("rating")) {
                ratingChannel = obj.get("rating").getAsString();
            }

            if (obj.has("serverpre")) {
                serverPrefix = obj.get("serverpre").getAsString();
            }

            if(obj.has("prefix")) {
                jsonToMap(obj.get("prefix").getAsJsonObject());
            }
        }
    }
}
