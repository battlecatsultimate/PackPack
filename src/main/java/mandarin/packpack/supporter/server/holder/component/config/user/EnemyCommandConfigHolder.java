package mandarin.packpack.supporter.server.holder.component.config.user;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.component.ComponentHolder;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.thumbnail.Thumbnail;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class EnemyCommandConfigHolder extends ComponentHolder {
    private final ConfigHolder config;
    private final ConfigHolder backup;

    public EnemyCommandConfigHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, ConfigHolder config, ConfigHolder backup, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.config = config;
        this.backup = backup;

        registerAutoExpiration(FIVE_MIN);
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "description" -> {
                config.showEnemyDescription = !config.showEnemyDescription;

                applyResult(event);
            }
            case "back" -> goBack(event);
            case "confirm" -> {
                event.deferEdit()
                        .setComponents(TextDisplay.of(LangID.getStringByID("config.applied", lang)))
                        .useComponentsV2()
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
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
                        .setComponents(TextDisplay.of(LangID.getStringByID("config.canceled", backup.lang)))
                        .useComponentsV2()
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
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

        message.editMessageComponents(TextDisplay.of(LangID.getStringByID("config.expired", lang)))
                .useComponentsV2()
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    @Override
    public void onConnected(@NotNull IMessageEditCallback event, @NotNull Holder parent) {
        applyResult(event);
    }

    private void applyResult(IMessageEditCallback event) {
        event.deferEdit()
                .setComponents(getComponents())
                .useComponentsV2()
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private List<MessageTopLevelComponent> getComponents() {
        List<MessageTopLevelComponent> components = new ArrayList<>();

        List<ContainerChildComponent> containerComponents = new ArrayList<>();

        String iconLink = StaticStore.assetManager.uploadIf("CONFIG-EMBED-ENEMY-ICON", new File("./data/bot/defaultAssets/enemyIconExample.png"));

        String healthKb = "10000 - 10";
        String dropBarrierSpeed = "1000 - " + LangID.getStringByID("data.none", lang) + " - 10";
        String attackTimings = (config.useFrame ? "100f" : "100s") + " : " + (config.useFrame ? "50f" : "50s") + " -> " + (config.useFrame ? "20f" : "20s") + " -> " + (config.useFrame ? "50f" : "50s");
        String damageDPS = "123456 [123456]";

        if (iconLink != null) {
            containerComponents.add(Section.of(
                    Thumbnail.fromUrl(iconLink),
                    TextDisplay.of("## " + LangID.getStringByID("config.command.embed.enemy.title", lang) + "[123]"),
                    TextDisplay.of(
                            "**" + LangID.getStringByID("data.enemy.magnification", lang) + "**\n" +
                                    "100%"
                    ),
                    TextDisplay.of(
                            "**" + LangID.getStringByID("data.compact.healthKb", lang) + "**\n" +
                                    healthKb
                    )
            ));
        } else {
            containerComponents.add(TextDisplay.of("## " + LangID.getStringByID("config.command.embed.enemy.title", lang) + "[123]"));

            containerComponents.add(TextDisplay.of(
                    "**" + LangID.getStringByID("data.enemy.magnification", lang) + "**\n" +
                            "100%"
            ));

            containerComponents.add(TextDisplay.of(
                    "**" + LangID.getStringByID("data.compact.healthKb", lang) + "**\n" +
                            healthKb
            ));
        }

        containerComponents.add(TextDisplay.of(
                "**" + LangID.getStringByID("data.compact.dropBarrierSpeed", lang) + "**\n" +
                        dropBarrierSpeed
        ));

        containerComponents.add(TextDisplay.of(
                "**" + LangID.getStringByID("data.range", lang) + "**\n" +
                        "400"
        ));

        containerComponents.add(TextDisplay.of(
                "**" + LangID.getStringByID("data.compact.attackTimings", lang) + "**\n" +
                        attackTimings
        ));

        containerComponents.add(TextDisplay.of(
                "**" + LangID.getStringByID("data.compact.damageDPS", lang).formatted(LangID.getStringByID("data.attackTypes.area", lang)) + "**\n" +
                        damageDPS
        ));

        containerComponents.add(TextDisplay.of(
                "**" + LangID.getStringByID("data.trait", lang) + "**\n" +
                        LangID.getStringByID("config.command.embed.unit.trait", lang)
        ));

        containerComponents.add(TextDisplay.of(
                "**" + LangID.getStringByID("data.ability", lang) + "**\n" +
                        LangID.getStringByID("config.command.embed.unit.abilities", lang)
        ));

        if(config.showEnemyDescription) {
            containerComponents.add(Separator.create(true, Separator.Spacing.LARGE));

            containerComponents.add(TextDisplay.of(
                    "**" + LangID.getStringByID("data.enemy.description", lang) + "**\n" +
                            LangID.getStringByID("config.command.embed.enemy.description", lang)
            ));
        }

        components.add(Container.of(containerComponents).withAccentColor(StaticStore.rainbow[0]));

        components.add(Separator.create(false, Separator.Spacing.LARGE));

        List<ContainerChildComponent> buttonPanelComponents = new ArrayList<>();

        buttonPanelComponents.add(TextDisplay.of(LangID.getStringByID("config.command.check.enemy", lang)));

        buttonPanelComponents.add(Separator.create(true, Separator.Spacing.LARGE));

        Emoji showEnemyDescription = config.showEnemyDescription ? EmojiStore.SWITCHON : EmojiStore.SWITCHOFF;

        buttonPanelComponents.add(ActionRow.of(
                Button.secondary("description", LangID.getStringByID("config.command.button.enemy.description", lang)).withEmoji(showEnemyDescription)
        ));

        buttonPanelComponents.add(Separator.create(false, Separator.Spacing.SMALL));

        buttonPanelComponents.add(ActionRow.of(
                Button.secondary("back", LangID.getStringByID("ui.button.back", lang)).withEmoji(EmojiStore.BACK),
                Button.success("confirm", LangID.getStringByID("ui.button.confirm", lang)).withEmoji(EmojiStore.CHECK),
                Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang)).withEmoji(EmojiStore.CROSS)
        ));

        components.add(Container.of(buttonPanelComponents));

        return components;
    }
}
