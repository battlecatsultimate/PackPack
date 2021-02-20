package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.IDHolder;

public class HelpBC implements Command {
    private final int lang;
    private final IDHolder holder;

    public HelpBC(int lang, IDHolder holder) {
        this.lang = lang;
        this.holder = holder;
    }

    @Override
    public void doSomething(MessageCreateEvent event) {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        ch.createEmbed(emb -> {
            emb.setTitle(LangID.getStringByID("helpbc_command", lang));
            emb.setDescription(LangID.getStringByID("helpbc_desc", lang));
            emb.setColor(StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)]);
            emb.addField(holder.serverPrefix+"formstat", LangID.getStringByID("helpbc_fs", lang).replace("_", holder.serverPrefix), false);
            emb.addField(holder.serverPrefix+"enemystat",LangID.getStringByID("helpbc_es", lang).replace("_", holder.serverPrefix),false);
            emb.addField(holder.serverPrefix+"stageinfo", LangID.getStringByID("helpbc_si", lang).replace("_", holder.serverPrefix), false);
            emb.addField(holder.serverPrefix+"music",LangID.getStringByID("helpbc_ms", lang).replace("_", holder.serverPrefix),false);
            emb.addField(holder.serverPrefix+"castle",LangID.getStringByID("helpbc_cs", lang).replace("_", holder.serverPrefix), false);
            String content = LangID.getStringByID("helpbc_fimg", lang)
                    .replace("_", holder.serverPrefix)
                    .replace("A0", LangID.getStringByID("fimg_walk", lang))
                    .replace("A1", LangID.getStringByID("fimg_idle", lang))
                    .replace("A2", LangID.getStringByID("fimg_atk", lang))
                    .replace("A3", LangID.getStringByID("fimg_hitback", lang))
                    .replace("A4", LangID.getStringByID("fimg_enter", lang))
                    .replace("A5", LangID.getStringByID("fimg_burrowdown", lang))
                    .replace("A6", LangID.getStringByID("fimg_burrowmove", lang))
                    .replace("A7", LangID.getStringByID("fimg_burrowup", lang));
            emb.addField(holder.serverPrefix+"formimage", content, false);
            content = LangID.getStringByID("helpbc_eimg", lang)
                    .replace("_", holder.serverPrefix)
                    .replace("A0", LangID.getStringByID("fimg_walk", lang))
                    .replace("A1", LangID.getStringByID("fimg_idle", lang))
                    .replace("A2", LangID.getStringByID("fimg_atk", lang))
                    .replace("A3", LangID.getStringByID("fimg_hitback", lang))
                    .replace("A4", LangID.getStringByID("fimg_enter", lang))
                    .replace("A5", LangID.getStringByID("fimg_burrowdown", lang))
                    .replace("A6", LangID.getStringByID("fimg_burrowmove", lang))
                    .replace("A7", LangID.getStringByID("fimg_burrowup", lang));
            emb.addField(holder.serverPrefix+"enemyimage", content, false);
            emb.addField(holder.serverPrefix+"background", LangID.getStringByID("helpbc_bg", lang).replace("_", holder.serverPrefix), false);
        }).subscribe();
    }
}
