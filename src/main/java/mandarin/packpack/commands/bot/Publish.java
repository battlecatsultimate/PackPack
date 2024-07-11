package mandarin.packpack.commands.bot;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class Publish extends ConstraintCommand {
    public Publish(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        String[] contents = loader.getContent().split(" ");

        if(contents.length < 2) {
            replyToMessageSafely(ch, "`p!pub [Importance]`\n-i : Important\n-n : Not important", loader.getMessage(), a -> a);

            return;
        }

        boolean important = contents[1].equals("-i");

        ShardManager client = ch.getJDA().getShardManager();

        if (client == null)
            return;

        if(!StaticStore.announcements.containsKey(CommonStatic.Lang.Locale.EN)) {
            createMessageWithNoPings(ch, "You have to at least make announcement for English!");
            return;
        }

        for(String id : StaticStore.idHolder.keySet()) {
            if(id == null || id.isBlank())
                continue;

            IDHolder holder = StaticStore.idHolder.get(id);

            if(holder.announceChannel == null)
                continue;

            Guild g = client.getGuildById(id);

            if(g == null)
                continue;

            CommonStatic.Lang.Locale language = holder.config.lang;

            if (language == null)
                continue;

            GuildChannel c = g.getGuildChannelById(holder.announceChannel);

            if(c instanceof NewsChannel) {
                String content = null;

                CommonStatic.Lang.Locale[] pref = CommonStatic.Lang.pref[language.ordinal()];

                for(CommonStatic.Lang.Locale p : pref) {
                    if(StaticStore.announcements.containsKey(p)) {
                        content = StaticStore.announcements.get(p);
                        break;
                    }
                }

                if(content != null && ((NewsChannel) c).canTalk()) {
                    ((NewsChannel) c)
                            .sendMessage(content)
                            .setAllowedMentions(new ArrayList<>())
                            .queue(m -> {
                                if(m != null && holder.publish)
                                    m.crosspost().queue();

                                if(!holder.announceMessage.isBlank()) {
                                    if(important) {
                                        ((NewsChannel) c).sendMessage(holder.announceMessage).queue();
                                    } else {
                                        ((NewsChannel) c).sendMessage(holder.announceMessage)
                                                .setSuppressedNotifications(true)
                                                .queue();
                                    }
                                }
                            });
                }
            } else if(c instanceof GuildMessageChannel) {
                String content = null;

                CommonStatic.Lang.Locale[] pref = CommonStatic.Lang.pref[language.ordinal()];

                for(CommonStatic.Lang.Locale p : pref) {
                    if(StaticStore.announcements.containsKey(p)) {
                        content = StaticStore.announcements.get(p);
                        break;
                    }
                }

                if(content != null) {
                    if(((GuildMessageChannel) c).canTalk()) {
                        ((GuildMessageChannel) c).sendMessage(content)
                                .setAllowedMentions(new ArrayList<>())
                                .queue();
                    }

                    if(!holder.announceMessage.isBlank()) {
                        if(important) {
                            ((GuildMessageChannel) c).sendMessage(holder.announceMessage).queue();
                        } else {
                            ((GuildMessageChannel) c).sendMessage(holder.announceMessage)
                                    .setSuppressedNotifications(true)
                                    .queue();
                        }
                    }
                }
            }
        }

        StaticStore.announcements.clear();

        createMessageWithNoPings(ch, "Successfully announced");
    }
}
