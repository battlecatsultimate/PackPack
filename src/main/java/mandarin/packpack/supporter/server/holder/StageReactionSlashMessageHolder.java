package mandarin.packpack.supporter.server.holder;

import common.pack.Identifier;
import common.pack.UserProfile;
import common.util.pack.Background;
import common.util.stage.CastleImg;
import common.util.stage.CastleList;
import common.util.stage.Music;
import common.util.stage.Stage;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.bc.Castle;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class StageReactionSlashMessageHolder extends MessageHolder<MessageReactionAddEvent> {
    private final String embedID;
    private final IDHolder holder;
    private final int lang;
    private final String channelID;
    private final String memberID;
    private final Stage st;

    private final Message m;

    public StageReactionSlashMessageHolder(Message m, Stage st, String embedID, String channelID, String memberID, IDHolder holder, int lang) {
        super(MessageReactionAddEvent.class, null);

        this.embedID = embedID;
        this.holder = holder;
        this.lang = lang;
        this.channelID = channelID;
        this.memberID = memberID;
        this.st = st;

        this.m = m;

        Timer autoFinish = new Timer();

        autoFinish.schedule(new TimerTask() {
            @Override
            public void run() {
                if(expired)
                    return;

                expired = true;

                StaticStore.removeHolder(memberID, StageReactionSlashMessageHolder.this);

                if(!(m.getChannel() instanceof GuildChannel) || m.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) {
                    m.clearReactions().queue();
                }
            }
        }, TimeUnit.MINUTES.toMillis(5));
    }

    @Override
    public int handleEvent(MessageReactionAddEvent event) {
        if(expired) {
            System.out.println("Expired at StageReactionHolder!");
            return RESULT_FAIL;
        }

        MessageChannel ch = event.getChannel();

        if(!ch.getId().equals(channelID))
            return RESULT_STILL;

        if(!m.getId().equals(embedID))
            return RESULT_STILL;

        if(event.getUser() == null)
            return RESULT_STILL;

        User u = event.getUser();

        if(!u.getId().equals(memberID))
            return RESULT_STILL;

        Emoji e = event.getEmoji();

        if(!(e instanceof CustomEmoji))
            return RESULT_STILL;

        boolean emojiClicked = false;

        switch (e.getName()) {
            case "Castle":
                emojiClicked = true;

                CastleImg cs = Identifier.get(st.castle);

                if(cs == null) {
                    ArrayList<CastleList> lists = new ArrayList<>(CastleList.defset());

                    cs = lists.get(0).get(0);
                }

                new Castle(ConstraintCommand.ROLE.MEMBER, lang, holder, cs).execute(event);

                break;
            case "Background":
                emojiClicked = true;

                Background bg = Identifier.get(st.bg);

                if(bg == null) {
                    bg = UserProfile.getBCData().bgs.get(0);
                }

                new mandarin.packpack.commands.bc.Background(ConstraintCommand.ROLE.MEMBER, lang, holder, 10000, bg).execute(event);

                break;
            case "Music":
                emojiClicked = true;

                if(st.mus0 == null)
                    break;

                Music ms = Identifier.get(st.mus0);

                if(ms == null) {
                    ms = UserProfile.getBCData().musics.get(0);
                }

                new mandarin.packpack.commands.bc.Music(ConstraintCommand.ROLE.MEMBER, lang, holder, "music_", ms).execute(event);

                break;
            case "MusicBoss":
                emojiClicked = true;

                if(st.mus1 == null)
                    break;

                Music ms2 = Identifier.get(st.mus1);

                if(ms2 == null) {
                    ms2 = UserProfile.getBCData().musics.get(0);
                }

                new mandarin.packpack.commands.bc.Music(ConstraintCommand.ROLE.MEMBER, lang, holder, "music_", ms2).execute(event);

                break;
        }

        if(emojiClicked) {
            if(!(ch instanceof GuildChannel) || m.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) {
                m.clearReactions().queue();
            }

            expired = true;
        }

        return emojiClicked ? RESULT_FINISH : RESULT_STILL;
    }

    @Override
    public void clean() {

    }

    @Override
    public void expire(String id) {
        if(expired)
            return;

        expired = true;

        StaticStore.removeHolder(memberID, StageReactionSlashMessageHolder.this);

        MessageChannel ch = m.getChannel();

        if(!(ch instanceof GuildChannel) || m.getGuild().getSelfMember().hasPermission((GuildChannel) ch, Permission.MESSAGE_MANAGE)) {
            m.clearReactions().queue();
        }
    }
}
