package mandarin.packpack.commands;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.holder.CultButtonHolder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class SayHi extends Command {
    public SayHi(int lang) {
        super(lang);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);
        Member m = getMember(event);

        if(ch == null || m == null)
            return;

        double chance = StaticStore.random.nextDouble();

        if(chance <= 0.01) {
            if (StaticStore.cultist.contains(m.getId())) {
                createMessageWithNoPings(ch, LangID.getStringByID("hi_6_1", lang));
            } else {
                Message msg = ch.sendMessage(LangID.getStringByID("hi_6_0", lang))
                        .setAllowedMentions(new ArrayList<>())
                        .setActionRow(
                                Button.of(ButtonStyle.SUCCESS, "yes", LangID.getStringByID("button_yes", lang)),
                                Button.of(ButtonStyle.DANGER, "no", LangID.getStringByID("button_no", lang))
                        ).complete();

                StaticStore.putHolder(m.getId(), new CultButtonHolder(getMessage(event), msg, ch.getId(), m.getId(), lang));
            }
        } else {
            int index = StaticStore.random.nextInt(12);

            switch (index) {
                case 0:
                    Message msg = getMessageWithNoPings(ch, LangID.getStringByID("hi_" + index, lang));

                    if(msg != null) {
                        Timer changer = new Timer();

                        changer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                msg.editMessage(LangID.getStringByID("hi_0_0", lang)).queue();
                            }
                        }, 5000);
                    }

                    break;
                case 2:
                    Message message = getMessageWithNoPings(ch, LangID.getStringByID("hi_" + index, lang));

                    if(message != null) {
                        Timer changer = new Timer();

                        changer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                int luck = StaticStore.random.nextInt(3);

                                message.editMessage(
                                        LangID.getStringByID("hi_2", lang) + "\n\n" +
                                        LangID.getStringByID("hi_2_" + luck, lang)
                                ).queue();
                            }
                        }, 5000);
                    }

                    break;
                case 8:
                    Message ms = getMessageWithNoPings(ch, LangID.getStringByID("hi_" + index, lang));

                    if(ms != null) {
                        Timer changer = new Timer();

                        changer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                ms.editMessage(
                                        LangID.getStringByID("hi_8", lang) + "\n\n" +
                                                LangID.getStringByID("hi_8_0", lang)
                                ).queue();
                            }
                        }, 5000);
                    }

                    break;
                default:
                    createMessageWithNoPings(ch, LangID.getStringByID("hi_" + index, lang));
            }
        }
    }
}
