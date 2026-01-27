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
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
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
import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class UnitCommandConfigHolder extends ComponentHolder {
    private final ConfigHolder config;
    private final ConfigHolder backup;

    public UnitCommandConfigHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, ConfigHolder config, ConfigHolder backup, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.config = config;
        this.backup = backup;

        registerAutoExpiration(FIVE_MIN);
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "description" -> {
                config.showUnitDescription = !config.showUnitDescription;

                applyResult(event);
            }
            case "evolve" -> {
                config.showEvolveDescription = !config.showEvolveDescription;

                applyResult(event);
            }
            case "image" -> {
                config.showEvolveImage = !config.showEvolveImage;

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

        String iconLink = StaticStore.assetManager.uploadIf("CONFIG-EMBED-UNIT-ICON", new File("./data/bot/defaultAssets/iconExample.png"));

        String healthKb = "10000 - 5";
        String costCooldownSpeed = "5000 - " + (config.useFrame ? "100f" : "100s") + " - 10";
        String attackTimings = (config.useFrame ? "100f" : "100s") + " : " + (config.useFrame ? "50f" : "50s") + " -> " + (config.useFrame ? "20f" : "20s") + " -> " + (config.useFrame ? "50f" : "50s");
        String damageDPS = "123456 [123456]";

        List<ContainerChildComponent> children = new ArrayList<>();

        if (iconLink != null) {
            children.add(Section.of(
                    Thumbnail.fromUrl(iconLink),
                    TextDisplay.of("## " + LangID.getStringByID("config.command.embed.unit.title", lang) + " [123-456]"),
                    TextDisplay.of(
                            "**" + LangID.getStringByID("data.unit.level", lang) + "**\n" +
                                    config.defLevel
                    ),
                    TextDisplay.of(
                            "**" + LangID.getStringByID("data.compact.healthKb", lang) + "**\n" +
                                    healthKb
                    )
            ));
        } else {
            children.add(TextDisplay.of("## " + LangID.getStringByID("config.command.embed.unit.title", lang) + " [123-456]"));

            children.add(TextDisplay.of(
                    "**" + LangID.getStringByID("data.unit.level", lang) + "**\n" +
                            config.defLevel
            ));

            children.add(TextDisplay.of(
                    "**" + LangID.getStringByID("data.compact.healthKb", lang) + "**\n" +
                            healthKb
            ));
        }

        children.add(TextDisplay.of(
                "**" + LangID.getStringByID("data.compact.costCooldownSpeed", lang) + "**\n" +
                        costCooldownSpeed
        ));

        children.add(TextDisplay.of(
                "**" + LangID.getStringByID("data.range", lang) + "**\n400"
        ));

        children.add(TextDisplay.of(
                "**" + LangID.getStringByID("data.compact.attackTimings", lang) + "**\n" +
                        attackTimings
        ));

        children.add(TextDisplay.of(
                "**" + LangID.getStringByID("data.compact.damageDPS", lang).formatted(LangID.getStringByID("data.attackTypes.area", lang)) + "**\n" +
                        damageDPS
        ));

        children.add(TextDisplay.of(
                "**" + LangID.getStringByID("data.trait", lang) + "**\n" +
                        LangID.getStringByID("config.command.embed.unit.trait", lang)
        ));

        children.add(TextDisplay.of(
                "**" + LangID.getStringByID("data.ability", lang) + "**\n" +
                        LangID.getStringByID("config.command.embed.unit.abilities", lang)
        ));

        if(config.showUnitDescription) {
            children.add(TextDisplay.of(
                    "**" + LangID.getStringByID("data.unit.description", lang) + "**\n" +
                            LangID.getStringByID("config.command.embed.unit.description", lang)
            ));
        }

        if (config.showEvolveDescription || config.showEvolveImage) {
            children.add(TextDisplay.of("**" + LangID.getStringByID("data.unit.evolve", lang) + "**"));

            if (config.showEnemyDescription) {
                children.add(TextDisplay.of(LangID.getStringByID("config.command.embed.unit.evolve", lang)));
            }

            if (config.showEvolveImage) {
                String trueFormImage = StaticStore.assetManager.uploadIf("CONFIG-EMBED-UNIT-EVOLVE", new File("./data/bot/defaultAssets/evolveCostExample.png"));

                if (trueFormImage != null) {
                    children.add(MediaGallery.of(MediaGalleryItem.fromUrl(trueFormImage)));
                }
            }
        }

        components.add(Container.of(children).withAccentColor(StaticStore.rainbow[StaticStore.YELLOW]));

        List<ContainerChildComponent> panelComponents = new ArrayList<>();

        panelComponents.add(TextDisplay.of(LangID.getStringByID("config.command.check.unit", lang)));

        panelComponents.add(Separator.create(true, Separator.Spacing.LARGE));

        Emoji showUnitDescription = config.showUnitDescription ? EmojiStore.SWITCHON : EmojiStore.SWITCHOFF;
        Emoji showEvolveDescription = config.showEvolveDescription ? EmojiStore.SWITCHON : EmojiStore.SWITCHOFF;
        Emoji showEvolveImage = config.showEvolveImage ? EmojiStore.SWITCHON : EmojiStore.SWITCHOFF;

        panelComponents.add(ActionRow.of(
                Button.secondary("description", LangID.getStringByID("config.command.button.unit.description", lang)).withEmoji(showUnitDescription)
        ));

        panelComponents.add(ActionRow.of(
                Button.secondary("evolve", LangID.getStringByID("config.command.button.unit.evolve", lang)).withEmoji(showEvolveDescription)
        ));

        panelComponents.add(ActionRow.of(
                Button.secondary("image", LangID.getStringByID("config.command.button.unit.image", lang)).withEmoji(showEvolveImage)
        ));

        panelComponents.add(ActionRow.of(
                Button.secondary("back", LangID.getStringByID("ui.button.back", lang)).withEmoji(EmojiStore.BACK),
                Button.success("confirm", LangID.getStringByID("ui.button.confirm", lang)).withEmoji(EmojiStore.CHECK),
                Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang)).withEmoji(EmojiStore.CROSS)
        ));

        components.add(Container.of(panelComponents));

        return components;
    }
}
