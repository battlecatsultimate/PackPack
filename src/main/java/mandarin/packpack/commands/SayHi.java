package mandarin.packpack.commands;

import common.CommonStatic;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.holder.component.CultButtonHolder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import javax.annotation.Nonnull;

public class SayHi extends Command {
    public SayHi(CommonStatic.Lang.Locale lang) {
        super(lang, false);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();
        User u = loader.getUser();

        double chance = StaticStore.random.nextDouble();

        if(chance <= 0.01) {
            if (StaticStore.cultist.contains(u.getId())) {
                replyToMessageSafely(ch, LangID.getStringByID("hi.special.recognize", lang), loader.getMessage(), a -> a);
            } else {
                replyToMessageSafely(ch, LangID.getStringByID("hi.special.invitation", lang), loader.getMessage(), a -> a.setComponents(ActionRow.of(
                        Button.of(ButtonStyle.SUCCESS, "yes", LangID.getStringByID("ui.button.yes", lang)),
                        Button.of(ButtonStyle.DANGER, "no", LangID.getStringByID("ui.button.no", lang))
                )), msg -> StaticStore.putHolder(u.getId(), new CultButtonHolder(loader.getMessage(), u.getId(), ch.getId(), msg, lang)));
            }
        } else if(chance <= 0.05) {
            replyToMessageSafely(ch, LangID.getStringByID("hi.dog", lang), loader.getMessage(), a -> a);
        } else if(StaticStore.cultist.contains(u.getId()) && chance <= 0.1) {
            replyToMessageSafely(ch, LangID.getStringByID("hi.special.recognize", lang), loader.getMessage(), a -> a);
        } else {
            int index = StaticStore.random.nextInt(13);

            switch (index) {
                case 0 ->
                        replyToMessageSafely(ch, LangID.getStringByID("hi.0.initial", lang), loader.getMessage(), a -> a, msg ->
                            StaticStore.executorHandler.postDelayed(5000, () -> msg.editMessage(LangID.getStringByID("hi.0.edited", lang)).mentionRepliedUser(false).queue())
                        );
                case 2 ->
                        replyToMessageSafely(ch, LangID.getStringByID("hi.2.initial", lang), loader.getMessage(), a -> a, message ->
                            StaticStore.executorHandler.postDelayed(5000, () -> {
                                int luck = StaticStore.random.nextInt(3);

                                message.editMessage(
                                        LangID.getStringByID("hi.2.initial", lang) + "\n\n" +
                                                LangID.getStringByID("hi.2.luck." + luck, lang)
                                ).mentionRepliedUser(false).queue();
                            })
                        );
                case 8 ->
                        replyToMessageSafely(ch, LangID.getStringByID("hi.8.initial", lang), loader.getMessage(), a -> a, ms ->
                            StaticStore.executorHandler.postDelayed(5000, () -> ms.editMessage(
                                    LangID.getStringByID("hi.8.initial", lang) + "\n\n" +
                                            LangID.getStringByID("hi.8.additional", lang)
                            ).mentionRepliedUser(false).queue())
                        );
                default ->
                        replyToMessageSafely(ch, LangID.getStringByID("hi." + index, lang), loader.getMessage(), a -> a);
            }
        }
    }
}
