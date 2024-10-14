package mandarin.packpack.supporter.server.data;

import com.google.gson.JsonObject;
import javax.annotation.Nonnull;

public class BoosterData {
    public enum INITIAL {
        ROLE, EMOJI
    }

    public static BoosterData parseJson(JsonObject obj) {
        String role;

        if(obj.has("role")) {
            if(obj.get("role").isJsonNull())
                role = null;
            else
                role = obj.get("role").getAsString();
        } else {
            role = null;
        }

        String emoji;

        if(obj.has("emoji")) {
            if(obj.get("emoji").isJsonNull())
                emoji = null;
            else
                emoji = obj.get("emoji").getAsString();
        } else {
            emoji = null;
        }

        if(role == null && emoji == null) {
            System.out.println("W/ Invalid booster holder data found");
            return null;
        }

        return new BoosterData(role, emoji);
    }

    public static final int ERR_ALREADY_ROLE_SET = -1;
    public static final int ERR_ALREADY_EMOJI_SET = -2;

    private String role;
    private String emoji;

    public BoosterData(@Nonnull String id, INITIAL type) throws IllegalStateException {
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

    private BoosterData(String role, String emoji) {
        this.role = role;
        this.emoji = emoji;
    }

    public int setRole(String role) {
        if(this.role != null) {
            return ERR_ALREADY_ROLE_SET;
        }

        this.role = role;

        return 0;
    }

    public int setEmoji(String emoji) {
        if(this.emoji != null) {
            return ERR_ALREADY_EMOJI_SET;
        }

        this.emoji = emoji;

        return 0;
    }

    public void removeRole() {
        role = null;
    }

    public void removeEmoji() {
        emoji = null;
    }

    public String getRole() {
        return role;
    }

    public String getEmoji() {
        return emoji;
    }

    public JsonObject jsonfy() {
        JsonObject obj = new JsonObject();

        obj.addProperty("role", role);
        obj.addProperty("emoji", emoji);

        return obj;
    }
}
