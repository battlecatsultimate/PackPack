package mandarin.packpack.supporter.server.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import mandarin.packpack.supporter.StaticStore;

public class BoosterData {
    public enum INITIAL {
        ROLE, EMOJI
    }

    public static BoosterData parseJson(JsonObject obj) {
        long role;

        if(obj.has("role")) {
            JsonElement element = obj.get("role");

            if (element instanceof JsonPrimitive primitive) {
                if (primitive.isString())
                    role = StaticStore.safeParseLong(primitive.getAsString());
                else
                    role = primitive.getAsLong();
            } else {
                role = -1L;
            }
        } else {
            role = -1L;
        }

        long emoji;

        if(obj.has("emoji")) {
            JsonElement element = obj.get("emoji");

            if (element instanceof JsonPrimitive primitive) {
                if (primitive.isString())
                    emoji = StaticStore.safeParseLong(primitive.getAsString());
                else
                    emoji = primitive.getAsLong();
            } else {
                emoji = -1L;
            }
        } else {
            emoji = -1L;
        }

        if(role == -1L && emoji == -1L) {
            StaticStore.logger.uploadLog("W/BoosterData::parseJson - Invalid booster holder data found");
            return null;
        }

        return new BoosterData(role, emoji);
    }

    public static final int ERR_ALREADY_ROLE_SET = -1;
    public static final int ERR_ALREADY_EMOJI_SET = -2;

    private long role = -1L;
    private long emoji = -1L;

    public BoosterData(long id, INITIAL type) throws IllegalStateException {
        switch (type) {
            case ROLE:
                this.role = id;
                break;
            case EMOJI:
                this.emoji = id;
                break;
            default:
                throw new IllegalStateException("Unknown INITIAL type : "+type);
        }
    }

    private BoosterData(long role, long emoji) {
        this.role = role;
        this.emoji = emoji;
    }

    public int setRole(long role) {
        if(this.role != -1L) {
            return ERR_ALREADY_ROLE_SET;
        }

        this.role = role;

        return 0;
    }

    public int setEmoji(long emoji) {
        if(this.emoji != -1L) {
            return ERR_ALREADY_EMOJI_SET;
        }

        this.emoji = emoji;

        return 0;
    }

    public void removeRole() {
        role = -1L;
    }

    public void removeEmoji() {
        emoji = -1L;
    }

    public long getRole() {
        return role;
    }

    public long getEmoji() {
        return emoji;
    }

    public JsonObject jsonfy() {
        JsonObject obj = new JsonObject();

        obj.addProperty("role", role);
        obj.addProperty("emoji", emoji);

        return obj;
    }
}
