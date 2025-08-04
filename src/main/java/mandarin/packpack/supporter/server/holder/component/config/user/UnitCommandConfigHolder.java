package mandarin.packpack.supporter.server.holder.component.config.user;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
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

    private String getContents() {
        return LangID.getStringByID("config.command.check.unit", lang);
    }

    private MessageEmbed getEmbed() {
        EmbedBuilder builder = new EmbedBuilder();

        String iconLink = StaticStore.assetManager.uploadIf("CONFIG-EMBED-UNIT-ICON", new File("./data/bot/defaultAssets/iconExample.png"));

        builder.setThumbnail(iconLink);
        builder.setColor(StaticStore.rainbow[StaticStore.YELLOW]);

        if (config.compact) {
            builder.setTitle(LangID.getStringByID("config.command.embed.unit.title", lang) + " [123-456]");

            String healthKb = "10000 - 5";
            String costCooldownSpeed = "5000 - " + (config.useFrame ? "100f" : "100s") + " - 10";
            String attackTimings = (config.useFrame ? "100f" : "100s") + " : " + (config.useFrame ? "50f" : "50s") + " -> " + (config.useFrame ? "20f" : "20s") + " -> " + (config.useFrame ? "50f" : "50s");
            String damageDPS = "123456 [123456]";

            builder.addField(LangID.getStringByID("data.unit.level", lang), String.valueOf(config.defLevel), false);
            builder.addField(LangID.getStringByID("data.compact.healthKb", lang), healthKb, false);

            builder.addField(LangID.getStringByID("data.compact.costCooldownSpeed", lang), costCooldownSpeed, true);
            builder.addField(LangID.getStringByID("data.range", lang), "400", true);

            builder.addField(LangID.getStringByID("data.compact.attackTimings", lang), attackTimings, false);
            builder.addField(LangID.getStringByID("data.compact.damageDPS", lang).replace("_TTT_", LangID.getStringByID("data.attackTypes.area", lang)), damageDPS, false);
        } else {
            builder.setTitle(LangID.getStringByID("config.command.embed.unit.title", lang));

            builder.addField(LangID.getStringByID("data.id", lang), "123-456", true);
            builder.addField(LangID.getStringByID("data.unit.level", lang), String.valueOf(config.defLevel), true);
            builder.addField(LangID.getStringByID("data.hp", lang), "10000", true);

            builder.addField(LangID.getStringByID("data.kb", lang), "5", true);
            builder.addField(LangID.getStringByID("data.unit.cooldown", lang), config.useFrame ? "100f" : "100s", true);
            builder.addField(LangID.getStringByID("data.speed", lang), "10", true);

            builder.addField(LangID.getStringByID("data.unit.cost", lang), "5000", true);
            builder.addField(LangID.getStringByID("data.range", lang), "400", true);
            builder.addField(LangID.getStringByID("data.attackTime", lang), config.useFrame ? "100f" : "100s", true);

            builder.addField(LangID.getStringByID("data.foreswing", lang), config.useFrame ? "50f" : "50s", true);
            builder.addField(LangID.getStringByID("data.backswing", lang), config.useFrame ? "20f" : "20s", true);
            builder.addField(LangID.getStringByID("data.tba", lang), config.useFrame ? "50f" : "50s", true);

            builder.addField(LangID.getStringByID("data.attackType", lang), LangID.getStringByID("data.attackTypes.area", lang), true);
            builder.addField(LangID.getStringByID("data.dps", lang), "123456", true);
            builder.addField(LangID.getStringByID("data.useAbility", lang), LangID.getStringByID("data.true", lang), true);

            builder.addField(LangID.getStringByID("data.damage", lang), "123456", true);
            builder.addField(LangID.getStringByID("data.trait", lang), LangID.getStringByID("config.command.embed.unit.trait", lang), true);
        }

        builder.addField(LangID.getStringByID("data.ability", lang), LangID.getStringByID("config.command.embed.unit.abilities", lang), false);

        if (config.showUnitDescription) {
            builder.addField(LangID.getStringByID("data.unit.description", lang), LangID.getStringByID("config.command.embed.unit.description", lang), false);
        }

        if (config.showEvolveDescription) {
            builder.addField(LangID.getStringByID("data.unit.evolve", lang), LangID.getStringByID("config.command.embed.unit.evolve", lang), false);
        }

        if (config.showEvolveImage) {
            String link = StaticStore.assetManager.uploadIf("CONFIG-EMBED-UNIT-EVOLVE", new File("./data/bot/defaultAssets/evolveCostExample.png"));

            builder.setImage(link);
        }

        return builder.build();
    }

    private List<MessageTopLevelComponent> getComponents() {
        List<MessageTopLevelComponent> result = new ArrayList<>();

        Emoji showUnitDescription = config.showUnitDescription ? EmojiStore.SWITCHON : EmojiStore.SWITCHOFF;
        Emoji showEvolveDescription = config.showEvolveDescription ? EmojiStore.SWITCHON : EmojiStore.SWITCHOFF;
        Emoji showEvolveImage = config.showEvolveImage ? EmojiStore.SWITCHON : EmojiStore.SWITCHOFF;

        result.add(ActionRow.of(
                Button.secondary("description", LangID.getStringByID("config.command.button.unit.description", lang)).withEmoji(showUnitDescription)
        ));

        result.add(ActionRow.of(
                Button.secondary("evolve", LangID.getStringByID("config.command.button.unit.evolve", lang)).withEmoji(showEvolveDescription)
        ));

        result.add(ActionRow.of(
                Button.secondary("image", LangID.getStringByID("config.command.button.unit.image", lang)).withEmoji(showEvolveImage)
        ));

        result.add(ActionRow.of(
                Button.secondary("back", LangID.getStringByID("ui.button.back", lang)).withEmoji(EmojiStore.BACK),
                Button.success("confirm", LangID.getStringByID("ui.button.confirm", lang)).withEmoji(EmojiStore.CHECK),
                Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang)).withEmoji(EmojiStore.CROSS)
        ));

        return result;
    }
}
