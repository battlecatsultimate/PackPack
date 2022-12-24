package mandarin.packpack.commands;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
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
            createEmbedOfSpecificCommand(messages[1], ch, getMessage(event));
        } else {
            EmbedBuilder builder = new EmbedBuilder();

            builder.setTitle(LangID.getStringByID("help_command", lang))
                    .setDescription(LangID.getStringByID("help_explain", lang))
                    .setColor(StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)])
                    .addField(LangID.getStringByID("help_normal", lang), "```analyze, calculator, config, locale, optout, prefix, timezone```", false)
                    .addField(LangID.getStringByID("help_bc", lang), "```background, castle, catcombo, enemygif, enemyimage, enemysprite, enemystat, findreward, findstage, formgif, formimage, formsprite, formstat, medal, music, soul, soulimage, soulsprite, stageinfo, talentinfo```", false)
                    .addField(LangID.getStringByID("help_server", lang), "```boosteremoji, boosteremojiremove, boosterrole, boosterroleremove, channelpermission, checkbcu, clearcache, commandban, commandunban, fixrole, idset, memory, save, serverconfig, serverjson, serverpre, serverstat, setup, subscribeevent, subscribescamlinkdetector, unsubscribescamlinkdetector, watchdm```", false)
                    .addField(LangID.getStringByID("help_data", lang), "```animanalyzer, announcement, checkeventupdate, comboanalyzer, downloadapk, enemystatanalyzer, eventdataarchive, printevent, printgachaevent, printitemevent, printstageevent, stageimage, stagestatanalyzer, statanalyzer, stagemapimage, talentanalyzer, trueformanalyzer```", false)
                    .addField(LangID.getStringByID("help_packpack", lang), "```alias, aliasadd, aliasremove, registerscamlink, statistic, suggest, unregisterscamlink```", false);

            replyToMessageSafely(ch, "", getMessage(event), a -> a.setEmbeds(builder.build()));
        }
    }

    public void createEmbedOfSpecificCommand(String command, MessageChannel ch, Message reference) {
        switch (command) {
            case "checkbcu":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("checkbcu", true, false, false)));
                break;
            case "serverstat":
            case "ss":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "bcustat", false, false, false)));
                break;
            case "analyze":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "analyze", false, false, false)));
                break;
            case "prefix":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "prefix", false, false, false)));
                break;
            case "serverpre":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "serverpre", false, false, false)));
                break;
            case "save":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "save", false, false, false)));
                break;
            case "stimg":
            case "stimage":
            case "stageimg":
            case "stageimage":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "stageimage", true, true, false)));
                break;
            case "stmimg":
            case "stmimage":
            case "stagemapimg":
            case "stagemapimage":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "stagemapimage", true, true, false)));
                break;
            case "formstat":
            case "fs":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "formstat", true, true, false)));
                break;
            case "locale":
            case "loc":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "locale", false, false, false)));
                break;
            case "music":
            case "ms":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "music", false, false, false)));
                break;
            case "enemystat":
            case "es":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "enemystat", true, true, false)));
                break;
            case "castle":
            case "cs":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "castle", true, true, false)));
                break;
            case "stageinfo":
            case "si":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "stageinfo", true, true, false)));
                break;
            case "memory":
            case "mm":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "memory", false, false,false)));
                break;
            case "formimage":
            case "formimg":
            case "fimage":
            case "fimg":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "formimage", true, true, true)));
                break;
            case "enemyimage":
            case "enemyimg":
            case "eimage":
            case "eimg":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "enemyimage", true, true, true)));
                break;
            case "background":
            case "bg":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "background", true, true, false)));
                break;
            case "formgif":
            case "fgif":
            case "fg":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "formgif", true, true, true)));
                break;
            case "enemygif":
            case "egif":
            case "eg":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "enemygif", true, true, true)));
                break;
            case "idset":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "idset", true, true, true)));
                break;
            case "clearcache":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "clearcache", false, false, false)));
                break;
            case "aa":
            case "animanalyzer":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "animanalyzer", true, false, true)));
                break;
            case "channelpermission":
            case "channelperm":
            case "chpermission":
            case "chperm":
            case "chp":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "channelpermission", true, true, true)));
                break;
            case "formsprite":
            case "fsprite":
            case "formsp":
            case "fsp":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "formsprite", true, true, false)));
                break;
            case "enemysprite":
            case "esprite":
            case "enemysp":
            case "esp":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "enemysprite", true, true, false)));
                break;
            case "medal":
            case "md":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "medal", false, true, false)));
                break;
            case "announcement":
            case "ann":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "announcement", true, true, false)));
                break;
            case "catcombo":
            case "combo":
            case "cc":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "catcombo", true, true, false)));
                break;
            case "serverjson":
            case "json":
            case "sj":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "serverjson", false, false ,false)));
                break;
            case "findstage":
            case "findst":
            case "fstage":
            case "fst":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "findstage", true, true, true)));
                break;
            case "suggest":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "suggest", true, true, true)));
                break;
            case "alias":
            case "al":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "alias", true, true, true)));
                break;
            case "aliasadd":
            case "ala":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "aliasadd", true, true, true)));
                break;
            case "aliasremove":
            case "alr":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "aliasremove", true, true, true)));
                break;
            case "statistic":
            case "stat":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "statistic", false, false, false)));
                break;
            case "serverlocale":
            case "slocale":
            case "serverloc":
            case "sloc":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "serverlocale", false, false, false)));
                break;
            case "boosterrole":
            case "boosterr":
            case "brole":
            case "br":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "boosterrole", true, true, true)));
                break;
            case "boosterroleremove":
            case "brremove":
            case "boosterrolerem":
            case "brrem":
            case "brr":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "boosterroleremove", true, false, false)));
                break;
            case "boosteremoji":
            case "boostere":
            case "bemoji":
            case "be":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "boosteremoji", false, true, true)));
                break;
            case "boosteremojiremove":
            case "beremove":
            case"boosteremojirem":
            case "berem":
            case "ber":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields( "boosteremojiremove", true, false, false)));
                break;
            case "setup":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("setup", false, false, true)));
                break;
            case "fixrole":
            case "fir":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("fixrole", true, true, true)));
                break;
            case "watchdm":
            case "wd":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("watchdm", false, false, true)));
                break;
            case "checkeventupdate":
            case "ceu":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("checkeventupdate", false, false, false)));
                break;
            case "printgachaevent":
            case "pge":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("printgachaevent", true, true, false)));
                break;
            case "printitemevent":
            case "pie":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("printitemevent", true, true, false)));
                break;
            case "printstageevent":
            case "pse":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("printstageevent", true, true, false)));
                break;
            case "subscribeevent":
            case "se":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("subscribeevent", false, true, true)));
                break;
            case "printevent":
            case "pe":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("printevent", true, true, false)));
                break;
            case "statanalyzer":
            case "sa":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("statanalyzer", true, true, true)));
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(new EmbedBuilder()
                        .setColor(StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)])
                        .setDescription(LangID.getStringByID("help_statanalyzer_adddesc", lang))
                        .addField("-name", LangID.getStringByID("help_statanalyzer_name", lang), false)
                        .addField("-trait", LangID.getStringByID("help_statanalyzer_trait", lang), false)
                        .addField("-cell", LangID.getStringByID("help_statanalyzer_cell", lang), false)
                        .addField("-abil", LangID.getStringByID("help_statanalyzer_abil", lang), false)
                        .addField("-proc", LangID.getStringByID("help_statanalyzer_proc", lang), false)
                        .build()));
                break;
            case "registerscamlink":
            case "rsl":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("registerscamlink", false, false, true)));
                break;
            case "unregisterscamlink":
            case "usl":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("unregisterscamlink", false, false, false)));
                break;
            case "subscribescamlinkdetector":
            case "ssld":
            case "ssd":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("subscribescamlinkdetector", true, false, true)));
                break;
            case "unsubscribescamlinkdetector":
            case "usld":
            case "usd":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("unsubscribescamlinkdetector", false, false, false)));
                break;
            case "optout":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("optout", false, false, false)));
                break;
            case "config":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("config", false, false, true)));
                break;
            case "downloadapk":
            case "da":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("downloadapk", true, true, false)));
                break;
            case "trueformanalyzer":
            case "tfanalyzer":
            case "trueforma":
            case "tfa":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("trueformanalyzer", true, true, true)));
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(new EmbedBuilder()
                        .setColor(StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)])
                        .setDescription(LangID.getStringByID("help_statanalyzer_adddesc", lang))
                        .addField("-name", LangID.getStringByID("help_statanalyzer_name", lang), false)
                        .addField("-trait", LangID.getStringByID("help_statanalyzer_trait", lang), false)
                        .addField("-cell", LangID.getStringByID("help_statanalyzer_cell", lang), false)
                        .addField("-abil", LangID.getStringByID("help_statanalyzer_abil", lang), false)
                        .addField("-proc", LangID.getStringByID("help_statanalyzer_proc", lang), false)
                        .build()));
                break;
            case "enemystatanalyzer":
            case "estatanalyzer":
            case "enemysa":
            case "esa":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("enemystatanalyzer", true, true, true)));
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(new EmbedBuilder()
                        .setColor(StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)])
                        .setDescription(LangID.getStringByID("help_statanalyzer_adddesc", lang))
                        .addField("-name", LangID.getStringByID("help_statanalyzer_name", lang), false)
                        .addField("-trait", LangID.getStringByID("help_statanalyzer_trait", lang), false)
                        .addField("-cell", LangID.getStringByID("help_statanalyzer_cell", lang), false)
                        .addField("-abil", LangID.getStringByID("help_statanalyzer_abil", lang), false)
                        .addField("-proc", LangID.getStringByID("help_statanalyzer_proc", lang), false)
                        .build()));
                break;
            case "stagestatanalyzer":
            case "sstatanalyzer":
            case "stagesa":
            case "ssa":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("stagestatanalyzer", true, true, true)));
                break;
            case "serverconfig":
            case "sconfig":
            case "serverc":
            case "sc":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("serverconfig", false, false, true)));
                break;
            case "commandban":
            case "cb":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("commandban", false, false, false)));
                break;
            case "commandunban":
            case "cub":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("commandunban", false, false, false)));
                break;
            case "talentinfo":
            case "ti":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("talentinfo", true, false, false)));
                break;
            case "soul":
            case "sl":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("soul", true, true, false)));
                break;
            case "soulimage":
            case "soulimg":
            case "simage":
            case "simg":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("soulimage", true, true, false)));
                break;
            case "soulsprite":
            case "ssprite":
            case "soulsp":
            case "ssp":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("soulsprite", false, false, false)));
                break;
            case "calculator":
            case "calc":
            case "c":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("calculator", false, true, true)));
                break;
            case "findreward":
            case "freward":
            case "findr":
            case "fr":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("findreward", true, true, false)));
                break;
            case "eventdataarchive":
            case "eventddataa":
            case "earchive":
            case "eda":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("eventdataarchive", true, true, false)));
                break;
            case "announcemessage":
            case "am":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("announcemessage", false, false, true)));
                break;
            case "talentanalyzer":
            case "tala":
            case "ta":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("talentanalyzer", true, true, false)));
                break;
            case "comboanalyzer":
            case "catcomboanalyzer":
            case "cca":
            case "ca":
                replyToMessageSafely(ch, "", reference, a -> a.setEmbeds(addFields("comboanalyzer", true, false, true)));
                break;
            default:
                replyToMessageSafely(ch, LangID.getStringByID("help_nocomm", lang).replace("_", command), reference, a -> a);
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
