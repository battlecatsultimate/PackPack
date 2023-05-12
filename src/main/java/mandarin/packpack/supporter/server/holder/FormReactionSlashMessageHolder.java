package mandarin.packpack.supporter.server.holder;

import common.util.unit.Form;
import common.util.unit.Level;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.TreasureHolder;
import mandarin.packpack.supporter.server.holder.segment.MessageHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class FormReactionSlashMessageHolder extends MessageHolder {
    private final Form f;
    private final ConfigHolder config;
    private final int lang;
    private final Message m;

    private final boolean isFrame;
    private final boolean talent;
    private final boolean extra;
    private final Level lv;
    private final boolean treasure;
    private final TreasureHolder t;

    public FormReactionSlashMessageHolder(Message m, Form f, String memberID, String channelID, ConfigHolder config, boolean isFrame, boolean talent, boolean extra, Level lv, boolean treasure, TreasureHolder t, int lang) {
        super(channelID, m.getId(), memberID);

        this.f = f;
        this.config = config;
        this.lang = lang;

        this.isFrame = isFrame;
        this.talent = talent;
        this.extra = extra;
        this.lv = lv;

        this.treasure = treasure;
        this.t = t;

        this.m = m;

        Timer autoFinish = new Timer();

        autoFinish.schedule(new TimerTask() {
            @Override
            public void run() {
                if(expired)
                    return;

                expired = true;

                StaticStore.removeHolder(memberID, FormReactionSlashMessageHolder.this);

                if(!(m.getChannel() instanceof GuildChannel) || m.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) {
                    m.clearReactions().queue();
                }
            }
        }, TimeUnit.MINUTES.toMillis(5));
    }

    @Override
    public STATUS onReactionEvent(MessageReactionAddEvent event) {
        if(expired) {
            System.out.println("Expired at FormReactionSlashHolder!");
            return STATUS.FAIL;
        }

        MessageChannel ch = event.getChannel();

        Emoji emoji = event.getEmoji();

        if(!(emoji instanceof CustomEmoji))
            return STATUS.WAIT;

        boolean emojiClicked = false;

        switch (emoji.getName()) {
            case "TwoPrevious" -> {
                emojiClicked = true;

                if (f.fid - 2 < 0)
                    break;

                if (f.unit == null)
                    break;

                Form newForm = f.unit.forms[f.fid - 2];

                try {
                    EntityHandler.showUnitEmb(newForm, ch, getAuthorMessage(), config, isFrame, talent, extra, false, false, lv, treasure, t, lang, false, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            case "Previous" -> {
                emojiClicked = true;

                if (f.fid - 1 < 0)
                    break;

                if (f.unit == null)
                    break;

                Form newForm = f.unit.forms[f.fid - 1];

                try {
                    EntityHandler.showUnitEmb(newForm, ch, getAuthorMessage(), config, isFrame, talent, extra, false, false, lv, treasure, t, lang, false, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            case "Next" -> {
                emojiClicked = true;

                if (f.unit == null)
                    break;

                if (f.fid + 1 >= f.unit.forms.length)
                    break;

                Form newForm = f.unit.forms[f.fid + 1];

                try {
                    EntityHandler.showUnitEmb(newForm, ch, getAuthorMessage(), config, isFrame, talent, extra, false, false, lv, treasure, t, lang, false, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            case "TwoNext" -> {
                emojiClicked = true;

                if (f.unit == null)
                    break;

                if (f.fid + 2 >= f.unit.forms.length)
                    break;

                Form newForm = f.unit.forms[f.fid + 2];

                try {
                    EntityHandler.showUnitEmb(newForm, ch, getAuthorMessage(), config, isFrame, talent, extra, false, false, lv, treasure, t, lang, false, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if(emojiClicked) {
            if(!(ch instanceof GuildChannel) || m.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) {
                m.clearReactions().queue();
            }

            expired = true;
        }

        return emojiClicked ? STATUS.FINISH : STATUS.WAIT;
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire(String id) {
        if(expired)
            return;

        expired = true;

        StaticStore.removeHolder(id, this);

        MessageChannel ch = m.getChannel();

        if(!(ch instanceof GuildChannel) || m.getGuild().getSelfMember().hasPermission((GuildChannel) ch, Permission.MESSAGE_MANAGE)) {
            m.clearReactions().queue();
        }
    }
}
