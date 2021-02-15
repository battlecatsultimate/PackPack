package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;

public class HelpBC implements Command {
    private final int lang;

    public HelpBC(int lang) {
        this.lang = lang;
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
            emb.addField(StaticStore.serverPrefix+"formstat", LangID.getStringByID("helpbc_fs", lang).replace("_", StaticStore.serverPrefix), false);
            emb.addField(StaticStore.serverPrefix+"enemystat",LangID.getStringByID("helpbc_es", lang).replace("_", StaticStore.serverPrefix),false);
            emb.addField(StaticStore.serverPrefix+"stageinfo", LangID.getStringByID("helpbc_si", lang).replace("_", StaticStore.serverPrefix), false);
            emb.addField(StaticStore.serverPrefix+"music",LangID.getStringByID("helpbc_ms", lang).replace("_", StaticStore.serverPrefix),false);
            emb.addField(StaticStore.serverPrefix+"castle",LangID.getStringByID("helpbc_cs", lang).replace("_", StaticStore.serverPrefix), false);
        }).subscribe();
    }
}
