package mandarin.packpack.commands;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

public class TimeZone extends ConstraintCommand {
    public TimeZone(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String[] contents = getContent(event).split(" ");

        if(contents.length < 2) {
            ch.sendMessage(LangID.getStringByID("timezone_noval", lang)).queue();

            return;
        }

        if(!StaticStore.isNumeric(contents[1])) {
            ch.sendMessage(LangID.getStringByID("timezone_notnum", lang)).queue();

            return;
        }

        int timeZone = Math.min(12, Math.max(-12, StaticStore.safeParseInt(contents[1])));

        Member m = getMember(event);

        if(m != null) {
            StaticStore.timeZones.put(m.getId(), timeZone);

            if(timeZone >= 0) {
                ch.sendMessage(LangID.getStringByID("timezone_done", lang).replace("_", "+" + timeZone)).queue();
            } else {
                ch.sendMessage(LangID.getStringByID("timezone_done", lang).replace("_", timeZone + "")).queue();
            }
        } else {
            ch.sendMessage(LangID.getStringByID("timezone_nomem", lang)).queue();
        }
    }
}
