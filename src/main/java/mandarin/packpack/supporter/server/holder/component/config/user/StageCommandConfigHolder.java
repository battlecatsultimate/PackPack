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
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
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

public class StageCommandConfigHolder extends ComponentHolder {
    private final ConfigHolder config;
    private final ConfigHolder backup;

    public StageCommandConfigHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, ConfigHolder config, ConfigHolder backup, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.config = config;
        this.backup = backup;

        registerAutoExpiration(FIVE_MIN);
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "misc" -> {
                config.showMiscellaneous = !config.showMiscellaneous;

                applyResult(event);
            }
            case "extra" -> {
                config.showExtraStage = !config.showExtraStage;

                applyResult(event);
            }
            case "material" -> {
                config.showMaterialDrop = !config.showMaterialDrop;

                applyResult(event);
            }
            case "drop" -> {
                config.showDropInfo = !config.showDropInfo;

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

        String schemeLink = StaticStore.assetManager.uploadIf("CONFIG-EMBED-STAGE-SCHEME", new File("./data/bot/defaultAssets/stageSchemeExample.png"));

        List<ContainerChildComponent> children = new ArrayList<>();

        String idDifficultyLevel = "ID-123-456 - ★1 - " + EmojiStore.CROWN_ON.getFormatted() + EmojiStore.CROWN_OFF.getFormatted().repeat(3) + " (100%)";
        String energyBaseXP = "100 - 10000 - 1000";
        String limitContinuableLength = "10 - " + LangID.getStringByID("data.true", lang) + " - 3000";
        String musicBackgroundCastle = "033 - 000 - 000";
        String guardBarrier = LangID.getStringByID("data.active", lang);

        children.add(TextDisplay.of("### " + LangID.getStringByID("config.command.embed.stage.title", lang)));

        children.add(TextDisplay.of(
                "**" + LangID.getStringByID("data.compact.idDifficultyLevel", lang) + "**\n" +
                        idDifficultyLevel
        ));

        children.add(TextDisplay.of(
                "**" + LangID.getStringByID("data.compact.energyBaseXP", lang) + "**\n" +
                        energyBaseXP
        ));

        children.add(TextDisplay.of(
                "**" + LangID.getStringByID("data.compact.limitContinuableLength", lang) + "**\n" +
                        limitContinuableLength
        ));

        children.add(TextDisplay.of(
                "**" + LangID.getStringByID("data.compact.musicBackgroundCastle", lang).formatted(0) + "**\n" +
                        musicBackgroundCastle
        ));

        children.add(TextDisplay.of(
                "**" + LangID.getStringByID("data.stage.guardBarrier", lang) + "**\n" +
                        guardBarrier
        ));

        if(config.showMiscellaneous) {
            children.add(TextDisplay.of(
                    "**" + LangID.getStringByID("data.stage.misc.title", lang) + "**\n" +
                            LangID.getStringByID("config.command.embed.stage.miscellaneous", lang)
            ));
        }

        if (config.showExtraStage) {
            children.add(TextDisplay.of(
                    "**" + LangID.getStringByID("data.stage.misc.exStage", lang) + "**\n" +
                            LangID.getStringByID("config.command.embed.stage.extra", lang)
            ));
        }

        if (config.showMaterialDrop) {
            children.add(TextDisplay.of(
                    "**" + LangID.getStringByID("data.stage.material.title", lang) + "**\n" +
                            LangID.getStringByID("config.command.embed.stage.material", lang)
            ));
        }

        if (config.showDropInfo) {
            children.add(TextDisplay.of(
                    "**" + LangID.getStringByID("data.stage.reward.type.chance.normal", lang) + "**\n" +
                            LangID.getStringByID("config.command.embed.stage.drop" , lang)
            ));

            children.add(TextDisplay.of(
                    "**" + LangID.getStringByID("data.stage.reward.type.score", lang) + "**\n" +
                            LangID.getStringByID("config.command.embed.stage.score", lang)
            ));
        }

        if(schemeLink != null) {
            children.add(Separator.create(true, Separator.Spacing.LARGE));
            children.add(TextDisplay.of("**" + LangID.getStringByID("data.stage.scheme", lang) + "**"));
            children.add(MediaGallery.of(MediaGalleryItem.fromUrl(schemeLink)));
        }

        children.add(TextDisplay.of("-# " + LangID.getStringByID("data.compact.minimumRespawn", lang).formatted(config.useFrame ? "1f" : "1s")));

        components.add(Container.of(children).withAccentColor(StaticStore.rainbow[0]));

        List<ContainerChildComponent> panelComponents = new ArrayList<>();

        panelComponents.add(TextDisplay.of(LangID.getStringByID("config.command.check.stage", lang)));

        panelComponents.add(Separator.create(true, Separator.Spacing.LARGE));

        Emoji showMiscellaneous = config.showMiscellaneous ? EmojiStore.SWITCHON : EmojiStore.SWITCHOFF;
        Emoji showExtraStage = config.showExtraStage ? EmojiStore.SWITCHON : EmojiStore.SWITCHOFF;
        Emoji showMaterialDrop = config.showMaterialDrop ? EmojiStore.SWITCHON : EmojiStore.SWITCHOFF;
        Emoji showDropInfo = config.showDropInfo ? EmojiStore.SWITCHON : EmojiStore.SWITCHOFF;


        panelComponents.add(ActionRow.of(
                Button.secondary("misc", LangID.getStringByID("config.command.button.stage.miscellaneous", lang)).withEmoji(showMiscellaneous)
        ));

        panelComponents.add(ActionRow.of(
                Button.secondary("extra", LangID.getStringByID("config.command.button.stage.extra", lang)).withEmoji(showExtraStage)
        ));

        panelComponents.add(ActionRow.of(
                Button.secondary("material", LangID.getStringByID("config.command.button.stage.material", lang)).withEmoji(showMaterialDrop)
        ));

        panelComponents.add(ActionRow.of(
                Button.secondary("drop", LangID.getStringByID("config.command.button.stage.drop", lang)).withEmoji(showDropInfo)
        ));

        panelComponents.add(Separator.create(false, Separator.Spacing.SMALL));

        panelComponents.add(ActionRow.of(
                Button.secondary("back", LangID.getStringByID("ui.button.back", lang)).withEmoji(EmojiStore.BACK),
                Button.success("confirm", LangID.getStringByID("ui.button.confirm", lang)).withEmoji(EmojiStore.CHECK),
                Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang)).withEmoji(EmojiStore.CROSS)
        ));

        components.add(Container.of(panelComponents));

        return components;
    }
}
