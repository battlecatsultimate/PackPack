package mandarin.packpack.supporter.server.holder.component.config.user;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.TreasureHolder;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.component.ComponentHolder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
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
                        .setContent(LangID.getStringByID("config.applied", lang))
                        .setComponents()
                        .setEmbeds()
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
                        .setEmbeds()
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

    private void applyResult(IMessageEditCallback event) {
        event.deferEdit()
                .setContent(getContents())
                .setEmbeds(getEmbed())
                .setComponents(getComponents())
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private List<MessageTopLevelComponent> getComponents() {
        List<MessageTopLevelComponent> result = new ArrayList<>();

        Emoji showMiscellaneous = config.showMiscellaneous ? EmojiStore.SWITCHON : EmojiStore.SWITCHOFF;
        Emoji showExtraStage = config.showExtraStage ? EmojiStore.SWITCHON : EmojiStore.SWITCHOFF;
        Emoji showMaterialDrop = config.showMaterialDrop ? EmojiStore.SWITCHON : EmojiStore.SWITCHOFF;
        Emoji showDropInfo = config.showDropInfo ? EmojiStore.SWITCHON : EmojiStore.SWITCHOFF;


        result.add(ActionRow.of(
                Button.secondary("misc", LangID.getStringByID("config.command.button.stage.miscellaneous", lang)).withEmoji(showMiscellaneous)
        ));

        result.add(ActionRow.of(
                Button.secondary("extra", LangID.getStringByID("config.command.button.stage.extra", lang)).withEmoji(showExtraStage)
        ));

        result.add(ActionRow.of(
                Button.secondary("material", LangID.getStringByID("config.command.button.stage.material", lang)).withEmoji(showMaterialDrop)
        ));

        result.add(ActionRow.of(
                Button.secondary("drop", LangID.getStringByID("config.command.button.stage.drop", lang)).withEmoji(showDropInfo)
        ));

        result.add(ActionRow.of(
                Button.secondary("back", LangID.getStringByID("ui.button.back", lang)).withEmoji(EmojiStore.BACK),
                Button.success("confirm", LangID.getStringByID("ui.button.confirm", lang)).withEmoji(EmojiStore.CHECK),
                Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang)).withEmoji(EmojiStore.CROSS)
        ));

        return result;
    }

    private String getContents() {
        return LangID.getStringByID("config.command.check.stage", lang);
    }

    private MessageEmbed getEmbed() {
        EmbedBuilder builder = new EmbedBuilder();

        builder.setTitle(LangID.getStringByID("config.command.embed.stage.title", lang));
        builder.setColor(StaticStore.rainbow[StaticStore.RED]);

        TreasureHolder treasure = StaticStore.treasure.getOrDefault(userID, TreasureHolder.global);

        if (treasure.differentFromGlobal()) {
            builder.setDescription(LangID.getStringByID("data.unit.treasure", lang));
        }

        if (config.compact) {
            String idDifficultyLevel = "ID-123-456 - ★1 - " + EmojiStore.CROWN_ON.getFormatted() + EmojiStore.CROWN_OFF.getFormatted().repeat(3) + " (100%)";
            String energyBaseXP = "100 - 10000 - 1000";
            String limitContinuableLength = "10 - " + LangID.getStringByID("data.true", lang) + " - 3000";
            String musicBackgroundCastle = "033 - 000 - 000";
            String guardBarrier = LangID.getStringByID("data.active", lang);

            builder.addField(LangID.getStringByID("data.compact.idDifficultyLevel", lang), idDifficultyLevel, false);
            builder.addField(LangID.getStringByID("data.compact.energyBaseXP", lang), energyBaseXP, false);
            builder.addField(LangID.getStringByID("data.compact.limitContinuableLength", lang), limitContinuableLength, false);
            builder.addField(LangID.getStringByID("data.compact.musicBackgroundCastle", lang).replace("_BBB_", "0"), musicBackgroundCastle, false);
            builder.addField(LangID.getStringByID("data.stage.guardBarrier", lang), guardBarrier, false);

            builder.setFooter(LangID.getStringByID("data.compact.minimumRespawn", lang).replace("_RRR_", config.useFrame ? "1f" : "1s"));
        } else {
            builder.addField(LangID.getStringByID("data.id", lang), "ID-123-456", true);
            builder.addField(LangID.getStringByID("data.unit.level", lang), EmojiStore.CROWN_ON.getFormatted() + EmojiStore.CROWN_OFF.getFormatted().repeat(3) + " (100%)", true);
            builder.addField(LangID.getStringByID("data.stage.energy", lang), "100", true);

            builder.addField(LangID.getStringByID("data.stage.baseHealth", lang), "10000", true);
            builder.addField(LangID.getStringByID("data.stage.xp", lang), "1000", true);
            builder.addField(LangID.getStringByID("data.stage.difficulty", lang), "★1", true);

            builder.addField(LangID.getStringByID("data.stage.continuable", lang), LangID.getStringByID("data.true", lang), true);
            builder.addField(LangID.getStringByID("data.stage.music", lang), "033", true);
            builder.addField("<0%", "000", true);

            builder.addField(LangID.getStringByID("data.stage.enemyLimit", lang), "10", true);
            builder.addField(LangID.getStringByID("data.stage.background", lang), "000", true);
            builder.addField(LangID.getStringByID("data.stage.castle", lang), "000", true);

            builder.addField(LangID.getStringByID("data.stage.length", lang), "3000", true);
            builder.addField(LangID.getStringByID("data.stage.minimumRespawn", lang), config.useFrame ? "1f" : "1s", true);
            builder.addField(LangID.getStringByID("data.stage.guardBarrier", lang), LangID.getStringByID("data.active", lang), true);
        }

        builder.addField(LangID.getStringByID("data.stage.limit.title", lang), LangID.getStringByID("config.command.embed.stage.limit", lang), false);

        if (config.showMiscellaneous) {
            builder.addField(LangID.getStringByID("data.stage.misc.title", lang), LangID.getStringByID("config.command.embed.stage.miscellaneous", lang), false);
        }

        if (config.showExtraStage) {
            builder.addField(LangID.getStringByID("data.stage.misc.exStage", lang), LangID.getStringByID("config.command.embed.stage.extra", lang), false);
        }

        if (config.showMaterialDrop) {
            builder.addField(LangID.getStringByID("data.stage.material.title", lang), LangID.getStringByID("config.command.embed.stage.material", lang), false);
        }

        if (config.showDropInfo) {
            builder.addField(LangID.getStringByID("data.stage.reward.type.chance.normal", lang), LangID.getStringByID("config.command.embed.stage.drop" , lang), false);

            builder.addField(LangID.getStringByID("data.stage.reward.type.score", lang), LangID.getStringByID("config.command.embed.stage.score", lang), false);
        }

        String link = StaticStore.assetManager.uploadIf("CONFIG-EMBED-STAGE-SCHEME", new File("./data/bot/defaultAssets/stageSchemeExample.png"));

        if (link != null) {
            builder.setImage(link);
        }

        return builder.build();
    }
}
