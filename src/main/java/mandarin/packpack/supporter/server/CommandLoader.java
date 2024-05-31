package mandarin.packpack.supporter.server;

import mandarin.packpack.supporter.StaticStore;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.requests.RestAction;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Consumer;

public class CommandLoader {
    private JDA client = null;
    private Guild guild = null;
    private MessageChannel channel = null;
    private Member member = null;
    private User user = null;
    private Message message = null;
    private String content = null;

    public void load(GenericMessageEvent event, Consumer<CommandLoader> onLoaded) {
        RestAction<Message> lazyMessageConsumer = null;

        client = event.getJDA();

        try {
            Method m = event.getClass().getMethod("getMessage");

            Object obj = m.invoke(event);

            if(obj instanceof Message)
                message = (Message) obj;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            try {
                MessageChannel ch = event.getChannel();

                Method m = event.getClass().getMethod("getMessageId");

                Object obj = m.invoke(event);

                if(obj instanceof String && ch instanceof GuildChannel) {
                    Guild g = ((GuildChannel) ch).getGuild();

                    if (g.getSelfMember().hasPermission(Permission.MESSAGE_HISTORY)) {
                        lazyMessageConsumer = ch.retrieveMessageById((String) obj);
                    }
                }
            } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException invocationTargetException) {
                StaticStore.logger.uploadErrorLog(e, "Failed to get Message from this class : "+event.getClass().getName());
            }
        }

        if (lazyMessageConsumer != null) {
            lazyMessageConsumer.queue(m -> {
                message = m;

                channel = message.getChannel();

                if (channel instanceof GuildChannel) {
                    guild = message.getGuild();
                    member = message.getMember();
                }

                user = message.getAuthor();
                content = message.getContentRaw();

                onLoaded.accept(this);
            });
        } else {
            if (message != null) {
                channel = message.getChannel();

                if (channel instanceof GuildChannel) {
                    guild = message.getGuild();
                    member = message.getMember();
                }

                user = message.getAuthor();
                content = message.getContentRaw();

                onLoaded.accept(this);
            }
        }
    }

    @NotNull
    public Guild getGuild() {
        if (guild == null)
            throw new NullPointerException("Guild is null while loader doesn't support it");

        return guild;
    }

    @NotNull
    public MessageChannel getChannel() {
        if (channel == null) {
            throw new NullPointerException("Channel is null while loader doesn't support it");
        }

        return channel;
    }

    @NotNull
    public Message getMessage() {
        if (message == null) {
            throw new NullPointerException("Message is null while loader doesn't support it");
        }

        return message;
    }

    @NotNull
    public Member getMember() {
        if (member == null) {
            throw new NullPointerException("Member is null while loader doesn't support it");
        }

        return member;
    }

    @NotNull
    public User getUser() {
        if (user == null) {
            throw new NullPointerException("User is null while loader doesn't support it");
        }

        return user;
    }

    @NotNull
    public String getContent() {
        if (content == null) {
            throw new NullPointerException("Content is null while loader doesn't support it");
        }

        return content;
    }

    @NotNull
    public JDA getClient() {
        if (client == null) {
            throw new NullPointerException("Client is null while loader doesn't support it");
        }

        return client;
    }

    public boolean hasGuild() {
        return guild != null;
    }
}
