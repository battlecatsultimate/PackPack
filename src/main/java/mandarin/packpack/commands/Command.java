package mandarin.packpack.commands;

import mandarin.packpack.supporter.Pauser;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.SpamPrevent;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.interactions.components.ActionComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("unused")
public abstract class Command {
    public static MessageAction registerConfirmButtons(MessageAction m, int lang) {
        List<ActionComponent> components = new ArrayList<>();

        components.add(Button.success("confirm", LangID.getStringByID("button_confirm", lang)));
        components.add(Button.danger("cancel", LangID.getStringByID("button_cancel", lang)));

        return m.setActionRow(components);
    }

    public static void sendMessageWithFile(MessageChannel ch, String content, File f) {
        ch.sendMessage(content)
                .addFile(f)
                .allowedMentions(new ArrayList<>())
                .queue(m -> {
                    if(f.exists() && !f.delete()) {
                        StaticStore.logger.uploadLog("Failed to delete file : "+f.getAbsolutePath());
                    }
                }, e -> {
                    StaticStore.logger.uploadErrorLog(e, "E/Command::sendMessageWithFile - Failed to upload message with file");

                    if(f.exists() && !f.delete()) {
                        StaticStore.logger.uploadLog("Failed to delete file : "+f.getAbsolutePath());
                    }
                });
    }

    public static void sendMessageWithFile(MessageChannel ch, String content, File f, String fileName) {
        ch.sendMessage(content)
                .addFile(f, fileName)
                .allowedMentions(new ArrayList<>())
                .queue(m -> {
                    if(f.exists() && !f.delete()) {
                        StaticStore.logger.uploadLog("Failed to delete file : "+f.getAbsolutePath());
                    }
                }, e -> {
                    StaticStore.logger.uploadErrorLog(e, "E/Command::sendMessageWithFile - Failed to upload message with file");

                    if(f.exists() && !f.delete()) {
                        StaticStore.logger.uploadLog("Failed to delete file : "+f.getAbsolutePath());
                    }
                });
    }

    public final int DEFAULT_ERROR = -1;
    public Pauser pause = new Pauser();
    public final int lang;

    public Command(int lang) {
        this.lang = lang;
    }

    public void execute(GenericMessageEvent event) {
        MessageChannel ch = getChannel(event);
        Guild g = getGuild(event);

        if(ch == null || g == null)
            return;

        AtomicReference<Boolean> prevented = new AtomicReference<>(false);

        Member m = getMember(event);

        if(m != null) {
            SpamPrevent spam;

            if(StaticStore.spamData.containsKey(m.getId())) {
                spam = StaticStore.spamData.get(m.getId());

                prevented.set(spam.isPrevented(ch, lang, m.getId()));
            } else {
                spam = new SpamPrevent();

                StaticStore.spamData.put(m.getId(), spam);
            }
        }

        if (prevented.get())
            return;

        StaticStore.executed++;

        boolean canTry = false;

        if(ch instanceof GuildMessageChannel) {
            GuildMessageChannel tc = ((GuildMessageChannel) ch);

            canTry = tc.canTalk();
        }

        if(!canTry) {
            if(m != null) {
                String serverName = g.getName();
                String channelName = ch.getName();

                String content;

                content = LangID.getStringByID("no_permch", lang).replace("_SSS_", serverName).replace("_CCC_", channelName);

                m.getUser().openPrivateChannel()
                        .flatMap(pc -> pc.sendMessage(content))
                        .queue();
            }

            return;
        }

        try {
            new Thread(() -> {
                try {
                    doSomething(event);
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e, "Failed to perform command");
                    e.printStackTrace();
                    onFail(event, DEFAULT_ERROR);
                }
            }).start();
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "Failed to perform command");
            e.printStackTrace();
            onFail(event, DEFAULT_ERROR);
        }
    }

    public abstract void doSomething(GenericMessageEvent event) throws Exception;

    public void onFail(GenericMessageEvent event, int error) {
        StaticStore.executed--;

        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        ch.sendMessage(StaticStore.ERROR_MSG).queue();
    }

    public void onSuccess(GenericMessageEvent event) {}

    public void onCancel(GenericMessageEvent event) {}

    public MessageChannel getChannel(GenericMessageEvent event) {
        return event.getChannel();
    }

    public Message getMessage(GenericMessageEvent event) {
        try {
            Method m = event.getClass().getMethod("getMessage");

            Object obj = m.invoke(event);

            if(obj instanceof Message)
                return (Message) obj;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            try {
                MessageChannel ch = event.getChannel();

                Method m = event.getClass().getMethod("getMessageId");

                Object obj = m.invoke(event);

                if(obj instanceof String) {
                    return ch.retrieveMessageById((String) obj).complete();
                }
            } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException invocationTargetException) {
                StaticStore.logger.uploadErrorLog(e, "Failed to get Message from this class : "+event.getClass().getName());
            }
        }

        return null;
    }

    public String getContent(GenericMessageEvent event) {
        Message msg = getMessage(event);

        return msg == null ? null : msg.getContentRaw();
    }

    public Member getMember(GenericMessageEvent event) {
        try {
            Method m = event.getClass().getMethod("getMember");

            Object obj = m.invoke(event);

            if(obj instanceof Member)
                return (Member) obj;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            StaticStore.logger.uploadErrorLog(e, "Failed to get Member from this class : "+event.getClass().getName());
        }

        return null;
    }

    public Guild getGuild(GenericMessageEvent event) {
        return event.getGuild();
    }

    public void createMessageWithNoPings(MessageChannel ch, String content) {
        ch.sendMessage(content)
                .allowedMentions(new ArrayList<>())
                .queue();
    }

    public Message getMessageWithNoPings(MessageChannel ch, String content) {
        return ch.sendMessage(content)
                .allowedMentions(new ArrayList<>())
                .complete();
    }
}
