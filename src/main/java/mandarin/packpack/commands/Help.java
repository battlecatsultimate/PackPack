package mandarin.packpack.commands;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.help.HelpCategoryHolder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Help extends Command {
    public enum HelpCategory {
        NORMAL,
        MATH,
        BC,
        SERVER,
        DATA,
        BOT
    }

    @Nullable
    private final IDHolder holder;

    public Help(CommonStatic.Lang.Locale lang, @Nullable IDHolder holder) {
        super(lang, false);

        this.holder = holder;
    }

    @Override
    public void prepare() {
        registerRequiredPermission(Permission.MESSAGE_EMBED_LINKS);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        String[] messages = loader.getContent().split(" ");

        if(messages.length >= 2) {
            createEmbedOfSpecificCommand(messages[1], ch, loader.getMessage());
        } else {
            EmbedBuilder builder = new EmbedBuilder();

            int color = StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)];

            builder.setTitle(LangID.getStringByID("help.main.command", lang))
                    .setDescription(LangID.getStringByID("help.main.description", lang))
                    .setColor(color)
                    .addField(LangID.getStringByID("help.main.category.normal", lang), "```analyze, config, donate, locale, optout, prefix, timezone```", false)
                    .addField(LangID.getStringByID("help.main.category.math", lang), "```calculator, differentiate, integrate, plot, plotrtheta, tplot, solve```", false)
                    .addField(LangID.getStringByID("help.main.category.bc", lang), "```background, castle, catcombo, enemydps, enemygif, enemyimage, enemysprite, enemystat, findreward, findstage, formdps, formgif, formimage, formsprite, formstat, medal, music, soul, soulimage, soulsprite, stageinfo, talentinfo, treasure```", false)
                    .addField(LangID.getStringByID("help.main.category.server", lang), "```boosteremoji, boosteremojiremove, boosterrole, boosterroleremove, channelpermission, hasrole, idset, serverconfig, serverpre, serverstat, setup, subscribeevent, subscribescamlinkdetector, unsubscribescamlinkdetector, watchdm```", false)
                    .addField(LangID.getStringByID("help.main.category.data", lang), "```animanalyzer, announcement, checkeventupdate, comboanalyzer, downloadapk, enemystatanalyzer, eventdataarchive, printevent, printgachaevent, printitemevent, printstageevent, stageimage, stagestatanalyzer, statanalyzer, stagemapimage, talentanalyzer, trueformanalyzer```", false)
                    .addField(LangID.getStringByID("help.main.category.bot", lang), "```alias, aliasadd, aliasremove, memory, registerscamlink, save, serverjson, statistic, suggest, unregisterscamlink```", false);

            replyToMessageSafely(ch, "", loader.getMessage(),
                    a -> a.setEmbeds(builder.build()).setComponents(getComponents()),
                    msg -> StaticStore.putHolder(loader.getUser().getId(), new HelpCategoryHolder(loader.getMessage(), ch.getId(), msg, lang, color))
            );
        }
    }

    public void createEmbedOfSpecificCommand(String command, MessageChannel ch, Message reference) {
        String prefix;

        if (ch instanceof GuildChannel) {
            if (holder == null) {
                prefix = StaticStore.globalPrefix;
            } else {
                prefix = holder.config.prefix;
            }
        } else {
            prefix = StaticStore.globalPrefix;
        }

        switch (command) {
            case "serverstat", "ss" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("serverStat", prefix)));
            case "analyze" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("analyze", prefix)));
            case "prefix" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("prefix", prefix)));
            case "serverpre" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("serverPrefix", prefix)));
            case "save" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("save", prefix)));
            case "stimg", "stimage", "stageimg", "stageimage" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("stageImage", prefix)));
            case "stmimg", "stmimage", "stagemapimg", "stagemapimage" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("stageMapImage", prefix)));
            case "formstat", "fs", "catstat", "cs", "unitstat", "us" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("formStat", prefix)));
            case "locale", "loc" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("locale", prefix)));
            case "music", "ms" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("music", prefix)));
            case "enemystat", "es" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("enemyStat", prefix)));
            case "castle", "cas" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("castle", prefix)));
            case "stageinfo", "si" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("stageInfo", prefix)));
            case "memory", "mm" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("memory", prefix)));
            case "formimage", "formimg", "fimage", "fimg", "catimage", "catimg", "cimage", "cimg", "unitimage", "unitimg", "uimage", "uimg" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("formImage", prefix)));
            case "enemyimage", "enemyimg", "eimage", "eimg" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("enemyImage", prefix)));
            case "background", "bg" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("background", prefix)));
            case "formgif", "fgif", "fg", "catgif", "cgif", "cg", "unitgif", "ugif", "ug" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("formGif", prefix)));
            case "enemygif", "egif", "eg" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("enemyGif", prefix)));
            case "idset" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("idSet", prefix)));
            case "aa", "animanalyzer" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("animationAnalyzer", prefix)));
            case "channelpermission", "channelperm", "chpermission", "chperm", "chp" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("channelPermission", prefix)));
            case "formsprite", "fsprite", "formsp", "fsp", "catsprite", "csprite", "catsp", "csp", "unitsprite", "usprite", "unitsp", "usp" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("formSprite", prefix)));
            case "enemysprite", "esprite", "enemysp", "esp" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("enemySprite", prefix)));
            case "medal", "md" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("medal", prefix)));
            case "announcement", "ann" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("announcement", prefix)));
            case "catcombo", "combo", "cc" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("catCombo", prefix)));
            case "serverjson", "json", "sj" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("serverJson", prefix)));
            case "findstage", "findst", "fstage", "fst" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("findStage", prefix)));
            case "suggest" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("suggest", prefix)));
            case "alias", "al" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("alias", prefix)));
            case "aliasadd", "ala" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("aliasAdd", prefix)));
            case "aliasremove", "alr" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("aliasRemove", prefix)));
            case "statistic", "stat" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("statistic", prefix)));
            case "serverlocale", "slocale", "serverloc", "sloc" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("serverLocale", prefix)));
            case "boosterrole", "boosterr", "brole", "br" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("boosterRole", prefix)));
            case "boosterroleremove", "brremove", "boosterrolerem", "brrem", "brr" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("boosterRoleRemove", prefix)));
            case "boosteremoji", "boostere", "bemoji", "be" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("boosterEmoji", prefix)));
            case "boosteremojiremove", "beremove", "boosteremojirem", "berem", "ber" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("boosterEmojiRemove", prefix)));
            case "setup" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("setup", prefix)));
            case "watchdm", "wd" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("watchDM", prefix)));
            case "checkeventupdate", "ceu" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("checkEventUpdate", prefix)));
            case "printgachaevent", "pge" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("printGachaEvent", prefix)));
            case "printitemevent", "pie" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("printItemEvent", prefix)));
            case "printstageevent", "pse" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("printStageEvent", prefix)));
            case "subscribeevent", "se" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("subscribeEvent", prefix)));
            case "printevent", "pe" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("printEvent", prefix)));
            case "statanalyzer", "sa" -> {
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("statAnalyzer", prefix)));
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(new EmbedBuilder()
                        .setColor(StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)])
                        .setDescription(LangID.getStringByID("help.statAnalyzer.additionalDescription", lang))
                        .addField("-name", LangID.getStringByID("help.statAnalyzer.name", lang), false)
                        .addField("-trait", LangID.getStringByID("help.statAnalyzer.trait", lang), false)
                        .addField("-cell", LangID.getStringByID("help.statAnalyzer.cell", lang), false)
                        .addField("-abil", LangID.getStringByID("help.statAnalyzer.passive", lang), false)
                        .addField("-proc", LangID.getStringByID("help.statAnalyzer.active", lang), false)
                        .build()));
            }
            case "registerscamlink", "rsl" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("registerScamLink", prefix)));
            case "unregisterscamlink", "usl" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("unregisterScamLink", prefix)));
            case "subscribescamlinkdetector", "ssld", "ssd" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("subscribeScamDetector", prefix)));
            case "unsubscribescamlinkdetector", "usld", "usd" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("unsubscribeScamDetector", prefix)));
            case "optout" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("optOut", prefix)));
            case "config" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("config", prefix)));
            case "downloadapk", "da" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("downloadApk", prefix)));
            case "trueformanalyzer", "tfanalyzer", "trueforma", "tfa" -> {
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("trueFormAnalyzer", prefix)));
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(new EmbedBuilder()
                        .setColor(StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)])
                        .setDescription(LangID.getStringByID("help.statAnalyzer.additionalDescription", lang))
                        .addField("-name", LangID.getStringByID("help.statAnalyzer.name", lang), false)
                        .addField("-trait", LangID.getStringByID("help.statAnalyzer.trait", lang), false)
                        .addField("-cell", LangID.getStringByID("help.statAnalyzer.cell", lang), false)
                        .addField("-abil", LangID.getStringByID("help.statAnalyzer.passive", lang), false)
                        .addField("-proc", LangID.getStringByID("help.statAnalyzer.active", lang), false)
                        .build()));
            }
            case "enemystatanalyzer", "estatanalyzer", "enemysa", "esa" -> {
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("enemyStatAnalyzer", prefix)));
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(new EmbedBuilder()
                        .setColor(StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)])
                        .setDescription(LangID.getStringByID("help.statAnalyzer.additionalDescription", lang))
                        .addField("-name", LangID.getStringByID("help.statAnalyzer.name", lang), false)
                        .addField("-trait", LangID.getStringByID("help.statAnalyzer.trait", lang), false)
                        .addField("-cell", LangID.getStringByID("help.statAnalyzer.cell", lang), false)
                        .addField("-abil", LangID.getStringByID("help.statAnalyzer.passive", lang), false)
                        .addField("-proc", LangID.getStringByID("help.statAnalyzer.active", lang), false)
                        .build()));
            }
            case "stagestatanalyzer", "sstatanalyzer", "stagesa", "ssa" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("stageStatAnalyzer", prefix)));
            case "serverconfig", "sconfig", "serverc", "sc" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("serverConfig", prefix)));
            case "talentinfo", "ti" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("talentInfo", prefix)));
            case "soul", "sl" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("soul", prefix)));
            case "soulimage", "soulimg", "simage", "simg" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("soulImage", prefix)));
            case "soulsprite", "ssprite", "soulsp", "ssp" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("soulSprite", prefix)));
            case "calculator", "calc", "c" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("calculator", prefix)));
            case "findreward", "freward", "findr", "fr" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("findReward", prefix)));
            case "eventdataarchive", "eventddataa", "earchive", "eda" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("eventDataArchive", prefix)));
            case "talentanalyzer", "tala", "ta" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("talentAnalyzer", prefix)));
            case "comboanalyzer", "catcomboanalyzer", "cca", "ca" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("comboAnalyzer", prefix)));
            case "plot", "p" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("plot", prefix)));
            case "tplot", "tp" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("tPlot", prefix)));
            case "solve", "sv" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("solve", prefix)));
            case "differentiate", "diff", "dx" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("differentiate", prefix)));
            case "integrate", "int" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("integrate", prefix)));
            case "plotrtheta", "prtheta", "plotrt", "prt", "rt" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("plotRTheta", prefix)));
            case "donate", "donation", "don" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("donate", prefix)));
            case "treasure", "tr" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("treasure", prefix)));
            case "boosterpin", "bp" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("boosterPin", prefix)));
            case "formdps", "catdps", "unitdps", "fdps", "cdps", "udps", "fd", "cd", "ud" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("formDps", prefix)));
            case "enemydps", "edps", "ed" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("enemyDps", prefix)));
            case "hasrole", "hr" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("hasRole", prefix)));
            default ->
                    replyToMessageSafely(ch, LangID.getStringByID("help.noCommand", lang).replace("_", command), reference, a -> a);
        }
    }

    private MessageEmbed addFields(String selectedCommand, String prefix) {
        String usage = LangID.getStringByID("help." + selectedCommand + ".usage", lang).replace("`", "").formatted(prefix);
        String command = usage.split(" ")[0];

        EmbedBuilder builder = new EmbedBuilder();

        if(LangID.hasID("help." + selectedCommand + ".url", lang)) {
            builder.setTitle(command, LangID.getStringByID("help." + selectedCommand + ".url", lang));
            builder.setDescription(LangID.getStringByID("help.format.guide", lang));
        } else {
            builder.setTitle(command);
        }

        builder.setColor(StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)]);

        builder.addField(LangID.getStringByID("help.format.usage", lang), usage, false);

        if (LangID.hasID("help." + selectedCommand + ".alias", lang)) {
            builder.addField(LangID.getStringByID("help.format.alias", lang), LangID.getStringByID("help." + selectedCommand + ".alias", lang), false);
        }

        builder.addField(LangID.getStringByID("help.format.description", lang), LangID.getStringByID("help." + selectedCommand + ".description", lang), false);

        if (LangID.hasID("help." + selectedCommand + ".parameter", lang)) {
            builder.addField(LangID.getStringByID("help.format.parameter", lang), LangID.getStringByID("help." + selectedCommand + ".parameter", lang), false);
        }

        if (LangID.hasID("help." + selectedCommand + ".example", lang)) {
            builder.addField(LangID.getStringByID("help.format.example", lang), LangID.getStringByID("help." + selectedCommand + ".example", lang), false);
        }

        int tipIndex = 1;

        while (LangID.hasID("help." + selectedCommand + ".tip" + (tipIndex == 1 ? "" : String.valueOf(tipIndex)), lang)) {
            String id = "help." + selectedCommand + ".tip" + (tipIndex == 1 ? "" : String.valueOf(tipIndex));
            String tips = LangID.getStringByIDSuppressed(id, lang);

            builder.addField(tipIndex == 1 ? LangID.getStringByID("help.format.tip", lang) : "** **", tips, false);

            tipIndex++;
        }

        return builder.build();
    }

    private List<LayoutComponent> getComponents() {
        List<LayoutComponent> result = new ArrayList<>();

        List<SelectOption> categoryOptions = new ArrayList<>();

        for (HelpCategory category : HelpCategory.values()) {
            SelectOption option = SelectOption.of(LangID.getStringByID("help.main.category." + category.name().toLowerCase(Locale.ENGLISH), lang), category.name());

            switch (category) {
                case BC -> option = option.withEmoji(EmojiStore.CAT);
                case DATA -> option = option.withEmoji(EmojiStore.FILE);
                case MATH -> option = option.withEmoji(Emoji.fromUnicode("📟"));
                case NORMAL -> option = option.withEmoji(Emoji.fromUnicode("🎚️"));
                case SERVER -> option = option.withEmoji(EmojiStore.MODERATOR);
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
