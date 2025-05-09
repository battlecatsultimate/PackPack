package mandarin.packpack.supporter.server.holder.modal;

import common.CommonStatic;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;

public class ServerPrefixModalHolder extends ModalHolder {
    private final IDHolder holder;

    public ServerPrefixModalHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, IDHolder holder, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.holder = holder;
    }

    @Override
    public void onEvent(@Nonnull ModalInteractionEvent event) {
        if (!event.getModalId().equals("prefix"))
            return;

        String value = getValueFromMap(event.getValues(), "prefix");

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

        holder.config.prefix = value.replaceAll("\\s", "");

        holder.bannedPrefix.removeIf(prefix -> prefix.toLowerCase(Locale.ENGLISH).equals(holder.config.prefix.toLowerCase(Locale.ENGLISH)));

        event.deferReply()
                .setContent(LangID.getStringByID("serverConfig.general.prefixSuccess", lang).formatted(value))
                .setEphemeral(true)
                .queue();

        goBack();
    }

    @Override
    public void clean() {

    }
}
