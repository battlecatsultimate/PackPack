package mandarin.packpack.supporter.server.data;

import com.google.gson.JsonObject;
import mandarin.packpack.supporter.StaticStore;

import javax.annotation.Nonnull;

public class EventDataConfigHolder implements Cloneable {
    public static EventDataConfigHolder fromJson(JsonObject obj) {
        if (!obj.has("channelID")) {
            return null;
        }

        EventDataConfigHolder data = new EventDataConfigHolder();

        data.channelID = obj.get("channelID").getAsLong();

        if (obj.has("newVersionChannelID")) {
            data.newVersionChannelID = obj.get("newVersionChannelID").getAsLong();
        } else if (obj.has("notifyNewVersion")) {
            boolean notify = obj.get("notifyNewVersion").getAsBoolean();

            if (notify) {
                data.newVersionChannelID = data.channelID;
            } else {
                data.newVersionChannelID = -1;
            }
        }

        if (obj.has("eventMessage")) {
            data.eventMessage = obj.get("eventMessage").getAsString();
        }

        if (obj.has("eventRaw")) {
            data.eventRaw = obj.get("eventRaw").getAsBoolean();
        }

        if (obj.has("newVersionMessage")) {
            data.newVersionMessage = obj.get("newVersionMessage").getAsString();
        }

        return data;
    }

    /**
     * ID of the Discord channel where bot will send this event data
     */
    public long channelID = -1L;
    /**
     * ID of the Discord channel where bot will notify new version detection
     */
    public long newVersionChannelID = -1L;
    /**
     * The message that will be sent together whenever event data is posted
     */
    @Nonnull
    public String eventMessage = "";
    /**
     * Sorting method for posting event data. Bot will post event data with raw order in the file if
     * this is true
     */
    public boolean eventRaw;
    /**
     * The message that will be sent together whenever new version is detected
     */
    @Nonnull
    public String newVersionMessage = "";

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();

        obj.addProperty("channelID", channelID);
        obj.addProperty("newVersionChannelID", newVersionChannelID);
        obj.addProperty("eventMessage", eventMessage);
        obj.addProperty("eventRaw", eventRaw);
        obj.addProperty("newVersionMessage", newVersionMessage);

        return obj;
    }

    @Override
    protected EventDataConfigHolder clone() throws CloneNotSupportedException {
        EventDataConfigHolder cloned;

        try {
            Object o = super.clone();

            if (o instanceof EventDataConfigHolder e) {
                cloned = e;
            } else {
                cloned = new EventDataConfigHolder();
            }
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/EventDataConfigHolder::clone - Failed to call super.clone()");

            cloned = new EventDataConfigHolder();
        }

        cloned.channelID = channelID;
        cloned.newVersionChannelID = newVersionChannelID;
        cloned.eventRaw = eventRaw;
        cloned.eventMessage = eventMessage;
        cloned.newVersionMessage = newVersionMessage;

        return cloned;
    }
}
