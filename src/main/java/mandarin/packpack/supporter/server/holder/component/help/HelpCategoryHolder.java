package mandarin.packpack.supporter.server.holder.component.help;

import common.CommonStatic;
import mandarin.packpack.commands.Help;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.component.ComponentHolder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HelpCategoryHolder extends ComponentHolder {
    private final int color;

    public HelpCategoryHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message, @NotNull CommonStatic.Lang.Locale lang, int color) {
        super(author, channelID, message, lang);

        this.color = color;

        registerAutoExpiration(FIVE_MIN);
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        if (!event.getComponentId().equals("category"))
            return;

        if (!(event instanceof StringSelectInteractionEvent e))
            return;

        String name = e.getValues().getFirst();

        Help.HelpCategory category = Help.HelpCategory.valueOf(name);

        connectTo(event, new HelpCommandHolder(getAuthorMessage(), channelID, message, lang, category, color));
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire() {
        message.editMessageComponents()
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    @Override
    public void onBack(@NotNull Holder child) {
        applyResult();
    }

    @Override
    public void onBack(@NotNull IMessageEditCallback event, @NotNull Holder child) {
        applyResult(event);
    }

    private void applyResult(IMessageEditCallback event) {
        event.deferEdit()
                .setEmbeds(getEmbed())
                .setComponents(getComponents())
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private void applyResult() {
        message.editMessageEmbeds(getEmbed())
                .setComponents(getComponents())
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private MessageEmbed getEmbed() {
        EmbedBuilder builder = new EmbedBuilder();

        builder.setTitle(LangID.getStringByID("help.main.command", lang))
                .setDescription(LangID.getStringByID("help.main.description", lang))
                .setColor(color)
                .addField(LangID.getStringByID("help.main.category.normal", lang), "```analyze, config, donate, locale, optout, prefix, timezone```", false)
                .addField(LangID.getStringByID("help.main.category.math", lang), "```calculator, differentiate, integrate, plot, plotrtheta, tplot, solve```", false)
                .addField(LangID.getStringByID("help.main.category.bc", lang), "```background, castle, catcombo, enemydps, enemygif, enemyimage, enemysprite, enemystat, findreward, findstage, formdps, formgif, formimage, formsprite, formstat, medal, music, soul, soulimage, soulsprite, stageinfo, talentinfo, treasure```", false)
                .addField(LangID.getStringByID("help.main.category.server", lang), "```boosteremoji, boosteremojiremove, boosterrole, boosterroleremove, channelpermission, clearcache, eventmessage, save, serverconfig, serverjson, serverpre, serverstat, setup, subscribeevent, subscribescamlinkdetector, unsubscribescamlinkdetector, watchdm```", false)
                .addField(LangID.getStringByID("help.main.category.data", lang), "```animanalyzer, announcement, checkeventupdate, comboanalyzer, downloadapk, enemystatanalyzer, eventdataarchive, printevent, printgachaevent, printitemevent, printstageevent, stageimage, stagestatanalyzer, statanalyzer, stagemapimage, talentanalyzer, trueformanalyzer```", false)
                .addField(LangID.getStringByID("help.main.category.bot", lang), "```alias, aliasadd, aliasremove, memory, registerscamlink, statistic, suggest, unregisterscamlink```", false);

        return builder.build();
    }

    private List<LayoutComponent> getComponents() {
        List<LayoutComponent> result = new ArrayList<>();

        List<SelectOption> categoryOptions = new ArrayList<>();

        for (Help.HelpCategory category : Help.HelpCategory.values()) {
            SelectOption option = SelectOption.of(LangID.getStringByID("help.main.category." + category.name().toLowerCase(Locale.ENGLISH), lang), category.name());

            switch (category) {
                case BC -> option = option.withEmoji(EmojiStore.CAT);
                case DATA -> option = option.withEmoji(EmojiStore.FILE);
                case MATH -> option = option.withEmoji(Emoji.fromUnicode("📟"));
                case NORMAL -> option = option.withEmoji(Emoji.fromUnicode("🎚️"));
                case SERVER -> option = option.withEmoji(EmojiStore.MODERATOR);
                case BOT -> option = option.withEmoji(Emoji.fromUnicode("🤖"));
            }

            categoryOptions.add(option);
        }

        result.add(ActionRow.of(
                StringSelectMenu.create("category")
                        .addOptions(categoryOptions)
                        .setPlaceholder(LangID.getStringByID("help.main.selectCategory", lang))
                        .build()
        ));

        return result;
    }
}
