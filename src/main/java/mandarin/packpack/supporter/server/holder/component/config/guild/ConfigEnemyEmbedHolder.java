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

public class ConfigEnemyEmbedHolder extends ServerConfigHolder {

    public ConfigEnemyEmbedHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, @Nonnull IDHolder holder, @Nonnull IDHolder backup, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, holder, backup, lang);
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "description" -> {
                holder.config.showEnemyDescription = !holder.config.showEnemyDescription;

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
        return LangID.getStringByID("config.command.check.enemy", lang);
    }

    private MessageEmbed getEmbed() {
        EmbedBuilder builder = new EmbedBuilder();

        String iconLink = StaticStore.assetManager.uploadIf("CONFIG-EMBED-ENEMY-ICON", new File("./data/bot/defaultAssets/enemyIconExample.png"));

        builder.setColor(StaticStore.rainbow[0]);
        builder.setThumbnail(iconLink);

        if (holder.config.compact) {
            builder.setTitle(LangID.getStringByID("config.command.embed.enemy.title", lang) + " [123]");

            String healthKb = "10000 - 10";
            String dropBarrierSpeed = "1000 - " + LangID.getStringByID("data.none", lang) + " - 10";
            String attackTimings = (holder.config.useFrame ? "100f" : "100s") + " : " + (holder.config.useFrame ? "50f" : "50s") + " -> " + (holder.config.useFrame ? "20f" : "20s") + " -> " + (holder.config.useFrame ? "50f" : "50s");
            String damageDPS = "123456 [123456]";

            builder.addField(LangID.getStringByID("data.enemy.magnification", lang), "100%", false);
            builder.addField(LangID.getStringByID("data.compact.healthKb", lang), healthKb, false);

            builder.addField(LangID.getStringByID("data.compact.dropBarrierSpeed", lang), dropBarrierSpeed, true);
            builder.addField(LangID.getStringByID("data.range", lang), "400", true);

            builder.addField(LangID.getStringByID("data.compact.attackTimings", lang), attackTimings, false);
            builder.addField(LangID.getStringByID("data.compact.damageDPS", lang).replace("_TTT_", LangID.getStringByID("data.attackTypes.area", lang)), damageDPS, false);
            builder.addField(LangID.getStringByID("data.trait", lang), LangID.getStringByID("config.command.embed.unit.trait", lang), false);
        } else {
            builder.setTitle(LangID.getStringByID("config.command.embed.enemy.title", lang));

            builder.addField(LangID.getStringByID("data.enemy.magnification", lang), "100%", true);
            builder.addField(LangID.getStringByID("data.hp", lang), "10000", true);
            builder.addField(LangID.getStringByID("data.kb", lang), "10", true);

            builder.addField(LangID.getStringByID("data.enemy.barrier", lang), LangID.getStringByID("data.none", lang), true);
            builder.addField(LangID.getStringByID("data.speed", lang), "10", true);
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

        if (holder.config.showEnemyDescription) {
            builder.addField(LangID.getStringByID("data.enemy.description", lang), LangID.getStringByID("config.command.embed.enemy.description", lang), false);
        }

        builder.setFooter(LangID.getStringByID("enemyStat.source", lang));

        return builder.build();
    }

    private List<LayoutComponent> getComponents() {
        List<LayoutComponent> result = new ArrayList<>();

        Emoji showEnemyDescription = holder.config.showEnemyDescription ? EmojiStore.SWITCHON : EmojiStore.SWITCHOFF;

        result.add(ActionRow.of(
                Button.secondary("description", LangID.getStringByID("config.command.button.enemy.description", lang)).withEmoji(showEnemyDescription)
        ));

        result.add(ActionRow.of(
                Button.secondary("back", LangID.getStringByID("ui.button.back", lang)).withEmoji(EmojiStore.BACK),
                Button.success("confirm", LangID.getStringByID("ui.button.confirm", lang)).withEmoji(EmojiStore.CHECK),
                Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang)).withEmoji(EmojiStore.CROSS)
        ));

        return result;
    }
}
