package mandarin.packpack.supporter.server.holder.modal;

import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class CustomRoleNameModalHolder extends ModalHolder {
    private final IDHolder holder;
    private final int lang;

    private final Consumer<String> onSelected;

    public CustomRoleNameModalHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message, @NotNull IDHolder holder, int lang, Consumer<String> onSelected) {
        super(author, channelID, message);

        this.holder = holder;
        this.lang = lang;

        this.onSelected = onSelected;
    }

    @Override
    public void onEvent(@NotNull ModalInteractionEvent event) {
        if (!event.getModalId().equals("register"))
            return;

        String value = getValueFromMap(event.getValues(), "name");

        if (value.trim().isBlank()) {
            event.deferReply()
                    .setContent(LangID.getStringByID("sercon_empty", lang))
                    .setEphemeral(true)
                    .queue();

            return;
        }

        if (value.matches("(.+)?http(s)?://(.+)?")) {
            event.deferReply()
                    .setContent(LangID.getStringByID("sercon_nourl", lang))
                    .setEphemeral(true)
                    .queue();

            return;
        }

        if (holder.ID.containsKey(value)) {
            String id = holder.ID.get(value);

            if (id != null) {
                event.deferReply()
                        .setContent(LangID.getStringByID("sercon_customsamename", lang).formatted("<@&" + id + ">"))
                        .setEphemeral(true)
                        .queue();

                return;
            }
        }

        onSelected.accept(value);

        event.deferReply()
                .setContent(LangID.getStringByID("sercon_customsuc", lang))
                .setEphemeral(true)
                .queue();

        goBack();
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire(String id) {

    }
}
