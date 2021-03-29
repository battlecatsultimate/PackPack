package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.IDHolder;

public class Help implements Command {
    private final int lang;
    private final IDHolder holder;

    public Help(int lang, IDHolder holder) {
        this.lang = lang;
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
            ch.createEmbed(emb -> {
                emb.setTitle(LangID.getStringByID("help_command", lang));
                emb.setDescription(LangID.getStringByID("help_explain", lang));
                emb.setColor(StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)]);
                emb.addField(LangID.getStringByID("help_normal", lang), "```analyze, locale, prefix```", false);
                emb.addField(LangID.getStringByID("help_bc", lang), "```background, castle, enemygif, enemyimage, enemysprite, enemystat, formgif, formimage, formsprite, formstat, medal, music, stageinfo```", false);
                emb.addField(LangID.getStringByID("help_server", lang), "```bcustat, channelpermission, checkbcu, clearcache, idset, memory, save, serverprefix```", false);
                emb.addField(LangID.getStringByID("help_data", lang), "```animanalyzer, announcement, stageimage, stagemapimage```", false);
            }).subscribe();
        }
    }

    public void createEmbedOfSpecificCommand(String command, MessageChannel ch) {
        switch (command) {
            case "checkbcu":
                ch.createEmbed(e -> addFields(e, "checkbcu", false, false, false)).subscribe();
                break;
            case "bcustat":
                ch.createEmbed(e -> addFields(e, "bcustat", false, false, false)).subscribe();
                break;

            case "analyze":
                ch.createEmbed(e -> addFields(e, "analyze", false, false, false)).subscribe();
                break;
            case "prefix":
                ch.createEmbed(e -> addFields(e, "prefix", false, false, false)).subscribe();
                break;
            case "serverpre":
                ch.createEmbed(e -> addFields(e, "serverpre", false, false, false)).subscribe();
                break;
            case "save":
                ch.createEmbed(e -> addFields(e, "save", false, false, false)).subscribe();
                break;
            case "stimg":
            case "stimage":
            case "stageimg":
            case "stageimage":
                ch.createEmbed(e -> addFields(e, "stageimage", true, true, false)).subscribe();
                break;
            case "stmimg":
            case "stmimage":
            case "stagemapimg":
            case "stagemapimage":
                ch.createEmbed(e -> addFields(e, "stagemapimage", true, true, false)).subscribe();
                break;
            case "formstat":
            case "fs":
                ch.createEmbed(e -> addFields(e, "formstat", true, true, false)).subscribe();
                break;
            case "locale":
            case "loc":
                ch.createEmbed(e -> addFields(e, "locale", false, false, false)).subscribe();
                break;
            case "music":
            case "ms":
                ch.createEmbed(e -> addFields(e, "music", false, false, false)).subscribe();
                break;
            case "enemystat":
            case "es":
                ch.createEmbed(e -> addFields(e, "enemystat", true, true, false)).subscribe();
                break;
            case "castle":
            case "cs":
                ch.createEmbed(e -> addFields(e, "castle", true, true, false)).subscribe();
                break;
            case "stageinfo":
            case "si":
                ch.createEmbed(e -> addFields(e, "stageinfo", true, true, false)).subscribe();
                break;
            case "memory":
            case "mm":
                ch.createEmbed(e -> addFields(e, "memory", false, false,false)).subscribe();
                break;
            case "formimage":
            case "formimg":
            case "fimage":
            case "fimg":
                ch.createEmbed(e -> addFields(e, "formimage", true, true, true)).subscribe();
                break;
            case "enemyimage":
            case "enemyimg":
            case "eimage":
            case "eimg":
                ch.createEmbed(e -> addFields(e, "enemyimage", true, true, true)).subscribe();
                break;
            case "background":
            case "bg":
                ch.createEmbed(e -> addFields(e, "background", true, true, false)).subscribe();
                break;
            case "formgif":
            case "fgif":
            case "fg":
                ch.createEmbed(e -> addFields(e, "formgif", true, true, true)).subscribe();
                break;
            case "enemygif":
            case "egif":
            case "eg":
                ch.createEmbed(e -> addFields(e, "enemygif", true, true, true)).subscribe();
                break;
            case "idset":
                ch.createEmbed(e -> addFields(e, "idset", true, true, true)).subscribe();
                break;
            case "clearcache":
                ch.createEmbed(e -> addFields(e, "clearcache", false, false, false)).subscribe();
                break;
            case "aa":
            case "animanalyzer":
                ch.createEmbed(e -> addFields(e, "animanalyzer", true, false, true)).subscribe();
                break;
            case "channelpermission":
            case "channelperm":
            case "chpermission":
            case "chperm":
            case "chp":
                ch.createEmbed(e -> addFields(e, "channelpermission", true, true, true)).subscribe();
                break;
            case "formsprite":
            case "fsprite":
            case "formsp":
            case "fsp":
                ch.createEmbed(e -> addFields(e, "formsprite", true, true, false)).subscribe();
                break;
            case "enemysprite":
            case "esprite":
            case "enemysp":
            case "esp":
                ch.createEmbed(e -> addFields(e, "enemysprite", true, true, false)).subscribe();
                break;
            case "medal":
            case "md":
                ch.createEmbed(e -> addFields(e, "medal", false, true, false)).subscribe();
                break;
            case "announcement":
            case "ann":
                ch.createEmbed(e -> addFields(e, "announcement", true, true, false)).subscribe();
                break;
            case "catcombo":
            case "combo":
            case "cc":
                ch.createEmbed(e -> addFields(e, "catcombo", true, true, false)).subscribe();
                break;
            default:
                ch.createMessage(LangID.getStringByID("help_nocomm", lang).replace("_", command)).subscribe();
        }
    }

    private void addFields(EmbedCreateSpec e, String mainCommand, boolean argument, boolean example, boolean tip) {
        e.setColor(StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)]);
        e.setTitle(holder.serverPrefix+mainCommand);
        e.addField(LangID.getStringByID("help_use", lang), LangID.getStringByID("help_"+mainCommand+"_use", lang).replace("_", holder.serverPrefix), false);
        e.addField(LangID.getStringByID("help_desc", lang), LangID.getStringByID("help_"+mainCommand+"_desc", lang), false);

        if(argument) {
            e.addField(LangID.getStringByID("help_argu", lang), LangID.getStringByID("help_"+mainCommand+"_argu", lang), false);
        }

        if(example) {
            e.addField(LangID.getStringByID("help_exam", lang), LangID.getStringByID("help_"+mainCommand+"_exam", lang), false);
        }

        if(tip) {
            e.addField(LangID.getStringByID("help_tip", lang), LangID.getStringByID("help_"+mainCommand+"_tip", lang), false);
        }
    }
}
