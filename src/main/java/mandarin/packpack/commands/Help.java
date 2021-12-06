package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;

public class Help extends Command {
    private final IDHolder holder;

    public Help(int lang, IDHolder holder) {
        super(lang);

        this.holder = holder;
    }

    @Override
    public void doSomething(MessageEvent event) {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String[] messages = getContent(event).split(" ");

        if(messages.length >= 2) {
            createEmbedOfSpecificCommand(messages[1], ch);
        } else {
            EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder();

            builder.title(LangID.getStringByID("help_command", lang))
                    .description(LangID.getStringByID("help_explain", lang))
                    .color(StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)])
                    .addField(LangID.getStringByID("help_normal", lang), "```analyze, locale, prefix```", false)
                    .addField(LangID.getStringByID("help_bc", lang), "```background, castle, catcombo, enemygif, enemyimage, enemysprite, enemystat, findstage, formgif, formimage, formsprite, formstat, medal, music, stageinfo```", false)
                    .addField(LangID.getStringByID("help_server", lang), "```bcustat, boosteremoji, boosteremojiremove, boosterrole, boosterroleremove, channelpermission, checkbcu, clearcache, fixrole, idset, memory, save, serverjson, serverpre, setup, subscribeevent, watchdm```", false)
                    .addField(LangID.getStringByID("help_data", lang), "```animanalyzer, announcement, checkeventupdate, printgachaevent, printitemevent, printstageevent, stageimage, stagemapimage```", false)
                    .addField(LangID.getStringByID("help_packpack", lang), "```alias, aliasadd, aliasremove, statistic, suggest```", false);

