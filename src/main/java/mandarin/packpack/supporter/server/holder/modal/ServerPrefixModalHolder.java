package mandarin.packpack.supporter.server.holder.modal;

import common.CommonStatic;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import org.jetbrains.annotations.NotNull;

public class ServerPrefixModalHolder extends ModalHolder {
    private final ConfigHolder config;

    public ServerPrefixModalHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message, ConfigHolder config, CommonStatic.Lang.Locale lang) {
        super(author, channelID, message, lang);

        this.config = config;
    }

    @Override
    public void onEvent(@NotNull ModalInteractionEvent event) {
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

        config.prefix = value.replaceAll("\\s", "");

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
