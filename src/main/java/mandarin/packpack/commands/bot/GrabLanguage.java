package mandarin.packpack.commands.bot;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class GrabLanguage extends ConstraintCommand {
    public GrabLanguage(ROLE role, CommonStatic.Lang.Locale lang, @Nullable IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        String[] contents = loader.getContent().split(" ", 4);

        if(contents.length < 3) {
            replyToMessageSafely(ch, loader.getMessage(), "Format : `p!gl [Locale] [ID]`");

            return;
        }

        CommonStatic.Lang.Locale l = getLocale(contents[1]);

        if (l == null) {
            replyToMessageSafely(ch, loader.getMessage(), "Valid Locale Code :\n- en\n- tw\n- kr\n- jp\n- ru");

            return;
        }

        if(contents.length > 3) {
            try {
                replyToMessageSafely(ch, loader.getMessage(), String.format(LangID.getStringByID(contents[2], l), (Object[]) contents[3].split("\\\\")));
            } catch (Exception e) {
                replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID(contents[2], l));
            }
        } else {
            replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID(contents[2], l));
        }
    }

    private CommonStatic.Lang.Locale getLocale(String code) {
        switch (code) {
            case "-en" -> {
                return CommonStatic.Lang.Locale.EN;
            }
            case "-tw" -> {
                return CommonStatic.Lang.Locale.ZH;
            }
            case "-jp" -> {
                return CommonStatic.Lang.Locale.JP;
            }
            case "-kr" -> {
                return CommonStatic.Lang.Locale.KR;
            }
            case "-ru" -> {
                return CommonStatic.Lang.Locale.RU;
            }
            default -> {
                return null;
            }
        }
    }
}
