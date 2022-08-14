package mandarin.packpack.commands;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

public class Help extends Command {
    private final IDHolder holder;

    public Help(int lang, IDHolder holder) {
        super(lang);

        this.holder = holder;
    }

    @Override
    public void prepare() throws Exception {
        registerRequiredPermission(Permission.MESSAGE_EMBED_LINKS);
    }

    @Override
    public void doSomething(GenericMessageEvent event) {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String[] messages = getContent(event).split(" ");

        if(messages.length >= 2) {
            createEmbedOfSpecificCommand(messages[1], ch);
        } else {
            EmbedBuilder builder = new EmbedBuilder();

            builder.setTitle(LangID.getStringByID("help_command", lang))
                    .setDescription(LangID.getStringByID("help_explain", lang))
                    .setColor(StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)])
                    .addField(LangID.getStringByID("help_normal", lang), "```analyze, config, locale, optout, prefix, timezone```", false)
                    .addField(LangID.getStringByID("help_bc", lang), "```background, castle, catcombo, enemygif, enemyimage, enemysprite, enemystat, findstage, formgif, formimage, formsprite, formstat, medal, music, stageinfo```", false)
                    .addField(LangID.getStringByID("help_server", lang), "```bcustat, boosteremoji, boosteremojiremove, boosterrole, boosterroleremove, channelpermission, checkbcu, clearcache, fixrole, idset, memory, save, serverconfig, serverjson, serverpre, setup, subscribeevent, subscribescamlinkdetector, unsubscribescamlinkdetector, watchdm```", false)
                    .addField(LangID.getStringByID("help_data", lang), "```animanalyzer, announcement, checkeventupdate, printevent, printgachaevent, printitemevent, printstageevent, stageimage, stagestatanalyzer, statanalyzer, stagemapimage, trueformanalyzer```", false)
                    .addField(LangID.getStringByID("help_packpack", lang), "```alias, aliasadd, aliasremove, registerscamlink, statistic, suggest, unregisterscamlink```", false);

