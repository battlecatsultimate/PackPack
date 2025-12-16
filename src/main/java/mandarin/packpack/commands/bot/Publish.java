package mandarin.packpack.commands.bot;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.sharding.ShardManager;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class Publish extends ConstraintCommand {
    public Publish(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        String[] contents = loader.getContent().split(" ");

        if(contents.length < 2) {
            replyToMessageSafely(ch, loader.getMessage(), "`p!pub [Importance]`\n-i : Important\n-n : Not important");

            return;
        }

        boolean important = contents[1].equals("-i");

        ShardManager client = ch.getJDA().getShardManager();

        if (client == null)
            return;

        if(!StaticStore.announcements.containsKey(CommonStatic.Lang.Locale.EN)) {
            replyToMessageSafely(ch, loader.getMessage(), "You have to at least make announcement for English!");

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
                CommonStatic.Lang.Locale selectedLocale = null;

                CommonStatic.Lang.Locale[] pref = CommonStatic.Lang.pref[language.ordinal()];

                for(CommonStatic.Lang.Locale p : pref) {
                    if(StaticStore.announcements.containsKey(p)) {
                        content = StaticStore.announcements.get(p);
                        selectedLocale = p;
                        break;
                    }
                }

                if(content != null && ((NewsChannel) c).canTalk()) {
                    List<ContainerChildComponent> components = new ArrayList<>();

                    components.add(TextDisplay.of("## " + LangID.getStringByID("bot.announcement", selectedLocale).formatted(loader.getClient().getSelfUser().getIdLong())));
                    components.add(Separator.create(true, Separator.Spacing.LARGE));
                    components.add(TextDisplay.of(content));

                    Container container = Container.of(components);

                    ((NewsChannel) c)
                            .sendMessageComponents(container)
                            .useComponentsV2()
                            .setAllowedMentions(new ArrayList<>())
                            .queue(m -> {
                                if(m != null && holder.publish)
                                    m.crosspost().queue();

                                if(!holder.announceMessage.isBlank()) {
                                    if(important) {
                                        ((NewsChannel) c).sendMessageComponents(TextDisplay.of(holder.announceMessage))
                                                .useComponentsV2()
                                                .queue();
                                    } else {
                                        ((NewsChannel) c).sendMessageComponents(TextDisplay.of(holder.announceMessage))
                                                .useComponentsV2()
                                                .setSuppressedNotifications(true)
                                                .queue();
                                    }
                                }
                            });
                }
            } else if(c instanceof GuildMessageChannel) {
                String content = null;
                CommonStatic.Lang.Locale selectedLocale = null;

                CommonStatic.Lang.Locale[] pref = CommonStatic.Lang.pref[language.ordinal()];

                for(CommonStatic.Lang.Locale p : pref) {
                    if(StaticStore.announcements.containsKey(p)) {
                        selectedLocale = p;
                        content = StaticStore.announcements.get(p);
                        break;
                    }
                }

                if(content != null) {
                    List<ContainerChildComponent> components = new ArrayList<>();

                    components.add(TextDisplay.of("## " + LangID.getStringByID("bot.announcement", selectedLocale).formatted(loader.getClient().getSelfUser().getIdLong())));
                    components.add(Separator.create(true, Separator.Spacing.LARGE));
                    components.add(TextDisplay.of(content));

                    Container container = Container.of(components);

                    ((GuildMessageChannel) c).sendMessageComponents(container)
                            .useComponentsV2()
                            .setAllowedMentions(new ArrayList<>())
                            .queue(unused -> {
                                if(!holder.announceMessage.isBlank()) {
                                    if(important) {
                                        ((GuildMessageChannel) c).sendMessageComponents(TextDisplay.of(holder.announceMessage))
                                                .useComponentsV2()
                                                .queue();
                                    } else {
                                        ((GuildMessageChannel) c).sendMessageComponents(TextDisplay.of(holder.announceMessage))
                                                .useComponentsV2()
                                                .setSuppressedNotifications(true)
                                                .queue();
                                    }
                                }
                            });
                }
            }
        }

        StaticStore.announcements.clear();

        replyToMessageSafely(ch, loader.getMessage(), "Successfully announced");
    }
}
