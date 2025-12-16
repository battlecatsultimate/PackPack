package mandarin.packpack.commands.bot;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import javax.annotation.Nonnull;

public class News extends ConstraintCommand {
    public News(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        String content = loader.getContent();

        String[] contents = content.split(" ", 3);

        if(contents.length != 3) {
            replyToMessageSafely(ch, loader.getMessage(), "Usage : [Locale] [Desc]\nEN : 0, ZH : 1, KR : 2, JP : 3, FR : 4, IT : 5, ES : 6, DE : 7, TH : 8");
        } else {
            if(StaticStore.isNumeric(contents[1])) {
                int loc = StaticStore.safeParseInt(contents[1]);

                if(loc >= 9) {
                    replyToMessageSafely(ch, loader.getMessage(), "Locale must be less than 8");

                    return;
                }

                CommonStatic.Lang.Locale locale = switch (loc) {
                    case 1 -> CommonStatic.Lang.Locale.ZH;
                    case 2 -> CommonStatic.Lang.Locale.KR;
                    case 3 -> CommonStatic.Lang.Locale.JP;
                    case 4 -> CommonStatic.Lang.Locale.FR;
                    case 5 -> CommonStatic.Lang.Locale.IT;
                    case 6 -> CommonStatic.Lang.Locale.ES;
                    case 7 -> CommonStatic.Lang.Locale.DE;
                    case 8 -> CommonStatic.Lang.Locale.TH;
                    default -> CommonStatic.Lang.Locale.EN;
                };

                if(contents[2].isBlank()) {
                    replyToMessageSafely(ch, loader.getMessage(), "Announcement content is empty!");
                    return;
                }

                StaticStore.announcements.put(locale, contents[2]);

                replyToMessageSafely(ch, loader.getMessage(), "Announcement added for " + locale.code);
            } else {
                replyToMessageSafely(ch, loader.getMessage(), "Locale must be number\nEN : 0, ZH : 1, KR : 2, JP : 3, FR : 4, IT : 5, ES : 6, DE : 7");
            }
        }
    }
}
