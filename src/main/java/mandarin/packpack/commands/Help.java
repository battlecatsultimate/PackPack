package mandarin.packpack.commands;

import common.CommonStatic;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class Help extends Command {
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

            builder.setTitle(LangID.getStringByID("help.main.command", lang))
                    .setDescription(LangID.getStringByID("help.main.description", lang))
                    .setColor(StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)])
                    .addField(LangID.getStringByID("help.main.category.normal", lang), "```analyze, config, donate, locale, optout, prefix, timezone```", false)
                    .addField(LangID.getStringByID("help.main.category.math", lang), "```calculator, differentiate, integrate, plot, plotrtheta, tplot, solve```", false)
                    .addField(LangID.getStringByID("help.main.category.bc", lang), "```background, castle, catcombo, enemydps, enemygif, enemyimage, enemysprite, enemystat, findreward, findstage, formdps, formgif, formimage, formsprite, formstat, medal, music, soul, soulimage, soulsprite, stageinfo, talentinfo, treasure```", false)
                    .addField(LangID.getStringByID("help.main.category.server", lang), "```boosteremoji, boosteremojiremove, boosterrole, boosterroleremove, channelpermission, clearcache, eventmessage, save, serverconfig, serverjson, serverpre, serverstat, setup, subscribeevent, subscribescamlinkdetector, unsubscribescamlinkdetector, watchdm```", false)
                    .addField(LangID.getStringByID("help.main.category.data", lang), "```animanalyzer, announcement, checkeventupdate, comboanalyzer, downloadapk, enemystatanalyzer, eventdataarchive, printevent, printgachaevent, printitemevent, printstageevent, stageimage, stagestatanalyzer, statanalyzer, stagemapimage, talentanalyzer, trueformanalyzer```", false)
                    .addField(LangID.getStringByID("help.format.packPack", lang), "```alias, aliasadd, aliasremove, memory, registerscamlink, statistic, suggest, unregisterscamlink```", false);

            replyToMessageSafely(ch, "", loader.getMessage(), a -> a.setEmbeds(builder.build()));
        }
    }

    public void createEmbedOfSpecificCommand(String command, MessageChannel ch, Message reference) {
        switch (command) {
            case "serverstat", "ss" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("serverstat", false, false, false)));
            case "analyze" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("analyze", false, false, false)));
            case "prefix" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("prefix", false, false, false)));
            case "serverpre" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("serverpre", false, false, false)));
            case "save" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("save", false, false, false)));
            case "stimg", "stimage", "stageimg", "stageimage" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("stageimage", true, true, false)));
            case "stmimg", "stmimage", "stagemapimg", "stagemapimage" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("stagemapimage", true, true, false)));
            case "formstat", "fs", "catstat", "cs", "unitstat", "us" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("formstat", true, true, false)));
            case "locale", "loc" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("locale", false, false, false)));
            case "music", "ms" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("music", false, false, false)));
            case "enemystat", "es" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("enemystat", true, true, false)));
            case "castle", "cas" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("castle", true, true, false)));
            case "stageinfo", "si" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("stageinfo", true, true, true)));
            case "memory", "mm" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("memory", false, false, false)));
            case "formimage", "formimg", "fimage", "fimg", "catimage", "catimg", "cimage", "cimg", "unitimage", "unitimg", "uimage", "uimg" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("formimage", true, true, true)));
            case "enemyimage", "enemyimg", "eimage", "eimg" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("enemyimage", true, true, true)));
            case "background", "bg" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("background", true, true, false)));
            case "formgif", "fgif", "fg", "catgif", "cgif", "cg", "unitgif", "ugif", "ug" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("formgif", true, true, true)));
            case "enemygif", "egif", "eg" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("enemygif", true, true, true)));
            case "idset" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("idset", true, true, true)));
            case "clearcache" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("clearcache", false, false, false)));
            case "aa", "animanalyzer" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("animanalyzer", true, false, true)));
            case "channelpermission", "channelperm", "chpermission", "chperm", "chp" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("channelpermission", false, false, false)));
            case "formsprite", "fsprite", "formsp", "fsp", "catsprite", "csprite", "catsp", "csp", "unitsprite", "usprite", "unitsp", "usp" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("formsprite", true, true, false)));
            case "enemysprite", "esprite", "enemysp", "esp" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("enemysprite", true, true, false)));
            case "medal", "md" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("medal", false, true, false)));
            case "announcement", "ann" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("announcement", true, true, false)));
            case "catcombo", "combo", "cc" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("catcombo", true, true, false)));
            case "serverjson", "json", "sj" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("serverjson", false, false, false)));
            case "findstage", "findst", "fstage", "fst" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("findstage", true, true, true)));
            case "suggest" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("suggest", true, true, true)));
            case "alias", "al" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("alias", true, true, true)));
            case "aliasadd", "ala" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("aliasadd", true, true, true)));
            case "aliasremove", "alr" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("aliasremove", true, true, true)));
            case "statistic", "stat" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("statistic", false, false, false)));
            case "serverlocale", "slocale", "serverloc", "sloc" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("serverlocale", false, false, false)));
            case "boosterrole", "boosterr", "brole", "br" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("boosterrole", true, true, true)));
            case "boosterroleremove", "brremove", "boosterrolerem", "brrem", "brr" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("boosterroleremove", true, false, false)));
            case "boosteremoji", "boostere", "bemoji", "be" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("boosteremoji", false, true, true)));
            case "boosteremojiremove", "beremove", "boosteremojirem", "berem", "ber" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("boosteremojiremove", true, false, false)));
            case "setup" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("setup", false, false, true)));
            case "watchdm", "wd" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("watchdm", false, false, true)));
            case "checkeventupdate", "ceu" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("checkeventupdate", false, false, false)));
            case "printgachaevent", "pge" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("printgachaevent", true, true, false)));
            case "printitemevent", "pie" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("printitemevent", true, true, false)));
            case "printstageevent", "pse" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("printstageevent", true, true, false)));
            case "subscribeevent", "se" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("subscribeevent", false, false, false)));
            case "printevent", "pe" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("printevent", true, true, false)));
            case "statanalyzer", "sa" -> {
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("statanalyzer", true, true, true)));
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
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("registerscamlink", false, false, true)));
            case "unregisterscamlink", "usl" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("unregisterscamlink", false, false, false)));
            case "subscribescamlinkdetector", "ssld", "ssd" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("subscribescamlinkdetector", true, false, true)));
            case "unsubscribescamlinkdetector", "usld", "usd" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("unsubscribescamlinkdetector", false, false, false)));
            case "optout" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("optout", false, false, false)));
            case "config" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("config", false, false, true)));
            case "downloadapk", "da" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("downloadapk", true, true, false)));
            case "trueformanalyzer", "tfanalyzer", "trueforma", "tfa" -> {
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("trueformanalyzer", true, true, true)));
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
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("enemystatanalyzer", true, true, true)));
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
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("stagestatanalyzer", true, true, true)));
            case "serverconfig", "sconfig", "serverc", "sc" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("serverconfig", false, false, true)));
            case "talentinfo", "ti" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("talentinfo", true, false, false)));
            case "soul", "sl" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("soul", true, true, false)));
            case "soulimage", "soulimg", "simage", "simg" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("soulimage", true, true, false)));
            case "soulsprite", "ssprite", "soulsp", "ssp" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("soulsprite", false, false, false)));
            case "calculator", "calc", "c" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("calculator", false, true, true)));
            case "findreward", "freward", "findr", "fr" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("findreward", true, true, false)));
            case "eventdataarchive", "eventddataa", "earchive", "eda" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("eventdataarchive", true, true, false)));
            case "talentanalyzer", "tala", "ta" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("talentanalyzer", true, true, false)));
            case "comboanalyzer", "catcomboanalyzer", "cca", "ca" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("comboanalyzer", true, false, true)));
            case "plot", "p" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("plot", true, true, true)));
            case "tplot", "tp" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("tplot", true, true, true)));
            case "solve", "sv" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("solve", true, true, true)));
            case "differentiate", "diff", "dx" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("differentiate", true, true, true)));
            case "integrate", "int" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("integrate", true, true, true)));
            case "plotrtheta", "prtheta", "plotrt", "prt", "rt" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("plotrtheta", true, true, true)));
            case "donate", "donation", "don" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("donate", false, false, false)));
            case "eventmessage", "em" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("eventmessage", true, false, true)));
            case "treasure", "tr" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("treasure", false, false, true)));
            case "boosterpin", "bp" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("boosterpin", false, true, false)));
            case "formdps", "catdps", "unitdps", "fdps", "cdps", "udps", "fd", "cd", "ud" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("formdps", true, true, false)));
            case "enemydps", "edps", "ed" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("enemydps", true, true, false)));
            case "hasrole", "hr" ->
                    replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("hasrole", false, false, false)));
            default ->
                    replyToMessageSafely(ch, LangID.getStringByID("help.noCommand", lang).replace("_", command), reference, a -> a);
        }
    }

    private MessageEmbed addFields(String mainCommand, boolean parameter, boolean example, boolean tip) {
        EmbedBuilder builder = new EmbedBuilder();

        builder.setColor(StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)]);

        if(LangID.hasID("help_" + mainCommand + "_url", lang)) {
            builder.setTitle((holder == null ? StaticStore.globalPrefix : holder.config.prefix) + mainCommand, LangID.getStringByID("help_" + mainCommand + "_url", lang));
            builder.setDescription(LangID.getStringByID("help.format.guide", lang));
        } else {
            builder.setTitle((holder == null ? StaticStore.globalPrefix : holder.config.prefix) + mainCommand);
        }

        builder.addField(LangID.getStringByID("help.format.usage", lang), LangID.getStringByID("help_"+mainCommand+"_use", lang).replace("_", holder == null ? StaticStore.globalPrefix : holder.config.prefix), false);
        builder.addField(LangID.getStringByID("help.format.description", lang), LangID.getStringByID("help_"+mainCommand+"_desc", lang), false);

        if(parameter) {
            builder.addField(LangID.getStringByID("help.format.parameter", lang), LangID.getStringByID("help_"+mainCommand+"_argu", lang), false);
        }

        if(example) {
            builder.addField(LangID.getStringByID("help.format.example", lang), LangID.getStringByID("help_"+mainCommand+"_exam", lang), false);
        }

        if(tip) {
            int tipIndex = 1;

            while(true) {
                String id = "help_" + mainCommand + "_tip" + (tipIndex == 1 ? "" : String.valueOf(tipIndex));
                String tips = LangID.getStringByID(id, lang);

                if(!tips.equals(id)) {
                    builder.addField(tipIndex == 1 ? LangID.getStringByID("help.format.tip", lang) : "** **", tips, false);
                    tipIndex++;
                } else {
                    break;
                }
            }
        }

        return builder.build();
    }
}
