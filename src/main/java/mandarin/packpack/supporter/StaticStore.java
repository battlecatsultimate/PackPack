package mandarin.packpack.supporter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import common.CommonStatic;
import common.io.assets.UpdateCheck;
import common.util.lang.MultiLangCont;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Role;
import discord4j.rest.util.Color;
import mandarin.packpack.supporter.server.*;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public class StaticStore {
    public static String ratingChannel = "";

    public static boolean initialized = false;

    public static String serverPrefix = "p!";

    public static Map<String, String> prefix = new HashMap<>();
    public static Map<String, String> langs = new HashMap<>();
    public static Map<String, Integer> locales = new HashMap<>();

    private static Map<String, Holder> holders = new HashMap<>();

    public static ImgurDataHolder imgur = new ImgurDataHolder(null);

    public static Map<String, TimeBoolean> canDo = new HashMap<>();

    public static final MultiLangCont<Integer, String> MEDNAME = new MultiLangCont<>();
    public static final MultiLangCont<Integer, String> MEDEXP = new MultiLangCont<>();

    public static final Map<String, Long> timeLimit = new HashMap<>();

    public static Timer saver = null;

    public static Color[] rainbow = {Color.of(217, 65, 68), Color.of(217, 128, 65), Color.of(224, 213, 85)
    , Color.of(118, 224, 85), Color.of(85, 169, 224), Color.of(185, 85, 224)};

    public static Color[] coolHot = {
            Color.of(52, 147, 235),
            Color.of(38, 189, 178),
            Color.of(51, 196, 106),
            Color.of(186, 219, 53),
            Color.of(245, 217, 42),
            Color.of(240, 140, 53),
            Color.of(240, 112, 60),
            Color.of(222, 51, 60)
    };

    public static boolean checkingBCU = false;

    public static final Random random = new Random();
    public static final BigInteger max = new BigInteger(Integer.toString(Integer.MAX_VALUE));

    public static final String MANDARIN_SMELL = "460409259021172781";

    public static String downPack = "./pack/download";
    public static String tempPack = "./pack/download";

    public static final String ERROR_MSG = "`INTERNAL_ERROR`";

    public static Map<String, IDHolder> idHolder = new HashMap<>();

    public static final String BCU_SERVER = "490262537527623692";
    public static final String BCU_KR_SERVER = "679858366389944409";

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

        if(list[0].toLowerCase(Locale.ENGLISH).startsWith(prefix.toLowerCase(Locale.ENGLISH)))
            return list[0].toLowerCase(Locale.ENGLISH).replaceFirst(Pattern.quote(prefix.toLowerCase(Locale.ENGLISH)), "");
        else
            return "";
    }

    public static JsonObject mapToJsonIDHolder(Map<String, IDHolder> map) {
        JsonObject obj = new JsonObject();

        int i = 0;

        for(String key : map.keySet()) {
            IDHolder value = map.get(key);

            if(value == null) {
                System.out.println("Warning! : Key "+key+" returns null!");
                continue;
            }

            JsonObject id = new JsonObject();

            id.add("val", value.jsonfy());
            id.addProperty("key", key);

            obj.add(Integer.toString(i), id);

            i++;
        }

        return obj;
    }

    public static Map<String, IDHolder> jsonToMapIDHolder(JsonObject obj) {
        Map<String, IDHolder> map = new HashMap<>();

        int i = 0;

        while(true) {
            if(obj.has(Integer.toString(i))) {
                JsonObject id = obj.get(Integer.toString(i)).getAsJsonObject();

                if(id.has("val") && id.has("key")) {
                    JsonObject val = id.getAsJsonObject("val");
                    String key = id.get("key").getAsString();

                    IDHolder holder = IDHolder.jsonToIDHolder(val);

                    map.put(key, holder);
                }

                i++;
            } else {
                break;
            }
        }

        return map;
    }

    public static JsonObject mapToJsonString(Map<String, String> map) {
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

    public static JsonObject mapToJsonInt(Map<String, Integer> map) {
        JsonObject obj = new JsonObject();

        int i = 0;

        for(String key : map.keySet()) {
            int value = map.get(key);

            JsonObject set = new JsonObject();

            set.addProperty("key", key);
            set.addProperty("val", value);

            obj.add(Integer.toString(i), set);

            i++;
        }

        return obj;
    }

    public static Map<String, String> jsonToMapString(JsonObject obj) {
        Map<String, String> map = new HashMap<>();

        int i = 0;

        while(true) {
            if(obj.has(Integer.toString(i))) {
                JsonObject set = obj.getAsJsonObject(Integer.toString(i));

                if(set.has("key") && set.has("val")) {
                    map.put(set.get("key").getAsString(), set.get("val").getAsString());
                }

                i++;
            } else {
                break;
            }
        }

        return map;
    }

    public static Map<String, Integer> jsonToMapInt(JsonObject obj) {
        Map<String, Integer> map = new HashMap<>();

        int i = 0;

        while(true) {
            if(obj.has(Integer.toString(i))) {
                JsonObject set = obj.getAsJsonObject(Integer.toString(i));

                if(set.has("key") && set.has("val")) {
                    map.put(set.get("key").getAsString(), set.get("val").getAsInt());
                }

                i++;
            } else {
                break;
            }
        }

        return map;
    }

    public static JsonObject getJsonFile(String name) {
        File f = new File("./data/"+name+".json");

        if(!f.exists())
            return null;

        try {
            InputStreamReader reader = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(reader);

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
        obj.add("prefix", mapToJsonString(prefix));
        obj.add("lang", mapToJsonString(langs));
        obj.add("locale", mapToJsonInt(locales));
        obj.add("imgur", imgur.getData());
        obj.add("idholder", mapToJsonIDHolder(idHolder));

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
                prefix = jsonToMapString(obj.get("prefix").getAsJsonObject());
            }

            if(obj.has("lang")) {
                langs = jsonToMapString(obj.get("lang").getAsJsonObject());
            }

            if(obj.has("locale")) {
                locales = jsonToMapInt(obj.get("locale").getAsJsonObject());
            }

            if(obj.has("imgur")) {
                imgur = new ImgurDataHolder(obj.getAsJsonObject("imgur"));
            }

            if(obj.has("idholder")) {
                idHolder = jsonToMapIDHolder(obj.getAsJsonObject("idholder"));
            }
        }
    }

    public static void deleteFile(File f, boolean selfDelete) {
        if(f.isFile()) {
            boolean res = f.delete();

            if(!res) {
                System.out.println("Failed to delete file : "+f.getAbsolutePath());
            }
        } else if(f.isDirectory()) {
            File[] files = f.listFiles();

            if(files != null) {
                for(File g : files) {
                    if(g.isFile()) {
                        boolean res = g.delete();

                        if(!res) {
                            System.out.println("Failed to delete file : "+g.getAbsolutePath());
                        }
                    } else if(g.isDirectory()) {
                        deleteFile(g, true);
                    }
                }
            }

            if(selfDelete) {
                boolean res = f.delete();

                if(!res) {
                    System.out.println("Failed to delete folder : "+f.getAbsolutePath());
                }
            }
        }
    }

    public static String findFileName(File folder, String name, String extension) {
        int n = 0;

        while(true) {
            String fileName;

            if(n == 0)
                fileName = name+extension;
            else
                fileName = name+"_"+n+extension;

            File test = new File(folder, fileName);

            if(!test.exists())
                return fileName;
            else
                n++;
        }
    }

    public static boolean isNumeric(String name) {
        for(int i = 0; i < name.length(); i++) {
            if(!Character.isDigit(name.charAt(i)))
                return false;
        }

        return true;
    }

    public static int safeParseInt(String value) {
        if(isNumeric(value)) {
            BigInteger big = new BigInteger(value);

            if(big.compareTo(max) > 0) {
                return Integer.MAX_VALUE;
            } else {
                return Integer.parseInt(value);
            }
        } else {
            throw new IllegalStateException("Value isn't numeric! : "+value);
        }
    }

    public static String fileToMD5(File f) {
        if(!f.exists() || f.isDirectory())
            return null;

        try {
            FileInputStream fis = new FileInputStream(f);

            byte[] buffer = new byte[65536];

            MessageDigest md5 = MessageDigest.getInstance("MD5");
            int n;

            while((n = fis.read(buffer)) != -1) {
                md5.update(buffer, 0, n);
            }

            byte[] result = md5.digest();

            BigInteger big = new BigInteger(1, result);

            String str = big.toString(16);

            return String.format("%32s", str).replace(' ', '0');
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String safeMultiLangGet(Object any, int lang) {
        int oldConfig = CommonStatic.getConfig().lang;
        CommonStatic.getConfig().lang = lang;

        String res = MultiLangCont.get(any);

        CommonStatic.getConfig().lang = oldConfig;

        return res;
    }

    public static UpdateCheck.Downloader getDownloader(Attachment att, File container) {
        if(!container.exists() || container.isFile())
            return null;

        String url = att.getUrl();

        File target = new File(container, att.getFilename());
        File temp = new File(container, att.getFilename()+".tmp");

        return new UpdateCheck.Downloader(url, target, temp, "", false);
    }

    public static String getRoleIDByName(String name, Guild g) {
        AtomicReference<String> id = new AtomicReference<>(null);

        g.getRoles().collectList().subscribe(l -> {
            for(Role r : l) {
                if(r.getName().equals(name)) {
                    id.set(r.getId().asString());
                    return;
                }
            }
        });

        return id.get();
    }

    synchronized public static void putHolder(String id, Holder holder) {
        Holder oldHolder = holders.get(id);

        if(oldHolder != null) {
            oldHolder.expire(id);
        }

        holders.put(id, holder);
    }

    synchronized public static void removeHolder(String id, Holder holder) {
        Holder thisHolder = holders.get(id);

        if(thisHolder != null && thisHolder.equals(holder))
            holders.remove(id);
    }

    synchronized public static Holder getHolder(String id) {
        return holders.get(id);
    }

    public static boolean holderContainsKey(String id) {
        return holders.containsKey(id);
    }
}
