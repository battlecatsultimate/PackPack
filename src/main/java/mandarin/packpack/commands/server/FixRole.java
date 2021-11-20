package mandarin.packpack.commands.server;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.AllowedMentions;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.ConfirmButtonHolder;

import java.util.concurrent.atomic.AtomicLong;

public class FixRole extends ConstraintCommand {
    public FixRole(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        Guild g = getGuild(event).block();

        if(g == null)
            return;

        if(!StaticStore.needFixing.contains(g.getId().asString())) {
            createMessageWithNoPings(ch, LangID.getStringByID("fixrole_nofixing", lang).replace("_", StaticStore.MANDARIN_SMELL));
            return;
        }

        if(holder.MEMBER == null) {
            createMessage(ch, m -> m.content(LangID.getStringByID("fixrole_nomem", lang)));
            return;
        }

        String preID = getPreMemberID(getContent(event));

        if(preID == null && holder.ID.containsKey("Pre Member")) {
            preID = holder.ID.get("Pre Member");
        } else if(preID == null && !holder.ID.containsKey("Pre Member")) {
            createMessage(ch, m -> m.content(LangID.getStringByID("chbcu_pre", lang)));
            return;
        }

        if(preID == null)
            return;

        final String finalPre = preID;
        String ignore = getIgnore(getContent(event));

        Message msg = createMessage(ch, m -> {
            m.content(LangID.getStringByID("fixrole_confirm", lang).replace("_PPP_", finalPre).replace("_MMM_", holder.MEMBER));
            m.allowedMentions(AllowedMentions.builder().build());
            registerConfirmButtons(m, lang);
        });

        if(msg == null)
            return;

        getMember(event).ifPresent(me -> StaticStore.putHolder(me.getId().asString(), new ConfirmButtonHolder(msg, getMessage(event), ch.getId().asString(), me.getId().asString(), () -> {

            AtomicLong fixed = new AtomicLong();

            g.getMembers()
                    .filter(m -> !m.isBot() && (ignore == null || !StaticStore.rolesToString(m.getRoleIds()).contains(ignore)))
                    .toStream()
                    .forEach(m -> {
                        String roles = StaticStore.rolesToString(m.getRoleIds());

                        if(!roles.contains(finalPre) && !roles.contains(holder.MEMBER)) {
                            m.addRole(Snowflake.of(finalPre)).subscribe();
                            fixed.getAndIncrement();
                        }
                    });

            if(fixed.get() == 0) {
                createMessage(ch, m -> m.content(LangID.getStringByID("fixrole_noneed", lang)));
            } else {
                createMessage(ch, m -> m.content(LangID.getStringByID("fixrole_fixed", lang).replace("_", "" + fixed.get())));
            }
        }, lang)));
    }

    private String getPreMemberID(String content) {
        String[] contents = content.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if((contents[i].equals("-p") || contents[i].equals("-pre")) && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1]))
                return contents[i + 1];
        }

        return null;
    }

    private String getIgnore(String content) {
        String[] contents = content.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if((contents[i].equals("-i") || contents[i].equals("-ignore")) && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1]))
                return contents[i + 1];
        }

        return null;
    }
}
