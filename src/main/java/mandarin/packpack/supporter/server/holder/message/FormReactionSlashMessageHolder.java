package mandarin.packpack.supporter.server.holder.message;

import common.CommonStatic;
import common.util.unit.Form;
import common.util.unit.Level;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.TreasureHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;

public class FormReactionSlashMessageHolder extends MessageHolder {
    private final Form f;
    private final ConfigHolder config;

    private final boolean isFrame;
    private final boolean talent;
    private final boolean extra;
    private final Level lv;
    private final boolean treasure;
    private final TreasureHolder t;

    public FormReactionSlashMessageHolder(GenericCommandInteractionEvent event, Message message, Form f, ConfigHolder config, boolean isFrame, boolean talent, boolean extra, Level lv, boolean treasure, TreasureHolder t, CommonStatic.Lang.Locale lang) {
        super(event, message, lang);

        this.f = f;
        this.config = config;

        this.isFrame = isFrame;
        this.talent = talent;
        this.extra = extra;
        this.lv = lv;

        this.treasure = treasure;
        this.t = t;

        registerAutoExpiration(FIVE_MIN);
    }

    @Override
    public STATUS onReactionEvent(MessageReactionAddEvent event) {
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
                    EntityHandler.showUnitEmb(newForm, ch, null, config, isFrame, talent, extra, false, false, lv, treasure, t, lang, false, false, msg -> { });
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e, "E/FormReactionSlashMessageHolder::onReactionEvent - Failed to upload unit embed");
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
                    EntityHandler.showUnitEmb(newForm, ch, null, config, isFrame, talent, extra, false, false, lv, treasure, t, lang, false, false, msg -> { });
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e, "E/FormReactionSlashMessageHolder::onReactionEvent - Failed to upload unit embed");
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
                    EntityHandler.showUnitEmb(newForm, ch, null, config, isFrame, talent, extra, false, false, lv, treasure, t, lang, false, false, msg -> { });
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e, "E/FormReactionSlashMessageHolder::onReactionEvent - Failed to upload unit embed");
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
                    EntityHandler.showUnitEmb(newForm, ch, null, config, isFrame, talent, extra, false, false, lv, treasure, t, lang, false, false, msg -> { });
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e, "E/FormReactionSlashMessageHolder::onReactionEvent - Failed to upload unit embed");
                }
            }
        }

        if(emojiClicked) {
            if(!(ch instanceof GuildChannel) || message.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) {
                message.clearReactions().queue();
            }

            end(true);
        }

        return emojiClicked ? STATUS.FINISH : STATUS.WAIT;
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire() {
        MessageChannel ch = message.getChannel();

        if(!(ch instanceof GuildChannel) || message.getGuild().getSelfMember().hasPermission((GuildChannel) ch, Permission.MESSAGE_MANAGE)) {
            message.clearReactions().queue();
        }
    }
}
