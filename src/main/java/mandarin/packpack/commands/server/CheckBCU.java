package mandarin.packpack.commands.server;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.Command;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;

import java.util.concurrent.atomic.AtomicReference;

public class CheckBCU extends Command {
    private final IDHolder holder;

    public CheckBCU(int lang, IDHolder holder) {
        super(lang);

        this.holder = holder;
    }

    @Override
    public void doSomething(MessageEvent event) {
        Message msg = getMessage(event);

        if(msg == null)
            return;

        MessageChannel ch = msg.getChannel().block();

        if(ch == null)
            return;

        String preID = getPreMemberID(getContent(event));

        if(preID == null && !holder.ID.containsKey("Pre Member")) {
            createMessage(ch, m -> m.content(LangID.getStringByID("chbcu_pre", lang)));
            return;
        } else if(preID == null && holder.ID.containsKey("Pre Member")) {
            preID = holder.ID.get("Pre Member");
        }

        final String finalPre = preID;

        if(StaticStore.checkingBCU) {
            ch.createMessage(LangID.getStringByID("chbcu_perform", lang)).subscribe();
        } else {
            StaticStore.checkingBCU = true;

            AtomicReference<StringBuilder> both = new AtomicReference<>(new StringBuilder("BOTH : "));
            AtomicReference<StringBuilder> none = new AtomicReference<>(new StringBuilder("NONE : "));

            getGuild(event)
                    .subscribe(g -> g.getMembers()
                        .filter(m -> !m.isBot() && (!holder.ID.containsKey("Muted") || !StaticStore.rolesToString(m.getRoleIds()).contains(holder.ID.get("Muted"))))
                        .subscribe(m -> {
                            boolean pre = false;
                            boolean mem = false;

                            String role = StaticStore.rolesToString(m.getRoleIds());

                            if(role.contains(finalPre))
                                pre = true;

                            if(holder.MEMBER != null && role.contains(holder.MEMBER))
                                mem = true;

                            if (!pre && !mem)
                                none.get().append(m.getUsername()).append(", ");

                            if (pre && mem)
                                both.get().append(m.getUsername()).append(", ");
                        }, e -> ch.createMessage(StaticStore.ERROR_MSG).subscribe(), pause::resume));

            pause.pause(() -> onFail(event, DEFAULT_ERROR));

            ch.createMessage(both.get().substring(0, both.get().length()-2)+"\n"+none.get().substring(0, none.get().length()-2)).subscribe();

            StaticStore.checkingBCU = false;
        }
    }

    private String getPreMemberID(String content) {
        String[] contents = content.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if((contents[i].equals("-p") || contents[i].equals("-pre")) && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1]))
                return contents[i + 1];
        }

        return null;
    }
}