            ch.sendMessageEmbeds(builder.build()).queue();
        }
    }

    public void createEmbedOfSpecificCommand(String command, MessageChannel ch) {
        switch (command) {
            case "checkbcu":
                ch.sendMessageEmbeds(addFields("checkbcu", true, false, false)).queue();
                break;
            case "bcustat":
                ch.sendMessageEmbeds(addFields( "bcustat", false, false, false)).queue();
                break;
            case "analyze":
                ch.sendMessageEmbeds(addFields( "analyze", false, false, false)).queue();
                break;
            case "prefix":
                ch.sendMessageEmbeds(addFields( "prefix", false, false, false)).queue();
                break;
            case "serverpre":
                ch.sendMessageEmbeds(addFields( "serverpre", false, false, false)).queue();
                break;
            case "save":
                ch.sendMessageEmbeds(addFields( "save", false, false, false)).queue();
                break;
            case "stimg":
            case "stimage":
            case "stageimg":
            case "stageimage":
                ch.sendMessageEmbeds(addFields( "stageimage", true, true, false)).queue();
                break;
            case "stmimg":
            case "stmimage":
            case "stagemapimg":
            case "stagemapimage":
                ch.sendMessageEmbeds(addFields( "stagemapimage", true, true, false)).queue();
                break;
            case "formstat":
            case "fs":
                ch.sendMessageEmbeds(addFields( "formstat", true, true, false)).queue();
                break;
            case "locale":
            case "loc":
                ch.sendMessageEmbeds(addFields( "locale", false, false, false)).queue();
                break;
            case "music":
            case "ms":
                ch.sendMessageEmbeds(addFields( "music", false, false, false)).queue();
                break;
            case "enemystat":
            case "es":
                ch.sendMessageEmbeds(addFields( "enemystat", true, true, false)).queue();
                break;
            case "castle":
            case "cs":
                ch.sendMessageEmbeds(addFields( "castle", true, true, false)).queue();
                break;
            case "stageinfo":
            case "si":
                ch.sendMessageEmbeds(addFields( "stageinfo", true, true, false)).queue();
                break;
            case "memory":
            case "mm":
                ch.sendMessageEmbeds(addFields( "memory", false, false,false)).queue();
                break;
            case "formimage":
            case "formimg":
            case "fimage":
            case "fimg":
                ch.sendMessageEmbeds(addFields( "formimage", true, true, true)).queue();
                break;
            case "enemyimage":
            case "enemyimg":
            case "eimage":
            case "eimg":
                ch.sendMessageEmbeds(addFields( "enemyimage", true, true, true)).queue();
                break;
            case "background":
            case "bg":
                ch.sendMessageEmbeds(addFields( "background", true, true, false)).queue();
                break;
            case "formgif":
            case "fgif":
            case "fg":
                ch.sendMessageEmbeds(addFields( "formgif", true, true, true)).queue();
                break;
            case "enemygif":
            case "egif":
            case "eg":
                ch.sendMessageEmbeds(addFields( "enemygif", true, true, true)).queue();
                break;
            case "idset":
                ch.sendMessageEmbeds(addFields( "idset", true, true, true)).queue();
                break;
            case "clearcache":
                ch.sendMessageEmbeds(addFields( "clearcache", false, false, false)).queue();
                break;
            case "aa":
            case "animanalyzer":
                ch.sendMessageEmbeds(addFields( "animanalyzer", true, false, true)).queue();
                break;
            case "channelpermission":
            case "channelperm":
            case "chpermission":
            case "chperm":
            case "chp":
                ch.sendMessageEmbeds(addFields( "channelpermission", true, true, true)).queue();
                break;
            case "formsprite":
            case "fsprite":
            case "formsp":
            case "fsp":
                ch.sendMessageEmbeds(addFields( "formsprite", true, true, false)).queue();
                break;
            case "enemysprite":
            case "esprite":
            case "enemysp":
            case "esp":
                ch.sendMessageEmbeds(addFields( "enemysprite", true, true, false)).queue();
                break;
            case "medal":
            case "md":
                ch.sendMessageEmbeds(addFields( "medal", false, true, false)).queue();
                break;
            case "announcement":
            case "ann":
                ch.sendMessageEmbeds(addFields( "announcement", true, true, false)).queue();
                break;
            case "catcombo":
            case "combo":
            case "cc":
                ch.sendMessageEmbeds(addFields( "catcombo", true, true, false)).queue();
                break;
            case "serverjson":
            case "json":
            case "sj":
                ch.sendMessageEmbeds(addFields( "serverjson", false, false ,false)).queue();
                break;
            case "findstage":
            case "findst":
            case "fstage":
            case "fst":
                ch.sendMessageEmbeds(addFields( "findstage", true, true, false)).queue();
                break;
            case "suggest":
                ch.sendMessageEmbeds(addFields( "suggest", true, true, true)).queue();
                break;
            case "alias":
            case "al":
                ch.sendMessageEmbeds(addFields( "alias", true, true, true)).queue();
                break;
            case "aliasadd":
            case "ala":
                ch.sendMessageEmbeds(addFields( "aliasadd", true, true, true)).queue();
                break;
            case "aliasremove":
            case "alr":
                ch.sendMessageEmbeds(addFields( "aliasremove", true, true, true)).queue();
                break;
            case "statistic":
            case "stat":
                ch.sendMessageEmbeds(addFields( "statistic", false, false, false)).queue();
                break;
            case "serverlocale":
            case "slocale":
            case "serverloc":
            case "sloc":
                ch.sendMessageEmbeds(addFields( "serverlocale", false, false, false)).queue();
                break;
            case "boosterrole":
            case "boosterr":
            case "brole":
            case "br":
                ch.sendMessageEmbeds(addFields( "boosterrole", true, true, true)).queue();
                break;
            case "boosterroleremove":
            case "brremove":
            case "boosterrolerem":
            case "brrem":
            case "brr":
                ch.sendMessageEmbeds(addFields( "boosterroleremove", true, false, false)).queue();
                break;
            case "boosteremoji":
            case "boostere":
            case "bemoji":
            case "be":
                ch.sendMessageEmbeds(addFields( "boosteremoji", false, true, true)).queue();
                break;
            case "boosteremojiremove":
            case "beremove":
            case"boosteremojirem":
            case "berem":
            case "ber":
                ch.sendMessageEmbeds(addFields( "boosteremojiremove", true, false, false)).queue();
                break;
            case "setup":
                ch.sendMessageEmbeds(addFields("setup", false, false, true)).queue();
                break;
            case "fixrole":
            case "fr":
                ch.sendMessageEmbeds(addFields("fixrole", true, true, true)).queue();
                break;
            case "watchdm":
            case "wd":
                ch.sendMessageEmbeds(addFields("watchdm", false, false, true)).queue();
                break;
            case "checkeventupdate":
            case "ceu":
                ch.sendMessageEmbeds(addFields("checkeventupdate", false, false, false)).queue();
                break;
            case "printgachaevent":
            case "pge":
                ch.sendMessageEmbeds(addFields("printgachaevent", true, true, false)).queue();
                break;
            case "printitemevent":
            case "pie":
                ch.sendMessageEmbeds(addFields("printitemevent", true, true, false)).queue();
                break;
            case "printstageevent":
            case "pse":
                ch.sendMessageEmbeds(addFields("printstageevent", true, true, false)).queue();
                break;
            case "subscribeevent":
            case "se":
                ch.sendMessageEmbeds(addFields("subscribeevent", false, true, true)).queue();
                break;
            case "printevent":
            case "pe":
                ch.sendMessageEmbeds(addFields("printevent", true, true, false)).queue();
                break;
            case "statanalyzer":
            case "sa":
                ch.sendMessageEmbeds(addFields("statanalyzer", true, true, true)).queue();
                ch.sendMessageEmbeds(new EmbedBuilder()
                        .setColor(StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)])
                        .setDescription(LangID.getStringByID("help_statanalyzer_adddesc", lang))
                        .addField("-name", LangID.getStringByID("help_statanalyzer_name", lang), false)
                        .addField("-trait", LangID.getStringByID("help_statanalyzer_trait", lang), false)
                        .addField("-cell", LangID.getStringByID("help_statanalyzer_cell", lang), false)
                        .addField("-abil", LangID.getStringByID("help_statanalyzer_abil", lang), false)
                        .addField("-proc", LangID.getStringByID("help_statanalyzer_proc", lang), false)
                        .build()).queue();
                break;
            case "registerscamlink":
            case "rsl":
                ch.sendMessageEmbeds(addFields("registerscamlink", false, false, true)).queue();
                break;
            case "unregisterscamlink":
            case "usl":
                ch.sendMessageEmbeds(addFields("unregisterscamlink", false, false, false)).queue();
                break;
            case "subscribescamlinkdetector":
            case "ssld":
            case "ssd":
                ch.sendMessageEmbeds(addFields("subscribescamlinkdetector", true, false, true)).queue();
                break;
            case "unsubscribescamlinkdetector":
            case "usld":
            case "usd":
                ch.sendMessageEmbeds(addFields("unsubscribescamlinkdetector", false, false, false)).queue();
                break;
            case "optout":
                ch.sendMessageEmbeds(addFields("optout", false, false, false)).queue();
                break;
            case "config":
                ch.sendMessageEmbeds(addFields("config", false, false, true)).queue();
                break;
            case "downloadapk":
            case "da":
                ch.sendMessageEmbeds(addFields("downloadapk", true, true, false)).queue();
                break;
            case "trueformanalyzer":
            case "tfanalyzer":
            case "trueforma":
            case "tfa":
                ch.sendMessageEmbeds(addFields("trueformanalyzer", true, true, true)).queue();
                ch.sendMessageEmbeds(new EmbedBuilder()
                        .setColor(StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)])
                        .setDescription(LangID.getStringByID("help_statanalyzer_adddesc", lang))
                        .addField("-name", LangID.getStringByID("help_statanalyzer_name", lang), false)
                        .addField("-trait", LangID.getStringByID("help_statanalyzer_trait", lang), false)
                        .addField("-cell", LangID.getStringByID("help_statanalyzer_cell", lang), false)
                        .addField("-abil", LangID.getStringByID("help_statanalyzer_abil", lang), false)
                        .addField("-proc", LangID.getStringByID("help_statanalyzer_proc", lang), false)
                        .build()).queue();
                break;
            case "enemystatanalyzer":
            case "estatanalyzer":
            case "enemysa":
            case "esa":
                ch.sendMessageEmbeds(addFields("enemystatanalyzer", true, true, true)).queue();
                ch.sendMessageEmbeds(new EmbedBuilder()
                        .setColor(StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)])
                        .setDescription(LangID.getStringByID("help_statanalyzer_adddesc", lang))
                        .addField("-name", LangID.getStringByID("help_statanalyzer_name", lang), false)
                        .addField("-trait", LangID.getStringByID("help_statanalyzer_trait", lang), false)
                        .addField("-cell", LangID.getStringByID("help_statanalyzer_cell", lang), false)
                        .addField("-abil", LangID.getStringByID("help_statanalyzer_abil", lang), false)
                        .addField("-proc", LangID.getStringByID("help_statanalyzer_proc", lang), false)
                        .build()).queue();
                break;
            case "stagestatanalyzer":
            case "sstatanalyzer":
            case "stagesa":
            case "ssa":
                ch.sendMessageEmbeds(addFields("stagestatanalyzer", true, true, true)).queue();
                break;
            case "serverconfig":
            case "sconfig":
            case "serverc":
            case "sc":
                ch.sendMessageEmbeds(addFields("serverconfig", false, false, true)).queue();
                break;
            default:
                createMessageWithNoPings(ch, LangID.getStringByID("help_nocomm", lang).replace("_", command));
        }
    }

    private MessageEmbed addFields(String mainCommand, boolean argument, boolean example, boolean tip) {
        EmbedBuilder builder = new EmbedBuilder();

        builder.setColor(StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)]);
        builder.setTitle(holder.serverPrefix+mainCommand);
        builder.addField(LangID.getStringByID("help_use", lang), LangID.getStringByID("help_"+mainCommand+"_use", lang).replace("_", holder.serverPrefix), false);
        builder.addField(LangID.getStringByID("help_desc", lang), LangID.getStringByID("help_"+mainCommand+"_desc", lang), false);

        if(argument) {
            builder.addField(LangID.getStringByID("help_argu", lang), LangID.getStringByID("help_"+mainCommand+"_argu", lang), false);
        }

        if(example) {
            builder.addField(LangID.getStringByID("help_exam", lang), LangID.getStringByID("help_"+mainCommand+"_exam", lang), false);
        }

        if(tip) {
            builder.addField(LangID.getStringByID("help_tip", lang), LangID.getStringByID("help_"+mainCommand+"_tip", lang), false);
        }

        return builder.build();
    }
}
