package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;

public class Help implements Command {
    private final int lang;

    public Help(int lang) {
        this.lang = lang;
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
            emb.addField(StaticStore.serverPrefix+"bcustat", LangID.getStringByID("help_bcustat", lang).replace("_", StaticStore.serverPrefix), false);
            emb.addField(StaticStore.serverPrefix+"checkbcu",LangID.getStringByID("help_checkbcu", lang).replace("_", StaticStore.serverPrefix),false);
            emb.addField(StaticStore.serverPrefix+"analyze",LangID.getStringByID("help_analyze", lang).replace("_", StaticStore.serverPrefix),false);
            emb.addField(StaticStore.serverPrefix+"prefix",LangID.getStringByID("help_prefix", lang).replace("_", StaticStore.serverPrefix), false);
            emb.addField(StaticStore.serverPrefix+"serverpre",LangID.getStringByID("help_serverpre", lang).replace("_", StaticStore.serverPrefix), false);
            emb.addField(StaticStore.serverPrefix+"stageimage", LangID.getStringByID("help_stimg", lang).replace("_", StaticStore.serverPrefix), false);
            emb.addField(StaticStore.serverPrefix+"stmimage",LangID.getStringByID("help_stmimg", lang).replace("_", StaticStore.serverPrefix), false);
            emb.addField(StaticStore.serverPrefix+"locale", LangID.getStringByID("help_locale", lang).replace("_", StaticStore.serverPrefix), false);
            emb.addField(StaticStore.serverPrefix+"helpbc", LangID.getStringByID("help_helpbc", lang).replace("_", StaticStore.serverPrefix), false);
        }).subscribe();
    }
}
