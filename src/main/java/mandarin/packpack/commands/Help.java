package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
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
    public void doSomething(MessageCreateEvent event) {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        ch.createEmbed(emb -> {
            emb.setTitle(LangID.getStringByID("help_command", lang));
            emb.setDescription(LangID.getStringByID("help_desc", lang));
            emb.setColor(StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)]);
            emb.addField(holder.serverPrefix+"bcustat", LangID.getStringByID("help_bcustat", lang).replace("_", holder.serverPrefix), false);
            emb.addField(holder.serverPrefix+"checkbcu",LangID.getStringByID("help_checkbcu", lang).replace("_", holder.serverPrefix),false);
            emb.addField(holder.serverPrefix+"analyze",LangID.getStringByID("help_analyze", lang).replace("_", holder.serverPrefix),false);
            emb.addField(holder.serverPrefix+"prefix",LangID.getStringByID("help_prefix", lang).replace("_", holder.serverPrefix), false);
            emb.addField(holder.serverPrefix+"serverpre",LangID.getStringByID("help_serverpre", lang).replace("_", holder.serverPrefix), false);
            emb.addField(holder.serverPrefix+"stageimage", LangID.getStringByID("help_stimg", lang).replace("_", holder.serverPrefix), false);
            emb.addField(holder.serverPrefix+"stmimage",LangID.getStringByID("help_stmimg", lang).replace("_", holder.serverPrefix), false);
            emb.addField(holder.serverPrefix+"locale", LangID.getStringByID("help_locale", lang).replace("_", holder.serverPrefix), false);
            emb.addField(holder.serverPrefix+"helpbc", LangID.getStringByID("help_helpbc", lang).replace("_", holder.serverPrefix), false);
        }).subscribe();
    }
}
