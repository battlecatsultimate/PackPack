package mandarin.packpack.supporter.server.data;

import com.google.gson.JsonObject;
import mandarin.packpack.supporter.StaticStore;

import javax.annotation.Nonnull;

public class EventDataConfigHolder implements Cloneable {
    public static EventDataConfigHolder fromJson(JsonObject obj) {
        if (!obj.has("channelID")) {
            return null;
        }

        EventDataConfigHolder data = new EventDataConfigHolder(obj.get("channelID").getAsLong());

        if (obj.has("eventMessage")) {
            data.eventMessage = obj.get("eventMessage").getAsString();
        }

        if (obj.has("eventRaw")) {
            data.eventRaw = obj.get("eventRaw").getAsBoolean();
        }

        if (obj.has("notifyNewVersion")) {
            data.notifyNewVersion = obj.get("notifyNewVersion").getAsBoolean();
        }

        if (obj.has("newVersionMessage")) {
            data.newVersionMessage = obj.get("newVersionMessage").getAsString();
        }

        return data;
    }

    /**
     * ID of the Discord channel where bot will send this event data
     */
    public long channelID;
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
     * Config whether bot will notify new version
     */
    public boolean notifyNewVersion;
    /**
     * The message that will be sent together whenever new version is detected
     */
    @Nonnull
    public String newVersionMessage = "";

    public EventDataConfigHolder(long channelID) {
        this.channelID = channelID;
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();

        obj.addProperty("channelID", channelID);
        obj.addProperty("eventMessage", eventMessage);
        obj.addProperty("eventRaw", eventRaw);
        obj.addProperty("notifyNewVersion", notifyNewVersion);
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
                cloned = new EventDataConfigHolder(channelID);
            }
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/EventDataConfigHolder::clone - Failed to call super.clone()");

            cloned = new EventDataConfigHolder(channelID);
        }

        cloned.eventRaw = eventRaw;
        cloned.notifyNewVersion = notifyNewVersion;
        cloned.eventMessage = eventMessage;
        cloned.newVersionMessage = newVersionMessage;

        return cloned;
    }
}
