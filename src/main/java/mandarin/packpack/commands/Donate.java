package mandarin.packpack.commands;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Donate extends ConstraintCommand {
    public Donate(ROLE role, CommonStatic.Lang.Locale lang, @Nullable IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader)  {
        MessageChannel ch = loader.getChannel();

        EmbedBuilder builder = new EmbedBuilder();

        builder.setTitle(LangID.getStringByID("donate.donation", lang));
        builder.setColor(StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)]);
        builder.setDescription(LangID.getStringByID("donate.description", lang));

        builder.addField(EmojiStore.PAYPAL.getFormatted() + " " + LangID.getStringByID("donate.method.payPal", lang), "[" + LangID.getStringByID("donate.link", lang) + "](" + StaticStore.PAYPAL + ")", false);
        builder.addField(EmojiStore.CASHAPP.getFormatted() + " " + LangID.getStringByID("donate.method.cashApp", lang), "[" + LangID.getStringByID("donate.link", lang) + "](" + StaticStore.CASHAPP + ")", false);

        replyToMessageSafely(ch, "", loader.getMessage(), a -> a.setEmbeds(builder.build()));
    }
}
