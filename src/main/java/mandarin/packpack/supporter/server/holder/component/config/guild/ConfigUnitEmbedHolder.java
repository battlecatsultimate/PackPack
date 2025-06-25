package mandarin.packpack.supporter.server.holder.component.config.guild;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ConfigUnitEmbedHolder extends ServerConfigHolder {

    public ConfigUnitEmbedHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, @Nonnull IDHolder holder, @Nonnull IDHolder backup, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, holder, backup, lang);
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "description" -> {
                holder.config.showUnitDescription = !holder.config.showUnitDescription;

                applyResult(event);
            }
            case "evolve" -> {
                holder.config.showEvolveDescription = !holder.config.showEvolveDescription;

                applyResult(event);
            }
            case "image" -> {
                holder.config.showEvolveImage = !holder.config.showEvolveImage;

                applyResult(event);
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
        builder.setColor(StaticStore.rainbow[2]);

        if (holder.config.compact) {
            builder.setTitle(LangID.getStringByID("config.command.embed.unit.title", lang) + " [123-456]");

            String healthKb = "10000 - 5";
            String costCooldownSpeed = "5000 - " + (holder.config.useFrame ? "100f" : "100s") + " - 10";
            String attackTimings = (holder.config.useFrame ? "100f" : "100s") + " : " + (holder.config.useFrame ? "50f" : "50s") + " -> " + (holder.config.useFrame ? "20f" : "20s") + " -> " + (holder.config.useFrame ? "50f" : "50s");
            String damageDPS = "123456 [123456]";

            builder.addField(LangID.getStringByID("data.unit.level", lang), String.valueOf(holder.config.defLevel), false);
            builder.addField(LangID.getStringByID("data.compact.healthKb", lang), healthKb, false);

            builder.addField(LangID.getStringByID("data.compact.costCooldownSpeed", lang), costCooldownSpeed, true);
            builder.addField(LangID.getStringByID("data.range", lang), "400", true);

            builder.addField(LangID.getStringByID("data.compact.attackTimings", lang), attackTimings, false);
            builder.addField(LangID.getStringByID("data.compact.damageDPS", lang).replace("_TTT_", LangID.getStringByID("data.attackTypes.area", lang)), damageDPS, false);
        } else {
            builder.setTitle(LangID.getStringByID("config.command.embed.unit.title", lang));

            builder.addField(LangID.getStringByID("data.id", lang), "123-456", true);
            builder.addField(LangID.getStringByID("data.unit.level", lang), String.valueOf(holder.config.defLevel), true);
            builder.addField(LangID.getStringByID("data.hp", lang), "10000", true);

            builder.addField(LangID.getStringByID("data.kb", lang), "5", true);
            builder.addField(LangID.getStringByID("data.unit.cooldown", lang), holder.config.useFrame ? "100f" : "100s", true);
            builder.addField(LangID.getStringByID("data.speed", lang), "10", true);

            builder.addField(LangID.getStringByID("data.unit.cost", lang), "5000", true);
            builder.addField(LangID.getStringByID("data.range", lang), "400", true);
            builder.addField(LangID.getStringByID("data.attackTime", lang), holder.config.useFrame ? "100f" : "100s", true);

            builder.addField(LangID.getStringByID("data.foreswing", lang), holder.config.useFrame ? "50f" : "50s", true);
            builder.addField(LangID.getStringByID("data.backswing", lang), holder.config.useFrame ? "20f" : "20s", true);
            builder.addField(LangID.getStringByID("data.tba", lang), holder.config.useFrame ? "50f" : "50s", true);

            builder.addField(LangID.getStringByID("data.attackType", lang), LangID.getStringByID("data.attackTypes.area", lang), true);
            builder.addField(LangID.getStringByID("data.dps", lang), "123456", true);
            builder.addField(LangID.getStringByID("data.useAbility", lang), LangID.getStringByID("data.true", lang), true);

            builder.addField(LangID.getStringByID("data.damage", lang), "123456", true);
            builder.addField(LangID.getStringByID("data.trait", lang), LangID.getStringByID("config.command.embed.unit.trait", lang), true);
        }

        builder.addField(LangID.getStringByID("data.ability", lang), LangID.getStringByID("config.command.embed.unit.abilities", lang), false);

        if (holder.config.showUnitDescription) {
            builder.addField(LangID.getStringByID("data.unit.description", lang), LangID.getStringByID("config.command.embed.unit.description", lang), false);
        }

        if (holder.config.showEvolveDescription) {
            builder.addField(LangID.getStringByID("data.unit.evolve", lang), LangID.getStringByID("config.command.embed.unit.evolve", lang), false);
        }

        if (holder.config.showEvolveImage) {
            String link = StaticStore.assetManager.uploadIf("CONFIG-EMBED-UNIT-EVOLVE", new File("./data/bot/defaultAssets/evolveCostExample.png"));

            builder.setImage(link);
        }

        return builder.build();
    }

    private List<LayoutComponent> getComponents() {
        List<LayoutComponent> result = new ArrayList<>();

        Emoji showUnitDescription = holder.config.showUnitDescription ? EmojiStore.SWITCHON : EmojiStore.SWITCHOFF;
        Emoji showEvolveDescription = holder.config.showEvolveDescription ? EmojiStore.SWITCHON : EmojiStore.SWITCHOFF;
        Emoji showEvolveImage = holder.config.showEvolveImage ? EmojiStore.SWITCHON : EmojiStore.SWITCHOFF;

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
