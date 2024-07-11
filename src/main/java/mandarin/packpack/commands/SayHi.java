package mandarin.packpack.commands;

import common.CommonStatic;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.holder.component.CultButtonHolder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

public class SayHi extends Command {
    public SayHi(CommonStatic.Lang.Locale lang) {
        super(lang, false);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();
        User u = loader.getUser();

        double chance = StaticStore.random.nextDouble();

        if(chance <= 0.01) {
            if (StaticStore.cultist.contains(u.getId())) {
                replyToMessageSafely(ch, LangID.getStringByID("hi_sp_1", lang), loader.getMessage(), a -> a);
            } else {
                replyToMessageSafely(ch, LangID.getStringByID("hi_sp_0", lang), loader.getMessage(), a -> a.setActionRow(
                        Button.of(ButtonStyle.SUCCESS, "yes", LangID.getStringByID("button_yes", lang)),
                        Button.of(ButtonStyle.DANGER, "no", LangID.getStringByID("button_no", lang))
                ), msg -> StaticStore.putHolder(u.getId(), new CultButtonHolder(loader.getMessage(), msg, ch.getId(), u.getId(), lang)));
            }
        } else if(chance <= 0.05) {
            replyToMessageSafely(ch, LangID.getStringByID("hi_d", lang), loader.getMessage(), a -> a);
        } else if(StaticStore.cultist.contains(u.getId()) && chance <= 0.1) {
            replyToMessageSafely(ch, LangID.getStringByID("hi_sp_1", lang), loader.getMessage(), a -> a);
        } else {
            int index = StaticStore.random.nextInt(13);

            switch (index) {
                case 0 ->
                        replyToMessageSafely(ch, LangID.getStringByID("hi_" + index, lang), loader.getMessage(), a -> a, msg ->
                            StaticStore.executorHandler.postDelayed(5000, () -> msg.editMessage(LangID.getStringByID("hi_0_0", lang)).mentionRepliedUser(false).queue())
                        );
                case 2 ->
                        replyToMessageSafely(ch, LangID.getStringByID("hi_" + index, lang), loader.getMessage(), a -> a, message ->
                            StaticStore.executorHandler.postDelayed(5000, () -> {
                                int luck = StaticStore.random.nextInt(3);

                                message.editMessage(
                                        LangID.getStringByID("hi_2", lang) + "\n\n" +
                                                LangID.getStringByID("hi_2_" + luck, lang)
                                ).mentionRepliedUser(false).queue();
                            })
                        );
                case 8 ->
                        replyToMessageSafely(ch, LangID.getStringByID("hi_" + index, lang), loader.getMessage(), a -> a, ms ->
                            StaticStore.executorHandler.postDelayed(5000, () -> ms.editMessage(
                                    LangID.getStringByID("hi_8", lang) + "\n\n" +
                                            LangID.getStringByID("hi_8_0", lang)
                            ).mentionRepliedUser(false).queue())
                        );
                default ->
                        replyToMessageSafely(ch, LangID.getStringByID("hi_" + index, lang), loader.getMessage(), a -> a);
            }
        }
    }
}
