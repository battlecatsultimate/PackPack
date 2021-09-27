package mandarin.packpack.supporter.server.holder;

import common.pack.Identifier;
import common.pack.UserProfile;
import common.util.pack.Background;
import common.util.stage.CastleImg;
import common.util.stage.CastleList;
import common.util.stage.Music;
import common.util.stage.Stage;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.bc.Castle;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.data.IDHolder;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class StageReactionMessageHolder extends MessageHolder<ReactionAddEvent> {
    private final Message embed;
    private final IDHolder holder;
    private final int lang;
    private final String channelID;
    private final String memberID;
    private final Stage st;

    public StageReactionMessageHolder(Stage st, Message author, Message msg, IDHolder holder, int lang, String channelID, String memberID) {
        super(ReactionAddEvent.class);

        this.st = st;
        embed = msg;
        this.holder = holder;
        this.lang = lang;
        this.channelID = channelID;
        this.memberID = memberID;

        Timer autoFinish = new Timer();

        autoFinish.schedule(new TimerTask() {
            @Override
            public void run() {
                if(expired)
                    return;

                expired = true;

                author.getAuthor().ifPresent(u -> StaticStore.removeHolder(u.getId().asString(), StageReactionMessageHolder.this));

                embed.removeAllReactions().subscribe();
            }
        }, TimeUnit.MINUTES.toMillis(5));
    }

    @Override
    public int handleEvent(ReactionAddEvent event) {
        if(expired) {
            System.out.println("Expired at StageReactionHolder!");
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

        Optional<ReactionEmoji.Custom> uni = event.getEmoji().asCustomEmoji();

        AtomicReference<Boolean> emojiClicked = new AtomicReference<>(false);

        uni.ifPresent(em -> {
            switch (em.getId().asString()) {
                case StaticStore.CASTLE:
                    emojiClicked.set(true);

                    CastleImg cs = Identifier.get(st.castle);

                    if(cs == null) {
                        ArrayList<CastleList> lists = new ArrayList<>(CastleList.defset());

                        cs = lists.get(0).get(0);
                    }

                    new Castle(ConstraintCommand.ROLE.MEMBER, lang, holder, cs).execute(event);

                    break;
                case StaticStore.BG:
                    emojiClicked.set(true);

                    Background bg = Identifier.get(st.bg);

                    if(bg == null) {
                        bg = UserProfile.getBCData().bgs.get(0);
                    }

                    new mandarin.packpack.commands.bc.Background(ConstraintCommand.ROLE.MEMBER, lang, holder, 10000, bg).execute(event);

                    break;
                case StaticStore.MUSIC:
                    emojiClicked.set(true);

                    if(st.mus0 == null)
                        return;

                    Music ms = Identifier.get(st.mus0);

                    if(ms == null) {
                        ms = UserProfile.getBCData().musics.get(0);
                    }

                    new mandarin.packpack.commands.bc.Music(ConstraintCommand.ROLE.MEMBER, lang, holder, "music_", ms).execute(event);
                    
                    break;
                case StaticStore.MUSIC2:
                    emojiClicked.set(true);

                    if(st.mus1 == null)
                        return;

                    Music ms2 = Identifier.get(st.mus1);

                    if(ms2 == null) {
                        ms2 = UserProfile.getBCData().musics.get(0);
                    }

                    new mandarin.packpack.commands.bc.Music(ConstraintCommand.ROLE.MEMBER, lang, holder, "music_", ms2).execute(event);
                    
                    break;
            }
        });

        if(emojiClicked.get()) {
            embed.removeAllReactions().subscribe();
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
