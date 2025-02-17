package mandarin.packpack.commands;

import common.CommonStatic;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import javax.annotation.Nonnull;

public class TimeZone extends ConstraintCommand {
    public TimeZone(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        String[] contents = loader.getContent().split(" ");

        if(contents.length < 2) {
            replyToMessageSafely(ch, LangID.getStringByID("timeZone.failed.noValue", lang), loader.getMessage(), a -> a);

            return;
        }

        if(!StaticStore.isNumeric(contents[1])) {
            replyToMessageSafely(ch, LangID.getStringByID("timeZone.failed.notNumber", lang), loader.getMessage(), a -> a);

            return;
        }

        int timeZone = Math.min(12, Math.max(-12, StaticStore.safeParseInt(contents[1])));

        User u = loader.getUser();

        StaticStore.timeZones.put(u.getId(), timeZone);

        if(timeZone >= 0) {
            replyToMessageSafely(ch, LangID.getStringByID("timeZone.done", lang).replace("_", "+" + timeZone), loader.getMessage(), a -> a);
        } else {
            replyToMessageSafely(ch, LangID.getStringByID("timeZone.done", lang).replace("_", String.valueOf(timeZone)), loader.getMessage(), a -> a);
        }
    }
}
