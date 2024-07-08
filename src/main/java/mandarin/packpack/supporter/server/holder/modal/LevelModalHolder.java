package mandarin.packpack.supporter.server.holder.modal;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public class LevelModalHolder extends ModalHolder {
    private final ConfigHolder config;
    private final Consumer<ModalInteractionEvent> editor;

    public LevelModalHolder(Message author, Message msg, String channelID, ConfigHolder config, Consumer<ModalInteractionEvent> editor) {
        super(author, channelID, msg);

        this.config = config;
        this.editor = editor;
    }

    @Override
    public void onEvent(@Nonnull ModalInteractionEvent event) {
        String value = getValueFromMap(event.getValues(), "level");

        if(!StaticStore.isNumeric(value)) {
            event.reply(LangID.getStringByID("config_nonumber", config.lang)).setEphemeral(true).queue();
        } else {
            int level = StaticStore.safeParseInt(value);

            if(level < 1 || level > 60) {
                event.reply(LangID.getStringByID("config_levelrange", config.lang)).setEphemeral(true).queue();
            } else {
                config.defLevel = StaticStore.safeParseInt(value);

                editor.accept(event);
            }
        }

        expire(userID);
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire(String id) {

    }
}
