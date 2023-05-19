package mandarin.packpack.commands;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.holder.component.CultButtonHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import java.util.Timer;
import java.util.TimerTask;

public class SayHi extends Command {
    public SayHi(int lang) {
        super(lang, false);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);
        User u = getUser(event);

        if(ch == null || u == null)
            return;

        double chance = StaticStore.random.nextDouble();

        if(chance <= 0.01) {
            if (StaticStore.cultist.contains(u.getId())) {
                replyToMessageSafely(ch, LangID.getStringByID("hi_sp_1", lang), getMessage(event), a -> a);
            } else {
                Message msg = getRepliedMessageSafely(ch, LangID.getStringByID("hi_sp_0", lang), getMessage(event), a -> a.setActionRow(
                        Button.of(ButtonStyle.SUCCESS, "yes", LangID.getStringByID("button_yes", lang)),
                        Button.of(ButtonStyle.DANGER, "no", LangID.getStringByID("button_no", lang))
                ));

                StaticStore.putHolder(u.getId(), new CultButtonHolder(getMessage(event), msg, ch.getId(), u.getId(), lang));
            }
        } else if(chance <= 0.05) {
            replyToMessageSafely(ch, LangID.getStringByID("hi_d", lang), getMessage(event), a -> a);
        } else if(StaticStore.cultist.contains(u.getId()) && chance <= 0.1) {
            replyToMessageSafely(ch, LangID.getStringByID("hi_sp_1", lang), getMessage(event), a -> a);
        } else {
            int index = StaticStore.random.nextInt(13);

            switch (index) {
                case 0 -> {
                    Message msg = getRepliedMessageSafely(ch, LangID.getStringByID("hi_" + index, lang), getMessage(event), a -> a);

                    if (msg != null) {
                        Timer changer = new Timer();

                        changer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                msg.editMessage(LangID.getStringByID("hi_0_0", lang)).mentionRepliedUser(false).queue();
                            }
                        }, 5000);
                    }
                }
                case 2 -> {
                    Message message = getRepliedMessageSafely(ch, LangID.getStringByID("hi_" + index, lang), getMessage(event), a -> a);

                    if (message != null) {
                        Timer changer = new Timer();

                        changer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                int luck = StaticStore.random.nextInt(3);

                                message.editMessage(
                                        LangID.getStringByID("hi_2", lang) + "\n\n" +
                                                LangID.getStringByID("hi_2_" + luck, lang)
                                ).mentionRepliedUser(false).queue();
                            }
                        }, 5000);
                    }
                }
                case 8 -> {
                    Message ms = getRepliedMessageSafely(ch, LangID.getStringByID("hi_" + index, lang), getMessage(event), a -> a);

                    if (ms != null) {
                        Timer changer = new Timer();

                        changer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                ms.editMessage(
                                        LangID.getStringByID("hi_8", lang) + "\n\n" +
                                                LangID.getStringByID("hi_8_0", lang)
                                ).mentionRepliedUser(false).queue();
                            }
                        }, 5000);
                    }
                }
                default ->
                        replyToMessageSafely(ch, LangID.getStringByID("hi_" + index, lang), getMessage(event), a -> a);
            }
        }
    }
}
