package mandarin.packpack.supporter.server.holder.modal;

import common.CommonStatic;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Locale;

public class PrefixBanHolder extends ModalHolder {
    private final IDHolder holder;

    public PrefixBanHolder(@Nullable Message author, @NotNull String userID, @NotNull String channelID, @NotNull Message message, @NotNull CommonStatic.Lang.Locale lang, IDHolder holder) {
        super(author, userID, channelID, message, lang);

        this.holder = holder;
    }

    @Override
    public void clean() {

    }

    @Override
    public void onEvent(@NotNull ModalInteractionEvent event) {
        if (!event.getModalId().equals("prefixBan")) {
            return;
        }

        String value = getValueFromMap(event.getValues(), "prefix").trim();

        if (value.isBlank()) {
            event.deferReply()
                    .setContent(LangID.getStringByID("serverConfig.general.noWhiteSpace", lang))
                    .setEphemeral(true)
                    .queue();

            return;
        }

        if (value.toLowerCase(Locale.ENGLISH).equals(StaticStore.globalPrefix)) {
            event.deferReply()
                    .setContent(LangID.getStringByID("serverConfig.general.noGlobalPrefix", lang).formatted(StaticStore.globalPrefix))
                    .setEphemeral(true)
                    .queue();

            return;
        }

        if (value.toLowerCase(Locale.ENGLISH).equals(holder.config.prefix.toLowerCase(Locale.ENGLISH))) {
            event.deferReply()
                    .setContent(LangID.getStringByID("serverConfig.general.noServerPrefix", lang).formatted(holder.config.prefix))
                    .setAllowedMentions(new ArrayList<>())
                    .setEphemeral(true)
                    .queue();

            return;
        }

        if (holder.bannedPrefix.contains(value)) {
            event.deferReply()
                    .setContent(LangID.getStringByID("serverConfig.general.prefixContain", lang))
                    .setEphemeral(true)
                    .queue();

            return;
        }

        holder.bannedPrefix.add(value.toLowerCase(Locale.ENGLISH));

        event.deferReply()
                .setContent(LangID.getStringByID("serverConfig.general.prefixDisallowed", lang))
                .setEphemeral(true)
                .queue();

        goBack();
    }
}
