package mandarin.packpack.supporter.server.holder.component.config.user;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.component.ComponentHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CommandListHolder extends ComponentHolder {
    public enum EmbedConfig {
        ENEMY,
        STAGE,
        UNIT
    }

    private final ConfigHolder config;
    private final ConfigHolder backup;

    public CommandListHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, ConfigHolder config, ConfigHolder backup, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.config = config;
        this.backup = backup;

        registerAutoExpiration(FIVE_MIN);
    }


    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "embed" -> {
                if (!(event instanceof StringSelectInteractionEvent e)) {
                    return;
                }

                EmbedConfig c = EmbedConfig.valueOf(e.getValues().getFirst());

                switch (c) {
                    case ENEMY -> connectTo(event, new EnemyCommandConfigHolder(getAuthorMessage(), userID, channelID, message, config, backup, lang));
                    case STAGE -> connectTo(event, new StageCommandConfigHolder(getAuthorMessage(), userID, channelID, message, config, backup, lang));
                    case UNIT -> connectTo(event, new UnitCommandConfigHolder(getAuthorMessage(), userID, channelID, message, config, backup, lang));
                }
            }
            case "back" -> goBack(event);
            case "confirm" -> {
                event.deferEdit()
                        .setContent(LangID.getStringByID("config.applied", lang))
                        .setComponents()
                        .queue();

                if (!StaticStore.config.containsKey(userID)) {
                    StaticStore.config.put(userID, config);
                }

                end(true);
            }
            case "cancel" -> {
                if(StaticStore.config.containsKey(userID)) {
                    StaticStore.config.put(userID, backup);
                }

                event.deferEdit()
                        .setContent(LangID.getStringByID("config.canceled", backup.lang))
                        .setComponents()
                        .queue();

                end(true);
            }
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire() {
        if(StaticStore.config.containsKey(userID)) {
            StaticStore.config.put(userID, backup);
        }

        message.editMessage(LangID.getStringByID("config.expired", lang))
                .setComponents()
                .setEmbeds()
                .mentionRepliedUser(false)
                .queue();
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

    private List<MessageTopLevelComponent> getComponents() {
        List<MessageTopLevelComponent> result = new ArrayList<>();

        List<SelectOption> options = new ArrayList<>();

        EmbedConfig[] values = EmbedConfig.values();

        for (int i = 0; i < values.length; i++) {
            EmbedConfig c = values[i];

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

        EmbedConfig[] values = EmbedConfig.values();

        for (int i = 0; i < values.length; i++) {
            EmbedConfig c = values[i];

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