            ch.createMessage(builder.build()).subscribe();
        }
    }

    public void createEmbedOfSpecificCommand(String command, MessageChannel ch) {
        switch (command) {
            case "checkbcu":
                ch.createMessage(addFields("checkbcu", true, false, false)).subscribe();
                break;
            case "bcustat":
                ch.createMessage(addFields( "bcustat", false, false, false)).subscribe();
                break;
            case "analyze":
                ch.createMessage(addFields( "analyze", false, false, false)).subscribe();
                break;
            case "prefix":
                ch.createMessage(addFields( "prefix", false, false, false)).subscribe();
                break;
            case "serverpre":
                ch.createMessage(addFields( "serverpre", false, false, false)).subscribe();
                break;
            case "save":
                ch.createMessage(addFields( "save", false, false, false)).subscribe();
                break;
            case "stimg":
            case "stimage":
            case "stageimg":
            case "stageimage":
                ch.createMessage(addFields( "stageimage", true, true, false)).subscribe();
                break;
            case "stmimg":
            case "stmimage":
            case "stagemapimg":
            case "stagemapimage":
                ch.createMessage(addFields( "stagemapimage", true, true, false)).subscribe();
                break;
            case "formstat":
            case "fs":
                ch.createMessage(addFields( "formstat", true, true, false)).subscribe();
                break;
            case "locale":
            case "loc":
                ch.createMessage(addFields( "locale", false, false, false)).subscribe();
                break;
            case "music":
            case "ms":
                ch.createMessage(addFields( "music", false, false, false)).subscribe();
                break;
            case "enemystat":
            case "es":
                ch.createMessage(addFields( "enemystat", true, true, false)).subscribe();
                break;
            case "castle":
            case "cs":
                ch.createMessage(addFields( "castle", true, true, false)).subscribe();
                break;
            case "stageinfo":
            case "si":
                ch.createMessage(addFields( "stageinfo", true, true, false)).subscribe();
                break;
            case "memory":
            case "mm":
                ch.createMessage(addFields( "memory", false, false,false)).subscribe();
                break;
            case "formimage":
            case "formimg":
            case "fimage":
            case "fimg":
                ch.createMessage(addFields( "formimage", true, true, true)).subscribe();
                break;
            case "enemyimage":
            case "enemyimg":
            case "eimage":
            case "eimg":
                ch.createMessage(addFields( "enemyimage", true, true, true)).subscribe();
                break;
            case "background":
            case "bg":
                ch.createMessage(addFields( "background", true, true, false)).subscribe();
                break;
            case "formgif":
            case "fgif":
            case "fg":
                ch.createMessage(addFields( "formgif", true, true, true)).subscribe();
                break;
            case "enemygif":
            case "egif":
            case "eg":
                ch.createMessage(addFields( "enemygif", true, true, true)).subscribe();
                break;
            case "idset":
                ch.createMessage(addFields( "idset", true, true, true)).subscribe();
                break;
            case "clearcache":
                ch.createMessage(addFields( "clearcache", false, false, false)).subscribe();
                break;
            case "aa":
            case "animanalyzer":
                ch.createMessage(addFields( "animanalyzer", true, false, true)).subscribe();
                break;
            case "channelpermission":
            case "channelperm":
            case "chpermission":
            case "chperm":
            case "chp":
                ch.createMessage(addFields( "channelpermission", true, true, true)).subscribe();
                break;
            case "formsprite":
            case "fsprite":
            case "formsp":
            case "fsp":
                ch.createMessage(addFields( "formsprite", true, true, false)).subscribe();
                break;
            case "enemysprite":
            case "esprite":
            case "enemysp":
            case "esp":
                ch.createMessage(addFields( "enemysprite", true, true, false)).subscribe();
                break;
            case "medal":
            case "md":
                ch.createMessage(addFields( "medal", false, true, false)).subscribe();
                break;
            case "announcement":
            case "ann":
                ch.createMessage(addFields( "announcement", true, true, false)).subscribe();
                break;
            case "catcombo":
            case "combo":
            case "cc":
                ch.createMessage(addFields( "catcombo", true, true, false)).subscribe();
                break;
            case "serverjson":
            case "json":
            case "sj":
                ch.createMessage(addFields( "serverjson", false, false ,false)).subscribe();
                break;
            case "findstage":
            case "findst":
            case "fstage":
            case "fst":
                ch.createMessage(addFields( "findstage", true, true, false)).subscribe();
                break;
            case "suggest":
                ch.createMessage(addFields( "suggest", true, true, true)).subscribe();
                break;
            case "alias":
            case "al":
                ch.createMessage(addFields( "alias", true, true, true)).subscribe();
                break;
            case "aliasadd":
            case "ala":
                ch.createMessage(addFields( "aliasadd", true, true, true)).subscribe();
                break;
            case "aliasremove":
            case "alr":
                ch.createMessage(addFields( "aliasremove", true, true, true)).subscribe();
                break;
            case "statistic":
            case "stat":
                ch.createMessage(addFields( "statistic", false, false, false)).subscribe();
                break;
            case "serverlocale":
            case "slocale":
            case "serverloc":
            case "sloc":
                ch.createMessage(addFields( "serverlocale", false, false, false)).subscribe();
                break;
            case "boosterrole":
            case "boosterr":
            case "brole":
            case "br":
                ch.createMessage(addFields( "boosterrole", true, true, true)).subscribe();
                break;
            case "boosterroleremove":
            case "brremove":
            case "boosterrolerem":
            case "brrem":
            case "brr":
                ch.createMessage(addFields( "boosterroleremove", true, false, false)).subscribe();
                break;
            case "boosteremoji":
            case "boostere":
            case "bemoji":
            case "be":
                ch.createMessage(addFields( "boosteremoji", false, true, true)).subscribe();
                break;
            case "boosteremojiremove":
            case "beremove":
            case"boosteremojirem":
            case "berem":
            case "ber":
                ch.createMessage(addFields( "boosteremojiremove", true, false, false)).subscribe();
                break;
            case "setup":
                ch.createMessage(addFields("setup", false, false, true)).subscribe();
                break;
            case "fixrole":
            case "fr":
                ch.createMessage(addFields("fixrole", true, true, true)).subscribe();
                break;
            case "watchdm":
            case "wd":
                ch.createMessage(addFields("watchdm", false, false, true)).subscribe();
                break;
            case "checkeventupdate":
            case "ceu":
                ch.createMessage(addFields("checkeventupdate", false, false, false)).subscribe();
                break;
            case "printgachaevent":
            case "pge":
                ch.createMessage(addFields("printgachaevent", true, true, false)).subscribe();
                break;
            case "printitemevent":
            case "pie":
                ch.createMessage(addFields("printitemevent", true, true, false)).subscribe();
                break;
            case "printstageevent":
            case "pse":
                ch.createMessage(addFields("printstageevent", true, true, false)).subscribe();
                break;
            case "subscribeevent":
            case "se":
                ch.createMessage(addFields("subscribeevent", false, true, true)).subscribe();
                break;
            default:
                createMessageWithNoPings(ch, LangID.getStringByID("help_nocomm", lang).replace("_", command));
        }
    }

    private EmbedCreateSpec addFields(String mainCommand, boolean argument, boolean example, boolean tip) {
        EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder();

        builder.color(StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)]);
        builder.title(holder.serverPrefix+mainCommand);
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
