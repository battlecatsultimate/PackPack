package mandarin.packpack.supporter.server.holder.modal;

import common.CommonStatic;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

public class CustomRoleNameModalHolder extends ModalHolder {
    private final IDHolder holder;

    private final Consumer<String> onSelected;

    public CustomRoleNameModalHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, @Nonnull IDHolder holder, CommonStatic.Lang.Locale lang, Consumer<String> onSelected) {
        super(author, userID, channelID, message, lang);

        this.holder = holder;

        this.onSelected = onSelected;
    }

    @Override
    public void onEvent(@Nonnull ModalInteractionEvent event) {
        if (!event.getModalId().equals("register"))
            return;

        String value = getValueFromMap(event.getValues(), "name");

        if (value.trim().isBlank()) {
            event.deferReply()
                    .setContent(LangID.getStringByID("serverConfig.general.noWhiteSpace", lang))
                    .setEphemeral(true)
                    .queue();

            return;
        }

        if (value.matches("(.+)?http(s)?://(.+)?")) {
            event.deferReply()
                    .setContent(LangID.getStringByID("serverConfig.general.noURL", lang))
                    .setEphemeral(true)
                    .queue();

            return;
        }

        if (holder.ID.containsKey(value)) {
            String id = holder.ID.get(value);

            if (id != null) {
                event.deferReply()
                        .setContent(LangID.getStringByID("serverConfig.general.custom.nameRegistered", lang).formatted("<@&" + id + ">"))
                        .setEphemeral(true)
                        .queue();

                return;
            }
        }

        onSelected.accept(value);

        event.deferReply()
                .setContent(LangID.getStringByID("serverConfig.general.custom.success", lang))
                .setEphemeral(true)
                .queue();

        goBack();
    }

    @Override
    public void clean() {

    }
}
