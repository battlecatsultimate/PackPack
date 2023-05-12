package mandarin.packpack.commands;

import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import org.jetbrains.annotations.Nullable;

public class Donate extends ConstraintCommand {
    public Donate(ROLE role, int lang, @Nullable IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        EmbedBuilder builder = new EmbedBuilder();

        builder.setTitle(LangID.getStringByID("donate_donation", lang));
        builder.setColor(StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)]);
        builder.setDescription(LangID.getStringByID("donate_below", lang));

        builder.addField(EmojiStore.PAYPAL.getFormatted() + " " + LangID.getStringByID("donate_paypal", lang), "[" + LangID.getStringByID("donate_link", lang) + "](" + StaticStore.PAYPAL + ")", false);
        builder.addField(EmojiStore.CASHAPP.getFormatted() + " " + LangID.getStringByID("donate_cashapp", lang), "[" + LangID.getStringByID("donate_link", lang) + "](" + StaticStore.CASHAPP + ")", false);

        replyToMessageSafely(ch, "", getMessage(event), a -> a.setEmbeds(builder.build()));
    }
}
