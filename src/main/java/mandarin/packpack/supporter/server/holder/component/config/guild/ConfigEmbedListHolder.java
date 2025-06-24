package mandarin.packpack.supporter.server.holder.component.config.guild;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder;
import mandarin.packpack.supporter.server.holder.component.config.user.CommandListHolder;
import mandarin.packpack.supporter.server.holder.component.config.user.EnemyCommandConfigHolder;
import mandarin.packpack.supporter.server.holder.component.config.user.StageCommandConfigHolder;
import mandarin.packpack.supporter.server.holder.component.config.user.UnitCommandConfigHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ConfigEmbedListHolder extends ServerConfigHolder {

    public ConfigEmbedListHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, @Nonnull IDHolder holder, @Nonnull IDHolder backup, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, holder, backup, lang);
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "embed" -> {
                if (!(event instanceof StringSelectInteractionEvent e)) {
                    return;
                }

                CommandListHolder.EmbedConfig c = CommandListHolder.EmbedConfig.valueOf(e.getValues().getFirst());

                switch (c) {
                    case ENEMY -> connectTo(event, new ConfigEnemyEmbedHolder(getAuthorMessage(), userID, channelID, message, holder, backup, lang));
                    case STAGE -> connectTo(event, new ConfigStageEmbedHolder(getAuthorMessage(), userID, channelID, message, holder, backup, lang));
                    case UNIT -> connectTo(event, new ConfigUnitEmbedHolder(getAuthorMessage(), userID, channelID, message, holder, backup, lang));
                }
            }
            case "back" -> goBack(event);
            case "confirm" -> {
                event.deferEdit()
                        .setContent(LangID.getStringByID("serverConfig.applied", lang))
                        .setComponents()
                        .setEmbeds()
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .queue();

                end(true);
            }
            case "cancel" -> {
                registerPopUp(event, LangID.getStringByID("serverConfig.cancelConfirm", lang));

                connectTo(new ConfirmPopUpHolder(getAuthorMessage(), userID, channelID, message, e -> {
                    e.deferEdit()
                            .setContent(LangID.getStringByID("serverConfig.canceled", lang))
                            .setComponents()
                            .setEmbeds()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .queue();

                    holder.inject(backup);

                    end(true);
                }, lang));
            }
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onConnected(@NotNull IMessageEditCallback event, @NotNull Holder parent) {
        applyResult(event);
    }

    @Override
    public void onBack(@NotNull IMessageEditCallback event, @NotNull Holder child) {
        applyResult(event);
    }

    private void applyResult(IMessageEditCallback event) {
        event.deferEdit()
                .setContent(getContents())
                .setComponents(getComponents())
                .setEmbeds()
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private List<LayoutComponent> getComponents() {
        List<LayoutComponent> result = new ArrayList<>();

        List<SelectOption> options = new ArrayList<>();

        CommandListHolder.EmbedConfig[] values = CommandListHolder.EmbedConfig.values();

        for (int i = 0; i < values.length; i++) {
            CommandListHolder.EmbedConfig c = values[i];

            String label = switch (c) {
                case ENEMY -> LangID.getStringByID("config.commandList.list.type.menu.enemy", lang);
                case STAGE -> LangID.getStringByID("config.commandList.list.type.menu.stage", lang);
                case UNIT -> LangID.getStringByID("config.commandList.list.type.menu.unit", lang);
            };

            options.add(SelectOption.of(label.formatted(i + 1), c.name()));
        }

        result.add(ActionRow.of(
                StringSelectMenu.create("embed")
                        .addOptions(options)
                        .setPlaceholder(LangID.getStringByID("config.commandList.list.placeholder", lang))
                        .build()
        ));

        result.add(ActionRow.of(
                Button.secondary("back", LangID.getStringByID("ui.button.back", lang)).withEmoji(EmojiStore.BACK),
                Button.success("confirm", LangID.getStringByID("ui.button.confirm", lang)).withEmoji(EmojiStore.CHECK),
                Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang)).withEmoji(EmojiStore.CROSS)
        ));

        return result;
    }

    private String getContents() {
        StringBuilder builder = new StringBuilder();

        builder.append(LangID.getStringByID("config.commandList.title", lang)).append("\n\n").append(LangID.getStringByID("config.commandList.description", lang)).append("\n\n");

        builder.append(LangID.getStringByID("config.commandList.list.title", lang)).append("\n\n");

        CommandListHolder.EmbedConfig[] values = CommandListHolder.EmbedConfig.values();

        for (int i = 0; i < values.length; i++) {
            CommandListHolder.EmbedConfig c = values[i];

            switch (c) {
                case ENEMY -> builder.append(i + 1).append(". ").append(LangID.getStringByID("config.commandList.list.type.text.enemy", lang));
                case STAGE -> builder.append(i + 1).append(". ").append(LangID.getStringByID("config.commandList.list.type.text.stage", lang));
                case UNIT -> builder.append(i + 1).append(". ").append(LangID.getStringByID("config.commandList.list.type.text.unit", lang));
            }

            builder.append("\n");
        }

        return builder.toString();
    }
}
