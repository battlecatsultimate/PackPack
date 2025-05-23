package mandarin.packpack.supporter.server.holder.modal;

import common.CommonStatic;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

public class LevelModalHolder extends ModalHolder {
    private final ConfigHolder config;
    private final Consumer<ModalInteractionEvent> editor;

    public LevelModalHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, ConfigHolder config, Consumer<ModalInteractionEvent> editor, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.config = config;
        this.editor = editor;
    }

    @Override
    public void onEvent(@Nonnull ModalInteractionEvent event) {
        String value = getValueFromMap(event.getValues(), "level");

        if(!StaticStore.isNumeric(value)) {
            event.reply(LangID.getStringByID("config.defaultLevel.set.notNumber", config.lang)).setEphemeral(true).queue();
        } else {
            int level = StaticStore.safeParseInt(value);

            if(level < 1 || level > 60) {
                event.reply(LangID.getStringByID("config.defaultLevel.set.invalidRange", config.lang)).setEphemeral(true).queue();
            } else {
                config.defLevel = StaticStore.safeParseInt(value);

                editor.accept(event);
            }
        }
    }

    @Override
    public void clean() {

    }
}
