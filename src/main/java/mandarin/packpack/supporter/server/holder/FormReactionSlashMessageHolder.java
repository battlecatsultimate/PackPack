package mandarin.packpack.supporter.server.holder;

import common.util.unit.Form;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class FormReactionSlashMessageHolder extends MessageHolder<ReactionAddEvent> {
    private final Form f;
    private final String memberID;
    private final long channelID;
    private final long embID;
    private final int lang;

    private final boolean isFrame;
    private final boolean talent;
    private final boolean extra;
    private final ArrayList<Integer> lv;

    private final GatewayDiscordClient client;

    public FormReactionSlashMessageHolder(GatewayDiscordClient client, Form f, String memberID, long channelID, long embID, boolean isFrame, boolean talent, boolean extra, ArrayList<Integer> lv, int lang) {
        super(ReactionAddEvent.class);

        this.f = f;
        this.memberID = memberID;
        this.channelID = channelID;
        this.embID = embID;
        this.lang = lang;

        this.isFrame = isFrame;
        this.talent = talent;
        this.extra = extra;
        this.lv = lv;

        this.client = client;

        Timer autoFinish = new Timer();

        autoFinish.schedule(new TimerTask() {
            @Override
            public void run() {
                if(expired)
                    return;

                expired = true;

                StaticStore.removeHolder(memberID, FormReactionSlashMessageHolder.this);

                client.getRestClient().getChannelService()
                        .deleteAllReactions(channelID, embID).subscribe();
            }
        }, TimeUnit.MINUTES.toMillis(5));
    }

    @Override
    public int handleEvent(ReactionAddEvent event) {
        if(expired) {
            System.out.println("Expired at FormReactionSlashHolder!");
            return RESULT_FAIL;
        }

        MessageChannel ch = event.getChannel().block();

        if(ch == null)
            return RESULT_STILL;

        if(ch.getId().asLong() != channelID)
            return RESULT_STILL;

        Message msg = event.getMessage().block();

        if(msg == null || msg.getId().asLong() != embID)
            return RESULT_STILL;

        if(event.getMember().isEmpty())
            return RESULT_STILL;

        Member mem = event.getMember().get();

        if(!mem.getId().asString().equals(memberID))
            return RESULT_STILL;

        Optional<ReactionEmoji.Custom> emoji = event.getEmoji().asCustomEmoji();

        AtomicReference<Boolean> emojiClicked = new AtomicReference<>(false);

        emoji.ifPresent(em -> {
            switch (em.getId().asString()) {
                case StaticStore.TWOPREVIOUS:
                    emojiClicked.set(true);

                    if(f.fid - 2 < 0)
                        return;

                    if(f.unit == null)
                        return;

                    Form newForm = f.unit.forms[f.fid - 2];

                    try {
                        EntityHandler.showUnitEmb(newForm, ch, isFrame, talent, extra, lv, lang, false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    break;
                case StaticStore.PREVIOUS:
                    emojiClicked.set(true);

                    if(f.fid - 1 < 0)
                        return;

                    if(f.unit == null)
                        return;

                    newForm = f.unit.forms[f.fid - 1];

                    try {
                        EntityHandler.showUnitEmb(newForm, ch, isFrame, talent, extra, lv, lang, false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    break;
                case StaticStore.NEXT:
                    emojiClicked.set(true);

                    if(f.unit == null)
                        return;

                    if(f.fid + 1 >= f.unit.forms.length)
                        return;

                    newForm = f.unit.forms[f.fid + 1];

                    try {
                        EntityHandler.showUnitEmb(newForm, ch, isFrame, talent, extra, lv, lang, false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    break;
                case StaticStore.TWONEXT:
                    emojiClicked.set(true);

                    if(f.unit == null)
                        return;

                    if(f.fid + 2 >= f.unit.forms.length)
                        return;

                    newForm = f.unit.forms[f.fid + 2];

                    try {
                        EntityHandler.showUnitEmb(newForm, ch, isFrame, talent, extra, lv, lang, false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    break;
            }
        });

        if(emojiClicked.get()) {
            client.getRestClient().getChannelService()
                    .deleteMessage(channelID, embID, null).subscribe();
            expired = true;
        }

        return emojiClicked.get() ? RESULT_FINISH : RESULT_STILL;
    }

    @Override
    public void clean() {

    }

    @Override
    public void expire(String id) {
        if(expired)
            return;

        expired = true;

        StaticStore.removeHolder(id, this);

        client.getRestClient().getChannelService()
                .deleteAllReactions(channelID, embID).subscribe();
    }
}
