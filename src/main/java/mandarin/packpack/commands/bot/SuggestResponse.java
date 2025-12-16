package mandarin.packpack.commands.bot;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.sharding.ShardManager;

import javax.annotation.Nonnull;

public class SuggestResponse extends ConstraintCommand {

    public SuggestResponse(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        ShardManager client = ch.getJDA().getShardManager();

        if (client == null)
            return;

        String[] contents = loader.getContent().split(" ", 6);

        if(contents.length < 6) {
            replyToMessageSafely(ch, loader.getMessage(), "Invalid Format : `p!suggestresponse [Guild ID] [Channel ID] [Member ID] [Suggestion ID] Contents`");

            return;
        }

        Guild g = client.getGuildById(contents[1]);

        if(g == null)
            return;

        GuildChannel c = g.getGuildChannelById(contents[2]);

        if(c == null)
            return;

        if(c instanceof GuildMessageChannel) {
            User me = client.getUserById(StaticStore.MANDARIN_SMELL);

            if(me == null)
                return;

            me.openPrivateChannel().queue(pv -> {
                Message emb = pv.getHistory().getMessageById(contents[4]);

                if(emb == null || emb.getEmbeds().isEmpty())
                    return;

                MessageEmbed embed = emb.getEmbeds().getFirst();

                EmbedBuilder builder = new EmbedBuilder()
                        .setTitle(LangID.getStringByID("suggest.response.title", lang))
                        .setColor(StaticStore.rainbow[StaticStore.random.nextInt(StaticStore.rainbow.length)]);

                MessageEmbed.AuthorInfo info = embed.getAuthor();

                if(info == null || info.getName() == null)
                    return;

                builder.addField(info.getName(), contents[5], false);

                User user = client.getUserById(contents[3]);

                if(user != null) {
                    builder.setFooter(LangID.getStringByID("suggest.response.author", lang).replace("_UUU_", user.getEffectiveName()), user.getAvatarUrl());

                    user.openPrivateChannel()
                            .flatMap(pc -> pc.sendMessageEmbeds(builder.build()))
                            .queue();
                }

                ((GuildMessageChannel) c).sendMessageEmbeds(builder.build()).queue();
            });
        }
    }
}
