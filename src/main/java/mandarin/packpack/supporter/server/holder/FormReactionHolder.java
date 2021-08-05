package mandarin.packpack.supporter.server.holder;

import common.util.unit.Form;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;

import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class FormReactionHolder extends Holder<ReactionAddEvent> {
    public static final String TWOPREVIOUS = "821449061332025404";
    public static final String PREVIOUS = "821449028767187014";
    public static final String NEXT = "821449099936923739";
    public static final String TWONEXT = "821449111109894164";

    private final Message embed;
    private final int lang;
    private final String channelID;
    private final String memberID;
    private final Form f;

    private final boolean isFrame;
    private final boolean talent;
    private final int[] lv;

    public FormReactionHolder(Form f, Message author, Message msg, boolean isFrame, boolean talent, int[] lv, int lang, String channelID, String memberID) {
        super(ReactionAddEvent.class);

        this.embed = msg;
        this.lang = lang;
        this.channelID = channelID;
        this.memberID = memberID;
        this.f = f;

        this.isFrame = isFrame;
        this.talent = talent;
        this.lv = lv;

        Timer autoFinsh = new Timer();

        autoFinsh.schedule(new TimerTask() {
            @Override
            public void run() {
                if(expired)
                    return;

                expired = true;

                author.getAuthor().ifPresent(u -> StaticStore.removeHolder(u.getId().asString(), FormReactionHolder.this));

                embed.removeAllReactions().subscribe();
            }
        }, TimeUnit.MINUTES.toMillis(5));
    }

    @Override
    public int handleEvent(ReactionAddEvent event) {
        if(expired) {
            System.out.println("Expired at FormReactionHolder!");
            return RESULT_FAIL;
        }

        MessageChannel ch = event.getChannel().block();

        if(ch == null)
            return RESULT_STILL;

        if(!ch.getId().asString().equals(channelID))
            return RESULT_STILL;

        Message msg = event.getMessage().block();

        if(msg == null || !msg.getId().asString().equals(embed.getId().asString()))
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
                case TWOPREVIOUS:
                    emojiClicked.set(true);

                    if(f.fid - 2 < 0)
                        return;

                    if(f.unit == null)
                        return;

                    Form newForm = f.unit.forms[f.fid - 2];

                    try {
                        EntityHandler.showUnitEmb(newForm, ch, isFrame, talent, lv, lang, false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    break;
                case PREVIOUS:
                    emojiClicked.set(true);

                    if(f.fid - 1 < 0)
                        return;

                    if(f.unit == null)
                        return;

                    newForm = f.unit.forms[f.fid - 1];

                    try {
                        EntityHandler.showUnitEmb(newForm, ch, isFrame, talent, lv, lang, false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    break;
                case NEXT:
                    emojiClicked.set(true);

                    if(f.unit == null)
                        return;

                    if(f.fid + 1 >= f.unit.forms.length)
                        return;

                    newForm = f.unit.forms[f.fid + 1];

                    try {
                        EntityHandler.showUnitEmb(newForm, ch, isFrame, talent, lv, lang, false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    break;
                case TWONEXT:
                    emojiClicked.set(true);

                    if(f.unit == null)
                        return;

                    if(f.fid + 2 >= f.unit.forms.length)
                        return;

                    newForm = f.unit.forms[f.fid + 2];

                    try {
                        EntityHandler.showUnitEmb(newForm, ch, isFrame, talent, lv, lang, false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    break;
            }
        });

        if(emojiClicked.get()) {
            embed.delete().subscribe();
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

        embed.removeAllReactions().subscribe();
    }
}
