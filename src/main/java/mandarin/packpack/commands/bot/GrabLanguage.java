package mandarin.packpack.commands.bot;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import org.jetbrains.annotations.Nullable;

public class GrabLanguage extends ConstraintCommand {
    public GrabLanguage(ROLE role, int lang, @Nullable IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if (ch == null)
            return;

        String[] contents = getContent(event).split(" ", 4);

        if(contents.length < 3) {
            replyToMessageSafely(ch, "Format : `p!gl [Locale Number] [ID]`", getMessage(event), a -> a);

            return;
        }

        int l = LangID.EN;

        if(StaticStore.isNumeric(contents[1])) {
            l = StaticStore.safeParseInt(contents[1]);
        }

        if(contents.length > 3) {
            try {
                replyToMessageSafely(ch, String.format(LangID.getStringByID(contents[2], l), (Object[]) contents[3].split("\\\\")), getMessage(event), a -> a);
            } catch (Exception e) {
                replyToMessageSafely(ch, LangID.getStringByID(contents[2], l), getMessage(event), a -> a);
            }
        } else {
            replyToMessageSafely(ch, LangID.getStringByID(contents[2], l), getMessage(event), a -> a);
        }
    }
}
